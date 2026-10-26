#!/usr/bin/env python3
"""Remote helper for webpanel-b only. Invoked by deploy-small.py via SSH."""
import fcntl
import hashlib
import json
import os
from pathlib import Path
import re
import shlex
import sqlite3
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.request
import zipfile

SERVICE = 'webpanel-b.service'
USE_SERVICE_COMMAND = False


def run(*args):
    return subprocess.check_output(args, text=True, stderr=subprocess.PIPE, timeout=10).strip()


def prop(name):
    return run('systemctl', 'show', SERVICE, '--property=' + name, '--value')


def resolve_jar(exec_start, working_directory):
    # systemctl show wraps the real argv in a structured textual record. Never eval it.
    matches = re.findall(r'argv\[\]=(.*?)\s+;', exec_start)
    if len(matches) != 1:
        raise RuntimeError('Не удалось однозначно прочитать команду запуска webpanel-b')
    args = shlex.split(matches[0])
    if args.count('-jar') != 1 or args.index('-jar') + 1 >= len(args):
        raise RuntimeError('webpanel-b должен запускать один JAR через java -jar')
    jar = Path(args[args.index('-jar') + 1])
    if not jar.is_absolute():
        if not working_directory.startswith('/'):
            raise RuntimeError('Для относительного JAR не задан WorkingDirectory')
        jar = Path(working_directory) / jar
    if jar.suffix != '.jar' or jar.is_symlink():
        raise RuntimeError('Ожидался обычный JAR, не символическая ссылка')
    return jar.resolve()


def inspect():
    if prop('LoadState') != 'loaded':
        raise RuntimeError('Сервис webpanel-b не найден')
    jar = resolve_jar(prop('ExecStart'), prop('WorkingDirectory'))
    if not jar.is_file() or not os.access(str(jar.parent), os.W_OK):
        raise RuntimeError('Нет существующего JAR или права записи в его каталог')
    if jar.stat().st_uid != os.getuid():
        raise RuntimeError('SSH-пользователь должен быть владельцем JAR для сохранения прав')
    state = prop('ActiveState')
    if state not in ('active', 'inactive', 'failed'):
        raise RuntimeError('Сервис уже запускается/останавливается. Повторите позднее')
    return jar, state


