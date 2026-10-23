"""Build the five item assets in DeathsAreItemsModels2 from Minecraft 1.20.1.

Usage: python3 tools/build_models2_assets.py /path/to/1.20.1/client.jar
"""

from io import BytesIO
import json
from pathlib import Path
import sys
import zipfile

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/deathsareitems'
FACES = ('north', 'south', 'east', 'west', 'up', 'down')
CACTUS_ROWS = (
    '................',
    '................',
    '.11S12....22011.',
    '.13330....133S1.',
    '.233301..103332.',
    '.S23023113S032S.',
    '.12022323322022.',
    '.000S2323422100.',
    '...0323233231...',
    '...13243S3231...',
    '...0333243231...',
    '...0333233330...',
    '...0S33233220...',
    '....023232S0....',
    '.....232320.....',
    '................',
)
CACTUS_PALETTE = {
    '0': (29, 51, 9),
    '1': (36, 61, 11),
    '2': (43, 67, 14),
    '3': (49, 76, 17),
    '4': (56, 81, 20),
    'S': (126, 132, 100),
}
HUNGER_ROWS = (
    '..bbb...........',
    '.bWWb...........',
    '.bWWbOO.........',
    '..bBOrrO........',
    '..ORRRrrO.......',
    '..ORRrrccO......',
    '..OrrccCccO.....',
    '..OccCCAccO.....',
    '...OcCAAccO.....',
    '...OccAcccO.....',
    '....OccccOO.....',
    '.....OOOOObb....',
    '..........bWb...',
    '..........bWWb..',
    '...........bbb..',
    '................',
)
HUNGER_PALETTE = {
    'b': (150, 150, 124),
    'W': (231, 230, 207),
    'B': (195, 194, 164),
    'O': (69, 24, 9),
    'R': (184, 37, 24),
    'r': (151, 39, 20),
    'c': (133, 65, 28),
    'C': (159, 82, 35),
    'A': (173, 103, 46),
}


def save_texture(name, texture):
    target = ASSETS / 'textures' / (name + '.png')
    target.parent.mkdir(parents=True, exist_ok=True)
    texture.save(target)


def save_model(name, data):
    target = ASSETS / 'models/item' / (name + '.json')
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, indent=2) + '\n')


def pixel_image(rows, palette):
    texture = Image.new('RGBA', (16, 16))
    for pixel_y, row in enumerate(rows):
        if len(row) != 16:
            raise ValueError(f'Expected sixteen pixels: {row}')
        for pixel_x, symbol in enumerate(row):
            if symbol != '.':
                texture.putpixel((pixel_x, pixel_y), (*palette[symbol], 255))
    return texture


def cactus_armor(cactus, vanilla_mask):
    """Project the icon's body and shoulders onto their actual armor UV faces."""
    armor = Image.new('RGBA', (64, 32))
    thorn = (*CACTUS_PALETTE['S'], 255)

    def panel(source_rect, target_rect):
        source = cactus.crop(source_rect)
        u, v, U, V = target_rect
        width, height = U - u, V - v
        sw, sh = source.size
        opaque = [(x, y) for y in range(sh) for x in range(sw)
                  if source.getpixel((x, y))[3]]
        for y in range(height):
            for x in range(width):
                sx = min(sw - 1, int((x + .5) * sw / width))
                sy = min(sh - 1, int((y + .5) * sh / height))
                if not source.getpixel((sx, sy))[3]:
                    # The icon's silhouette must not cut extra holes into armor.
                    sx, sy = min(opaque, key=lambda p: (p[0]-sx)**2 + (p[1]-sy)**2)
                armor.putpixel((u + x, v + y), source.getpixel((sx, sy)))
        # Keep every thorn at its proportional icon position, including pixels
        # that nearest-neighbor sampling skips when ten body columns become eight.
        visible = [(x, y) for y in range(v, V) for x in range(u, U)
                   if vanilla_mask.getpixel((x, y))]
        for sy in range(sh):
            for sx in range(sw):
                if source.getpixel((sx, sy)) == thorn:
                    tx = u + min(width-1, int((sx+.5)*width/sw))
                    ty = v + min(height-1, int((sy+.5)*height/sh))
                    if visible:
                        # Keep hem thorns inside the shaped vanilla edge.
                        tx, ty = min(visible, key=lambda p: (p[0]-tx)**2 + (p[1]-ty)**2)
                        armor.putpixel((tx, ty), thorn)

    # Body: left side / front / right side / back, each twelve pixels high.
    panel((3, 3, 7, 15), (16, 20, 20, 32))
    panel((3, 3, 13, 15), (20, 20, 28, 32))
    panel((9, 3, 13, 15), (28, 20, 32, 32))
    panel((3, 3, 13, 15), (32, 20, 40, 32))
    panel((3, 2, 13, 6), (20, 16, 28, 20))
    panel((3, 11, 13, 15), (28, 16, 36, 20))
    # Vanilla shares mirrored arm UVs. Reuse the icon's shoulder pattern on all
    # four sleeve faces; its top thorn and lower side thorn stay on the shoulder.
    for u in (40, 44, 48, 52):
        panel((1, 2, 6, 8), (u, 20, u + 4, 25))
    panel((1, 2, 6, 6), (44, 16, 48, 20))
    # Apply the previously verified vanilla boundary last, including sleeve bottoms.
    for y in range(32):
        for x in range(64):
            alpha = vanilla_mask.getpixel((x, y)) if 16 <= x < 56 and 16 <= y < 32 else 0
            r, g, b, _ = armor.getpixel((x, y))
            armor.putpixel((x, y), (r, g, b, alpha))
    return armor


