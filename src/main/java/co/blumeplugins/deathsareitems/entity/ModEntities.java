package co.blumeplugins.deathsareitems.entity;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.entity.custom.RewardChestEntity;
import co.blumeplugins.deathsareitems.entity.custom.LaunchedBlockEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DeathsAreItems.MOD_ID);

    public static final RegistryObject<EntityType<RewardChestEntity>> REWARD_CHEST = ENTITY_TYPES.register("reward_chest",
            () -> EntityType.Builder.<RewardChestEntity>of(RewardChestEntity::new, MobCategory.MISC)
                    .sized(.9F, .9F).clientTrackingRange(12).updateInterval(1).fireImmune().build("reward_chest"));
    public static final RegistryObject<EntityType<LaunchedBlockEntity>> LAUNCHED_BLOCK = ENTITY_TYPES.register("launched_block",
            () -> EntityType.Builder.<LaunchedBlockEntity>of(LaunchedBlockEntity::new, MobCategory.MISC)
                    .sized(.8F, .8F).clientTrackingRange(10).updateInterval(1).build("launched_block"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
