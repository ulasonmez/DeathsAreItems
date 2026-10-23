# Soul Storm particle assets

## Current rendering

- White souls reference all eleven `minecraft:soul_0` through `minecraft:soul_10` frames. The `soul_monochrome` shader neutralizes RGB while preserving the animation, shading and alpha; the old standalone `white_soul.png` is no longer referenced.
- Purple-black waves reference all sixteen vanilla `sonic_boom` frames. The same shader removes cyan before the particle applies a dark purple/black tint. No vanilla cyan sonic-boom particle is emitted.
- Purple flames use the generated `src/main/resources/assets/deathsareitems/textures/particle/soul_flame.png`, framed and sampled to 16×16 with nearest-neighbor sampling. The pixel-flame render pass discards soft alpha below .8 and makes surviving sprite pixels opaque before lifetime fading.
- Black smoke uses vanilla generic particle sprites tinted nearly black.

The Soul Storm item follows page 2 of `DeathsAreItemsModels3.pdf`: a stepped head, four recessed face ridges and one inset purple eye. The head and eye are preserved exactly in the screenshot revision; six tapered, segmented octopus arms attach directly beneath the head (two front, two side, two back). The long neck and torso are removed; each arm dips slightly, then curls just above its attachment height. The front pair fans left/right, the rear pair sweeps backward, and the side pair curls forward. Built-in Forge composite/element models combine segment yaw with pitch without altering the head mesh or textures. The old staff shaft and extra amethyst block were removed. `tools/build_models3_assets.py` builds the JSON mesh and its restrained dark obsidian-derived skin and pixel eye textures; no image generation is used for this item model. Its built-in enchantment glint is disabled so the reference colors remain visible. Shader assets are in `src/main/resources/assets/deathsareitems/shaders/core/`.

## References inspected

- `/Users/ulassonmez/Documents/DeathsAreItems4.pdf`, pages 1–2: coarse purple flame spiral, flowing white souls with intermittent glass, dark projectile effects.
- Local Minecraft 1.20.1 client resources: original soul and sonic-boom animation frame lists.

## Purple flame generation

Built-in `image_gen` was used for generation and a targeted edit. The selected generated source is `exec-b38591c2-40a5-4a50-809b-54b9205e0824.png`; it was inspected before nearest-neighbor optimization. Transparency is retained in the saved PNG; the game shader performs the hard alpha cutoff.

Initial prompt:

> Use case: stylized-concept. Asset: one transparent pixel-art Minecraft soul-storm purple flame sprite. Create exactly ONE tiny upright dark violet ghostly flame glyph, on transparent background. Extremely coarse crisp pixel art, authored as an 8 by 8 pixel Minecraft particle then enlarged with nearest-neighbor only. Silhouette resembles the vanilla Minecraft flame particle: wide flat square bottom, irregular stepped side, one tall off-center flame tongue and a shorter left fork, small transparent notch. Mostly deep dark plum purple (#321641), with one muted violet (#642680) inner patch and two small lighter purple pixels (#8d449c). About 6 coarse pixels wide and 8 tall. No skull, no eyes, no smoke, no outlines, no gradients, no blur, no glow, no shading beyond three flat colors. Centered with generous transparent padding. No text or scenery. This must look like a native low-resolution Minecraft magic flame, not a high resolution game icon.

Final edit prompt:

> Edit target: this particle sprite. Replace the diffuse soft glow with a fully solid, sharp, coarse pixel flame silhouette. Remove ALL haze, glow, gradients and stray detached pixels. Background must be exactly transparent. Inside the flame every pixel must be opaque and readily visible against black. Need a simple solid flame as in Minecraft's vanilla flame particle, occupying middle 60% of square canvas: exactly 8 coarse logical pixels wide and 10 coarse logical pixels tall, large hard square pixels, narrow stepped top and flat broad bottom, one fork. THREE flat colors ONLY: dark plum #442052 outer silhouette, violet #71358a body, muted lavender #9855aa inner two pixels. Do not add anything around the silhouette. Opaque pixel glyph on transparent canvas, no scene. No black background. No glow, absolutely no partially transparent halo. Crisp extremely low resolution pixel art, not painterly.