def box(lower, upper, texture, uv=None, rotation=None):
    element = {
        'from': lower,
        'to': upper,
        'faces': {
            face: {'texture': '#' + texture, 'uv': uv or [0, 0, 16, 16]}
            for face in FACES
        },
    }
    if rotation:
        element['rotation'] = rotation
    return element


def staff_wood_box(lower, upper):
    """Keep birch pixels square on every face instead of stretching a full tile."""
    width, height, depth = (b - a for a, b in zip(lower, upper))
    element = box(lower, upper, 'shaft')
    for face in ('north', 'south'):
        element['faces'][face]['uv'] = [0, 0, width, height]
    for face in ('east', 'west'):
        element['faces'][face]['uv'] = [0, 0, depth, height]
    for face in ('up', 'down'):
        element['faces'][face]['uv'] = [0, 0, width, depth]
    return element


def staff_model(hunger):
    # Two continuous tiles along the 30-unit shaft; no face at the shared seam.
    lower = staff_wood_box([6.5, -16, 6.5], [9.5, -2, 9.5])
    upper = staff_wood_box([6.5, -2, 6.5], [9.5, 14, 9.5])
    del lower['faces']['up']
    del upper['faces']['down']
    elements = [lower, upper]
    for step in range(4):
        inner = 1.5 + step * 1.5
        outer = inner + 1.5
        bottom = 11.5 + step * 2.5
        top = bottom + 3
        elements.extend([
            staff_wood_box([8 + inner, bottom, 6.5], [8 + outer, top, 9.5]),
            staff_wood_box([8 - outer, bottom, 6.5], [8 - inner, top, 9.5]),
            staff_wood_box([6.5, bottom, 8 + inner], [9.5, top, 8 + outer]),
            staff_wood_box([6.5, bottom, 8 - outer], [9.5, top, 8 - inner]),
        ])
    for pixel_y in range(16):
        for pixel_x in range(16):
            if not hunger.getpixel((pixel_x, pixel_y))[3]:
                continue
            element = box(
                [pixel_x + .5, 31 - pixel_y, 7.25],
                [pixel_x + 1.5, 32 - pixel_y, 8.75],
                'hunger', [pixel_x, pixel_y, pixel_x + 1, pixel_y + 1],
            )
            neighbors = {
                'west': (pixel_x - 1, pixel_y),
                'east': (pixel_x + 1, pixel_y),
                'up': (pixel_x, pixel_y - 1),
                'down': (pixel_x, pixel_y + 1),
            }
            for face, (neighbor_x, neighbor_y) in neighbors.items():
                if 0 <= neighbor_x < 16 and 0 <= neighbor_y < 16 and hunger.getpixel((neighbor_x, neighbor_y))[3]:
                    del element['faces'][face]
            elements.append(element)
    save_model('hunger_staff_base', {
        'gui_light': 'front',
        'textures': {
            'shaft': 'deathsareitems:item/hunger_staff_shaft',
            'hunger': 'deathsareitems:item/hunger_staff_head',
            'particle': 'deathsareitems:item/hunger_staff_shaft',
        },
        'elements': elements,
        'display': {
            'gui': {'rotation': [12, -25, -28], 'scale': [.30, .30, .30]},
            'firstperson_righthand': {'rotation': [0, -18, -8], 'translation': [1, 0, -2], 'scale': [.75, .75, .75]},
            'firstperson_lefthand': {'rotation': [0, 18, 8], 'translation': [1, 0, -2], 'scale': [.75, .75, .75]},
            'thirdperson_righthand': {'rotation': [0, -90, -90], 'translation': [0, 3, 0], 'scale': [.65, .65, .65]},
            'thirdperson_lefthand': {'rotation': [0, 90, 90], 'translation': [0, 3, 0], 'scale': [.65, .65, .65]},
            'ground': {'translation': [0, 4, 0], 'scale': [.22, .22, .22]},
            'fixed': {'rotation': [0, 180, 0], 'scale': [.30, .30, .30]},
        },
    })


