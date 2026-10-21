package co.blumeplugins.blumemod.sound;

import co.blumeplugins.blumemod.BlumeMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BlumeMod.MOD_ID);

    // Example sound registration:
    // public static final RegistryObject<SoundEvent> MY_SOUND = registerSoundEvent("my_sound");

    // private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
    //     return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
    //             new ResourceLocation(TemplateForgeMod.MOD_ID, name)));
    // }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
