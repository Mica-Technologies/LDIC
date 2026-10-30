package com.micatechnologies.minecraft.ldic.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HotbarVisibilityTest {

    private static void run(HotbarVisibility v, double seconds, boolean moving) {
        for (double t = 0; t < seconds - 1e-9; t += 0.05) {
            v.tick(0.05, moving);
        }
    }

    @Test
    void withoutAutoHideOnlyTheKeyHidesIt() {
        HotbarVisibility v = new HotbarVisibility();
        v.configure(false, 2);
        run(v, 10, true);
        assertTrue(v.target());
        v.toggle();
        assertFalse(v.target());
        v.activity();
        assertFalse(v.target(), "activity does not override the player's choice");
        v.toggle();
        assertTrue(v.target());
    }

    @Test
    void autoHideWaitsForTheGracePeriodOfMovement() {
        HotbarVisibility v = new HotbarVisibility();
        v.configure(true, 2);
        run(v, 10, false);
        assertTrue(v.target(), "standing still never hides it");
        run(v, 1.5, true);
        assertTrue(v.target());
        run(v, 1, true);
        assertFalse(v.target());
    }

    @Test
    void activityResetsTheGracePeriod() {
        HotbarVisibility v = new HotbarVisibility();
        v.configure(true, 2);
        run(v, 1.5, true);
        v.activity();
        run(v, 1.5, true);
        assertTrue(v.target());
        run(v, 0.6, true);
        assertFalse(v.target());
        v.activity();
        assertTrue(v.target());
    }

    @Test
    void keyHidesAtOnceAndShowsAgainWithAutoHide() {
        HotbarVisibility v = new HotbarVisibility();
        v.configure(true, 2);
        v.toggle();
        assertFalse(v.target());
        v.toggle();
        assertTrue(v.target());
    }

    @Test
    void slideIsEasedAndClamped() {
        HotbarVisibility v = new HotbarVisibility();
        v.configure(false, 2);
        assertEquals(1, v.visibility(), 1e-9);
        v.toggle();
        v.tick(HotbarVisibility.SLIDE_SECONDS / 2, false);
        assertEquals(0.5, v.visibility(), 1e-9);
        run(v, 1, false);
        assertEquals(0, v.visibility(), 1e-9);
    }
}
