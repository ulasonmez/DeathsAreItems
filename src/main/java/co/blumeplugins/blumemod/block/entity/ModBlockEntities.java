package co.blumeplugins.blumemod.block.entity;

import co.blumeplugins.blumemod.BlumeMod;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BlumeMod.MOD_ID);

    // Example block entity registration:
    // public static final RegistryObject<BlockEntityType<MyBlockEntity>> MY_BLOCK_ENTITY =
    //         BLOCK_ENTITIES.register("my_block_entity", () ->
    //                 BlockEntityType.Builder.of(MyBlockEntity::new, ModBlocks.MY_BLOCK.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
