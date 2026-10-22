package co.blumeplugins.deathsareitems.ability;

/** A mask charge must begin with a fresh press while both hands are already empty. */
public final class MaskChargeInput {
    private boolean wasDown = true;
    private boolean charging;

    public boolean update(boolean down, boolean eligible) {
        if (!down || !eligible) charging = false;
        else if (!wasDown) charging = true;
        wasDown = down;
        return charging;
    }

    public void reset() {
        wasDown = true;
        charging = false;
    }
}
