import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock, patch
import urllib.request
import zipfile

import github_release as protocol
import updater


class ProtocolTests(unittest.TestCase):
    def test_redirect_never_sends_github_token_to_storage(self):
        request = urllib.request.Request(protocol.API + '/releases/assets/1', headers={'Authorization': 'Bearer secret'})
        redirected = protocol.SafeRedirect().redirect_request(request, None, 302, '', {},
                                      'https://release-assets.githubusercontent.com/example?signature=private')
        self.assertIsNone(redirected.get_header('Authorization'))
        self.assertEqual(request.get_header('Authorization'), 'Bearer secret')

    def test_redirect_rejects_other_hosts_and_plain_http(self):
        for url in ('https://evil.example/a', 'http://api.github.com/a', 'https://user@api.github.com/a'):
            with self.assertRaises(RuntimeError):
                protocol.SafeRedirect().redirect_request(urllib.request.Request(protocol.API), None, 302, '', {}, url)

    def test_failed_or_wrong_workflow_never_authorizes_release(self):
        release, manifest = fixture()
        good = dict(status='completed', conclusion='success', event='workflow_dispatch',
                    head_branch='main', head_sha=manifest['commit'], run_attempt=1,
                    path=protocol.WORKFLOW, repository={'full_name': protocol.REPOSITORY})
        for bad in ({'conclusion':'failure'}, {'head_branch':'codex/test'}, {'event':'pull_request'},
                    {'head_sha':'b'*40}, {'run_attempt':2}, {'path':'.github/workflows/other.yml'}):
            github = protocol.GitHub('unused')
            github.json = Mock(side_effect=[manifest, dict(good, **bad)])
            with self.assertRaises(RuntimeError):
                github.manifest(release, 'production')

    def test_lab_branch_can_publish_only_lab_releases(self):
        release, manifest = fixture()
        run = dict(status='completed', conclusion='success', event='push',
                   head_branch=protocol.LAB_BRANCH, head_sha=manifest['commit'], run_attempt=1,
                   path=protocol.WORKFLOW, repository={'full_name': protocol.REPOSITORY})
        github = protocol.GitHub('unused')
        github.json = Mock(side_effect=[manifest, run])
        with self.assertRaises(RuntimeError):
            github.manifest(release, 'production')
        manifest['target'] = 'lab'
        release['tag_name'] = 'large-lab-10-1'
        github.json = Mock(side_effect=[manifest, run])
        self.assertEqual(github.manifest(release, 'lab'), manifest)

    def test_running_workflow_is_waited_for(self):
        release, manifest = fixture()
        github = protocol.GitHub('unused')
        github.json = Mock(side_effect=[manifest, {'status':'in_progress'}])
        self.assertIsNone(github.manifest(release, 'production'))

    def test_incomplete_or_wrong_hash_download_removed(self):
        for data, size, checksum in ((b'partial', 10, '0'*64), (b'bad', 3, '0'*64)):
            with tempfile.TemporaryDirectory() as folder:
                destination = Path(folder)/'sauna_large.jar'
                github = protocol.GitHub('unused')
                github.open = Mock(return_value=io.BytesIO(data))
                with self.assertRaises(RuntimeError):
                    github.download({'id':1, 'size':size}, destination, checksum, size)
                self.assertFalse(destination.exists())

    def test_target_and_path_cannot_be_changed_in_manifest(self):
        release, manifest = fixture()
        for change in ({'target':'lab'}, {'jar':'../../sauna_large.jar'}, {'size':0}, {'run_id':11}):
            with self.assertRaises(RuntimeError):
                protocol.validate_manifest(dict(manifest, **change), release, 'production')


def fixture():
    manifest = dict(schema=1, repository=protocol.REPOSITORY, target='production', jar='sauna_large.jar',
                    run_id=10, run_attempt=1, commit='a'*40, sha256='0'*64, size=5)
    release = dict(tag_name='large-production-10-1', assets=[
        dict(name='manifest.json', id=1, size=400, state='uploaded'),
        dict(name='sauna_large.jar', id=2, size=5, state='uploaded')])
    return release, manifest


class UpdateFlowTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.release, self.manifest = fixture()
        sample = self.root/'source.jar'
        with zipfile.ZipFile(sample, 'w') as z:
            z.writestr('BOOT-INF/classes/static/index.html', 'new version')
        self.data = sample.read_bytes()
        self.manifest.update(size=len(self.data), sha256=hashlib.sha256(self.data).hexdigest())
        self.release['assets'][1]['size'] = len(self.data)
        self.github = Mock()
        self.github.latest.return_value = self.release
        self.github.json.return_value = self.release
        self.github.manifest.return_value = self.manifest
        self.github.download.side_effect = lambda a, path, h, size: path.write_bytes(self.data)
        self.config = dict(target='production', root=str(self.root))
        self.smoke = patch.object(updater, 'smoke').start()
        self.install = patch.object(updater, 'production').start()
        self.addCleanup(patch.stopall)

    def test_publication_only_stages_until_matching_admin_request(self):
        self.assertEqual(updater.once(self.config, self.github)['status'], 'available')
        self.install.assert_not_called()
        self.smoke.assert_called_once()
        updater.save_json(self.root/'request.json', {'tag':'large-production-9-1'})
        updater.once(self.config, self.github)
        self.install.assert_not_called()
        self.assertEqual(self.github.download.call_count, 1)
        updater.save_json(self.root/'request.json', {'tag':self.release['tag_name']})
        self.assertEqual(updater.once(self.config, self.github)['status'], 'success')
        self.install.assert_called_once()
        updater.once(self.config, self.github)
        self.install.assert_called_once()

    def test_automatic_install_requires_explicit_configuration(self):
        self.config['automatic']=True
        self.assertEqual(updater.once(self.config,self.github)['status'],'success')
        self.install.assert_called_once()
        updater.once(self.config,self.github)
        self.install.assert_called_once()

    def test_lab_never_calls_production_even_with_request(self):
        self.config['target'] = 'lab'
        updater.save_json(self.root/'request.json', {'tag':self.release['tag_name']})
        self.assertEqual(updater.once(self.config, self.github)['status'], 'success')
        self.install.assert_not_called()

    def test_failed_install_is_not_retried_every_minute(self):
        updater.save_json(self.root/'request.json', {'tag':self.release['tag_name']})
        self.install.side_effect = RuntimeError('rollback completed')
        with self.assertRaises(RuntimeError):
            updater.once(self.config, self.github)
        self.assertEqual(updater.once(self.config, self.github)['status'], 'failed')
        self.assertEqual(self.install.call_count, 1)
        self.assertFalse((self.root/'request.json').exists())

    def test_interrupted_transaction_requires_review(self):
        updater.save_json(self.root/'attempt.json', {'status':'installing'})
        with self.assertRaisesRegex(RuntimeError, 'Interrupted'):
            updater.once(self.config, self.github)
        self.install.assert_not_called()
        self.github.latest.assert_not_called()

    def test_preview_bootstraps_once_then_requires_exact_admin_approval(self):
        self.config.update(target='lab', preview=True, preview_port=18082)
        self.release['tag_name'] = 'large-lab-10-1'
        self.manifest['target'] = 'lab'
        folder = self.root/'instance'
        folder.mkdir()
        def install(*args):
            (folder/'sauna_large.jar').write_bytes(b'installed')
        with patch.object(updater.lab_instance, 'instance', return_value=folder), \
             patch.object(updater.lab_instance, 'install', side_effect=install) as preview:
            self.assertEqual(updater.once(self.config, self.github)['status'], 'success')
            preview.assert_called_once()
            self.release['tag_name'] = 'large-lab-11-1'
            self.manifest['run_id'] = 11
            self.assertEqual(updater.once(self.config, self.github)['status'], 'available')
            preview.assert_called_once()
            updater.save_json(self.root/'request.json', {'tag': 'large-lab-11-1'})
            self.assertEqual(updater.once(self.config, self.github)['status'], 'success')
            self.assertEqual(preview.call_count, 2)
        self.install.assert_not_called()

    def test_smoke_failure_never_stops_real_service(self):
        self.smoke.side_effect = RuntimeError('broken application')
        updater.save_json(self.root/'request.json', {'tag':self.release['tag_name']})
        with self.assertRaises(RuntimeError):
            updater.once(self.config, self.github)
        self.install.assert_not_called()

    def test_locked_updater_does_not_download_or_install(self):
        with (self.root/'updater.lock').open('a') as lock:
            updater.fcntl.flock(lock, updater.fcntl.LOCK_EX | updater.fcntl.LOCK_NB)
            self.assertEqual(updater.once(self.config, self.github)['status'], 'busy')
        self.github.latest.assert_not_called()


if __name__ == '__main__':
    unittest.main()
