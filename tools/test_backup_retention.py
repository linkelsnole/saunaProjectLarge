import importlib.util
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('retention_installer', Path(__file__).with_name('deploy_large_remote.py'))
installer = importlib.util.module_from_spec(spec)
spec.loader.exec_module(installer)

class BackupRetentionTests(unittest.TestCase):
    def test_keeps_three_newest_pairs_and_does_not_touch_live_or_failure_data(self):
        with tempfile.TemporaryDirectory() as d:
            jar = Path(d)/'sauna.jar'
            jar.write_bytes(b'live')
            live_db = Path(d)/'sauna_app.db'
            live_db.write_bytes(b'data')
            copies = []
            for day in range(1, 6):
                p = Path(d)/('sauna.jar.backup-202610%02d-120000-100' % day)
                p.write_bytes(b'old')
                Path(str(p)+'.sqlite').write_bytes(b'snapshot')
                copies.append(p)
            evidence = Path(str(copies[0])+'.sqlite.failed')
            evidence.write_bytes(b'evidence')
            other = Path(d)/'other.jar.backup-20261001-120000'
            other.write_bytes(b'other')
            link = Path(d)/'sauna.jar.backup-20260901-120000'
            link.symlink_to(jar)
            self.assertEqual(installer.prune_install_backups(jar), 2)
            self.assertEqual([p.exists() for p in copies], [False,False,True,True,True])
            self.assertFalse(Path(str(copies[0])+'.sqlite').exists())
            self.assertEqual(jar.read_bytes(), b'live')
            self.assertEqual(live_db.read_bytes(), b'data')
            self.assertTrue(evidence.exists())
            self.assertTrue(other.exists())
            self.assertTrue(link.is_symlink())
            self.assertEqual(installer.prune_install_backups(jar), 0)

    def test_refuses_unsafe_retention(self):
        with self.assertRaises(ValueError):
            installer.prune_install_backups('missing.jar', 0)
