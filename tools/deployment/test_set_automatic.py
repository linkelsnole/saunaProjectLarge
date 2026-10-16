import fcntl
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from set_automatic import enable

class AutomaticConfigurationTest(unittest.TestCase):
    def test_preserves_existing_configuration_and_changes_only_mode(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            path = root / 'config.json'
            original = dict(root=str(root), target='production', automatic=False, jar='/existing/app.jar', database='/existing/app.db')
            path.write_text(json.dumps(original))
            with patch('set_automatic.load_config', side_effect=lambda p: json.loads(p.read_text())):
                enable(path)
            self.assertEqual(json.loads(path.read_text()), dict(original, automatic=True))

    def test_cannot_change_configuration_during_installation(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            path = root / 'config.json'
            path.write_text(json.dumps(dict(root=str(root), automatic=False)))
            with (root / 'updater.lock').open('a') as lock:
                fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
                with patch('set_automatic.load_config', side_effect=lambda p: json.loads(p.read_text())):
                    with self.assertRaises(BlockingIOError): enable(path)
            self.assertFalse(json.loads(path.read_text())['automatic'])