def cannon_textures():
    """Native 256px atlas, drawn at four pixels per model unit. Never resized.

    Side, deck, end and lid panels have their own UV islands; a forty-unit
    surface gets 160 authored pixels instead of stretching a square tile.
    """
    atlas = Image.new('RGBA', (256, 256), (57, 58, 38, 255))
    draw = ImageDraw.Draw(atlas)
    islands = ((0, 0, 64, 160), (64, 0, 128, 160),
               (128, 0, 192, 64), (128, 64, 192, 128))
    for left, top, right, bottom in islands:
        for y in range(top, bottom):
            for x in range(left, right):
                # Broad painted plates with fine, deliberately authored scratches.
                stripe = ((x-left)//11 + (y-top)//29) % 4
                shade = (0, 3, -2, 1)[stripe]
                color = (76+shade, 77+shade, 52+shade)
                if (x*43+y*19) % 211 == 0:
                    color = (89, 89, 64)
                atlas.putpixel((x, y), (*color, 255))
        draw.rectangle((left, top, right-1, bottom-1), outline=(32, 34, 21), width=2)
        draw.line((left+3, top+3, left+3, bottom-4), fill=(112, 111, 82), width=2)
        draw.line((right-4, top+3, right-4, bottom-4), fill=(52, 54, 32), width=2)
        # Long panels: horizontal black seam with independently edged plates.
        if bottom-top == 160:
            draw.rectangle((left+7, top+6, left+10, bottom-7), fill=(26, 29, 18))
            for seam in (56, 132):
                draw.rectangle((left+10, top+seam, right-4, top+seam+2), fill=(31, 34, 21))
                draw.line((left+11, top+seam+3, right-5, top+seam+3), fill=(99, 100, 70))
            for yy in (18, 43, 75, 104, 145):
                for xx in (18, 52):
                    draw.rectangle((left+xx, top+yy, left+xx+2, top+yy+2), fill=(147, 148, 132))
                    draw.point((left+xx, top+yy), fill=(179, 178, 154))
        else:
            draw.rectangle((left+7, top+7, right-8, bottom-8), outline=(26, 29, 18), width=3)
            draw.rectangle((left+11, top+11, right-12, bottom-12), outline=(97, 98, 71), width=1)
    # Solid material islands for small geometric details, never a shrunken panel.
    draw.rectangle((192, 0, 207, 63), fill=(51, 53, 48))
    draw.line((193, 1, 193, 62), fill=(109, 111, 101), width=2)
    draw.line((204, 1, 204, 62), fill=(30, 32, 29), width=2)
    draw.rectangle((208, 0, 223, 15), fill=(26, 29, 19))
    draw.rectangle((224, 0, 239, 15), fill=(106, 107, 78))
    draw.rectangle((240, 0, 255, 15), fill=(151, 152, 134))
    save_texture('item/tnt_cannon_body', atlas)


def cannon_model():
    # UV coordinates refer to the native atlas; all other faces use material islands.
    def part(name, lower, upper, material='paint', panel=None, rotation=None):
        tiles = {'paint': [14, 0, 14.25, .25], 'dark': [13, 0, 13.5, .5],
                 'metal': [12, 0, 13, 4], 'rivet': [15, 0, 15.25, .25]}
        element = box(lower, upper, 'body', tiles[material], rotation)
        element['name'] = name
        if panel:
            face, uv = panel
            element['faces'][face]['uv'] = uv
            if face in ('west', 'east'):
                element['faces'][face]['rotation'] = 90
        return element

    elements = [
        part('left_wall', [0, 0, -14], [1.5, 16, 26], panel=('west', [0, 0, 4, 10])),
        part('right_wall', [14.5, 0, -14], [16, 16, 26], panel=('east', [0, 0, 4, 10])),
        part('floor', [1.5, 0, -14], [14.5, 1.5, 26], panel=('down', [4.375, 0, 7.625, 10])),
        part('roof', [1.5, 14.5, -14], [14.5, 16, 26], panel=('up', [4.375, 0, 7.625, 10])),
        part('rear_plate', [1.5, 1.5, 24.5], [14.5, 14.5, 26], panel=('south', [8.375, .375, 11.625, 3.625])),
        part('recess', [1.5, 1.5, -7], [14.5, 14.5, -5.5], 'dark'),
    ]
    # Edge rails, panel seams and proud rivets remain visible from grazing angles.
    for x in (-.2, 15.7):
        for y in (.15, 14.85):
            elements.append(part('edge_rail', [x, y, -13.5], [x+.5, y+1, 25.5]))
        elements.append(part('long_side_seam', [x, 11.75, -13.5], [x+.5, 12.2, 25.5], 'dark'))
        for z in (-.5, 18.5):
            elements.append(part('panel_band', [x-.05, .25, z], [x+.55, 15.75, z+.65], 'dark'))
        for z in (-10, -4, 5, 12, 22):
            for y in (3.25, 13.4):
                elements.append(part('raised_rivet', [x-.12, y, z], [x+.62, y+.55, z+.55], 'rivet'))
    # Four TNT blocks stand proud of the mouth, with separate wrapper, top and base UVs.
    for shell_x in (2, 8.5):
        for shell_y in (2, 8.5):
            shell = box([shell_x, shell_y, -15.1], [shell_x+5.5, shell_y+5.5, -7.25], 'tnt_side')
            shell['name'] = 'protruding_tnt'
            shell['faces']['up']['texture'] = '#tnt_top'
            shell['faces']['down']['texture'] = '#tnt_bottom'
            # The exposed end is a labelled wrapper, as on the reference.
            elements.append(shell)
    elements.extend([
        part('vertical_retainer', [7.65, 1.5, -15.5], [8.35, 14.5, -15.15], 'dark'),
        part('horizontal_retainer', [1.5, 7.65, -15.5], [14.5, 8.35, -15.15], 'dark'),
        part('side_mount', [-2, 4, 6], [0, 12, 15], 'metal'),
        part('handle_front', [5, -4, 7.5], [7, 0, 10], 'metal'),
        part('handle_rear', [5, -4, 18], [7, 0, 20.5], 'metal'),
        part('handle_grip', [5, -6, 7.5], [7, -4, 20.5], 'metal'),
    ])
    # Top hinge sits 2.5 units behind the lip. Lid opens forward and UP above it.
    hinge = {'origin': [8, 16, -11.5], 'axis': 'x', 'angle': -45}
    elements.extend([
        part('top_hinge', [1, 15.6, -12.2], [15, 16.5, -10.8], 'metal'),
        part('open_lid', [0, 16, -12.25], [16, 32, -11.5],
             panel=('north', [8, 4, 12, 8]), rotation=hinge),
        part('lid_inner', [.8, 16.8, -11.5], [15.2, 31.2, -11.25], 'dark', rotation=hinge),
    ])
    for lower, upper in (([1.5, 17.5, -11.25], [14.5, 18.3, -11.05]),
                         ([1.5, 29.7, -11.25], [14.5, 30.5, -11.05]),
                         ([1.5, 18.3, -11.25], [2.3, 29.7, -11.05]),
                         ([13.7, 18.3, -11.25], [14.5, 29.7, -11.05])):
        elements.append(part('lid_inner_frame', lower, upper, 'metal', rotation=hinge))
    save_model('tnt_cannon_base', {
        'textures': {
            'body': 'deathsareitems:item/tnt_cannon_body',
            'tnt_side': 'minecraft:block/tnt_side',
            'tnt_top': 'minecraft:block/tnt_top',
            'tnt_bottom': 'minecraft:block/tnt_bottom',
            'particle': 'deathsareitems:item/tnt_cannon_body',
        },
        'elements': elements,
        'display': {
            'gui': {'rotation': [32, 150, 0], 'translation': [0, 1, 0], 'scale': [.31, .31, .31]},
            'firstperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 1, -4], 'scale': [.65, .65, .65]},
            'firstperson_lefthand': {'rotation': [0, 0, 0], 'translation': [0, 1, -4], 'scale': [.65, .65, .65]},
            'thirdperson_righthand': {'rotation': [65, 0, 0], 'translation': [1.3, 7, 6], 'scale': [.65, .65, .65]},
            'thirdperson_lefthand': {'rotation': [65, 0, 0], 'translation': [1.3, 7, 6], 'scale': [.65, .65, .65]},
            'ground': {'rotation': [0, 0, 90], 'translation': [0, 4, 0], 'scale': [.25, .25, .25]},
            'fixed': {'rotation': [0, 90, 0], 'scale': [.31, .31, .31]},
        },
    })


def build(client):
    with zipfile.ZipFile(client) as jar:
        def vanilla(name):
            return Image.open(BytesIO(jar.read('assets/minecraft/textures/' + name + '.png'))).convert('RGBA')

        hunger = pixel_image(HUNGER_ROWS, HUNGER_PALETTE)
        save_texture('item/hunger_staff_head', hunger)
        save_texture('particle/hunger', hunger)
        hud = Image.new('RGBA', (48, 16))
        empty = hunger.copy()
        for pixel_y in range(16):
            for pixel_x in range(16):
                red, green, blue, alpha = empty.getpixel((pixel_x, pixel_y))
                if alpha:
                    shade = (red + green + blue) // 9
                    empty.putpixel((pixel_x, pixel_y), (shade, shade, shade, alpha))
        hud.paste(empty, (0, 0))
        hud.paste(hunger, (16, 0))
        half = empty.copy()
        half.paste(hunger.crop((0, 0, 8, 16)), (0, 0))
        hud.paste(half, (32, 0))
        save_texture('gui/hunger_staff', hud)

        shaft = vanilla('block/stripped_birch_log')
        save_texture('item/hunger_staff_shaft', shaft)
        staff_model(hunger)

        cactus = pixel_image(CACTUS_ROWS, CACTUS_PALETTE)
        original = vanilla('item/iron_chestplate')
        if cactus.getchannel('A').tobytes() != original.getchannel('A').tobytes():
            raise ValueError('The cactus chestplate must retain the vanilla chestplate silhouette')
        save_texture('item/cactus_chestplate', cactus)
        mask = vanilla('models/armor/iron_layer_1').getchannel('A')
        save_texture('models/armor/cactus_chestplate_layer_1', cactus_armor(cactus, mask))
        save_model('cactus_chestplate_base', {
            'parent': 'minecraft:item/generated',
            'textures': {'layer0': 'deathsareitems:item/cactus_chestplate'},
        })

        cannon_textures()
        cannon_model()

        for name, particle in (('swarm_of_anvils', 'minecraft:block/anvil'), ('entity_crammer', 'minecraft:block/pink_terracotta')):
            hand_rotation = [75, 0, 0] if name == 'swarm_of_anvils' else [0, 0, 0]
            save_model(name + '_base', {
                'parent': 'minecraft:builtin/entity',
                'gui_light': 'side',
                'textures': {'particle': particle},
                'display': {
                    'ground': {'translation': [0, 3, 0], 'scale': [.35, .35, .35]},
                    'fixed': {'rotation': [0, 180, 0], 'scale': [.65, .65, .65]},
                    'thirdperson_righthand': {'rotation': hand_rotation, 'translation': [0, 2.5, 0]},
                    'thirdperson_lefthand': {'rotation': hand_rotation, 'translation': [0, 2.5, 0]},
                },
            })


if __name__ == '__main__':
    build(sys.argv[1])
