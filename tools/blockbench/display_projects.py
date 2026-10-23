#!/usr/bin/env python3
"""Build Blockbench display projects, or copy ONLY changed display slots into source JSONs.

python3 tools/blockbench/display_projects.py build
python3 tools/blockbench/display_projects.py apply [project.bbmodel ...] [--write]
The default apply operation is a read-only preview. No third-party Python packages required.
"""
import argparse
import base64
from copy import deepcopy
from datetime import datetime
import hashlib
import json
import math
from pathlib import Path
import struct
import sys
import uuid

ROOT = Path(__file__).resolve().parents[2]
EDITOR = ROOT / 'blockbench'
PROJECTS = EDITOR / 'projects'
SOURCES = ROOT / 'src/main/resources/assets/deathsareitems/models/item'
SNAPSHOTS = ROOT / 'tmp/blockbench-export-client/snapshots'
MANIFEST = EDITOR / 'manifest.json'
SLOTS = ('gui', 'firstperson_righthand', 'firstperson_lefthand',
         'thirdperson_righthand', 'thirdperson_lefthand', 'ground', 'fixed', 'head')
FIELDS = {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]}


def read(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))


def save(path, data, compact=False):
    options = {'separators': (',', ':')} if compact else {'indent': 2}
    path.write_text(json.dumps(data, ensure_ascii=False, **options) + '\n', encoding='utf-8')


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def uid(name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'deathsareitems:display/' + name))


def vector(values):
    return [round(v, 6) for v in values]


def normalized(slot):
    if not isinstance(slot, dict) or set(slot) - FIELDS.keys():
        raise ValueError('Display yuvasında desteklenmeyen bir alan var.')
    result = {}
    for field, default in FIELDS.items():
        value = slot.get(field, default)
        if (not isinstance(value, list) or len(value) != 3
                or any(type(v) not in (int, float) or not math.isfinite(v) for v in value)):
            raise ValueError(f'Geçersiz {field}: üç sonlu sayı gerekli.')
        # Minecraft 1.20.1 clamps these values when reading model JSON.
        limit = 80 if field == 'translation' else 4 if field == 'scale' else None
        if limit is not None and any(abs(v) > limit for v in value):
            raise ValueError(f'{field} Minecraft sınırını aşıyor: ±{limit}')
        result[field] = vector(value)
    return result


def same(a, b):
    return all(abs((a[k][i] - b[k][i] + 180) % 360 - 180
                   if k == 'rotation' else a[k][i] - b[k][i]) <= 0.00001
               for k in FIELDS for i in range(3))


def build():
    if not (SNAPSHOTS / 'SUCCESS').exists():
        raise ValueError('Önce ./gradlew -I tools/blockbench/export.init.gradle runClient çalıştırın.')
    inputs = sorted(p for p in SNAPSHOTS.glob('*.json') if p.stem != 'textures')
    if len(inputs) != 14:
        raise ValueError('14 modelin tamamı yakalanmış olmalı.')
    occupied = [PROJECTS / (p.stem + '.bbmodel') for p in inputs if (PROJECTS / (p.stem + '.bbmodel')).exists()]
    if occupied:
        raise ValueError('Mevcut .bbmodel dosyaları korunuyor. Yeniden üretmeden önce projects klasörünü taşıyın.')
    textures = read(SNAPSHOTS / 'textures.json')
    manifest = {'version': 1, 'items': {}}
    PROJECTS.mkdir(parents=True, exist_ok=True)
    for path in inputs:
        item = path.stem
        snapshot = read(path)
        source = SOURCES / (item + '_base.json')
        used = sorted({q['texture'] for qs in snapshot['contexts'].values() for q in qs})
        texture_list, dimensions = [], {}
        for index, texture in enumerate(used):
            data = base64.b64decode(textures[texture].split(',', 1)[1])
            width, height = struct.unpack('>II', data[16:24])
            dimensions[texture] = (width, height)
            texture_list.append({'name': texture.split('/')[-1], 'id': str(index),
                                 'uuid': uid(item + '/texture/' + texture),
                                 'mode': 'bitmap', 'source': textures[texture],
                                 'uv_width': width, 'uv_height': height,
                                 'particle': False, 'render_mode': 'default', 'render_sides': 'double'})
        elements = []
        for slot in SLOTS:
            for texture in used:
                quads = [q for q in snapshot['contexts'][slot] if q['texture'] == texture]
                if not quads:
                    continue
                vertices, faces = {}, {}
                width, height = dimensions[texture]
                for number, quad in enumerate(quads):
                    keys = [f'v{number}_{i}' for i in range(4)]
                    for key, point in zip(keys, quad['vertices']):
                        vertices[key] = vector(point[:3])
                    faces[f'f{number}'] = {'vertices': keys, 'uv': {
                        key: vector([point[3] * width, point[4] * height])
                        for key, point in zip(keys, quad['vertices'])}, 'texture': used.index(texture)}
                elements.append({'type': 'mesh', 'uuid': uid(item + '/' + slot + '/' + texture),
                                 'name': slot + '|' + texture, 'origin': [0, 0, 0],
                                 'rotation': [0, 0, 0], 'vertices': vertices, 'faces': faces,
                                 'visibility': slot == 'gui', 'locked': True, 'export': True})
        display = {slot: normalized(snapshot['display'][slot]) for slot in SLOTS}
        document = {'meta': {'format_version': '4.12', 'model_format': 'deathsareitems_display', 'box_uv': False},
                    'name': item, 'dai_item': item, 'resolution': {'width': 16, 'height': 16},
                    'elements': elements, 'outliner': [e['uuid'] for e in elements],
                    'textures': texture_list, 'display': display,
                    'front_gui_light': read(source).get('gui_light', 'side') == 'front'}
        save(PROJECTS / (item + '.bbmodel'), document, compact=True)
        manifest['items'][item] = {'source': str(source.relative_to(ROOT)),
                                   'source_sha256': digest(source), 'display': display,
                                   'meshes': len(elements), 'quads': sum(len(q) for q in snapshot['contexts'].values())}
        print(f'{item}: {len(elements)} önizleme parçası')
    save(MANIFEST, manifest)


