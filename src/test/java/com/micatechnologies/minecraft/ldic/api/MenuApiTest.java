package com.micatechnologies.minecraft.ldic.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class MenuApiTest {

    private static MenuEntry entry(String id, int order) {
        return MenuEntry.builder(id).label(id).order(order).runs(() -> { }).build();
    }

    private static List<String> ids(List<MenuEntry> entries) {
        List<String> out = new ArrayList<>();
        for (MenuEntry e : entries) {
            out.add(e.id());
        }
        return out;
    }

    @Test
    void entriesSortByOrderThenRegistration() {
        MenuRegistry r = new MenuRegistry();
        r.add(MenuLocation.PAUSE, entry("b:late", 2000));
        r.add(MenuLocation.PAUSE, entry("a:first", 1000));
        r.add(MenuLocation.PAUSE, entry("a:second", 1000));
        r.add(MenuLocation.PAUSE, entry("c:early", 10));
        r.add(MenuLocation.OPTIONS, entry("d:elsewhere", 0));
        assertEquals(Arrays.asList("c:early", "a:first", "a:second", "b:late"), ids(r.entries(MenuLocation.PAUSE)));
        assertEquals(Arrays.asList("d:elsewhere"), ids(r.entries(MenuLocation.OPTIONS)));
        assertTrue(r.entries(MenuLocation.MAIN).isEmpty());
    }

    @Test
    void addingAnIdAgainReplacesItInPlace() {
        MenuRegistry r = new MenuRegistry();
        r.add(MenuLocation.MAIN, entry("a:x", 1000));
        r.add(MenuLocation.MAIN, entry("a:y", 1000));
        MenuEntry again = MenuEntry.builder("a:x").label("again").runs(() -> { }).build();
        r.add(MenuLocation.MAIN, again);
        List<MenuEntry> entries = r.entries(MenuLocation.MAIN);
        assertEquals(Arrays.asList("a:x", "a:y"), ids(entries));
        assertEquals("again", entries.get(0).label().get());
        assertTrue(r.remove(MenuLocation.MAIN, "a:x"));
        assertFalse(r.remove(MenuLocation.MAIN, "a:x"));
        assertEquals(Arrays.asList("a:y"), ids(r.entries(MenuLocation.MAIN)));
    }

    @Test
    void anEntryNeedsALabelAndExactlyOneKind() {
        assertThrows(IllegalArgumentException.class, () -> MenuEntry.builder(""));
        assertThrows(IllegalStateException.class, () -> MenuEntry.builder("a:x").runs(() -> { }).build());
        assertThrows(IllegalStateException.class, () -> MenuEntry.builder("a:x").label("x").build());
        assertThrows(IllegalStateException.class, () -> MenuEntry.builder("a:x").label("x").runs(() -> { })
                .page(MenuPage.builder("p").build()).build());
    }

    @Test
    void locationsHaveThemeNames() {
        assertEquals("pause", MenuLocation.PAUSE.key());
        assertEquals(MenuLocation.OPTIONS, MenuLocation.byKey("Options"));
        assertNull(MenuLocation.byKey("inventory"));
    }

    @Test
    void toggleChoiceAndButtonRowsActOnAClick() {
        boolean[] on = {false};
        int[] choice = {2};
        int[] clicks = {0};
        MenuPage page = MenuPage.builder("Page")
                .section("Heading")
                .toggle("Switch", () -> on[0], v -> on[0] = v)
                .choice("Pick", Arrays.asList("A", "B", "C"), () -> choice[0], v -> choice[0] = v)
                .button("Go", () -> clicks[0]++)
                .build();
        List<MenuPage.Row> rows = page.rows();
        assertEquals(MenuPage.Kind.SECTION, rows.get(0).kind());
        assertFalse(rows.get(0).enabled(), "a heading is never clickable");
        rows.get(0).activate();

        rows.get(1).activate();
        assertTrue(on[0]);
        assertTrue(rows.get(1).on());

        assertEquals("C", rows.get(2).valueText());
        rows.get(2).activate();
        assertEquals(0, choice[0], "a choice wraps to its first option");
        assertEquals("A", rows.get(2).valueText());

        rows.get(3).activate();
        assertEquals(1, clicks[0]);
    }

    @Test
    void sliderSnapsToItsStepAndStaysInRange() {
        double[] value = {8};
        int[] sets = {0};
        MenuPage.Row slider = MenuPage.builder("Page")
                .slider("Range", 2, 32, 1, () -> value[0], v -> {
                    value[0] = v;
                    sets[0]++;
                })
                .build().rows().get(0);
        assertEquals(0.2, slider.fraction(), 1e-9);
        assertEquals("8", slider.valueText());

        slider.setFraction(0.51);
        assertEquals(17, value[0], 1e-9);
        slider.setFraction(0.51);
        assertEquals(1, sets[0], "the setter does not run again for the same snapped value");

        slider.setFraction(7);
        assertEquals(32, value[0], 1e-9);
        slider.setFraction(-3);
        assertEquals(2, value[0], 1e-9);
        assertEquals(1.0 / 30, slider.keyStep(), 1e-9);
    }

    @Test
    void aContinuousSliderFormatsTwoDecimalsAndADisabledRowIgnoresInput() {
        double[] value = {0.5};
        boolean[] allowed = {false};
        MenuPage.Row slider = MenuPage.builder("Page")
                .slider("Opacity", 0, 1, 0, () -> value[0], v -> value[0] = v)
                .enabledWhen(() -> allowed[0])
                .tooltip("How see-through")
                .build().rows().get(0);
        assertEquals("0.50", slider.valueText());
        assertEquals("How see-through", slider.tooltip());
        slider.setFraction(0.9);
        assertEquals(0.5, value[0], 1e-9, "disabled: unchanged");
        allowed[0] = true;
        slider.setFraction(0.9);
        assertEquals(0.9, value[0], 1e-9);
    }

    @Test
    void closeHookRunsEachTimeAndBadRowsAreRejected() {
        int[] closed = {0};
        MenuPage page = MenuPage.builder("Page").text("hello").onClose(() -> closed[0]++).build();
        page.closed();
        page.closed();
        assertEquals(2, closed[0]);
        assertThrows(IllegalArgumentException.class, () -> MenuPage.builder("p").slider("s", 5, 5, 1, () -> 5, v -> { }));
        assertThrows(IllegalArgumentException.class, () -> MenuPage.builder("p").choice("c", new ArrayList<>(), () -> 0, v -> { }));
        assertThrows(IllegalStateException.class, () -> MenuPage.builder("p").tooltip("no row yet"));
    }
}
