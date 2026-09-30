import os
import socket
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import sqlite3
import zipfile
import lab_instance as lab
import updater


class LabInstanceTests(unittest.TestCase):
    def test_cannot_point_preview_at_production_or_other_directories(self):
        for config in [dict(target='production',root='/opt/webpanel/sauna-updater-lab'),
                       dict(target='lab',root='/opt/webpanel/sauna-release-20260915-2317/small')]:
            with self.assertRaises(RuntimeError): lab.instance(config)

    @unittest.skipUnless(Path('/proc/self/cmdline').exists(), 'Linux process ownership check')
    def test_refuses_pid_owned_by_another_program(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);(root/'pid').write_text(str(os.getpid()))
            with patch.object(lab.os,'kill') as kill:
                with self.assertRaises(RuntimeError): lab.stop(root)
                kill.assert_not_called()

    def test_public_lab_refuses_production_port(self):
        with patch.object(lab.subprocess, 'Popen') as process:
            with self.assertRaises(RuntimeError):
                lab.start(Path('/unused'), Path('/unused'), 8082, '0.0.0.0')
            process.assert_not_called()

    def test_restart_accepts_time_wait_but_refuses_active_listener(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            with socket.socket() as listener:
                listener.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
                listener.bind(('127.0.0.1', 0))
                port = listener.getsockname()[1]
                listener.listen(1)
                with patch.object(lab.subprocess, 'Popen') as process:
                    with self.assertRaises(OSError): lab.start(root, root, port)
                    process.assert_not_called()
                with socket.create_connection(('127.0.0.1', port)) as client:
                    connection, _ = listener.accept()
                    connection.close()  # Server closes first: creates TIME_WAIT locally.
                    self.assertEqual(client.recv(1), b'')
            with patch.object(lab.subprocess, 'Popen') as process:
                process.return_value.pid = 123
                lab.start(root, root, port)
                process.assert_called_once()
                self.assertEqual((root/'pid').read_text(), '123')

    def test_failed_lab_start_restores_old_jar_and_database(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);instance=root/'instance';instance.mkdir()
            old=instance/'sauna_large.jar';candidate=root/'candidate.jar';db=instance/'users.db'
            for path,content in [(old,b'old'),(candidate,b'new')]:
                with zipfile.ZipFile(path,'w') as archive: archive.writestr('BOOT-INF/classes/static/index.html',content)
            original=old.read_bytes()
            with sqlite3.connect(db) as conn:
                conn.execute('create table test(value text)');conn.execute("insert into test values('original')")
            def start(*args):
                if old.read_bytes()!=original:
                    with sqlite3.connect(db) as conn: conn.execute("update test set value='new'")
            with patch.object(lab,'instance',return_value=instance), patch.object(lab,'stop'), \
                 patch.object(lab,'start',side_effect=start), patch.object(lab,'ready',side_effect=[False,True]):
                with self.assertRaises(RuntimeError): lab.install(dict(preview_port=18082),candidate,updater.installer)
            self.assertEqual(old.read_bytes(),original)
            with sqlite3.connect(db) as conn: self.assertEqual(conn.execute('select value from test').fetchone()[0],'original')