def plan(paths):
    manifest = read(MANIFEST)
    changes, seen = [], set()
    for path in paths:
        document = read(path)
        item = document.get('dai_item')
        if item not in manifest['items']:
            raise ValueError(f'{path.name}: tanınan bir DeathsAreItems projesi değil. Eklenti yüklü mü?')
        if item in seen:
            raise ValueError(f'Aynı item iki kez seçildi: {item}')
        seen.add(item)
        entry = manifest['items'][item]
        display = document.get('display')
        if not isinstance(display, dict) or set(display) - set(SLOTS):
            raise ValueError(f'{item}: geçersiz display verisi.')
        source = ROOT / entry['source']
        original = read(source)
        revised = deepcopy(original)
        changed_slots = []
        for slot in SLOTS:
            # Blockbench omits identity transforms on save; absence means identity,
            # not "leave the old non-identity slot in place".
            edited = normalized(display.get(slot, {}))
            baseline = normalized(entry['display'][slot])
            if not same(edited, baseline):
                revised.setdefault('display', {})[slot] = edited
                changed_slots.append(slot)
        if not changed_slots:
            continue
        # A previously omitted left-hand slot inherits the right-hand slot in
        # Minecraft. Preserve the independently previewed left pose when editing right.
        for right, left in [('firstperson_righthand', 'firstperson_lefthand'),
                            ('thirdperson_righthand', 'thirdperson_lefthand')]:
            if right in changed_slots and left not in original.get('display', {}) and left not in changed_slots:
                revised['display'][left] = normalized(display.get(left, {}))
                changed_slots.append(left)
        # Other source edits may have happened since capture. Do not use a stale preview.
        if digest(source) != entry['source_sha256']:
            # Reapplying the same saved project should be harmless.
            if all(same(normalized(original.get('display', {}).get(s, {})), revised['display'][s]) for s in changed_slots):
                continue
            raise ValueError(f'{item}: kaynak JSON önizlemeden sonra değişmiş. Önce güncel projeler oluşturulmalı.')
        changes.append((item, source, original, revised, changed_slots))
    return changes


def apply(paths, write=False):
    paths = paths or sorted(PROJECTS.glob('*.bbmodel'))
    if not paths:
        raise ValueError('Aktarılacak .bbmodel bulunamadı.')
    # Validate the complete batch before writing any files.
    changes = plan(paths)
    if not changes:
        print('Duruş değişikliği yok. Hiçbir model dosyasına dokunulmadı.')
        return
    backup = EDITOR / 'backups' / datetime.now().strftime('%Y%m%d-%H%M%S-%f')
    for item, source, original, revised, slots in changes:
        print(f'{item}: {", ".join(slots)}')
        if write:
            backup.mkdir(parents=True, exist_ok=True)
            (backup / source.name).write_bytes(source.read_bytes())
            # The only semantic difference allowed is the top-level display object.
            assert {k: v for k, v in original.items() if k != 'display'} == {k: v for k, v in revised.items() if k != 'display'}
            save(source, revised)
    if write:
        # Accept further incremental edits against the same preview geometry.
        manifest = read(MANIFEST)
        for item, source, _, revised, slots in changes:
            entry = manifest['items'][item]
            entry['source_sha256'] = digest(source)
            for slot in slots:
                entry['display'][slot] = revised['display'][slot]
        save(MANIFEST, manifest)
        print(f'Yalnızca display aktarıldı. Yedekler: {backup.relative_to(ROOT)}')
    else:
        print('Önizleme tamamlandı; dosyalar değişmedi. Aktarmak için --write ekleyin.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('operation', choices=['build', 'apply'])
    parser.add_argument('projects', nargs='*', type=Path)
    parser.add_argument('--write', action='store_true', help='Yalnızca display değişikliklerini kaydet')
    args = parser.parse_args()
    try:
        if args.operation == 'build':
            build()
        else:
            apply(args.projects, args.write)
    except (ValueError, OSError, KeyError, json.JSONDecodeError) as error:
        print(f'HATA: {error}', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
