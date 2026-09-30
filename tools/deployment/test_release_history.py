import json
from pathlib import Path
import sqlite3
import tempfile
import unittest
from unittest.mock import Mock, patch
import zipfile
import updater
import release_history as history

class HistoryTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name)
        self.db=self.root/'users.db'
        with sqlite3.connect(self.db) as db: db.execute('create table users(name text)')
    def version(self,number,current=True):
        jar=self.root/('source-%s.jar'%number)
        with zipfile.ZipFile(jar,'w') as z: z.writestr('BOOT-INF/classes/static/index.html',str(number))
        return history.remember(self.root,jar,updater.installer,metadata={'version':'large-production-%s-1'%number,'description':'version '+str(number)},database=self.db,current=current)
    def test_keeps_three_versions_and_current_after_pruning(self):
        for i in range(1,5):
            self.version(i)
            history.prune(self.root)
        saved=history.read(self.root)
        self.assertEqual(saved['current'],'large-production-4-1')
        self.assertEqual(len(saved['versions']),3)
        self.assertFalse((self.root/'versions/large-production-1-1').exists())
    def test_corrupt_jar_or_database_cannot_be_restored(self):
        self.version(1);self.version(2)
        jar=self.root/'versions/large-production-1-1/sauna_large.jar'
        content=jar.read_bytes();jar.write_bytes(b'broken')
        with self.assertRaises(RuntimeError):history.restore_files(self.root,'large-production-1-1',updater.installer)
        jar.write_bytes(content)
        (jar.parent/'database.sqlite').write_bytes(b'broken')
        with self.assertRaises(RuntimeError):history.restore_files(self.root,'large-production-1-1',updater.installer)
    def test_restore_works_without_any_github_request_and_sets_hold(self):
        self.version(1);self.version(2)
        updater.save_json(self.root/'attempt.json',{'status':'success','tag':'large-production-2-1','sequence':[2,1]})
        updater.save_json(self.root/'request.json',{'tag':'large-production-1-1','restore':True})
        github=Mock()
        with patch.object(updater,'production') as install:
            state=updater.once({'root':str(self.root),'target':'production'},github)
        self.assertEqual(state['status'],'success');install.assert_called_once()
        self.assertEqual(github.mock_calls,[])
        self.assertEqual(history.read(self.root)['current'],'large-production-1-1')
        self.assertEqual(updater.read_json(self.root/'hold.json')['sequence'],[2,1])
    def test_restore_failure_keeps_current_and_clears_request(self):
        self.version(1);self.version(2)
        updater.save_json(self.root/'request.json',{'tag':'large-production-1-1','restore':True})
        with patch.object(updater,'production',side_effect=RuntimeError('rolled back')):
            with self.assertRaises(RuntimeError):updater.once({'root':str(self.root),'target':'production'},Mock())
        self.assertEqual(history.read(self.root)['current'],'large-production-2-1')
        self.assertFalse((self.root/'request.json').exists())
    def test_rejects_paths_and_current_version(self):
        self.version(1)
        for tag in ('../outside','large-production-1-1'):
            with self.assertRaises(RuntimeError):history.restore_files(self.root,tag,updater.installer)
