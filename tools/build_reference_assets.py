"""Deterministic pixel edits of Minecraft 1.20.1 assets for the supplied model reference.

Usage: python3 tools/build_reference_assets.py /path/to/1.20.1/client.jar
The vanilla client is an input only; no downloaded/generated artwork is used.
"""
from pathlib import Path
from io import BytesIO
import json
import sys
import zipfile
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/deathsareitems'


def build(client):
    with zipfile.ZipFile(client) as jar:
        def vanilla(name):
            return Image.open(BytesIO(jar.read('assets/minecraft/textures/' + name + '.png'))).convert('RGBA')
        def save(im, name):
            path = ASSETS / 'textures' / (name + '.png')
            path.parent.mkdir(parents=True, exist_ok=True)
            im.save(path)

        # Retain the vanilla boots silhouette. Reference: ivory rim, muted ochre inset.
        boots = vanilla('item/golden_boots')
        gold = {(255,255,180):(185,142,39), (255,255,75):(174,128,31),
                (255,236,79):(161,115,25), (255,205,48):(151,104,20),
                (221,165,30):(142,95,19), (148,107,12):(117,80,17),
                (104,73,9):(96,70,22)}
        original = boots.copy()
        for y in range(16):
            for x in range(16):
                r,g,b,a = original.getpixel((x,y))
                if not a: continue
                # Left light edge, top cuff and toe edging, as in the supplied icon.
                edge = x == 0 or not original.getpixel((x-1,y))[3]
                cuff = y == 0 or not original.getpixel((x,y-1))[3]
                if edge or cuff:
                    c = (229,229,215) if cuff else (192,194,184)
                elif y == 14:
                    c = (111,112,105)
                else:
                    c = gold.get((r,g,b), (max(87,int(r*.62)),max(65,int(g*.49)),max(18,int(b*.45))))
                boots.putpixel((x,y),(*c,a))
        save(boots, 'item/fall_damage_boots')

        handle = vanilla('item/wooden_pickaxe')
        for y in range(16):
            for x in range(16):
                if y < 6 or x > 9 or abs(x+y-15) > 1:
                    handle.putpixel((x,y),(0,0,0,0))
        save(handle, 'item/mob_pickaxe_handle')

        # Original iron armor UV layout and shading, with ochre panels on each boot face.
        armor = vanilla('models/armor/iron_layer_1')
        # Vanilla boot leg: side/front/side/back at u=0/4/8/12, v=20..31.
        for y in range(20,32):
            for x in range(16):
                old = armor.getpixel((x,y))
                if y < 25:
                    armor.putpixel((x,y),(0,0,0,0))
                elif y <= 30 and x%4 in (1,2):
                    shade = sum(old[:3])/3/255
                    armor.putpixel((x,y),(int(158*shade),int(113*shade),int(29*shade),old[3]))
        save(armor, 'models/armor/fall_damage_boots_layer_1')

        # A charcoal pixel atlas for the mask, using the neutral vanilla black-concrete tones.
        mask = vanilla('block/black_concrete').resize((64,64),Image.Resampling.NEAREST)
        for y in range(64):
            for x in range(64):
                v = (30,32,34,31)[((x//4)+(y//3))%4]
                mask.putpixel((x,y),(v,v,v,255))
        save(mask, 'models/armor/drowning_mask')
        glass = Image.new('RGBA',(16,16),(163,207,211,39))
        save(glass, 'models/armor/drowning_mask_visor')

        # Sand is the continuous base; gravel occurs in irregular, coarse connected patches.
        sand, gravel = vanilla('block/sand'), vanilla('block/gravel')
        patches = ['................','......ggg.......','.....gggg...g...',
                   '......gg....gg..','...........ggg..','gg........ggggg.',
                   'ggg......ggg....','.gggg...ggg.....','...ggggggg......',
                   '....ggggg.......','....ggg.........','...gggg..gg.....',
                   '..gggg...ggg....','.ggg......ggg...','gg.........ggg..',
                   'g...........gg..']
        for variant in range(3):
            tile = sand.copy()
            for y in range(16):
                for x in range(16):
                    if patches[(y+variant*5)%16][(x+variant*3)%16] == 'g':
                        tile.putpixel((x,y),gravel.getpixel((x,y)))
            save(tile, 'item/suffocation_boulder_'+str(variant))

    def model(name,data):
        path=ASSETS/'models/item'/f'{name}.json';path.parent.mkdir(parents=True,exist_ok=True)
        path.write_text(json.dumps(data,indent=2)+'\n')
    model('fall_damage_boots_base',{'parent':'minecraft:item/generated','textures':{'layer0':'deathsareitems:item/fall_damage_boots'}})
    model('mob_pickaxe_handle',{'parent':'minecraft:item/generated','textures':{'layer0':'deathsareitems:item/mob_pickaxe_handle'}})
    # Built-in renderer composes the unmodified baked vanilla mob models.
    common={'gui':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]},
            'ground':{'translation':[0,3,0],'scale':[.35,.35,.35]},
            'fixed':{'rotation':[0,180,0],'scale':[.6,.6,.6]}}
    for name in ['drowning_mask','skeleton_bow','mob_pickaxe']:
        model(name+'_base',{'parent':'minecraft:builtin/entity','gui_light':'side','display':common})
    textures={str(i):'deathsareitems:item/suffocation_boulder_'+str(i) for i in range(3)}
    textures['particle']=textures['0']
    pieces=[([2,3,2],[14,13,14],0,None),([3,2,3],[13,14,13],1,None),
            ([1.5,4,3],[14.5,12,13],2,('y',22.5)),
            ([3,3,2],[13,13,14],0,('z',22.5)),
            ([3,2.5,3],[13,13.5,13],1,('x',-22.5)),
            ([2.5,5,1.5],[13.5,11,14.5],2,('y',-22.5))]
    elements=[]
    for lo,hi,tex,rot in pieces:
        e={'from':lo,'to':hi,'faces':{face:{'uv':[0,0,16,16],'texture':'#'+str((tex+j)%3)} for j,face in enumerate(['north','south','east','west','up','down'])}}
        if rot:e['rotation']={'origin':[8,8,8],'axis':rot[0],'angle':rot[1]}
        elements.append(e)
    model('suffocation_boulder_base',{'textures':textures,'elements':elements,'display':{
        'gui':{'rotation':[25,35,0],'scale':[.85,.85,.85]},
        'firstperson_righthand':{'rotation':[0,25,-12],'translation':[0,1,-2],'scale':[.78,.78,.78]},
        'firstperson_lefthand':{'rotation':[0,-25,12],'translation':[0,1,-2],'scale':[.78,.78,.78]},
        'thirdperson_righthand':{'rotation':[35,20,0],'translation':[0,2,0],'scale':[.65,.65,.65]},
        'thirdperson_lefthand':{'rotation':[35,-20,0],'translation':[0,2,0],'scale':[.65,.65,.65]},
        'ground':{'translation':[0,3,0],'scale':[.45,.45,.45]},'fixed':{'scale':[.8,.8,.8]}}})

if __name__=='__main__':
    build(sys.argv[1])
