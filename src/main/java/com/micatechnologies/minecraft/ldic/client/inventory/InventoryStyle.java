package com.micatechnologies.minecraft.ldic.client.inventory;

import com.micatechnologies.minecraft.ldic.client.LdicClient;
import com.micatechnologies.minecraft.ldic.client.LdicSettings;
import com.micatechnologies.minecraft.ldic.client.menu.MenuManager;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

/**
 * LDIC's in-game look, shared by the hotbar and the full-screen inventory: dark translucent
 * panels with a faint edge, and one accent colour taken from the menu theme's {@code accent}
 * constant so the game matches the menu (the pack's {@code inventory.accent} otherwise).
 */
public final class InventoryStyle {

    public static final int PANEL = 0xD0101418;
    public static final int PANEL_EDGE = 0x30FFFFFF;
    public static final int CELL = 0x50000000;
    public static final int CELL_EDGE = 0x18FFFFFF;
    public static final int TEXT = 0xFFE8ECF0;
    public static final int TEXT_DIM = 0xFF8A93A0;

    private InventoryStyle() {
    }

    public static int accent() {
        LdicSettings s = LdicClient.settings();
        int fallback = s == null ? 0xFF4FC3F7 : s.inventoryAccent;
        MenuManager menu = LdicClient.menu();
        int c = menu == null ? fallback : menu.themeAccent(fallback);
        return c | 0xFF000000;
    }

    public static int alpha(int rgb, int alpha) {
        return (rgb & 0xFFFFFF) | (alpha << 24);
    }

    /** A panel with its corners cut by one pixel, which reads as slightly rounded at GUI scale. */
    public static void panel(int x, int y, int r, int b, int fill, int edge) {
        Gui.drawRect(x + 1, y, r - 1, y + 1, edge);
        Gui.drawRect(x + 1, b - 1, r - 1, b, edge);
        Gui.drawRect(x, y + 1, x + 1, b - 1, edge);
        Gui.drawRect(r - 1, y + 1, r, b - 1, edge);
        Gui.drawRect(x + 1, y + 1, r - 1, b - 1, fill);
    }

    public static void panel(int x, int y, int r, int b) {
        panel(x, y, r, b, PANEL, PANEL_EDGE);
    }

    /** The 18 x 18 cell behind a slot whose item corner is at (x, y). */
    public static void cell(int x, int y) {
        Gui.drawRect(x - 1, y - 1, x + 17, y + 17, CELL_EDGE);
        Gui.drawRect(x, y, x + 16, y + 16, CELL);
    }

    /** A small labelled button. */
    public static void button(FontRenderer font, int x, int y, int w, int h, String label, boolean hovered, boolean active) {
        int accent = accent();
        panel(x, y, x + w, y + h, active ? alpha(accent, 0x60) : hovered ? 0x40FFFFFF : 0x20FFFFFF,
                hovered || active ? accent : PANEL_EDGE);
        font.drawString(label, x + (w - font.getStringWidth(label)) / 2, y + (h - 8) / 2, TEXT);
    }

    /** A rectangle outline one pixel wide. */
    public static void outline(int x, int y, int r, int b, int colour) {
        Gui.drawRect(x, y, r, y + 1, colour);
        Gui.drawRect(x, b - 1, r, b, colour);
        Gui.drawRect(x, y + 1, x + 1, b - 1, colour);
        Gui.drawRect(r - 1, y + 1, r, b - 1, colour);
    }
}
