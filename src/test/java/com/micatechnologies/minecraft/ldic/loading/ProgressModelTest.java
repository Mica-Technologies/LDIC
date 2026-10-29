package com.micatechnologies.minecraft.ldic.loading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.ldic.loading.ProgressModel.Bar;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ProgressModelTest {

    private static Bar loading(int step) {
        return new Bar("Loading", "", step, 7);
    }

    @Test
    void nothingYetIsZero() {
        ProgressModel m = new ProgressModel(null);
        m.update(Collections.emptyList(), 0, 0);
        assertEquals(0, m.progress());
        assertEquals("Starting", m.status());
        assertEquals(-1, m.eta());
    }

    @Test
    void nestedBarsGiveTheFractionWithinAStage() {
        ProgressModel m = new ProgressModel(new double[] {10, 10, 10, 10, 10, 10, 40});
        // Stage 2 of 7, with a mods bar at item 51 of 100 begun: 50 done.
        m.update(Arrays.asList(loading(2), new Bar("Pre-initializing mods", "JEI", 51, 100)), 5, 0);
        assertEquals((10 + 10 * 0.5) / 100.0, m.rawProgress(), 1e-9);
        assertEquals("Pre-initialization", m.status());
        assertEquals("JEI", m.subStatus());
        assertEquals(2, m.stage());
        assertEquals(100 * (1 - 0.15), m.eta(), 1e-9);
    }

    @Test
    void shownProgressNeverGoesBackwards() {
        ProgressModel m = new ProgressModel(null);
        m.update(Arrays.asList(loading(3), new Bar("Texture stitching", "", 90, 100)), 1, 0);
        double high = m.progress();
        // The nested bar is replaced by a fresh one at 0: raw drops, shown must not.
        m.update(Arrays.asList(loading(3), new Bar("ModelLoader: baking", "", 1, 100)), 2, 0.016);
        assertTrue(m.rawProgress() < high);
        assertEquals(high, m.progress(), 1e-9);
    }

    @Test
    void stageTimesAreMeasured() {
        ProgressModel m = new ProgressModel(null);
        for (int s = 1; s <= 7; s++) {
            m.update(Collections.singletonList(loading(s)), s * 10, 0);
        }
        m.finish(75);
        assertTrue(m.complete());
        double[] t = m.stageSeconds();
        assertEquals(10, t[1], 1e-9);
        assertEquals(5, t[6], 1e-9);
    }

    @Test
    void afterTheMainBarIsGoneItIsDone() {
        ProgressModel m = new ProgressModel(null);
        m.update(Collections.singletonList(loading(7)), 1, 0);
        m.update(Collections.emptyList(), 2, 0);
        assertEquals(1, m.rawProgress());
        assertEquals("Ready", m.status());
    }

    @Test
    void fractionHandlesDeepStacks() {
        // Outer: 2 of 4 begun = 1 done, plus the inner bar's fraction (2 of 4 begun = 1/4) = 1.25 / 4.
        assertEquals(0.3125, ProgressModel.fraction(Arrays.asList(new Bar("a", "", 2, 4), new Bar("b", "", 2, 4), new Bar("c", "", 0, 0)), 0), 1e-9);
    }
}
