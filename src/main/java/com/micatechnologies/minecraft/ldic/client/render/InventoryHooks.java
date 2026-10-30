package com.micatechnologies.minecraft.ldic.client.render;

/**
 * What the inventory-only elements ({@code slots}, {@code player_model}) need from the open
 * inventory screen. {@link ThemeRenderer#inventory} is null on every other screen, where those
 * elements draw nothing.
 */
public interface InventoryHooks {

    /** The item corners (x, y pairs, layout units) of a group's slots. */
    int[] slots(String group);

    /** Which pair in {@link #slots(String)} is the selected hotbar slot, or -1. */
    int selected(String group);

    /** Draws the player standing in a box, looking at the pointer unless {@code followMouse} is off. */
    void drawPlayer(double x, double y, double width, double height, boolean followMouse);
}
