#!/usr/bin/env python3
"""Outbound-only release updater. Lab is the default; production needs explicit configuration."""
import argparse
import fcntl
import json
import os
from pathlib import Path
import shutil
import signal
import socket
import sqlite3
import stat
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import deploy_large_remote as installer
import lab_instance
import release_history as history
from github_release import GitHub, find_asset, TAG


def save_json(path, data):
    fd, name = tempfile.mkstemp(prefix='.' + path.name, dir=path.parent)
    try:
        with os.fdopen(fd, 'w') as stream:
            json.dump(data, stream, ensure_ascii=False, indent=2)
            stream.write('\n')
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(name, path)
    finally:
        Path(name).unlink(missing_ok=True)


def read_json(path, default=None):
    return json.loads(path.read_text()) if path.exists() else default


def load_config(path):
    config = read_json(path)
    if not isinstance(config, dict) or config.get('target') not in ('lab', 'production'):
        raise RuntimeError('Set target to lab or production in updater configuration')
    root = Path(config['root'])
    token = Path(config['token'])
    if not root.is_absolute() or root.is_symlink() or not root.name.startswith('sauna-updater-'):
        raise RuntimeError('Updater needs a separate absolute sauna-updater-* directory')
    if not token.is_absolute() or token.is_symlink() or not token.is_file():
        raise RuntimeError('Missing regular GitHub token file')
    mode = token.stat()
    if mode.st_uid != os.getuid() or stat.S_IMODE(mode.st_mode) != 0o600:
        raise RuntimeError('GitHub token must be owned by this user with mode 0600')
    if config.get('preview') and (config['target'] != 'lab' or
            (config.get('preview_address', '127.0.0.1'), config.get('preview_port')) not in
            (('127.0.0.1', 18082), ('0.0.0.0', 8087))):
        raise RuntimeError('Preview supports loopback 18082 or public lab 8087 only')
    if config['target'] == 'production':
        if config.get('enabled') is not True or config.get('service') != 'webpanel-b':
            raise RuntimeError('Production deployment is disabled')
        for key in ('jar', 'database'):
            if not Path(config[key]).is_absolute() or Path(config[key]).is_symlink():
                raise RuntimeError('Production paths must be explicit regular absolute paths')
        if not 1024 <= int(config['port']) <= 65535:
            raise RuntimeError('Invalid web port')
    return config


def smoke(jar, root):
    """Run a disposable app with its own database, loopback web and no PLC connection."""
    index = installer.validate_archive(jar, installer.digest(jar))
    with tempfile.TemporaryDirectory(prefix='smoke-', dir=root) as folder:
        folder = Path(folder)
        with socket.socket() as sock:
            sock.bind(('127.0.0.1', 0))
            port = sock.getsockname()[1]
        # Data endpoint deliberately points to loopback discard port, never to a real PLC.
        command = ['java', '-Xms64m', '-Xmx256m', '-jar', str(jar.resolve()),
                   '--spring.profiles.active=prod', '--server.address=127.0.0.1',
                   '--server.port=' + str(port), '--modbus.api.ip=127.0.0.1',
                   '--modbus.api.port=1', '--modbus.api.log-name=Sauna21000PLC',
                   '--spring.datasource.url=jdbc:sqlite:' + str(folder/'smoke.db') + '?busy_timeout=10000',
                   '--spring.jpa.show-sql=false']
        with (root/'smoke.log').open('w') as log:
            process = subprocess.Popen(command, cwd=folder, stdout=log, stderr=subprocess.STDOUT,
                                       stdin=subprocess.DEVNULL, start_new_session=True)
            try:
                opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
                deadline = time.monotonic() + 120
                consecutive = 0
                while time.monotonic() < deadline:
                    if process.poll() is not None:
                        raise RuntimeError('Sandbox application exited; inspect smoke.log')
                    try:
                        with opener.open('http://127.0.0.1:%s/' % port, timeout=3) as response:
                            matches = response.status == 200 and response.read(len(index) + 1) == index
                        # SQLite and startup runners must also be ready, not just the HTTP listener.
                        db = folder/'smoke.db'
                        with sqlite3.connect('file:' + str(db) + '?mode=ro', uri=True) as connection:
                            users = connection.execute('select count(*) from app_users').fetchone()[0]
                        started = 'Started ModbusApiApplication' in (root/'smoke.log').read_text(errors='replace')
                        consecutive = consecutive + 1 if matches and users > 0 and started else 0
                        if consecutive >= 3:
                            return
                    except (OSError, sqlite3.Error):
                        consecutive = 0
                    time.sleep(2)
                raise RuntimeError('Sandbox application did not become ready in 120 seconds')
            finally:
                if process.poll() is None:
                    os.killpg(process.pid, signal.SIGTERM)
                    try:
                        process.wait(timeout=15)
                    except subprocess.TimeoutExpired:
                        os.killpg(process.pid, signal.SIGKILL)
                        process.wait(timeout=5)


