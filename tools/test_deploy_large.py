import hashlib
import importlib.util
from pathlib import Path
import tempfile
import subprocess
import unittest
from unittest.mock import patch
import zipfile

spec = importlib.util.spec_from_file_location('deploy', Path(__file__).with_name('deploy_large_remote.py'))
deploy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(deploy)

client_spec = importlib.util.spec_from_file_location('deploy_client', Path(__file__).resolve().parents[1]/'deploy-large.py')
client = importlib.util.module_from_spec(client_spec)
client_spec.loader.exec_module(client)

class DeployTests(unittest.TestCase):
    def test_default_uses_sudo_service_matching_server_permissions(self):
        with patch.object(deploy, 'USE_SERVICE_COMMAND', False), \
             patch.object(deploy.os, 'geteuid', return_value=1000), \
             patch.object(deploy.shutil, 'which', return_value='/usr/sbin/service'), \
             patch.object(deploy.subprocess, 'run') as run:
            deploy.service('stop')
            deploy.service('start')
        self.assertEqual([call.args[0] for call in run.call_args_list],
                         [['sudo', '-n', '/usr/sbin/service', 'webpanel-b', 'stop'],
                          ['sudo', '-n', '/usr/sbin/service', 'webpanel-b', 'start']])

    def test_no_sudo_uses_only_the_requested_service_commands(self):
        with patch.object(deploy, 'USE_SERVICE_COMMAND', True), \
             patch.object(deploy.shutil, 'which', return_value='/usr/sbin/service'), \
             patch.object(deploy.subprocess, 'run') as run:
            deploy.service('stop')
            deploy.service('start')
        self.assertEqual([call.args[0] for call in run.call_args_list],
                         [['/usr/sbin/service', 'webpanel-b', 'stop'],
                          ['/usr/sbin/service', 'webpanel-b', 'start']])

    def test_no_sudo_mode_is_used_for_the_whole_install_including_rollback(self):
        with patch.object(deploy, 'USE_SERVICE_COMMAND', False), \
             patch.object(deploy.sys, 'argv', ['installer.py', 'install-no-sudo', '/tmp/stage.jar', 'hash', '8082']), \
             patch.object(deploy, 'install', side_effect=lambda *args: self.assertTrue(deploy.USE_SERVICE_COMMAND)) as install:
            self.assertEqual(deploy.main(), 0)
            install.assert_called_once_with('/tmp/stage.jar', 'hash', 8082)

    def test_interactive_sudo_inherits_terminal_and_checks_both_permissions_first(self):
        with patch.object(client.subprocess, 'run') as run:
            client.install_interactively(['ssh', '-p', '22', 'webpanel@example'],
                                         '/tmp/stage/installer.py', '/tmp/stage', 'abc', 8082)
        argv = run.call_args.args[0]
        self.assertEqual(argv[:2], ['ssh', '-t'])
        self.assertEqual(run.call_args.kwargs, {'check': True})
        commands = argv[-1].split(' && ')
        self.assertEqual(commands[0], 'sudo -v')
        self.assertIn('service webpanel-b stop', commands[1])
        self.assertIn('service webpanel-b start', commands[2])
        self.assertEqual(commands[3], 'python3 /tmp/stage/installer.py install /tmp/stage/sauna_large.jar abc 8082')

    def test_sudo_failure_leaves_installed_jar_unchanged(self):
        with tempfile.TemporaryDirectory() as d:
            jar = Path(d)/'sauna_large.jar'
            stage = Path(d)/'stage.jar'
            old_hash = self.archive(jar, 'old')
            new_hash = self.archive(stage, 'new')
            with patch.object(deploy, 'inspect', return_value=(jar, 'active')), \
                 patch.object(deploy, 'service', side_effect=subprocess.CalledProcessError(1, 'sudo')), \
                 patch.object(deploy, 'prop', return_value='active'), \
                 patch.object(deploy, 'ready') as ready:
                with self.assertRaises(subprocess.CalledProcessError):
                    deploy.install(stage, new_hash, 8082)
                ready.assert_not_called()
            self.assertEqual(deploy.digest(jar), old_hash)

    def archive(self, path, content):
        with zipfile.ZipFile(path, 'w') as z:
            z.writestr('BOOT-INF/classes/static/index.html', content)
        return deploy.digest(path)

    def test_resolves_the_unit_jar_without_executing_arguments(self):
        p = deploy.resolve_jar('{ path=/usr/bin/java ; argv[]=/usr/bin/java -jar sauna_large.jar --server.port=8082 ; ignore_errors=no ; }', '/opt/small')
        self.assertEqual(str(p), '/opt/small/sauna_large.jar')

    def test_rejects_wrapper_and_multiple_jar_arguments(self):
        for command in ['{ argv[]=/bin/bash run.sh ; }','{ argv[]=java -jar a.jar -jar b.jar ; }']:
            with self.assertRaises(RuntimeError):
                deploy.resolve_jar(command, '/opt/small')

    def test_bad_checksum_never_stops_service(self):
        with tempfile.TemporaryDirectory() as d:
            stage = Path(d)/'stage.jar'
            self.archive(stage, 'new')
            with patch.object(deploy, 'service') as service:
                with self.assertRaises(RuntimeError):
                    deploy.install(stage, '0'*64, 8082)
                service.assert_not_called()

    def test_success_preserves_old_jar_and_database(self):
        self.exercise(False)

    def test_failed_start_restores_old_jar(self):
        self.exercise(True)

    def exercise(self, failed):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); jar = root/'sauna_large.jar'; stage = root/'stage.jar'; database = root/'sauna_app.db'
            old_hash = self.archive(jar,'old'); new_hash = self.archive(stage,'new'); database.write_bytes(b'database untouched')
            with patch.object(deploy,'inspect',return_value=(jar,'active')), \
                 patch.object(deploy,'prop',side_effect=lambda p:'inactive' if p=='ActiveState' else '0'), \
                 patch.object(deploy,'service') as service, \
                 patch.object(deploy,'ready',side_effect=[False,True] if failed else [True]):
                if failed:
                    with self.assertRaises(RuntimeError): deploy.install(stage,new_hash,8082)
                    self.assertEqual(deploy.digest(jar),old_hash)
                    self.assertEqual([c.args[0] for c in service.call_args_list],['stop','start','stop','start'])
                else:
                    deploy.install(stage,new_hash,8082)
                    self.assertEqual(deploy.digest(jar),new_hash)
                    self.assertEqual([c.args[0] for c in service.call_args_list],['stop','start'])
                self.assertEqual(database.read_bytes(),b'database untouched')
                backups=list(root.glob('sauna_large.jar.backup-*'))
                self.assertEqual(len(backups),1)
                self.assertEqual(deploy.digest(backups[0]),old_hash)

if __name__=='__main__': unittest.main()
