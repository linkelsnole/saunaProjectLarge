"""Persistent isolated laboratory instance. Never invokes service or sudo."""
import json
import os
from pathlib import Path
import shutil
import signal
import socket
import sqlite3
import subprocess
import time
import urllib.request


def instance(config):
    root = Path(config['root']).resolve()
    if config['target'] != 'lab' or root != Path('/opt/webpanel/sauna-updater-lab'):
        raise RuntimeError('Preview is restricted to the dedicated lab directory')
    return root/'instance'


def alive(folder):
    file = folder/'pid'
    if not file.exists():
        return False
    pid = int(file.read_text())
    proc = Path('/proc')/str(pid)
    try:
        if any(line.startswith('State:') and 'Z' in line for line in (proc/'status').read_text().splitlines()):
            return False
        args = (proc/'cmdline').read_bytes().split(b'\0')
    except FileNotFoundError:
        return False  # Process can exit between polling its status and reading cmdline.
    if str(folder/'sauna_large.jar').encode() not in args:
        raise RuntimeError('Lab PID belongs to another process; refusing to signal it')
    return True


def stop(folder):
    if not alive(folder):
        return
    pid = int((folder/'pid').read_text())
    os.kill(pid, signal.SIGTERM)
    for _ in range(30):
        if not alive(folder):
            return
        time.sleep(1)
    raise RuntimeError('Lab process did not stop; live services were not touched')


def ready(folder, port, expected_index, timeout=120):
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    deadline = time.monotonic() + timeout
    consecutive = 0
    while time.monotonic() < deadline:
        if not alive(folder):
            return False
        try:
            with opener.open('http://127.0.0.1:%s/' % port, timeout=3) as response:
                matches = response.status == 200 and response.read(len(expected_index)+1) == expected_index
            with sqlite3.connect('file:' + str(folder/'users.db') + '?mode=ro', uri=True) as db:
                count = db.execute('select count(*) from app_users').fetchone()[0]
            consecutive = consecutive + 1 if matches and count > 0 else 0
            if consecutive >= 3:
                return True
        except (OSError, sqlite3.Error):
            consecutive = 0
        time.sleep(2)
    return False


def start(folder, root, port, address="127.0.0.1"):
    if address != "127.0.0.1" and (address, port) != ("0.0.0.0", 8087):
        raise RuntimeError("Public lab is restricted to port 8087")
    with socket.socket() as sock:
        # Closed HTTP connections may remain in TIME_WAIT after Tomcat exits.
        # Reuse those addresses while still refusing any active listener.
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind((address, port))  # Fail without killing anything if port is already occupied.
    command = ['java', '-Xms64m', '-Xmx256m', '-jar', str(folder/'sauna_large.jar'),
               '--spring.profiles.active=prod', '--server.address=' + address,
               '--server.port=' + str(port), '--modbus.api.ip=127.0.0.1', '--modbus.api.port=1',
               '--modbus.api.log-name=', '--spring.jpa.show-sql=false',
               '--jwt.refresh-cookie-name=sauna_lab_refresh_token',
               '--spring.datasource.url=jdbc:sqlite:' + str(folder/'users.db') + '?busy_timeout=10000',
               '--panel.updates.directory=' + str(root), '--panel.updates.channel=lab']
    with (folder/'app.log').open('w') as log:
        child = subprocess.Popen(command, cwd=folder, stdin=subprocess.DEVNULL, stdout=log,
                                 stderr=subprocess.STDOUT, start_new_session=True)
    (folder/'pid').write_text(str(child.pid))


def ensure(config, installer):
    folder = instance(config)
    if not (folder/'sauna_large.jar').exists() or alive(folder):
        return
    start(folder, folder.parent, config['preview_port'], config.get('preview_address', '127.0.0.1'))
    index = installer.validate_archive(folder/'sauna_large.jar', installer.digest(folder/'sauna_large.jar'))
    if not ready(folder, config['preview_port'], index):
        raise RuntimeError('Lab preview failed to recover; inspect instance/app.log')


def install(config, candidate, installer):
    folder = instance(config)
    folder.mkdir(exist_ok=True, mode=0o700)
    root = folder.parent
    port = config['preview_port']
    jar, old = folder/'sauna_large.jar', folder/'previous.jar'
    db, old_db = folder/'users.db', folder/'previous.db'
    new = folder/'next.jar'
    index = installer.validate_archive(candidate, installer.digest(candidate))
    shutil.copy2(candidate, new)
    had_old = jar.exists()
    if had_old:
        shutil.copy2(jar, old)
    had_db = db.exists()
    if had_db:
        old_db.unlink(missing_ok=True)
        with sqlite3.connect('file:' + str(db) + '?mode=ro', uri=True) as source:
            with sqlite3.connect(old_db) as destination:
                source.backup(destination)
    stop(folder)
    try:
        os.replace(new, jar)
        (root/'ui-enabled').touch(mode=0o600)
        start(folder, root, port, config.get('preview_address', '127.0.0.1'))
        if not ready(folder, port, index):
            raise RuntimeError('Lab preview did not become ready')
    except BaseException:
        stop(folder)
        if had_old:
            shutil.copy2(old, jar)
            if had_db:
                for suffix in ('', '-wal', '-shm'):
                    path = Path(str(db) + suffix)
                    if path.exists():
                        os.replace(path, folder/('failed.db' + suffix))
                shutil.copy2(old_db, db)
            start(folder, root, port, config.get('preview_address', '127.0.0.1'))
            old_index = installer.validate_archive(old, installer.digest(old))
            if not ready(folder, port, old_index):
                raise RuntimeError('Lab rollback needs inspection; live services were not touched')
        raise
