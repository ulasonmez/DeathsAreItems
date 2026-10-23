"""Reproduce the four DeathsAreItemsModels3 references from local vanilla assets.

Usage: python3 tools/build_models3_assets.py /path/to/1.20.1/client.jar
Bed and shield cuboids/UVs come directly from BedRenderer and ShieldModel (1.20.1).
No downloaded artwork, generated artwork, or changes to vanilla resources are used.
"""
from io import BytesIO
import json
import math
from pathlib import Path
import sys
import zipfile

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/deathsareitems'
FACES = {
    'down': [(0, 0, 1), (0, 0, 0), (1, 0, 0), (1, 0, 1)],
    'up': [(0, 1, 0), (0, 1, 1), (1, 1, 1), (1, 1, 0)],
    'north': [(1, 1, 0), (1, 0, 0), (0, 0, 0), (0, 1, 0)],
    'south': [(0, 1, 1), (0, 0, 1), (1, 0, 1), (1, 1, 1)],
    'west': [(0, 1, 0), (0, 0, 0), (0, 0, 1), (0, 1, 1)],
    'east': [(1, 1, 1), (1, 0, 1), (1, 0, 0), (1, 1, 0)],
}
NORMALS = {'down': (0, -1, 0), 'up': (0, 1, 0), 'north': (0, 0, -1),
           'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0)}


def save_json(name, data):
    path = ASSETS / 'models' / (name + '.json')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n')


def save_texture(name, texture):
    path = ASSETS / 'textures' / (name + '.png')
    path.parent.mkdir(parents=True, exist_ok=True)
    texture.save(path)


def rotation(axis, degrees):
    a = math.radians(degrees)
    c, s = round(math.cos(a), 10), round(math.sin(a), 10)
    if axis == 'x':
        return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
    if axis == 'y':
        return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def native_cube(lower, size, tex_uv, matrix, offset, texture='bed'):
    """Bake an axis-aligned ModelPart cube after its vanilla 90-degree poses.

    Keep every original vertex's UV, including rotated and mirrored leg faces.
    This removes the need for a block entity or a different in-hand bed mesh.
    """
    x, y, z = lower
    w, h, d = size
    points = np.array([(x, y, z), (x+w, y, z), (x+w, y+h, z), (x, y+h, z),
                       (x, y, z+d), (x+w, y, z+d), (x+w, y+h, z+d), (x, y+h, z+d)])
    points = np.round(points @ matrix.T + offset, 8)
    lo, hi = points.min(axis=0), points.max(axis=0)
    u, v = tex_uv
    polygons = {
        'down': ([5, 4, 0, 1], [u+d, v, u+d+w, v+d]),
        'up': ([2, 3, 7, 6], [u+d+w, v+d, u+d+2*w, v]),
        'west': ([0, 4, 7, 3], [u, v+d, u+d, v+d+h]),
        'north': ([1, 0, 3, 2], [u+d, v+d, u+d+w, v+d+h]),
        'east': ([5, 1, 2, 6], [u+d+w, v+d, u+2*d+w, v+d+h]),
        'south': ([4, 5, 6, 7], [u+2*d+w, v+d, u+2*d+2*w, v+d+h]),
    }
    faces = {}
    for normal, (indices, uv) in polygons.items():
        n = np.round(matrix @ NORMALS[normal]).astype(int)
        face = next(k for k, value in NORMALS.items() if tuple(n) == value)
        u0, v0, u1, v1 = np.array(uv) / 4  # Vanilla 64x64 entity atlas.
        native_uv = [(u1, v0), (u0, v0), (u0, v1), (u1, v1)]
        canonical = [np.where(corner, hi, lo) for corner in FACES[face]]
        target = [native_uv[next(j for j, index in enumerate(indices)
                                 if np.allclose(points[index], corner))] for corner in canonical]
        found = None
        for ua, ub in ((u0, u1), (u1, u0)):
            for va, vb in ((v0, v1), (v1, v0)):
                ordered = [(ua, va), (ua, vb), (ub, vb), (ub, va)]
                for shift in range(4):
                    if np.allclose([ordered[(j+shift) % 4] for j in range(4)], target):
                        found = {'texture': '#' + texture, 'uv': [ua, va, ub, vb]}
                        if shift:
                            found['rotation'] = shift * 90
                        break
                if found:
                    break
            if found:
                break
        if found is None:
            raise ValueError('Cannot preserve native UV: ' + face)
        faces[face] = found
    return {'from': lo.tolist(), 'to': hi.tolist(), 'faces': faces}


def bed_piece(head, south=False, foot_offset=0):
    matrix = rotation('x', 90)
    offset = np.array([0, 9, foot_offset], dtype=float)
    if south:
        # BedRenderer.renderPiece: T(0,9,z) Rx(90) T(8) Rz(180) T(-8).
        matrix = matrix @ rotation('z', 180)
        offset += rotation('x', 90) @ (np.array([8, 8, 8]) - rotation('z', 180) @ [8, 8, 8])
    cubes = [native_cube([0, 0, 0], [16, 16, 6], [0, 0 if head else 22], matrix, offset)]
    legs = ([([0, 6, 0], [50, 6], 90), ([-16, 6, 0], [50, 18], 180)] if head else
            [([0, 6, -16], [50, 0], 0), ([-16, 6, -16], [50, 12], 270)])
    for lower, uv, angle in legs:
        leg_rotation = rotation('z', angle) @ rotation('x', 90)
        cubes.append(native_cube(lower, [3, 3, 3], uv, matrix @ leg_rotation, offset))
    return cubes


def fire_pants(vanilla):
    magma = vanilla('block/magma').crop((0, 0, 16, 16))  # Original first animation frame.
    silhouette = vanilla('item/iron_leggings')
    icon = magma.copy()
    icon.putalpha(silhouette.getchannel('A'))
    save_texture('item/fire_pants', icon)
    armor = Image.new('RGBA', (64, 32))
    # Vanilla layer 2 leaves the chest and the bottom of each leg transparent.
    # Its body UV contains only the waistband, not a second chestplate.
    mask = vanilla('models/armor/iron_layer_2').getchannel('A')
    for yy in range(32):
        for xx in range(64):
            if mask.getpixel((xx, yy)):
                armor.putpixel((xx, yy), magma.getpixel((xx % 16, yy % 16)))
    save_texture('models/armor/fire_pants_layer_2', armor)
    save_json('item/fire_pants_base', {'parent': 'minecraft:item/generated',
              'textures': {'layer0': 'deathsareitems:item/fire_pants'}})


def bedrock(vanilla, model):
    red = vanilla('entity/bed/red')
    bed = red.copy()
    rock = vanilla('block/bedrock')
    for y in range(64):
        for x in range(64):
            r, g, b, a = red.getpixel((x, y))
            # Only the original red textile; pillow, seams, wood, legs and alpha stay exact.
            if a and r > g * 1.5 and r > b * 1.5:
                bed.putpixel((x, y), rock.getpixel(((x-6) % 16, (y-6) % 16)))
    save_texture('item/bedrock', bed)
    textures = {'bed': 'deathsareitems:item/bedrock', 'particle': 'minecraft:block/bedrock'}
    for head in (True, False):
        save_json('block/bedrock_' + ('head' if head else 'foot'),
                  {'textures': textures, 'elements': bed_piece(head)})
    template = model('item/template_bed')
    save_json('item/bedrock_base', {'textures': textures, 'display': template['display'],
              'elements': bed_piece(True, True) + bed_piece(False, True, -16)})


def pearl_shield(vanilla, model):
    pearl = vanilla('item/ender_pearl')
    atlas = vanilla('entity/shield_base_nopattern')
    # Rectangular pearl rim and inset in the reference, including its corner notch.
    # Use the original pearl palette, without stretching its circular silhouette.
    rows = (
        'dddddddddddd',
        'dsssSSSSsssd',
        'dsHhhhHSSssd',
        'dshdSSSSSSsd',
        'dsHSmmmmSSsd',
        'dsHSmmmmmmSd',
        'dsHSmmmmmmSd',
        'dsHSccccmmSd',
        'dsHScccccmSd',
        'dsSScccccmSd',
        'dsSScccccmSd',
        'dsSScccccmSd',
        'dsSScccccmSd',
        'dsSScccccmSd',
        'dsSmcccccmSd',
        'dsSmcccccmSd',
        'dsSmmcccmSSd',
        'dsSmmmmmSHSd',
        'dsSSmmmmSHSd',
        'dssSSSShhSSd',
        'dsssSSSSsssd',
        'dddddddddddd',
    )
    palette = {'d': (3, 38, 32, 255), 'c': (6, 57, 49, 255),
               'm': (11, 77, 66, 255), 's': (16, 94, 81, 255),
               'S': (37, 132, 116, 255), 'H': (52, 153, 136, 255),
               'h': (44, 205, 177, 255)}
    assert set(palette.values()) <= {color for count, color in pearl.getcolors()}
    panel = Image.new('RGBA', (12, 22))
    for y, row in enumerate(rows):
        assert len(row) == 12
        for x, symbol in enumerate(row):
            panel.putpixel((x, y), palette[symbol])
    atlas.paste(panel, (1, 1))
    atlas.paste(panel.transpose(Image.Transpose.FLIP_LEFT_RIGHT), (14, 1))
    dark = pearl.getpixel((8, 14))
    for x in range(26):
        atlas.putpixel((x, 0), dark)
    for y in range(1, 23):
        atlas.putpixel((0, y), dark)
        atlas.putpixel((13, y), dark)
    # Keep the vanilla handle silhouette and shading, using the pearl palette.
    for y in range(12):
        for x in range(26, 42):
            r, g, b, a = atlas.getpixel((x, y))
            if a:
                shade = (r + g + b) / (3 * 255)
                atlas.putpixel((x, y), (int(13+shade*18), int(39+shade*57), int(35+shade*50), a))
    save_texture('item/ender_pearl_shield', atlas)
    flip = np.diag([1, -1, -1])
    elements = [native_cube([-6, -11, -2], [12, 22, 1], [0, 0], flip, [0, 0, 0], 'shield'),
                native_cube([-1, -3, -1], [2, 6, 6], [26, 0], flip, [0, 0, 0], 'shield')]
    save_json('item/ender_pearl_shield_base', {'gui_light': 'front',
              'textures': {'shield': 'deathsareitems:item/ender_pearl_shield',
                           'particle': 'minecraft:item/ender_pearl'},
              'display': model('item/shield')['display'], 'elements': elements})


def box(lower, upper, name, texture='skin', angle=0, origin=None):
    x, y, z = lower
    X, Y, Z = upper
    # One texture pixel per model unit, aligned across adjacent surfaces.
    rects = {'north': [16-X, 16-Y, 16-x, 16-y], 'south': [x, 16-Y, X, 16-y],
             'west': [z, 16-Y, Z, 16-y], 'east': [16-Z, 16-Y, 16-z, 16-y],
             'up': [x, z, X, Z], 'down': [x, 16-Z, X, 16-z]}
    faces = {}
    for face, uv in rects.items():
        u, v, U, V = uv
        # Integer tile offset preserves pixel density without UVs beyond the atlas.
        uo, vo = math.floor(u / 16) * 16, math.floor(v / 16) * 16
        u, U, v, V = u-uo, U-uo, v-vo, V-vo
        if U > 16:
            u, U = 0, X-x if face in ('north', 'south', 'up', 'down') else Z-z
        if V > 16:
            v, V = 0, Y-y if face not in ('up', 'down') else Z-z
        faces[face] = {'texture': '#' + texture, 'uv': [u, v, U, V]}
    # Face the player (+Z), with the recessed eye on the same side in every view.
    lower, upper = [x, y, 16-Z], [X, Y, 16-z]
    faces['north'], faces['south'] = faces['south'], faces['north']
    element = {'name': name, 'from': lower, 'to': upper, 'faces': faces}
    if angle:
        element['rotation'] = {'origin': [origin[0], origin[1], 16-origin[2]],
                               'axis': 'z', 'angle': angle, 'rescale': False}
    return element


def arm_segment(start, end, width, depth, name):
    sx, sy, sz = start
    ex, ey, ez = end
    front_back = abs(ez-sz) > abs(ex-sx)
    horizontal = ez-sz if front_back else ex-sx
    angle = math.degrees(math.atan2(ey-sy, horizontal))
    # Cuboid is long along X or Y; the remaining rotation is one legal Minecraft angle.
    axis_angle = round(angle / 90) * 90
    remainder = min((-45, -22.5, 0, 22.5, 45), key=lambda a: abs(a - (angle-axis_angle)))
    length = math.hypot(horizontal, ey-sy) + .4
    cx, cy, cz = (sx+ex)/2, (sy+ey)/2, (sz+ez)/2
    w, h = (length, width) if axis_angle % 180 == 0 else (width, length)
    if front_back:
        element = box([cx-depth/2, cy-h/2, cz-w/2], [cx+depth/2, cy+h/2, cz+w/2], name)
        if remainder:
            element['rotation'] = {'origin': [cx, cy, 16-cz], 'axis': 'x',
                                   'angle': remainder, 'rescale': False}
        return element
    return box([cx-w/2, cy-h/2, cz-depth/2], [cx+w/2, cy+h/2, cz+depth/2],
               name, angle=remainder, origin=[cx, cy, cz])


def exposed_box_faces(elements):
    """Clip buried and duplicate head faces without moving the accepted silhouette or UVs."""
    result = []
    axes = {'west': (0, -1), 'east': (0, 1), 'down': (1, -1),
            'up': (1, 1), 'north': (2, -1), 'south': (2, 1)}
    for index, element in enumerate(elements):
        lo, hi = element['from'], element['to']
        for face, data in element['faces'].items():
            axis, sign = axes[face]
            a, b = [i for i in range(3) if i != axis]
            plane = lo[axis] if sign < 0 else hi[axis]
            pieces = [(lo[a], lo[b], hi[a], hi[b])]
            for other_index, other in enumerate(elements):
                if other_index == index:
                    continue
                L, H = other['from'], other['to']
                # A box touching from outside hides the seam. Coincident outer
                # surfaces belong to the later box only, preventing z-fighting.
                covers = (L[axis] < plane <= H[axis] if sign < 0 else L[axis] <= plane < H[axis])
                same = (L[axis] if sign < 0 else H[axis]) == plane
                if not covers and not (same and other_index > index):
                    continue
                remaining = []
                for x, y, X, Y in pieces:
                    u, v, U, V = max(x, L[a]), max(y, L[b]), min(X, H[a]), min(Y, H[b])
                    if u >= U or v >= V:
                        remaining.append((x, y, X, Y))
                        continue
                    remaining.extend(rect for rect in ((x, y, u, Y), (U, y, X, Y),
                                                       (u, y, U, v), (u, V, U, Y))
                                     if rect[0] < rect[2] and rect[1] < rect[3])
                pieces = remaining
            for piece_index, (x, y, X, Y) in enumerate(pieces):
                lower, upper = list(lo), list(hi)
                lower[a], lower[b], upper[a], upper[b] = x, y, X, Y
                uaxis = 2 if face in ('west', 'east') else 0
                vaxis = 2 if face in ('up', 'down') else 1
                uflip, vflip = face in ('north', 'east'), face != 'up'
                def crop_uv(axis, flip, start, end):
                    l, h = ((hi[axis]-upper[axis], hi[axis]-lower[axis]) if flip
                            else (lower[axis]-lo[axis], upper[axis]-lo[axis]))
                    return [start+(end-start)*t/(hi[axis]-lo[axis]) for t in (l, h)]
                u, U = crop_uv(uaxis, uflip, data['uv'][0], data['uv'][2])
                v, V = crop_uv(vaxis, vflip, data['uv'][1], data['uv'][3])
                result.append({'name': f"{element['name']}_{face}_{piece_index}",
                               'from': lower, 'to': upper,
                               'faces': {face: dict(data, uv=[u, v, U, V])}})
    return result


def soul_storm(vanilla):
    source = vanilla('block/obsidian')
    skin = source.copy()
    for y in range(16):
        for x in range(16):
            r, g, b, a = source.getpixel((x, y))
            shade = (r + g + b) / 3
            skin.putpixel((x, y), (int(11+shade*.18), int(7+shade*.13), int(19+shade*.28), a))
    save_texture('item/soul_storm_skin', skin)
    eye = Image.new('RGBA', (8, 8))
    eye_rows = ('dddddddd', 'dHHHHMmd', 'dHMMMMmd', 'dHMDDmmd',
                'dHMDDmmd', 'dMmmmlmd', 'dmllllld', 'dddddddd')
    palette = {'d': (27, 14, 39, 255), 'H': (129, 91, 161, 255),
               'M': (100, 64, 137, 255), 'm': (80, 47, 116, 255),
               'D': (56, 30, 82, 255), 'l': (70, 39, 103, 255)}
    for y, row in enumerate(eye_rows):
        for x, symbol in enumerate(row):
            eye.putpixel((x, y), palette[symbol])
    save_texture('item/soul_storm_eye', eye)
    elements = [
        box([5, 14, 6], [11, 15.5, 11.5], 'short_underhead_join'),
        box([2.5, 15, 5], [13.5, 20, 12.5], 'lower_head'),
        box([2, 18, 5], [14, 24.5, 13], 'head'),
        box([3, 24.5, 5.75], [13, 25.5, 12.25], 'crown'),
        box([3.25, 25.5, 6.5], [12.75, 26, 11.5], 'crown_top'),
        box([2, 16, 4.5], [4.5, 19, 5.5], 'left_cheek'),
        box([11.5, 16, 4.5], [14, 19, 5.5], 'right_cheek'),
        box([4, 14.5, 4.25], [12, 16, 5.5], 'jaw'),
    ]
    # Recesses between the four short vertical ridges are visible below the eye.
    for i, x in enumerate((3.5, 6, 8.5, 11)):
        elements.append(box([x, 15.5, 4.25], [x+1, 18.5, 5.25], 'face_ridge_' + str(i)))
    # Dark bevel around an inset, single square purple eye. No glow or extra gems.
    elements.extend([
        box([5.75, 20, 4.5], [6.25, 24, 5.5], 'eye_left'),
        box([9.75, 20, 4.5], [10.25, 24, 5.5], 'eye_right'),
        box([6.25, 23.5, 4.5], [9.75, 24, 5.5], 'eye_top'),
        box([6.25, 20, 4.5], [9.75, 20.5, 5.5], 'eye_bottom'),
    ])
    gem = box([6.25, 20.5, 4.75], [9.75, 23.5, 5.125], 'purple_eye', 'eye')
    for face in gem['faces'].values():
        face['uv'] = [0, 0, 16, 16]
    elements.append(gem)
    elements = exposed_box_faces(elements)
    # Keep the accepted shallow vertical curve, but fan/curl each pair in XZ too.
    # Forge's built-in composite/elements loaders allow yaw plus the existing
    # per-cuboid pitch, preserving the exact head, skin, eye and display poses.
    textures = {'skin': 'deathsareitems:item/soul_storm_skin',
                'eye': 'deathsareitems:item/soul_storm_eye',
                'particle': 'deathsareitems:item/soul_storm_skin'}
    children = {'head': {'loader': 'forge:elements', 'textures': textures, 'elements': elements}}
    # Headings are in the source XZ plane: -Z is the eye/front side.
    # Front arms fan outward; rear arms sweep back; side arms curl forward.
    roots = (
        ('left', (3.5, 15, 8.5), (-180, -175, -160, -140, -120, -105)),
        ('right', (12.5, 15, 8.5), (0, -5, -20, -40, -60, -75)),
        ('front_left', (5.5, 15, 6), (-120, -125, -130, -135, -140, -145)),
        ('front_right', (10.5, 15, 6), (-60, -55, -50, -45, -40, -35)),
        ('back_left', (5.5, 15, 11.5), (145, 140, 130, 115, 100, 85)),
        ('back_right', (10.5, 15, 11.5), (35, 40, 50, 65, 80, 95)),
    )
    for name, root, headings in roots:
        start = root
        for j, (length, pitch, heading) in enumerate(zip(
                (2, 2.5, 3, 3, 2.5, 1.5), (-45, -22.5, 0, 22.5, 45, 67.5), headings)):
            radians, yaw = math.radians(pitch), math.radians(heading)
            horizontal = length * math.cos(radians)
            end = (start[0] + horizontal*math.cos(yaw),
                   start[1] + length*math.sin(radians),
                   start[2] + horizontal*math.sin(yaw))
            cx, cy, cz = ((a+b)/2 for a, b in zip(start, end))
            segment_name = f'{name}_{j}'
            segment = arm_segment((cx-horizontal/2, start[1], cz),
                                  (cx+horizontal/2, end[1], cz),
                                  3.1-j*.36, 3-j*.34, segment_name)
            children[segment_name] = {
                'loader': 'forge:elements', 'textures': textures,
                'transform': {'rotation': {'y': heading},
                              'origin': [cx/16, cy/16, (16-cz)/16]},
                'elements': [segment],
            }
            start = end
    save_json('item/soul_storm_base', {
        'loader': 'forge:composite',
        'textures': textures,
        'children': children,
        'display': {
            'gui': {'rotation': [8, -12, -5], 'translation': [0, -1, 0], 'scale': [.34, .34, .34]},
            'firstperson_righthand': {'rotation': [0, -8, 0], 'translation': [0, 1, -3], 'scale': [.65, .65, .65]},
            'firstperson_lefthand': {'rotation': [0, 8, 0], 'translation': [0, 1, -3], 'scale': [.65, .65, .65]},
            'thirdperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [.5, .5, .5]},
            'thirdperson_lefthand': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [.5, .5, .5]},
            'ground': {'translation': [0, 4, 0], 'scale': [.22, .22, .22]},
            'fixed': {'rotation': [0, 180, 0], 'scale': [.34, .34, .34]},
        },
    })


def build(client):
    with zipfile.ZipFile(client) as jar:
        def vanilla(name):
            return Image.open(BytesIO(jar.read('assets/minecraft/textures/' + name + '.png'))).convert('RGBA')

        def model(name):
            return json.loads(jar.read('assets/minecraft/models/' + name + '.json'))

        fire_pants(vanilla)
        bedrock(vanilla, model)
        pearl_shield(vanilla, model)
        soul_storm(vanilla)


if __name__ == '__main__':
    build(sys.argv[1])
