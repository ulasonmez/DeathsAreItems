package co.blumeplugins.deathsareitems.entity.custom;

import co.blumeplugins.deathsareitems.einfach.EinfachSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;

public final class ButtonTntEntity extends PrimedTnt {
    private String button = "";
    public ButtonTntEntity(EntityType<? extends PrimedTnt> type, Level level) { super(type, level); }
    public void configure(String button) { this.button = button; setFuse(60); }
    @Override public void tick() {
        if (!level().isClientSide && getFuse() > 0 && getFuse() % 10 == 0)
            level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT.get(), net.minecraft.sounds.SoundSource.BLOCKS, 2, 1.2F);
        super.tick();
    }
    @Override protected void explode() { level().explode(this, getX(), getY(.0625), getZ(), 4, Level.ExplosionInteraction.NONE); }
    @Override public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && level() instanceof ServerLevel level && !button.isEmpty()) {
            var data = EinfachSavedData.get(level.getServer());
            if (data.buttons.hasUUID(button) && data.buttons.getUUID(button).equals(getUUID())) { data.buttons.remove(button); data.setDirty(); }
        }
        super.remove(reason);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putString("Button", button); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); button = tag.getString("Button"); }
}
