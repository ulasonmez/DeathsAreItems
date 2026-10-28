package co.blumeplugins.deathsareitems.einfach;
import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.entity.ModEntities;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EinfachAttributes {
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.JUKY.get(), Wolf.createAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.ATTACK_DAMAGE, 8).build());
        event.put(ModEntities.VINY.get(), Axolotl.createAttributes().add(Attributes.MAX_HEALTH, 30).build());
    }
}
