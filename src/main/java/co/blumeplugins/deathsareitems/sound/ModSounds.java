package co.blumeplugins.deathsareitems.sound;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, DeathsAreItems.MOD_ID);
    public static final RegistryObject<SoundEvent> JUKY_SUMMON = registerSoundEvent("juky_summon");
    public static final RegistryObject<SoundEvent> JUKY_RECALL = registerSoundEvent("juky_recall");

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(DeathsAreItems.MOD_ID, name)));
    }
    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
