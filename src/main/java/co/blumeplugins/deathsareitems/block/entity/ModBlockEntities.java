package co.blumeplugins.deathsareitems.block.entity;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DeathsAreItems.MOD_ID);

    public static final net.minecraftforge.registries.RegistryObject<BlockEntityType<co.blumeplugins.deathsareitems.block.entity.custom.GustafJukeboxBlockEntity>> GUSTAFS_JUKEBOX = BLOCK_ENTITIES.register("gustafs_jukebox", () ->
            BlockEntityType.Builder.of(co.blumeplugins.deathsareitems.block.entity.custom.GustafJukeboxBlockEntity::new,
                    co.blumeplugins.deathsareitems.block.ModBlocks.GUSTAFS_JUKEBOX.get()).build(null));

    // Example block entity registration:
    // public static final RegistryObject<BlockEntityType<MyBlockEntity>> MY_BLOCK_ENTITY =
    //         BLOCK_ENTITIES.register("my_block_entity", () ->
    //                 BlockEntityType.Builder.of(MyBlockEntity::new, ModBlocks.MY_BLOCK.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
