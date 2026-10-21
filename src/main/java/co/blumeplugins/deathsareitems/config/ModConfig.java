package co.blumeplugins.deathsareitems.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ModConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue TERRAIN_LAUNCH;
    public static final ForgeConfigSpec.BooleanValue CUSHIONS;
    public static final ForgeConfigSpec.IntValue MAX_DEBRIS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        CUSHIONS = builder.comment("Place water, slime or hay in empty spaces before a normal landing.")
                .define("landingCushions", true);
        TERRAIN_LAUNCH = builder.comment("Sneak landings launch nearby breakable blocks. Block entities and protected blocks are excluded.")
                .define("launchTerrain", true);
        MAX_DEBRIS = builder.defineInRange("maxLaunchedBlocks", 9, 0, 18);
        SPEC = builder.build();
    }

    private ModConfig() {}
}
