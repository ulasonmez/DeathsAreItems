# CLAUDE.md — Forge 1.20.1 Mod Reference

Canonical code patterns for writing Forge 1.20.1 mods. Prose minimal, code maximal.

## 1. Intro & Conventions

**Placeholders** (replace everywhere):
- `<modid>` — lowercase mod id, e.g. `examplemod`
- `<MainClass>` — main class name, e.g. `ExampleMod`
- `<pkg>` — base package, e.g. `com.example.examplemod`

**Non-negotiable rules:**
- All registry objects MUST go through `DeferredRegister`. Never touch vanilla registries directly.
- Custom logic (custom Item/Block/Entity subclasses) lives under `*/custom/` packages.
- Every `DeferredRegister` MUST be wired in the main class constructor via `.register(modEventBus)`.
- Use `ResourceLocation(<MainClass>.MODID, "name")` — never hardcode strings.

## 2. Project Skeleton

```
src/main/
├── java/<pkg>/
│   ├── <MainClass>.java
│   ├── item/ModItems.java
│   ├── item/custom/
│   ├── block/ModBlocks.java
│   ├── block/custom/
│   ├── block/entity/ModBlockEntities.java
│   ├── entity/ModEntities.java
│   ├── entity/custom/
│   ├── entity/client/
│   ├── sound/ModSounds.java
│   ├── item/ModCreativeModTabs.java
│   └── datagen/
└── resources/
    ├── META-INF/mods.toml
    ├── pack.mcmeta
    └── assets/<modid>/{lang,models,textures,blockstates,sounds}/
```

**`pack.mcmeta`:**
```json
{
  "pack": {
    "description": "<modid> resources",
    "pack_format": 15
  }
}
```

## 3. Main Class

```java
package <pkg>;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(<MainClass>.MODID)
public class <MainClass> {
    public static final String MODID = "<modid>";
    public static final Logger LOGGER = LogUtils.getLogger();

    public <MainClass>() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        ModCreativeModTabs.CREATIVE_MODE_TABS.register(bus);
        ModItems.ITEMS.register(bus);
        ModBlocks.BLOCKS.register(bus);
        ModEntities.ENTITY_TYPES.register(bus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(bus);
        ModSounds.SOUND_EVENTS.register(bus);

        bus.addListener(this::commonSetup);
        bus.addListener(this::addCreative);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {}

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Optional: inject items into vanilla tabs
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {}
    }
}
```

## 4. DeferredRegister Pattern

```java
public static final DeferredRegister<Item> ITEMS =
    DeferredRegister.create(ForgeRegistries.ITEMS, <MainClass>.MODID);
```

| Class | DeferredRegister | Registry |
|---|---|---|
| `ModItems` | `ITEMS` | `ForgeRegistries.ITEMS` |
| `ModBlocks` | `BLOCKS` | `ForgeRegistries.BLOCKS` |
| `ModEntities` | `ENTITY_TYPES` | `ForgeRegistries.ENTITY_TYPES` |
| `ModBlockEntities` | `BLOCK_ENTITY_TYPES` | `ForgeRegistries.BLOCK_ENTITY_TYPES` |
| `ModSounds` | `SOUND_EVENTS` | `ForgeRegistries.SOUND_EVENTS` |
| `ModCreativeModTabs` | `CREATIVE_MODE_TABS` | `Registries.CREATIVE_MODE_TAB` |

## 5. Items

```java
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, <MainClass>.MODID);

    public static final RegistryObject<Item> RUBY = ITEMS.register("ruby",
        () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MAGIC_WAND = ITEMS.register("magic_wand",
        () -> new MagicWandItem(new Item.Properties().stacksTo(1)));
}
```

Custom item: `<pkg>/item/custom/MagicWandItem.java` extends `Item`.

Resource paths:
- `assets/<modid>/lang/en_us.json` → `"item.<modid>.ruby": "Ruby"`
- `assets/<modid>/models/item/ruby.json` → `{"parent":"item/generated","textures":{"layer0":"<modid>:item/ruby"}}`
- `assets/<modid>/textures/item/ruby.png`

## 6. Blocks

```java
public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, <MainClass>.MODID);

    public static final RegistryObject<Block> RUBY_BLOCK = registerBlock("ruby_block",
        () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
```

JSON files:
```json
// blockstates/ruby_block.json
{ "variants": { "": { "model": "<modid>:block/ruby_block" } } }
// models/block/ruby_block.json
{ "parent": "block/cube_all", "textures": { "all": "<modid>:block/ruby_block" } }
// models/item/ruby_block.json
{ "parent": "<modid>:block/ruby_block" }
```

## 7. Creative Mode Tabs (1.20.1)

```java
public class ModCreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, <MainClass>.MODID);

    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab",
        () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(ModItems.RUBY.get()))
            .title(Component.translatable("creativetab.<modid>.example_tab"))
            .displayItems((params, output) -> {
                output.accept(ModItems.RUBY.get());
                output.accept(ModItems.MAGIC_WAND.get());
                output.accept(ModBlocks.RUBY_BLOCK.get());
            })
            .build());
}
```

