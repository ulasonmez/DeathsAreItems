"""Integration checks for preserving model data during display-only imports."""
from copy import deepcopy
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('display_projects', Path(__file__).with_name('display_projects.py'))
editor = importlib.util.module_from_spec(spec)
spec.loader.exec_module(editor)


class DisplayImports(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.old = {name: getattr(editor, name) for name in ['ROOT', 'EDITOR', 'PROJECTS', 'MANIFEST']}
        self.addCleanup(lambda: [setattr(editor, k, v) for k, v in self.old.items()])
        editor.ROOT = Path(self.temp.name)
        editor.EDITOR = editor.ROOT / 'blockbench'
        editor.PROJECTS = editor.EDITOR / 'projects'
        editor.MANIFEST = editor.EDITOR / 'manifest.json'
        editor.PROJECTS.mkdir(parents=True)
        self.original = {'loader': 'forge:composite', 'textures': {'skin': 'test:skin'},
                         'children': {'head': {'elements': [{'from': [0, 0, 0], 'to': [8, 16, 8]}]}},
                         'display': {'gui': {'scale': [.5, .5, .5]}}}
        self.source = editor.ROOT / 'sample_base.json'
        editor.save(self.source, self.original)
        baseline = {slot: editor.normalized(self.original['display'].get(slot, {})) for slot in editor.SLOTS}
        editor.save(editor.MANIFEST, {'items': {'sample': {'source': self.source.name,
                    'source_sha256': editor.digest(self.source), 'display': baseline}}})
        self.project = editor.PROJECTS / 'sample.bbmodel'
        self.doc = {'dai_item': 'sample', 'display': deepcopy(baseline), 'elements': ['Never import this geometry']}
        editor.save(self.project, self.doc)

    def test_unchanged_does_not_write_even_when_identity_slots_are_omitted(self):
        self.doc['display'] = {'gui': {'scale': [.5] * 3}}
        editor.save(self.project, self.doc)
        before = self.source.read_bytes()
        editor.apply([self.project], True)
        self.assertEqual(before, self.source.read_bytes())
        self.assertFalse((editor.EDITOR / 'backups').exists())

    def test_import_preserves_geometry_and_supports_repeat_incremental_and_reset(self):
        self.doc['display']['gui']['scale'] = [.8] * 3
        editor.save(self.project, self.doc)
        editor.apply([self.project], True)
        revised = editor.read(self.source)
        self.assertEqual({k: v for k, v in self.original.items() if k != 'display'},
                         {k: v for k, v in revised.items() if k != 'display'})
        self.assertEqual(revised['display']['gui']['scale'], [.8] * 3)
        self.assertEqual(len(list((editor.EDITOR / 'backups').glob('*/*.json'))), 1)
        editor.apply([self.project], True)
        self.assertEqual(len(list((editor.EDITOR / 'backups').glob('*/*.json'))), 1)
        self.doc['display']['gui']['scale'] = [.6] * 3
        editor.save(self.project, self.doc)
        editor.apply([self.project], True)
        self.doc['display']['gui']['scale'] = [.5] * 3
        editor.save(self.project, self.doc)
        editor.apply([self.project], True)
        self.assertEqual(editor.read(self.source)['display']['gui']['scale'], [.5] * 3)
        backups = list((editor.EDITOR / 'backups').glob('*/*.json'))
        self.assertEqual(len(backups), 3)
        self.assertTrue(any(editor.read(p) == self.original for p in backups))

    def test_reset_to_identity_and_right_hand_inheritance(self):
        self.doc['display'].pop('gui')
        self.doc['display']['firstperson_righthand']['translation'] = [1, 2, 3]
        editor.save(self.project, self.doc)
        editor.apply([self.project], True)
        result = editor.read(self.source)['display']
        self.assertEqual(result['gui']['scale'], [1] * 3)
        self.assertEqual(result['firstperson_lefthand']['translation'], [0] * 3)

    def test_stale_source_rejected_without_writes(self):
        self.doc['display']['gui']['scale'] = [.8] * 3
        editor.save(self.project, self.doc)
        self.source.write_text(self.source.read_text() + ' ')
        before = self.source.read_bytes()
        with self.assertRaises(ValueError):
            editor.apply([self.project], True)
        self.assertEqual(before, self.source.read_bytes())

    def test_whole_batch_validated_before_any_source_is_written(self):
        self.doc['display']['gui']['scale'] = [.8] * 3
        editor.save(self.project, self.doc)
        invalid = editor.PROJECTS / 'invalid.bbmodel'
        editor.save(invalid, {'dai_item': 'unknown'})
        before = self.source.read_bytes()
        with self.assertRaises(ValueError):
            editor.apply([self.project, invalid], True)
        self.assertEqual(before, self.source.read_bytes())

    def test_bad_values_and_unsupported_pivots_rejected(self):
        for value in [{'scale': [float('nan'), 1, 1]}, {'rotation_pivot': [1, 0, 0]},
                      {'translation': [81, 0, 0]}, {'scale': [5, 1, 1]}]:
            with self.subTest(value=value), self.assertRaises(ValueError):
                editor.normalized(value)
        self.assertTrue(editor.same(editor.normalized({'rotation': [270, 0, 0]}),
                                    editor.normalized({'rotation': [-90, 0, 0]})))


if __name__ == '__main__':
    unittest.main()
