package com.micatechnologies.minecraft.ldic.inventory;

/**
 * Whether the hotbar (and the bars above it) is showing, and how far through its slide.
 *
 * <p>With auto-hide off, the hotbar shows unless the player hid it with the toggle key. With
 * auto-hide on, activity (changing slot, picking something up, taking damage) shows it; once
 * the player moves, it stays for a grace period of movement and then slides away. Standing
 * still does not use up the grace period. The toggle key hides it at once, or shows it again.</p>
 */
public final class HotbarVisibility {

    /** Seconds the slide takes either way. */
    static final double SLIDE_SECONDS = 0.25;

    private boolean autoHide;
    private double graceSeconds = 2;
    private boolean hiddenByKey;
    private boolean shown = true;
    private double movedFor;
    private double progress = 1;

    public void configure(boolean autoHide, double graceSeconds) {
        if (autoHide != this.autoHide) {
            shown = true;
            hiddenByKey = false;
            movedFor = 0;
        }
        this.autoHide = autoHide;
        this.graceSeconds = Math.max(0, graceSeconds);
    }

    /** Something happened the player should see; ignored while hidden with the key and auto-hide off. */
    public void activity() {
        if (autoHide) {
            shown = true;
            movedFor = 0;
        }
    }

    public void toggle() {
        if (autoHide) {
            if (target()) {
                shown = false;
            } else {
                activity();
            }
        } else {
            hiddenByKey = !hiddenByKey;
        }
    }

    public void tick(double dt, boolean moving) {
        if (autoHide && shown && moving) {
            movedFor += dt;
            if (movedFor >= graceSeconds) {
                shown = false;
            }
        }
        double step = dt / SLIDE_SECONDS;
        progress = target() ? Math.min(1, progress + step) : Math.max(0, progress - step);
    }

    /** Where the hotbar is heading: true for shown. */
    public boolean target() {
        return autoHide ? shown : !hiddenByKey;
    }

    /** 1 fully shown, 0 fully hidden, eased. */
    public double visibility() {
        double t = progress;
        return t * t * (3 - 2 * t);
    }
}
