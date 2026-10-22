package co.blumeplugins.deathsareitems.reward;

/** Shared by server interaction validation and client rendering. All values are ticks. */
public final class RewardTimings {
    public static final int RESPAWN_DELAY = 70;
    public static final int RISE = longer(40);
    public static final int SETTLE_START = longer(50);
    public static final int SLOT_DELAY = longer(16);
    public static final int SETTLE_DURATION = longer(14);
    public static final int READY = longer(50 + 2 * 16 + 14);
    // Vanilla's item activation lasts 40 ticks; delivery must remain aligned with it.
    public static final int REVEAL = 40;

    private RewardTimings() {}

    public static int settlesAt(int slot) {
        return longer(50 + slot * 16 + 14);
    }

    public static float originalAge(float age) { return age / 1.1F; }
    private static int longer(int ticks) { return Math.round(ticks * 1.1F); }
}