def digest(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for block in iter(lambda: f.read(1024 * 1024), b''):
            h.update(block)
    return h.hexdigest()


def validate_archive(path, expected):
    if digest(path) != expected:
        raise RuntimeError('SHA-256 загруженного JAR не совпадает')
    with zipfile.ZipFile(path) as z:
        if z.testzip():
            raise RuntimeError('Повреждён JAR')
        return z.read('BOOT-INF/classes/static/index.html')


def service(action):
    if action not in ('stop', 'start'):
        raise RuntimeError('Недопустимое действие службы')
    executable = shutil.which('service')
    if not executable:
        executable = next((p for p in ('/usr/sbin/service', '/sbin/service') if os.path.isfile(p)), None)
    if not executable:
        raise RuntimeError('Команда service на сервере не найдена')
    command = [executable, 'webpanel-b', action]
    if not USE_SERVICE_COMMAND and os.geteuid() != 0:
        command = ['sudo', '-n'] + command
    subprocess.run(command, check=True, timeout=75)


def ready(port, expected_index, timeout=60):
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    deadline = time.monotonic() + timeout
    consecutive = 0
    while time.monotonic() < deadline:
        try:
            active = prop('ActiveState') == 'active' and int(prop('MainPID')) > 0
            with opener.open('http://127.0.0.1:%d/' % port, timeout=3) as response:
                content = response.read(len(expected_index) + 1)
                matches = response.status == 200 and content == expected_index
            consecutive = consecutive + 1 if active and matches else 0
            if consecutive >= 3:
                return True
        except (OSError, ValueError, subprocess.SubprocessError):
            consecutive = 0
        time.sleep(2)
    return False


def install(stage, expected, port, database=None, restore_database=None, before_replace=None):
    stage = Path(stage)
    if stage.is_symlink() or not stage.is_file():
        raise RuntimeError('Файл загрузки отсутствует или является ссылкой')
    index = validate_archive(stage, expected)
    if database is not None:
        database = Path(database)
        if database.is_symlink() or not database.is_file():
            raise RuntimeError('Существующая база SQLite не найдена или является ссылкой')
    if restore_database is not None:
        restore_database = Path(restore_database)
        if database is None or restore_database.is_symlink() or not restore_database.is_file():
            raise RuntimeError('Отсутствует база для восстановления')
        with sqlite3.connect('file:' + str(restore_database) + '?mode=ro', uri=True) as source:
            if source.execute('PRAGMA integrity_check').fetchone()[0] != 'ok':
                raise RuntimeError('Повреждена база для восстановления')
    jar, old_state = inspect()
    with open(str(jar) + '.deploy.lock', 'a') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        backup = jar.with_name(jar.name + '.backup-' + time.strftime('%Y%m%d-%H%M%S') + '-' + str(time.time_ns()))
        if backup.exists():
            raise RuntimeError('Имя резервной копии уже занято')
        shutil.copy2(jar, backup)
        old_hash = digest(jar)
        if digest(backup) != old_hash:
            raise RuntimeError('Не удалось проверить резервную копию')
        old_index = validate_archive(backup, old_hash)
        fd, new_name = tempfile.mkstemp(prefix=jar.name + '.new-', dir=str(jar.parent))
        os.close(fd)
        new = Path(new_name)
        replaced = False
        database_replaced = False
        stop_attempted = False
        db_backup = None
        try:
            shutil.copyfile(stage, new)
            os.chmod(new, jar.stat().st_mode & 0o777)
            if digest(new) != expected:
                raise RuntimeError('Не совпала контрольная сумма перед заменой')
            print('Остановка ' + SERVICE, flush=True)
            stop_attempted = True
            service('stop')
            if prop('ActiveState') != 'inactive' or int(prop('MainPID')) != 0:
                raise RuntimeError('Сервис не остановлен полностью')
            if database is not None:
                db_backup = backup.with_suffix(backup.suffix + '.sqlite')
                # SQLite backup includes committed WAL data. The application is already stopped.
                with sqlite3.connect('file:' + str(database) + '?mode=ro', uri=True) as source:
                    with sqlite3.connect(str(db_backup)) as destination:
                        source.backup(destination)
                        if destination.execute('PRAGMA integrity_check').fetchone()[0] != 'ok':
                            raise RuntimeError('Резервная копия базы SQLite не прошла проверку')
                os.chmod(db_backup, database.stat().st_mode & 0o777)
            if before_replace is not None:
                before_replace(backup, db_backup)
            if restore_database is not None:
                # Current data is already backed up. Preserve its WAL files for failure recovery.
                database_replaced = True
                for suffix in ('', '-wal', '-shm'):
                    current = Path(str(database) + suffix)
                    if current.exists():
                        os.replace(current, Path(str(db_backup) + '.before-restore' + suffix))
                shutil.copy2(restore_database, database)
            os.replace(new, jar)
            replaced = True
            print('JAR установлен, запуск ' + SERVICE, flush=True)
            service('start')
            if not ready(port, index):
                raise RuntimeError('Новая версия не прошла проверку запуска')
            print(json.dumps({'result': 'deployed', 'service': SERVICE, 'jar': str(jar),
                              'sha256': digest(jar), 'backup': str(backup)}, ensure_ascii=False), flush=True)
        except BaseException:
            if replaced or database_replaced:
                print('Возврат предыдущего JAR', flush=True)
                service('stop')
                if prop('ActiveState') != 'inactive' or int(prop('MainPID')) != 0:
                    raise RuntimeError('Откат остановлен: новый процесс ещё работает')
                if db_backup is not None:
                    # Retain the failed application's data for diagnosis; restore the pre-update DB.
                    failed = Path(str(db_backup) + '.failed')
                    for suffix in ('', '-wal', '-shm'):
                        current = Path(str(database) + suffix)
                        if current.exists():
                            os.replace(current, Path(str(failed) + suffix))
                    shutil.copy2(db_backup, database)
                shutil.copy2(backup, new)
                os.replace(new, jar)
            if stop_attempted and old_state == 'active' and (replaced or prop('ActiveState') != 'active'):
                service('start')
                if not ready(port, old_index):
                    raise RuntimeError('JAR восстановлен, но прежний сервис требует ручной проверки')
            raise
        finally:
            if new.exists():
                new.unlink()
        # Cleanup failure must not roll back an already healthy application.
        try:
            prune_install_backups(jar)
        except OSError as error:
            print(json.dumps({'backup_cleanup': 'failed', 'error': type(error).__name__}), file=sys.stderr)


def prune_install_backups(jar, keep=3):
    """Bound installer backups independently from the UI's three-version history.

    Called only after a successful start, while the deployment lock is held.
    Do not touch live databases, failed-restore evidence, links or other apps.
    """
    if keep < 3:
        raise ValueError('At least three installer backups must be retained')
    jar = Path(jar)
    pattern = re.compile(re.escape(jar.name) + r'\.backup-\d{8}-\d{6}(?:-\d+)?\Z')
    backups = sorted((p for p in jar.parent.iterdir()
                      if pattern.fullmatch(p.name) and p.is_file() and not p.is_symlink()),
                     key=lambda p: p.name, reverse=True)
    removed = 0
    for backup in backups[keep:]:
        snapshot = Path(str(backup) + '.sqlite')
        if snapshot.is_symlink():
            continue
        if snapshot.is_file():
            snapshot.unlink()
        backup.unlink()
        removed += 1
    return removed


def main():
    global USE_SERVICE_COMMAND
    try:
        mode = sys.argv[1]
        if mode == 'check':
            jar, state = inspect()
            print(json.dumps({'service': SERVICE, 'state': state, 'jar': str(jar)}, ensure_ascii=False))
        elif mode in ('install', 'install-no-sudo'):
            USE_SERVICE_COMMAND = mode == 'install-no-sudo'
            install(sys.argv[2], sys.argv[3], int(sys.argv[4]))
        else:
            raise RuntimeError('Неизвестная операция')
    except Exception as e:
        # Do not print systemctl ExecStart or captured stderr: these may contain credentials.
        print('Обновление не выполнено: ' + (str(e) if isinstance(e, RuntimeError) else type(e).__name__), file=sys.stderr)
        return 1
    return 0

if __name__ == '__main__':
    sys.exit(main())
