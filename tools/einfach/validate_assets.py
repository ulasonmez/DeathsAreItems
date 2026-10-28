"""Check the complete Einfach asset set and preservation of supplied references."""
from pathlib import Path
from io import BytesIO
import json
import zipfile
from PIL import Image
from export_models import export, NAMES, ROOT, ASSETS, ART


def normalized(value):
    if isinstance(value, dict):
        return {k: normalized(v) for k,v in value.items() if k not in ('name',) and not (k=='rotation' and isinstance(v,dict) and v.get('angle')==0)}
    if isinstance(value,list): return [normalized(v) for v in value]
    if isinstance(value,float): return round(value,5)
    return value


def main():
    for name in NAMES:
        for target, data in export(name).items(): assert target.read_bytes()==data, target
    for name in ('cheese_slice', 'birch_splinters', 'vinys_bucket'):
        model=json.loads((ASSETS/f'models/item/{name}.json').read_text())
        assert model['parent']=='minecraft:item/generated' and 'elements' not in model
    for original, output in (('cheese.png','cheese_slice.png'),('book_cover.png','book_cover.png'),('magic_book.png','magic_book.png')):
        assert (ART/'references'/original).read_bytes()==(ASSETS/'textures/item'/output).read_bytes(), original
    source=json.loads((ART/'references/hamburger/hamburger.json').read_text())
    runtime=json.loads((ASSETS/'models/item/hamburger.json').read_text())
    assert source['elements']==runtime['elements'] and source['display']==runtime['display']
    assert (ART/'references/hamburger/hamburger.png').read_bytes()==(ASSETS/'textures/item/hamburger.png').read_bytes()
    source=json.loads((ART/'references/book_original.json').read_text())
    runtime=json.loads((ASSETS/'models/item/gustafs_new_book.json').read_text())
    assert normalized(source['elements'])==normalized(runtime['elements'])
    assert source['display']==runtime['display']
    dimensions={'birch_splinters':(16,16),'vinys_bucket':(16,16),'cheese_slice':(32,32),
                'hamburger':(16,16),'jukys_whistle':(16,16),'book_cover':(870,634),'magic_book':(32,32)}
    for name, size in dimensions.items():
        im=Image.open(ASSETS/f'textures/item/{name}.png');assert im.size==size,(name,im.size)
        if name in ('birch_splinters','vinys_bucket','jukys_whistle'):
            assert set(im.convert('RGBA').getchannel('A').getdata()) <= {0,255}
    for face in ('side','top'): assert Image.open(ASSETS/f'textures/block/gustafs_jukebox_{face}.png').size==(16,16)
    blockstate=json.loads((ASSETS/'blockstates/gustafs_jukebox.json').read_text())
    assert blockstate=={'variants':{'':{'model':'deathsareitems:block/gustafs_jukebox'}}}
    # Full local/vanilla parent and texture resolution, including inherited references.
    client=Path.home()/'.gradle/caches/forge_gradle/mcp_repo/de/oceanlabs/mcp/mcp_config/1.20.1-20230612.114412/joined/downloadClient/client.jar'
    with zipfile.ZipFile(client) as z:
        def resource(location,kind,ext):
            ns,path=location.split(':') if ':' in location else ('minecraft',location)
            key=f'assets/{ns}/{kind}/{path}.{ext}'
            return z.read(key) if ns=='minecraft' else (ROOT/'src/main/resources'/key).read_bytes()
        def inherit(location,chain=()):
            assert location not in chain
            if location in ('builtin/generated','minecraft:builtin/generated'): return {}
            model=json.loads(resource(location,'models','json'))
            textures=inherit(model['parent'],chain+(location,)) if 'parent' in model else {}
            textures.update(model.get('textures',{}))
            return textures
        def check(location):
            textures=inherit(location)
            for key,value in textures.items():
                seen=set()
                while value.startswith('#'):
                    assert value not in seen;seen.add(value);value=textures[value[1:]]
                Image.open(BytesIO(resource(value,'textures','png'))).verify()
        for name in ('birch_splinters','vinys_bucket','jukys_whistle','cheese_slice','hamburger','gustafs_new_book','gustafs_jukebox'):
            check('deathsareitems:item/'+name)
    for p in (ASSETS/'lang').glob('*.json'):
        assert json.loads(p.read_text())['item.deathsareitems.gustafs_new_book']=='EinfachGustaf_und_die_Wächter_der_Stille'
    staged=ART/'delivery/Einfach_textures.zip'
    with zipfile.ZipFile(staged) as z:
        assert z.testzip() is None
        for p in (ART/'delivery/Hamburger').rglob('*'):
            if p.is_file(): assert z.read(str(p.relative_to(ART/'delivery')))==p.read_bytes()
    print('PASS: 7 item models, 1 blockstate, PNG sizes/alpha, reference fidelity, source exports, 6 translations, delivery archive')


if __name__=='__main__': main()