def production(config, jar, checksum):
    actual, state = installer.inspect()
    if actual != Path(config['jar']).resolve() or state != 'active':
        raise RuntimeError('Service JAR changed or service was stopped; refusing automatic deployment')
    # Stop/start commands are fixed inside installer, never supplied by a release.
    installer.install(jar, checksum, int(config['port']), database=Path(config['database']),
                      restore_database=config.get('_restore_database'),
                      before_replace=lambda backup, db: history.remember(config['root'], backup, installer, database=db, current=True))


def once(config, github, retry=False):
    root = Path(config['root'])
    root.mkdir(parents=True, exist_ok=True, mode=0o700)
    os.chmod(root, 0o700)
    with (root/'updater.lock').open('a') as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            return {'status': 'busy'}
        previous = read_json(root/'attempt.json', {})
        if previous.get('status') == 'installing':
            raise RuntimeError('Interrupted installation; inspect the service and backups before retrying')
        save_json(root/'heartbeat.json', {'checked_at': int(time.time())})
        pending = read_json(root/'request.json', {})
        if pending.get('restore'):
            return restore(config, pending, previous)
        requested_tag = pending.get('tag', '')
        match = TAG.fullmatch(requested_tag)
        if match and match[1] == config['target']:
            release = github.json('/releases/tags/' + requested_tag)
        else:
            release = github.latest(config['target'])
        if release is None:
            result = {'status': 'waiting', 'target': config['target'], 'message': 'No release published yet'}
            save_json(root/'status.json', result)
            return result
        tag = release['tag_name']
        if previous.get('tag') == tag and previous.get('status') in ('success', 'failed') and not retry:
            if config.get('preview') and previous['status'] == 'success':
                lab_instance.ensure(config, installer)
            previous['checked_at'] = int(time.time())
            save_json(root/'status.json', previous)
            return previous  # Never restart a service every minute, including after failed installs.
        manifest = github.manifest(release, config['target'])
        if manifest is None:
            return {'status': 'waiting-for-ci', 'tag': tag}
        sequence = [manifest['run_id'], manifest['run_attempt']]
        hold = read_json(root/'hold.json', {})
        if not requested_tag and sequence <= hold.get('sequence', []):
            previous['checked_at'] = int(time.time())
            save_json(root/'status.json', previous)
            return previous
        if sequence < previous.get('sequence', [0, 0]):
            raise RuntimeError('Older release refused; publish a new run to roll back intentionally')
        cached = root/'candidate.jar'
        cached_manifest = read_json(root/'candidate.json', {})
        if cached_manifest != manifest or not cached.is_file() or installer.digest(cached) != manifest['sha256']:
            with tempfile.TemporaryDirectory(prefix='download-', dir=root) as folder:
                jar = Path(folder)/'sauna_large.jar'
                github.download(find_asset(release, 'sauna_large.jar'), jar, manifest['sha256'], manifest['size'])
                installer.validate_archive(jar, manifest['sha256'])
                save_json(root/'status.json', dict(status='checking', tag=tag, target=config['target']))
                smoke(jar, root)
                os.replace(jar, cached)
                save_json(root/'candidate.json', manifest)
        result = dict(tag=tag, sequence=sequence, commit=manifest['commit'],
                      sha256=manifest['sha256'], target=config['target'], status='available',
                      checked_at=int(time.time()))
        preview = config.get('preview', False)
        first_preview = preview and not (lab_instance.instance(config)/'sauna_large.jar').exists()
        if config['target'] == 'production' or preview:
            request = read_json(root/'request.json', {})
            if request.get('tag') != tag and not first_preview and config.get('automatic') is not True:
                save_json(root/'status.json', result)
                return result  # An administrator must press Update inside the web panel.
            result['status'] = 'installing'
            save_json(root/'attempt.json', result)
            save_json(root/'status.json', result)
            try:
                if preview:
                    lab_instance.install(config, cached, installer)
                else:
                    production(config, cached, manifest['sha256'])
                    history.remember(root, cached, installer, metadata={'version':tag, 'commit':manifest['commit'],
                                     'description':manifest.get('description') or release.get('name') or tag}, current=True)
                    history.prune(root)
            except Exception as error:
                result.update(status='failed', error=public_error(error), checked_at=int(time.time()))
                save_json(root/'attempt.json', result)
                save_json(root/'status.json', result)
                (root/'request.json').unlink(missing_ok=True)
                raise
            (root/'request.json').unlink(missing_ok=True)
        result.update(status='success', checked_at=int(time.time()))
        save_json(root/'attempt.json', result)
        save_json(root/'status.json', result)
        return result


