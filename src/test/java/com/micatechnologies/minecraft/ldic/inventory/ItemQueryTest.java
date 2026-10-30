package com.micatechnologies.minecraft.ldic.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ItemQueryTest {

    static final class Item implements ItemQuery.Facts {
        final String name;
        final String id;
        final String modName;
        final List<String> tooltip;
        int tooltipCalls;

        Item(String name, String id, String modName, String... tooltip) {
            this.name = name;
            this.id = id;
            this.modName = modName;
            this.tooltip = Arrays.asList(tooltip);
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String modId() {
            return id.substring(0, id.indexOf(':'));
        }

        @Override
        public String modName() {
            return modName;
        }

        @Override
        public List<String> tooltip() {
            tooltipCalls++;
            return tooltip;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final Item OAK_LOG = new Item("Oak Wood", "minecraft:log", "Minecraft", "Oak Wood", "§7Burns well");
    private static final Item OAK_SLAB = new Item("Oak Wood Slab", "minecraft:wooden_slab", "Minecraft", "Oak Wood Slab");
    private static final Item MARBLE = new Item("Marble Bricks", "quark:marble_bricks", "Quark", "Marble Bricks");

    @Test
    void emptyAndBlankMatchEverything() {
        assertTrue(ItemQuery.parse("").isEmpty());
        assertTrue(ItemQuery.parse("   ").matches(MARBLE));
        assertTrue(ItemQuery.parse("- @ #").isEmpty());
    }

    @Test
    void wordsMatchNameOrIdIgnoringCase() {
        assertTrue(ItemQuery.parse("OAK").matches(OAK_LOG));
        assertTrue(ItemQuery.parse("wooden_slab").matches(OAK_SLAB));
        assertFalse(ItemQuery.parse("oak").matches(MARBLE));
    }

    @Test
    void allWordsMustMatchInAnyOrder() {
        assertTrue(ItemQuery.parse("slab oak").matches(OAK_SLAB));
        assertFalse(ItemQuery.parse("slab oak").matches(OAK_LOG));
    }

    @Test
    void modTermsMatchIdOrName() {
        assertTrue(ItemQuery.parse("@qua").matches(MARBLE));
        assertTrue(ItemQuery.parse("@minecraft").matches(OAK_LOG));
        assertFalse(ItemQuery.parse("@quark").matches(OAK_LOG));
    }

    @Test
    void tooltipTermsIgnoreFormattingAndAreOnlyAskedWhenNeeded() {
        Item log = new Item("Oak Wood", "minecraft:log", "Minecraft", "§7Burns well");
        assertTrue(ItemQuery.parse("#burns").matches(log));
        assertEquals(1, log.tooltipCalls);
        ItemQuery.parse("oak @mine").matches(log);
        assertEquals(1, log.tooltipCalls);
    }

    @Test
    void negatedTermsExclude() {
        List<Item> all = Arrays.asList(OAK_LOG, OAK_SLAB, MARBLE);
        assertEquals(Collections.singletonList(OAK_LOG), filter(all, "oak -slab"));
        assertEquals(Collections.singletonList(MARBLE), filter(all, "-@minecraft"));
    }

    @Test
    void onlyASinglePlainWordIsSimple() {
        assertTrue(ItemQuery.parse("oak").isSimple());
        assertFalse(ItemQuery.parse("oak slab").isSimple());
        assertFalse(ItemQuery.parse("@quark").isSimple());
        assertFalse(ItemQuery.parse("-oak").isSimple());
        assertFalse(ItemQuery.parse("").isSimple());
    }

    private static List<Item> filter(List<Item> items, String query) {
        ItemQuery q = ItemQuery.parse(query);
        List<Item> out = new ArrayList<>();
        for (Item i : items) {
            if (q.matches(i)) {
                out.add(i);
            }
        }
        return out;
    }
}
