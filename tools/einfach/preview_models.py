"""Offline nearest-texel cuboid viewer using runtime JSON and native PNGs.
This checks geometry/UV; it does not simulate Minecraft shaders, hands or armor animation.
"""
from pathlib import Path
from zipfile import ZipFile
from io import BytesIO
import json, math
import numpy as np
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[2]
ASSETS=ROOT/'src/main/resources/assets/deathsareitems'
OUT=ROOT/'art/models/einfach/previews'
REFERENCES=ROOT/'art/models/einfach/references'

def texture(location):
    ns,path=location.split(':')
    if ns=='minecraft':
        return Image.open(REFERENCES/f'{Path(path).name}.png').convert('RGBA')
    return Image.open(ASSETS/f'textures/{path}.png').convert('RGBA')

def matrix(angles):
    x,y,z=np.radians(angles)
    X=np.array([[1,0,0],[0,np.cos(x),-np.sin(x)],[0,np.sin(x),np.cos(x)]])
    Y=np.array([[np.cos(y),0,np.sin(y)],[0,1,0],[-np.sin(y),0,np.cos(y)]])
    Z=np.array([[np.cos(z),-np.sin(z),0],[np.sin(z),np.cos(z),0],[0,0,1]])
    return X@Y@Z

def mesh(name):
    m=json.loads((ASSETS/f'models/item/{name}.json').read_text());tex=m['textures']
    def resolve(key):
        while key.startswith('#'): key=tex[key[1:]]
        return texture(key)
    faces=[]
    for e in m['elements']:
        x,y,z=e['from'];X,Y,Z=e['to']
        # Vanilla 1.20.1 FaceInfo order; BlockFaceUV maps TL, BL, BR, TR.
        points={'north':[(X,Y,z),(X,y,z),(x,y,z),(x,Y,z)],
                'south':[(x,Y,Z),(x,y,Z),(X,y,Z),(X,Y,Z)],
                'west':[(x,Y,z),(x,y,z),(x,y,Z),(x,Y,Z)],
                'east':[(X,Y,Z),(X,y,Z),(X,y,z),(X,Y,z)],
                'up':[(x,Y,z),(x,Y,Z),(X,Y,Z),(X,Y,z)],
                'down':[(x,y,Z),(x,y,z),(X,y,z),(X,y,Z)]}
        for side,f in e['faces'].items():
            im=resolve(f['texture']);a,b,c,d=f['uv']
            uv=np.array([(a,b),(a,d),(c,d),(c,b)])*np.array([im.width/16,im.height/16])
            uv=np.roll(uv,-f.get('rotation',0)//90,axis=0)
            p=np.array(points[side],float)
            if 'rotation' in e:
                r=e['rotation'];angles=[0,0,0];axis='xyz'.index(r['axis']);angles[axis]=r['angle']
                p=(p-r['origin'])@matrix(angles).T+r['origin']
            faces.append((p,uv,im))
    return faces,m

def transform(faces,pose):
    result=[]
    for p,uv,im in faces:
        q=((p-8)*pose.get('scale',[1]*3))@matrix(pose.get('rotation',[0]*3)).T+pose.get('translation',[0]*3)
        result.append((q,uv,im))
    return result

def render(faces,size=(300,260),yaw=35,pitch=25,scale=15,center=(0,0,0),checker=False):
    a,b=np.radians([yaw,pitch]);toward=np.array([np.sin(a)*np.cos(b),np.sin(b),-np.cos(a)*np.cos(b)])
    right=-np.array([np.cos(a),0,np.sin(a)]);up=np.cross(toward,right)
    rot=np.array([right,-up,toward]);w,h=size
    out=np.empty((h,w,4),dtype=np.uint8);out[:]=[38,43,51,255];depth=np.full((h,w),-1e10)
    for points,uv,im in faces:
        normal=np.cross(points[1]-points[0],points[2]-points[0]);normal/=np.linalg.norm(normal) or 1
        # Quad order in FaceInfo is clockwise when looking inward.
        if np.dot(normal,toward)<0:continue
        p=(points-center)@rot.T;p[:,:2]=p[:,:2]*scale+[w/2,h/2]
        tex=np.array(im)
        if checker:
            yy,xx=np.indices(tex.shape[:2]);tex=tex.copy();tex[:,:,:3]=np.where(((xx+yy)%2)[...,None],220,65)
        light=.65+.23*max(0,normal[1])+.12*abs(normal[0])
        for ids in [(0,1,2),(0,2,3)]:
            q=p[list(ids)];uvt=uv[list(ids)]
            x0,y0=np.maximum(np.floor(q[:,:2].min(0)).astype(int),0);x1,y1=np.minimum(np.ceil(q[:,:2].max(0)).astype(int),[w-1,h-1])
            if x1<x0 or y1<y0:continue
            xx,yy=np.meshgrid(np.arange(x0,x1+1)+.5,np.arange(y0,y1+1)+.5)
            mat=np.vstack([q[:,:2].T,np.ones(3)])
            if abs(np.linalg.det(mat))<1e-8:continue
            weights=np.linalg.inv(mat)@np.array([xx.ravel(),yy.ravel(),np.ones(xx.size)])
            zz=weights.T@q[:,2];coords=weights.T@uvt
            tx=np.clip(coords[:,0].astype(int),0,tex.shape[1]-1);ty=np.clip(coords[:,1].astype(int),0,tex.shape[0]-1)
            col=tex[ty,tx].copy();mask=(weights.min(0)>=-1e-6)&(zz>depth[y0:y1+1,x0:x1+1].ravel())&(col[:,3]>127)
            col[:,:3]=(col[:,:3]*light).astype('uint8')
            oy,ox=np.where(mask.reshape(xx.shape));out[y0+oy,x0+ox]=col[mask];depth[y0+oy,x0+ox]=zz[mask]
    return Image.fromarray(out)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    names = ('hamburger', 'gustafs_new_book', 'jukys_whistle')
    sheet = Image.new('RGB', (900, 620), '#262b33'); d = ImageDraw.Draw(sheet)
    for i, name in enumerate(names):
        faces, model = mesh(name)
        for row, (yaw, pitch) in enumerate(((35, 25), (215, 25))):
            im = render(transform(faces, {}), yaw=yaw, pitch=pitch, scale=13)
            sheet.paste(im.convert('RGB'), (i*300, row*290+30))
            d.text((i*300+5, row*290+10), name + (' / north-east' if row==0 else ' / south-west'), fill='white')
        strip = Image.new('RGB', (1500, 310), '#262b33'); sd = ImageDraw.Draw(strip)
        for j, key in enumerate(('gui', 'firstperson_righthand', 'firstperson_lefthand', 'thirdperson_righthand', 'thirdperson_lefthand')):
            im = render(transform(faces, model['display'].get(key, {})), yaw=180, pitch=0, scale=13)
            strip.paste(im.convert('RGB'), (j*300, 25)); sd.text((j*300+5, 8), key, fill='white')
        sd.text((8,290), 'Offline transforms; no player arms, perspective camera or Minecraft lighting.', fill='white')
        strip.save(OUT/f'{name}-display.png')
        render(transform(faces, {}), checker=True).save(OUT/f'{name}-checker.png')
    d.text((10,600), 'Runtime JSON/UV + native PNGs / offline viewer / same camera scale / not Minecraft screenshots', fill='white')
    sheet.save(OUT/'models.png')
    sheet = Image.new('RGB', (1000,310), '#30343b'); d = ImageDraw.Draw(sheet)
    for i, name in enumerate(('birch_splinters','vinys_bucket','cheese_slice','hamburger','jukys_whistle')):
        im = Image.open(ASSETS/f'textures/item/{name}.png').convert('RGBA').resize((192,192),Image.Resampling.NEAREST)
        sheet.paste(im,(i*200,30),im);d.text((i*200+5,5),name,fill='white')
    for i, name in enumerate(('gustafs_jukebox_side','gustafs_jukebox_top')):
        im = Image.open(ASSETS/f'textures/block/{name}.png').resize((80,80),Image.Resampling.NEAREST)
        sheet.paste(im,(i*200,225));d.text((i*200+85,250),'jukebox '+('side' if i==0 else 'top'),fill='white')
    sheet.save(OUT/'textures.png')


if __name__ == '__main__': main()