def restore(config, request, previous):
    root = Path(config['root'])
    if config['target'] != 'production':
        raise RuntimeError('Saved-version restore is supported for production only')
    version = request.get('tag', '')
    sequence = max(previous.get('sequence',[0,0]), read_json(root/'status.json',{}).get('sequence',[0,0]))
    result = dict(status='installing', tag=version, target=config['target'],
                  sequence=sequence, checked_at=int(time.time()), restore=True)
    try:
        entry, jar, database = history.restore_files(root, version, installer)
        result['commit'] = entry.get('commit','')
    except Exception as error:
        result.update(status='failed', error=public_error(error))
        save_json(root/'attempt.json', result)
        save_json(root/'status.json', result)
        save_json(root/'hold.json', {'sequence': sequence})
        (root/'request.json').unlink(missing_ok=True)
        raise
    save_json(root/'attempt.json', result)
    save_json(root/'status.json', result)
    try:
        production(dict(config, _restore_database=str(database)), jar, entry['sha256'])
        data = history.read(root)
        data['current'] = version
        history.save(root, data)
        # Do not immediately re-install the version the administrator just rolled back.
        save_json(root/'hold.json', {'sequence':result['sequence']})
        result['status'] = 'success'
    except Exception as error:
        result.update(status='failed', error=public_error(error))
        save_json(root/'hold.json', {'sequence': sequence})
        raise
    finally:
        result['checked_at'] = int(time.time())
        save_json(root/'attempt.json', result)
        save_json(root/'status.json', result)
        (root/'request.json').unlink(missing_ok=True)
    return result


def public_error(error):
    # Never serialize HTTP request headers, redirect URLs, subprocess stderr or token values.
    if isinstance(error, urllib.error.HTTPError):
        return 'GitHub HTTP %s' % error.code
    return str(error) if isinstance(error, RuntimeError) else type(error).__name__


def main():
    os.umask(0o077)
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', type=Path, required=True)
    parser.add_argument('--status', action='store_true')
    parser.add_argument('--retry-failed', action='store_true')
    args = parser.parse_args()
    try:
        config = load_config(args.config)
        root = Path(config['root'])
        if args.status:
            result = read_json(root/'status.json', {'status': 'not-started'})
        else:
            result = once(config, GitHub(Path(config['token']).read_text().strip()), args.retry_failed)
        print(json.dumps(result, ensure_ascii=False))
        return 1 if result.get('status') == 'failed' else 0
    except Exception as error:
        print(json.dumps({'status': 'error', 'error': public_error(error)}), file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
