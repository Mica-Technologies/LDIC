package com.micatechnologies.minecraft.ldic.inventory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.ldic.inventory.CreativeLayout.Rect;
import org.junit.jupiter.api.Test;

class CreativeLayoutTest {

    private static boolean overlap(Rect a, Rect b) {
        return a.x < b.right() && b.x < a.right() && a.y < b.bottom() && b.y < a.bottom();
    }

    @Test
    void wideScreensGetTheFullSidebarAndABigGrid() {
        CreativeLayout l = CreativeLayout.compute(960, 540);
        assertNotNull(l);
        assertFalse(l.compact);
        assertTrue(l.cols > 20, "cols " + l.cols);
        assertTrue(l.rows > 20, "rows " + l.rows);
    }

    @Test
    void smallScreensFallBackToIconsThenToNothing() {
        CreativeLayout l = CreativeLayout.compute(427, 240);
        assertNotNull(l);
        assertTrue(l.compact);
        assertTrue(l.cols >= 9 && l.rows >= 5);
        assertNull(CreativeLayout.compute(320, 240));
        assertNull(CreativeLayout.compute(640, 180));
    }

    @Test
    void sidebarGrowsForLongNamesOnlyWhileTheGridStillFits() {
        assertTrue(CreativeLayout.compute(960, 540, 150).sidebar.w == 150);
        assertTrue(CreativeLayout.compute(960, 540, 400).sidebar.w == CreativeLayout.SIDEBAR_MAX_W);
        assertTrue(CreativeLayout.compute(960, 540, 40).sidebar.w == CreativeLayout.SIDEBAR_W);
        // Room for the default width but not a wide one: the default, not icons only.
        CreativeLayout tight = CreativeLayout.compute(480, 270, 168);
        assertNotNull(tight);
        assertTrue(tight.cols >= 9);
        assertTrue(tight.compact || tight.sidebar.w < 168);
    }

    @Test
    void panelsDoNotOverlapAndStayOnScreen() {
        for (int[] size : new int[][] {{427, 240}, {640, 360}, {960, 540}, {1280, 720}, {480, 270}}) {
            CreativeLayout l = CreativeLayout.compute(size[0], size[1]);
            assertNotNull(l, size[0] + "x" + size[1]);
            assertFalse(overlap(l.sidebar, l.grid));
            assertFalse(overlap(l.grid, l.player));
            assertFalse(overlap(l.top, l.grid));
            assertFalse(overlap(l.search, l.sort));
            assertFalse(overlap(l.sort, l.toggle));
            assertTrue(l.player.bottom() <= size[1]);
            assertTrue(l.toggle.right() <= size[0]);
            // The last grid column ends before the scrollbar.
            assertTrue(l.gridX - 1 + l.cols * CreativeLayout.CELL <= l.scrollbar.x);
            assertTrue(l.gridY - 1 + l.rows * CreativeLayout.CELL <= l.grid.bottom());
            // The hotbar row is inside the player panel.
            assertTrue(l.hotbarY + 16 <= l.player.bottom());
        }
    }
}
