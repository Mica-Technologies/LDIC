package com.micatechnologies.minecraft.ldic.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.ldic.inventory.ItemQueryTest.Item;
import com.micatechnologies.minecraft.ldic.inventory.ItemSort.Order;
import com.micatechnologies.minecraft.ldic.inventory.ItemSort.Pref;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class ItemSortTest {

    private static final Item WOOL_10 = new Item("Wool 10", "minecraft:wool", "Minecraft");
    private static final Item MARBLE = new Item("Marble", "quark:marble", "Quark");
    private static final Item WOOL_2 = new Item("Wool 2", "minecraft:wool", "Minecraft");
    private static final Item ANDESITE = new Item("andesite", "minecraft:stone", "Minecraft");
    private static final Item BASALT = new Item("Basalt", "quark:basalt", "Quark");

    private static final List<Item> TAB = Arrays.asList(WOOL_10, MARBLE, WOOL_2, ANDESITE, BASALT);

    private static List<Item> sort(Order order, boolean descending) {
        return ItemSort.sort(TAB, new Pref(order, descending), Function.identity());
    }

    @Test
    void defaultKeepsTheTabOrderAndDescendingReversesIt() {
        assertEquals(TAB, sort(Order.DEFAULT, false));
        assertEquals(Arrays.asList(BASALT, ANDESITE, WOOL_2, MARBLE, WOOL_10), sort(Order.DEFAULT, true));
    }

    @Test
    void nameIsCaseInsensitiveAndNumbersCompareByValue() {
        assertEquals(Arrays.asList(ANDESITE, BASALT, MARBLE, WOOL_2, WOOL_10), sort(Order.NAME, false));
        assertEquals(Arrays.asList(WOOL_10, WOOL_2, MARBLE, BASALT, ANDESITE), sort(Order.NAME, true));
    }

    @Test
    void modGroupsByModAndKeepsEachModsOwnOrder() {
        assertEquals(Arrays.asList(WOOL_10, WOOL_2, ANDESITE, MARBLE, BASALT), sort(Order.MOD, false));
    }

    @Test
    void idTiesKeepTabOrder() {
        assertEquals(Arrays.asList(ANDESITE, WOOL_10, WOOL_2, BASALT, MARBLE), sort(Order.ID, false));
    }

    @Test
    void ordersCycleAndRoundTripByKey() {
        assertEquals(Order.NAME, Order.DEFAULT.next());
        assertEquals(Order.DEFAULT, Order.ID.next());
        for (Order o : Order.values()) {
            assertEquals(o, Order.byKey(o.key));
        }
        assertEquals(Order.DEFAULT, Order.byKey("nonsense"));
    }

    @Test
    void naturalCompareHandlesLeadingZerosAndPrefixes() {
        assertEquals(0, ItemSort.compareNatural("Item 007", "item 7"));
        assertTrue(ItemSort.compareNatural("Slab", "Slab 2") < 0);
        assertTrue(ItemSort.compareNatural("a9", "a10") < 0);
    }
}
