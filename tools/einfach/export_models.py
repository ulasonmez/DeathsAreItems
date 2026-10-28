"""Export supported static java_block projects without touching gameplay or animation code.
Default is review-only. --write applies; --check verifies generated resources are current.
.bbmodel is authoritative for 3D geometry, UV, embedded textures, and Display transforms.
"""
from pathlib import Path
import argparse, base64, json, math, sys
ROOT=Path(__file__).resolve().parents[2]
ART=ROOT/'art/models/einfach'
ASSETS=ROOT/'src/main/resources/assets/deathsareitems'
NAMES=('hamburger','gustafs_new_book','jukys_whistle')
FACES={'north','south','east','west','up','down'}
SLOTS={'gui','firstperson_righthand','firstperson_lefthand','thirdperson_righthand','thirdperson_lefthand','ground','fixed','head','none'}

def numbers(v,n):
    assert isinstance(v,list) and len(v)==n and all(isinstance(x,(int,float)) and math.isfinite(x) for x in v),v
    return v

def export(name):
    p=json.loads((ART/f'projects/{name}.bbmodel').read_text())
    assert p['meta']['model_format']=='java_block'
    assert not p.get('animations'), 'Animated projects need a different exporter'
    assert p.get('model_identifier',name)==name
    assert all(isinstance(node,str) for node in p['outliner']), 'Groups need explicit transform handling'
    meta=json.loads((ART/'runtime.json').read_text())[name]
    out={k:v for k,v in meta.items() if k in ('parent','ambientocclusion','gui_light','render_type')}
    out['gui_light']=('front' if p['front_gui_light'] else 'side') if 'front_gui_light' in p else p.get('gui_light',out.get('gui_light','side'))
    textures={};files={};uvsizes={}
    for i,t in enumerate(p['textures']):
        key=str(i);namespace=t.get('namespace','deathsareitems')
        folder=t.get('folder','item');stem=Path(t['name']).stem
        assert namespace in ('deathsareitems','minecraft')
        assert '/' not in stem and '..' not in folder
        textures[key]=f'{namespace}:{folder}/{stem}'
        uvsizes[i]=(t.get('uv_width',p['resolution']['width']),t.get('uv_height',p['resolution']['height']))
        if namespace=='deathsareitems':
            source=t['source'];assert source.startswith('data:image/png;base64,')
            files[ASSETS/f'textures/{folder}/{stem}.png']=base64.b64decode(source.split(',',1)[1],validate=True)
    textures['particle']='#1' if name=='gustafs_new_book' else '#0'
    out['textures']=textures;out['elements']=[]
    ids=set()
    for e in p['elements']:
        assert e.get('type','cube')=='cube' and e['uuid'] not in ids
        ids.add(e['uuid'])
        assert e.get('visibility',True) and e.get('export',True), 'Hidden/excluded cubes need explicit handling'
        lo=numbers(e['from'],3);hi=numbers(e['to'],3)
        assert all(-16<=a<=b<=32 for a,b in zip(lo,hi))
        el={'name':e['name'],'from':lo,'to':hi,'faces':{}}
        rotation=numbers(e.get('rotation',[0,0,0]),3)
        axes=[i for i,a in enumerate(rotation) if a]
        assert len(axes)<=1,'Java item cuboids only support one rotation axis'
        if axes:
            i=axes[0];assert rotation[i] in (-45,-22.5,22.5,45)
            el['rotation']=dict(origin=numbers(e['origin'],3),axis='xyz'[i],angle=rotation[i],rescale=e.get('rescale',False))
        if 'shade' in e: el['shade']=e['shade']
        for face,f in e['faces'].items():
            assert face in FACES
            if f.get('texture') is None:continue
            index=f['texture'];assert isinstance(index,int) and str(index) in textures
            uv=numbers(f['uv'],4);w,h=uvsizes[index]
            assert all(0<=v<=limit for v,limit in zip(uv,[w,h,w,h]))
            v=dict(uv=[uv[0]*16/w,uv[1]*16/h,uv[2]*16/w,uv[3]*16/h],texture='#'+str(index))
            if f.get('rotation'):
                assert f['rotation'] in (90,180,270);v['rotation']=f['rotation']
            if f.get('cullface'):
                assert f['cullface'] in FACES;v['cullface']=f['cullface']
            el['faces'][face]=v
        assert el['faces'];out['elements'].append(el)
    assert set(p['outliner'])==ids
    out['display']={}
    for slot,pose in p.get('display',{}).items():
        assert slot in SLOTS
        assert not set(pose)-{'rotation','translation','scale'}, 'Unsupported Display field'
        for k,v in pose.items():
            numbers(v,3)
            if k=='translation': assert all(abs(a)<=80 for a in v)
            if k=='scale': assert all(0<=a<=4 for a in v)
        out['display'][slot]=pose
    files[ASSETS/f'models/item/{name}.json']=(json.dumps(out,ensure_ascii=False,indent=2)+'\n').encode()
    return files

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write',action='store_true');parser.add_argument('--check',action='store_true')
    parser.add_argument('items',nargs='*',metavar='ITEM')
    args=parser.parse_args();stale=[];outputs={}
    if set(args.items)-set(NAMES):parser.error('Supported items: '+', '.join(NAMES))
    for name in args.items or NAMES: outputs.update(export(name))
    # Validate every input before writing any file.
    for target,data in outputs.items():
        if not target.exists() or target.read_bytes()!=data:
            stale.append(str(target.relative_to(ROOT)))
            if args.write:
                target.parent.mkdir(parents=True,exist_ok=True)
                temp=target.with_suffix(target.suffix+'.tmp');temp.write_bytes(data);temp.replace(target)
    print(('Updated' if args.write else 'Different')+f': {len(stale)} file(s)')
    for path in stale:print(path)
    if args.check and stale:sys.exit(1)
if __name__=='__main__':main()
