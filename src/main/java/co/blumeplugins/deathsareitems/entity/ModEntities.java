package co.blumeplugins.deathsareitems.entity;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.entity.custom.RewardChestEntity;
import co.blumeplugins.deathsareitems.entity.custom.LaunchedBlockEntity;
import co.blumeplugins.deathsareitems.entity.custom.MaskTridentEntity;
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

    public static final RegistryObject<EntityType<MaskTridentEntity>> MASK_TRIDENT = ENTITY_TYPES.register("mask_trident",
            () -> EntityType.Builder.<MaskTridentEntity>of(MaskTridentEntity::new, MobCategory.MISC)
                    .sized(.25F, .25F).clientTrackingRange(12).updateInterval(1).build("mask_trident"));

    public static final RegistryObject<EntityType<co.blumeplugins.deathsareitems.entity.custom.CannonTntEntity>> CANNON_TNT = ENTITY_TYPES.register("cannon_tnt",
            () -> EntityType.Builder.<co.blumeplugins.deathsareitems.entity.custom.CannonTntEntity>of(co.blumeplugins.deathsareitems.entity.custom.CannonTntEntity::new, MobCategory.MISC)
                    .sized(.98F,.98F).clientTrackingRange(12).updateInterval(1).fireImmune().build("cannon_tnt"));
    public static final RegistryObject<EntityType<co.blumeplugins.deathsareitems.entity.custom.ChaosPearlEntity>> CHAOS_PEARL = ENTITY_TYPES.register("chaos_pearl",
            () -> EntityType.Builder.<co.blumeplugins.deathsareitems.entity.custom.ChaosPearlEntity>of(co.blumeplugins.deathsareitems.entity.custom.ChaosPearlEntity::new, MobCategory.MISC)
                    .sized(.25F,.25F).clientTrackingRange(16).updateInterval(1).build("chaos_pearl"));

    public static final RegistryObject<EntityType<co.blumeplugins.deathsareitems.entity.custom.JukyEntity>> JUKY = ENTITY_TYPES.register("juky",
            () -> EntityType.Builder.of(co.blumeplugins.deathsareitems.entity.custom.JukyEntity::new, MobCategory.CREATURE).sized(.6F, .85F).clientTrackingRange(10).build("juky"));
    public static final RegistryObject<EntityType<co.blumeplugins.deathsareitems.entity.custom.VinyEntity>> VINY = ENTITY_TYPES.register("viny",
            () -> EntityType.Builder.of(co.blumeplugins.deathsareitems.entity.custom.VinyEntity::new, MobCategory.AXOLOTLS).sized(.75F, .42F).clientTrackingRange(10).build("viny"));
    public static final RegistryObject<EntityType<co.blumeplugins.deathsareitems.entity.custom.ButtonTntEntity>> BUTTON_TNT = ENTITY_TYPES.register("button_tnt",
            () -> EntityType.Builder.of(co.blumeplugins.deathsareitems.entity.custom.ButtonTntEntity::new, MobCategory.MISC).sized(.98F, .98F).clientTrackingRange(12).updateInterval(1).fireImmune().build("button_tnt"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
