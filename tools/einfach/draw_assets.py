"""Deterministic native-resolution Einfach textures; vanilla inputs are read-only.

Run with Python Pillow. This never overwrites editable .bbmodel projects.
"""
from pathlib import Path
import json
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ART = ROOT / 'art/models/einfach'
ASSETS = ROOT / 'src/main/resources/assets/deathsareitems'


def save(im, path):
    target = ASSETS / f'textures/{path}.png'
    target.parent.mkdir(parents=True, exist_ok=True)
    im.save(target)


def model(path, data):
    target = ASSETS / f'{path}.json'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, indent=2) + '\n')


def main():
    # Two separate jagged chips: pale birch, grey bark flecks and warm cut edges.
    rows = [
        '................', '..........o.....', '.........olo....',
        '...o.....olho...', '...lo...olhho...', '..olho..olhho...',
        '..olhho.olbho...', '..olbho.olhho...', '...olho.olho....',
        '...olho.olbo....', '...olho.olho....', '....olo.olho....',
        '....olo..oo.....', '.....o..........', '................',
        '................',
    ]
    palette = {'o': '#6b665c', 'l': '#eae4cd', 'h': '#bdb28c', 'b': '#555653'}
    splinters = Image.new('RGBA', (16, 16))
    for y, row in enumerate(rows):
        for x, value in enumerate(row):
            if value != '.': splinters.putpixel((x, y), tuple(bytes.fromhex(palette[value][1:])) + (255,))
    save(splinters, 'item/birch_splinters')

    # Preserve every metal/water pixel and the original vanilla bucket silhouette.
    bucket = Image.open(ART / 'references/axolotl_bucket.png').convert('RGBA')
    mapping = {
        (255, 198, 233): (171, 167, 255),
        (255, 173, 222): (143, 137, 249),
        (255, 153, 214): (132, 127, 241),
    }
    # Palette mapping from the actual icon (not a mask over the metal bucket).
    colours = sorted({bucket.getpixel((x,y))[:3] for y in range(10) for x in range(16)
                      if bucket.getpixel((x,y))[3] and bucket.getpixel((x,y))[0] > bucket.getpixel((x,y))[1] + 15})
    for c in colours:
        r,g,b = c
        if r > 220 and g > 130: mapping[c] = (171, 167, 255) if g > 190 else (143, 137, 249)
        elif r > 170: mapping[c] = (105, 83, 222)
        else: mapping[c] = (77, 54, 173)
    for y in range(16):
        for x in range(16):
            r,g,b,a = bucket.getpixel((x,y))
            if (r,g,b) in mapping: bucket.putpixel((x,y), (*mapping[(r,g,b)],a))
    save(bucket, 'item/vinys_bucket')

    # User-provided cheese is deliberately kept byte-for-byte, 2D and 32x32.
    (ASSETS/'textures/item/cheese_slice.png').write_bytes((ART/'references/cheese.png').read_bytes())
    for item in ('birch_splinters', 'vinys_bucket', 'cheese_slice'):
        model(f'models/item/{item}', {'parent': 'minecraft:item/generated',
              'textures': {'layer0': f'deathsareitems:item/{item}'}})

    # Vanilla 16x16 jukebox layout, with a navy cabinet and brass woven grille.
    original_side = Image.open(ART/'references/jukebox_side.png').convert('RGBA')
    original_top = Image.open(ART/'references/jukebox_top.png').convert('RGBA')
    side, top = original_side.copy(), original_top.copy()
    for im in (side, top):
        for y in range(16):
            for x in range(16):
                r,g,b,a = im.getpixel((x,y))
                v = (r+g+b)/3
                c = (22,28,43) if v < 42 else (34,44,65) if v < 65 else (46,61,85) if v < 92 else (61,79,105) if v < 116 else (82,103,130)
                im.putpixel((x,y), (*c,a))
    d = ImageDraw.Draw(side)
    for y in range(2,14):
        for x in range(2,14):
            old = original_side.getpixel((x,y))
            d.point((x,y), fill='#b08a47' if old[0]>130 else '#6d542f' if old[0]>90 else '#302d27')
    for x,y in ((1,1),(14,1),(1,14),(14,14)):
        d.point((x,y), fill='#d1b36a')
    d = ImageDraw.Draw(top)
    # Keep the actual disc slot and its dark bevel; brass rails border it.
    for y in range(4,12):
        for x in range(6,9): top.putpixel((x,y),original_top.getpixel((x,y)))
    d.line((4,3,4,12),fill='#9c7c43');d.line((11,3,11,12),fill='#9c7c43')
    for x,y in ((1,1),(14,1),(1,14),(14,14)): d.point((x,y),fill='#d1b36a')
    save(side,'block/gustafs_jukebox_side');save(top,'block/gustafs_jukebox_top')
    model('models/block/gustafs_jukebox', {'parent':'minecraft:block/cube_bottom_top','textures':{
        'side':'deathsareitems:block/gustafs_jukebox_side',
        'top':'deathsareitems:block/gustafs_jukebox_top','bottom':'minecraft:block/birch_planks'}})
    model('models/item/gustafs_jukebox', {'parent':'deathsareitems:block/gustafs_jukebox'})
    model('blockstates/gustafs_jukebox', {'variants':{'':{'model':'deathsareitems:block/gustafs_jukebox'}}})


if __name__ == '__main__': main()
