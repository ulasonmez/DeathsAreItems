package co.blumeplugins.blumemod.entity;

import co.blumeplugins.blumemod.BlumeMod;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BlumeMod.MOD_ID);

    // Example entity registration:
    // public static final RegistryObject<EntityType<MyEntity>> MY_ENTITY =
    //         ENTITY_TYPES.register("my_entity", () -> EntityType.Builder
    //                 .of(MyEntity::new, MobCategory.CREATURE)
    //                 .sized(1.0f, 1.5f)
    //                 .build("my_entity"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
