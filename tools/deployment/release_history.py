"""Three installed versions and paired SQLite snapshots, independent of GitHub availability."""
import json
import os
from pathlib import Path
import re
import shutil
import sqlite3
import tempfile
import time

VERSION = re.compile(r'(?:large-(?:production|lab)-[1-9][0-9]*-[1-9][0-9]*|local-[0-9a-f]{64})\Z')


def read(root):
    path = Path(root)/'history.json'
    return json.loads(path.read_text()) if path.exists() else {'current': '', 'versions': []}


def save(root, data):
    root = Path(root)
    fd, temporary = tempfile.mkstemp(prefix='.history-', dir=root)
    try:
        with os.fdopen(fd, 'w') as stream:
            json.dump(data, stream, ensure_ascii=False, indent=2)
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temporary, root/'history.json')
    finally:
        Path(temporary).unlink(missing_ok=True)


def folder(root, version):
    if not VERSION.fullmatch(version):
        raise RuntimeError('Invalid saved version')
    path = Path(root)/'versions'/version
    if path.is_symlink() or path.parent.is_symlink():
        raise RuntimeError('Unexpected history symlink')
    path.mkdir(parents=True, exist_ok=True, mode=0o700)
    return path


def copy_verified(source, destination, checksum, installer):
    source = Path(source)
    if source.is_symlink() or not source.is_file() or installer.digest(source) != checksum:
        raise RuntimeError('Saved version checksum mismatch')
    temporary = destination.with_suffix('.part')
    try:
        shutil.copyfile(source, temporary)
        with temporary.open('rb') as stream:
            os.fsync(stream.fileno())
        if installer.digest(temporary) != checksum:
            raise RuntimeError('History copy checksum mismatch')
        os.replace(temporary, destination)
    finally:
        temporary.unlink(missing_ok=True)


def validate_database(path):
    path = Path(path)
    if path.is_symlink() or not path.is_file():
        raise RuntimeError('Saved SQLite database is missing')
    with sqlite3.connect('file:' + str(path) + '?mode=ro', uri=True) as db:
        if db.execute('PRAGMA integrity_check').fetchone()[0] != 'ok':
            raise RuntimeError('Saved SQLite database is damaged')


def remember(root, jar, installer, metadata=None, database=None, current=False):
    data = read(root)
    checksum = installer.digest(jar)
    installer.validate_archive(jar, checksum)
    existing = next((v for v in data['versions'] if v['sha256'] == checksum), None)
    metadata = metadata or {}
    version = (existing or {}).get('version') or metadata.get('version') or 'local-' + checksum
    dest = folder(root, version)
    copy_verified(jar, dest/'sauna_large.jar', checksum, installer)
    entry = dict(existing or {}, version=version, sha256=checksum,
                 commit=metadata.get('commit', (existing or {}).get('commit', '')),
                 description=str(metadata.get('description') or (existing or {}).get('description') or
                                 'Версия, сохранённая перед обновлением')[:2000],
                 installedAt=(existing or {}).get('installedAt', metadata.get('installedAt', int(time.time()))),
                 restorable=(existing or {}).get('restorable', False))
    if database is not None:
        validate_database(database)
        db_hash = installer.digest(database)
        copy_verified(database, dest/'database.sqlite', db_hash, installer)
        entry.update(dbSha256=db_hash, databaseSavedAt=int(Path(database).stat().st_mtime), restorable=True)
    data['versions'] = [v for v in data['versions'] if v['version'] != version] + [entry]
    if current:
        data['current'] = version
    save(root, data)
    return entry


def prune(root):
    data = read(root)
    ordered = sorted(data['versions'], key=lambda v: (v['version'] == data['current'], v['installedAt']), reverse=True)
    data['versions'] = ordered[:3]
    save(root, data)  # Stop offering removed versions before deleting their files.
    for entry in ordered[3:]:
        shutil.rmtree(folder(root, entry['version']))


def restore_files(root, version, installer):
    data = read(root)
    entry = next((v for v in data['versions'] if v['version'] == version), None)
    if entry is None or not entry.get('restorable') or version == data['current']:
        raise RuntimeError('This saved version cannot be restored')
    dest = folder(root, version)
    jar, database = dest/'sauna_large.jar', dest/'database.sqlite'
    if jar.is_symlink() or database.is_symlink():
        raise RuntimeError('Unexpected history symlink')
    installer.validate_archive(jar, entry['sha256'])
    if installer.digest(database) != entry['dbSha256']:
        raise RuntimeError('Saved database checksum mismatch')
    validate_database(database)
    return entry, jar, database
