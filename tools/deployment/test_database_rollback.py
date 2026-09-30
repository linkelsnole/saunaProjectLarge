import sqlite3
import tempfile
from pathlib import Path
import unittest
from unittest.mock import patch
import zipfile
import updater

installer = updater.installer


class DatabaseRollbackTests(unittest.TestCase):
    def test_failed_new_version_restores_database_and_jar(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            old, new, db = root/'old.jar', root/'new.jar', root/'users.db'
            for path, text in ((old, 'old'), (new, 'new')):
                with zipfile.ZipFile(path, 'w') as archive:
                    archive.writestr('BOOT-INF/classes/static/index.html', text)
            old_hash = installer.digest(old)
            with sqlite3.connect(db) as connection:
                connection.execute('create table users (name text)')
                connection.execute("insert into users values ('original')")
            def start(action):
                if action == 'start' and installer.digest(old) != old_hash:
                    with sqlite3.connect(db) as connection:
                        connection.execute("update users set name='changed-by-new-version'")
            with patch.object(installer, 'inspect', return_value=(old, 'active')), \
                 patch.object(installer, 'service', side_effect=start), \
                 patch.object(installer, 'prop', side_effect=lambda key: 'inactive' if key == 'ActiveState' else '0'), \
                 patch.object(installer, 'ready', side_effect=[False, True]):
                with self.assertRaisesRegex(RuntimeError, 'проверку запуска'):
                    installer.install(new, installer.digest(new), 8082, database=db)
            self.assertEqual(installer.digest(old), old_hash)
            with sqlite3.connect(db) as connection:
                self.assertEqual(connection.execute('select name from users').fetchone()[0], 'original')
            self.assertEqual(len(list(root.glob('*.sqlite.failed'))), 1)

    def test_explicit_restore_failure_returns_current_jar_and_current_database(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder); current=root/'current.jar'; old=root/'old.jar'; db=root/'users.db'; snapshot=root/'snapshot.db'
            for path,text in ((current,'current'),(old,'old')):
                with zipfile.ZipFile(path,'w') as z:z.writestr('BOOT-INF/classes/static/index.html',text)
            for path,text in ((db,'current'),(snapshot,'old')):
                with sqlite3.connect(path) as c:c.execute('create table users(name text)');c.execute('insert into users values (?)',(text,))
            checksum=installer.digest(current)
            with patch.object(installer,'inspect',return_value=(current,'active')),patch.object(installer,'service'),patch.object(installer,'prop',side_effect=lambda p:'inactive' if p=='ActiveState' else '0'),patch.object(installer,'ready',side_effect=[False,True]):
                with self.assertRaises(RuntimeError):installer.install(old,installer.digest(old),8082,database=db,restore_database=snapshot)
            self.assertEqual(installer.digest(current),checksum)
            with sqlite3.connect(db) as c:self.assertEqual(c.execute('select name from users').fetchone()[0],'current')

    def test_absent_database_never_stops_service(self):
        with tempfile.TemporaryDirectory() as folder:
            jar = Path(folder)/'new.jar'
            with zipfile.ZipFile(jar, 'w') as archive:
                archive.writestr('BOOT-INF/classes/static/index.html', 'new')
            with patch.object(installer, 'service') as service:
                with self.assertRaises(RuntimeError):
                    installer.install(jar, installer.digest(jar), 8082, database=Path(folder)/'absent.db')
                service.assert_not_called()