## 8. Sounds

```java
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, <MainClass>.MODID);

    public static final RegistryObject<SoundEvent> CHIME = SOUND_EVENTS.register("chime",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(<MainClass>.MODID, "chime")));
}
```

```json
// assets/<modid>/sounds.json
{ "chime": { "sounds": [ "<modid>:chime" ] } }
```

## 9. Entities

```java
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, <MainClass>.MODID);

    public static final RegistryObject<EntityType<SpriteEntity>> SPRITE = ENTITY_TYPES.register("sprite",
        () -> EntityType.Builder.of(SpriteEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.8f)
            .build("sprite"));
}
```

Attributes (forge bus, in main class or `@Mod.EventBusSubscriber`):
```java
@SubscribeEvent
public static void registerAttributes(EntityAttributeCreationEvent event) {
    event.put(ModEntities.SPRITE.get(), SpriteEntity.createAttributes().build());
}
```

Renderer (in `ClientModEvents.onClientSetup`):
```java
EntityRenderers.register(ModEntities.SPRITE.get(), SpriteRenderer::new);
```

Spawn egg:
```java
public static final RegistryObject<Item> SPRITE_SPAWN_EGG = ITEMS.register("sprite_spawn_egg",
    () -> new ForgeSpawnEggItem(ModEntities.SPRITE, 0xAABBCC, 0x112233, new Item.Properties()));
```

## 10. Block Entities

```java
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, <MainClass>.MODID);

    public static final RegistryObject<BlockEntityType<RubyForgeBlockEntity>> RUBY_FORGE_BE =
        BLOCK_ENTITY_TYPES.register("ruby_forge_be",
            () -> BlockEntityType.Builder.of(RubyForgeBlockEntity::new,
                ModBlocks.RUBY_FORGE.get()).build(null));
}
```

Renderer registered in `ClientModEvents.onClientSetup` via `BlockEntityRenderers.register(...)`.

## 11. Datagen

**Loot (`dropSelf`):**
```java
public class ModBlockLootTables extends BlockLootSubProvider {
    protected ModBlockLootTables() { super(Set.of(), FeatureFlags.REGISTRY.allFlags()); }
    @Override protected void generate() {
        dropSelf(ModBlocks.RUBY_BLOCK.get());
    }
    @Override protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get)::iterator;
    }
}
```

**Recipes:**
```java
ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RUBY_BLOCK.get())
    .pattern("RRR").pattern("RRR").pattern("RRR")
    .define('R', ModItems.RUBY.get())
    .unlockedBy("has_ruby", has(ModItems.RUBY.get()))
    .save(writer);

ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.RUBY.get(), 9)
    .requires(ModBlocks.RUBY_BLOCK.get())
    .unlockedBy("has_ruby_block", has(ModBlocks.RUBY_BLOCK.get()))
    .save(writer, new ResourceLocation(<MainClass>.MODID, "ruby_from_block"));
```

**Block tags:**
```java
this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.RUBY_BLOCK.get());
this.tag(BlockTags.NEEDS_IRON_TOOL).add(ModBlocks.RUBY_BLOCK.get());
```

## 12. mods.toml

```toml
modLoader="javafml"
loaderVersion="[47,)"
license="MIT"

[[mods]]
modId="<modid>"
version="${file.jarVersion}"
displayName="<MainClass>"
authors="you"
description='''
A Forge 1.20.1 mod.
'''

[[dependencies.<modid>]]
    modId="forge"
    mandatory=true
    versionRange="[47,)"
    ordering="NONE"
    side="BOTH"

[[dependencies.<modid>]]
    modId="minecraft"
    mandatory=true
    versionRange="[1.20.1,1.21)"
    ordering="NONE"
    side="BOTH"
```

## 13. Adding 3rd-Party Dependencies

1. Add Maven repo to `build.gradle`: `repositories { maven { url "https://..." } }`
2. Add to dependencies: `implementation fg.deobf("group:artifact:version")`
3. Add `[[dependencies.<modid>]]` block in `mods.toml`
4. Refresh Gradle: `./gradlew --refresh-dependencies`

## 14. Gradle Commands

| Command | Purpose |
|---|---|
| `./gradlew runClient` | Launch dev client |
| `./gradlew runServer` | Launch dev server |
| `./gradlew runData` | Run datagen providers |
| `./gradlew build` | Build jar to `build/libs/` |

## 15. New Content Checklist

- [ ] Register in appropriate `Mod*` class via `DeferredRegister`
- [ ] Confirm `DeferredRegister` is wired in main class constructor
- [ ] Add lang entry in `en_us.json`
- [ ] Add model JSON (item and/or block)
- [ ] Add texture PNG
- [ ] Block? → blockstate + item-model + loot table + tags + recipe
- [ ] Entity? → attributes event + client renderer + spawn egg
- [ ] Block entity? → renderer (if needed) + tied to block
- [ ] Add to a creative tab in `ModCreativeModTabs`
- [ ] Run `./gradlew runData` then `runClient` to verify
