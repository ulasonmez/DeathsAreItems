"""Paint the reference mask's exact UV islands, without touching other item assets.

Run: python3 tools/build_drowning_mask_assets.py
Geometry and UV locations are read from DrowningMaskModel.java. All faces use
integer texel boundaries. The separate glass texture is uniformly transparent.
No face, eyes, skin colors or reflections are painted into the empty window.
"""
from pathlib import Path
import math
import re
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
MODEL = ROOT / 'src/main/java/co/blumeplugins/deathsareitems/client/DrowningMaskModel.java'
TEXTURE = ROOT / 'src/main/resources/assets/deathsareitems/textures/models/armor/drowning_mask.png'
GLASS_TEXTURE = TEXTURE.with_name('drowning_mask_visor.png')
BOX = re.compile(r'box\(head, sizes, "([^"]+)", (\d+), (\d+), ([^;]+)\);')


def parts():
    for match in BOX.finditer(MODEL.read_text()):
        name, u, v, numbers = match.groups()
        x, y, z, w, h, d = [float(n.strip().removesuffix('F')) for n in numbers.split(',')]
        dims = tuple(max(1, math.floor(n * 2 + .5)) for n in (w, h, d))
        yield name, int(u), int(v), dims, (x, y, z, w, h, d)


def face(name, direction, w, h):
    # Neutral graphite and gray from the empty-mask daylight reference. The cyan
    # patches seen on the swimmer belong to the SKIN behind the clear window.
    base = (47, 49, 53)
    if name in ('upper_brow', 'crown'):
        base = (59, 61, 65)
    elif name == 'receiver':
        base = (101, 104, 109)
    elif name == 'barrel':
        base = (116, 120, 125)
    elif name == 'receiver_underside':
        base = (45, 48, 53)
    elif name == 'muzzle':
        base = (49, 53, 58)
    elif 'strap' in name or 'window_post' in name:
        base = (26, 29, 33)
    im = Image.new('RGBA', (w, h))
    for y in range(h):
        for x in range(w):
            step = (0, 2, -1, 1)[((x // 3) + (y // 2)) % 4]
            if y == 0:
                step += 4
            im.putpixel((x, y), tuple(c + step for c in base) + (255,))
    draw = ImageDraw.Draw(im)
    if name == 'crown' and direction == 'up':
        # This is the dark, square, edged surface visible when the wearer swims.
        draw.rectangle((0, 0, w - 1, h - 1), fill=(66, 68, 72))
        draw.rectangle((1, 1, w - 2, h - 2), fill=(56, 58, 63))
        draw.line((1, 2, 1, h - 3), fill=(81, 84, 87))
        draw.line((2, h - 2, w - 4, h - 2), fill=(76, 80, 84))
        draw.line((w - 2, h - 5, w - 2, h - 2), fill=(76, 80, 84))
        draw.line((2, 0, 5, 0), fill=(79, 82, 84))
        draw.line((w - 6, 0, w - 3, 0), fill=(74, 77, 81))
    elif name == 'upper_brow' and direction in ('north', 'south'):
        # A thick horizontal charcoal brow; its bottom is a nearly black seal.
        for x in range(w):
            c = ((56, 58, 62), (47, 49, 53), (62, 64, 68), (51, 53, 57))[(x // 3) % 4]
            draw.line((x, 0, x, max(0, h - 2)), fill=c)
        draw.line((0, h - 1, w - 1, h - 1), fill=(16, 18, 21))
    elif name == 'lower_housing':
        if direction in ('north', 'south'):
            for x in range(w):
                c = ((47, 49, 53), (53, 55, 59), (43, 45, 49), (49, 51, 55))[(x // 3) % 4]
                draw.line((x, 1, x, h - 1), fill=c)
            draw.line((1, 0, w - 2, 0), fill=(67, 70, 74))
    elif name in ('left_cheek', 'right_cheek'):
        draw.line((0, h - 1, w - 1, h - 1), fill=(15, 17, 21))
    elif name == 'respirator_body':
        draw.line((0, h - 1, w - 1, h - 1), fill=(17, 20, 24))
        if direction == 'north':
            draw.line((1, 0, w - 2, 0), fill=(63, 66, 72))
    elif name == 'lower_keel':
        draw.line((0, h - 1, w - 1, h - 1), fill=(14, 17, 21))
    elif name in ('left_filter', 'right_filter'):
        draw.line((0, h - 2, w - 1, h - 2), fill=(62, 65, 71))
        if w > 3:
            draw.line((w - 2, 1, w - 2, h - 3), fill=(66, 70, 76))
    elif name.endswith(('face_guard', 'lower_guard', 'corner_return', 'temple_joint')):
        # A quiet raised edge and recessed outer seam on the cheek/temple armor.
        if direction in ('north', 'east', 'west'):
            edge = 0 if name.startswith('left_') else w - 1
            draw.line((edge, 0, edge, h - 1), fill=(58, 61, 66))
            if w > 2:
                draw.line((w - 1 - edge, 1, w - 1 - edge, h - 1), fill=(27, 30, 35))
        draw.line((0, h - 1, w - 1, h - 1), fill=(30, 33, 38))
    elif name == 'nose_bridge':
        draw.line((0, 0, w - 1, 0), fill=(22, 25, 29))
        if direction == 'north' and h > 2:
            draw.line((0, 1, w - 1, 1), fill=(59, 62, 67))
    elif name == 'socket':
        draw.line((0, 0, w - 1, 0), fill=(67, 71, 78))
    elif name == 'receiver':
        for x, y in ((0, 1), (3, 0), (2, 3), (0, 5)):
            if x < w and y < h:
                draw.rectangle((x, y, min(w - 1, x + 1), min(h - 1, y + 1)), fill=(70, 75, 82))
        if direction in ('west', 'east'):
            draw.line((0, h - 1, w - 1, h - 1), fill=(48, 53, 60))
    elif name == 'receiver_underside':
        draw.line((0, h - 1, w - 1, h - 1), fill=(23, 27, 33))
    elif name == 'collar':
        draw.line((0, h - 1, w - 1, h - 1), fill=(14, 18, 23))
    elif name == 'barrel':
        if direction in ('up', 'down'):
            for y in (1, 3):
                if y < h:
                    draw.line((0, y, w - 1, y), fill=(81, 87, 95))
        elif direction in ('west', 'east'):
            for x in (1, 3):
                if x < w:
                    draw.line((x, 0, x, h - 1), fill=(78, 85, 94))
    elif name == 'muzzle' and direction == 'north':
        draw.rectangle((0, 0, w - 1, h - 1), fill=(5, 8, 12))
    elif 'strap' in name:
        draw.line((0, h - 1, w - 1, h - 1), fill=(14, 17, 21))
    elif name == 'back_buckle' and w >= 3:
        draw.rectangle((1, 0, w - 2, h - 1), fill=(44, 48, 53))
    return im


def validate_geometry():
    """Guard the outer skin clearance and coplanar faces when editing the model."""
    faces = []
    count = 0
    for name, _, _, _, (x, y, z, w, h, d) in parts():
        lo, hi = (x, y, z), (x + w, y + h, z + d)
        assert min(w, h, d) > 0, f'Degenerate part: {name}'
        skin_lo, skin_hi = (-4.5, -8.5, -4.5), (4.5, .5, 4.5)
        overlap = [min(hi[i], skin_hi[i]) - max(lo[i], skin_lo[i]) for i in range(3)]
        assert min(overlap) <= 0, f'Part intersects the outer skin layer: {name}'
        for axis in range(3):
            for side in (-1, 1):
                plane = lo[axis] if side < 0 else hi[axis]
                faces.append((name, axis, side, plane, lo, hi))
        count += 1
    assert count == 33, f'Expected the complete 33-part mask, found {count}'
    names = {p[0] for p in parts()}
    assert not {'visor', 'lower_window'} & names, 'The eye window must stay open'
    # Both actual eye rows (-5..-3) must have an unobstructed central opening.
    for name, _, _, _, (x, y, z, w, h, d) in parts():
        in_eyes = min(y + h, -3) - max(y, -5) > 0
        in_center = min(x + w, 2.7) - max(x, -2.7) > 0
        assert not (in_eyes and in_center and z < -4.5), f'Part covers the eyes: {name}'
    for index, (name, axis, side, plane, lo, hi) in enumerate(faces):
        for other, other_axis, other_side, other_plane, other_lo, other_hi in faces[index + 1:]:
            if name == other or axis != other_axis or side != other_side:
                continue
            if abs(plane - other_plane) > .00001:
                continue
            overlap = [min(hi[i], other_hi[i]) - max(lo[i], other_lo[i])
                       for i in range(3) if i != axis]
            assert min(overlap) <= .00001, f'Coplanar overlapping surfaces: {name}, {other}'


def build():
    validate_geometry()
    atlas = Image.new('RGBA', (128, 128), (23, 30, 36, 255))
    occupied = set()
    count = 0
    for name, u, v, (w, h, d), _ in parts():
        bounds = (u + 2 * (w + d), v + d + h)
        assert bounds[0] <= 128 and bounds[1] <= 128, (name, bounds)
        island = {(x, y) for y in range(v, bounds[1]) for x in range(u, bounds[0])}
        assert not occupied.intersection(island), f'Overlapping UV island: {name}'
        occupied.update(island)
        rects = {
            'down': (u + d, v, w, d),
            'up': (u + d + w, v, w, d),
            'west': (u, v + d, d, h),
            'north': (u + d, v + d, w, h),
            'east': (u + d + w, v + d, d, h),
            'south': (u + 2 * d + w, v + d, w, h),
        }
        for direction, (x, y, fw, fh) in rects.items():
            tile = face(name, direction, fw, fh)
            if direction == 'up':
                tile = tile.transpose(Image.Transpose.FLIP_TOP_BOTTOM)
            atlas.paste(tile, (x, y))
        count += 1
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(TEXTURE)
    # Uniform faint green-gray tint only: no eyes or other opaque pixels.
    Image.new('RGBA', (16, 16), (173, 199, 189, 27)).save(GLASS_TEXTURE)
    print(f'Validated head clearance / coplanar faces; painted {count} cuboids / {count * 6} faces into {TEXTURE.relative_to(ROOT)}')


if __name__ == '__main__':
    build()
