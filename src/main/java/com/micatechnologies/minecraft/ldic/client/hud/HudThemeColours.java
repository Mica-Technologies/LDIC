package com.micatechnologies.minecraft.ldic.client.hud;

import com.micatechnologies.minecraft.ldic.client.LdicClient;
import com.micatechnologies.minecraft.ldic.client.inventory.InventoryStyle;
import com.micatechnologies.minecraft.ldic.client.menu.MenuManager;

/**
 * The HUD's colours from the menu theme: its {@code hud_background}, {@code hud_text} and
 * {@code hud_accent} constants, each falling back to a dark panel, light text and the theme's
 * {@code accent}. Used by the "Theme" style preset and the visual HUDs.
 *
 * <p>SUM reads {@link #colours} by reflection, so its own HUDs match under the same preset; keep
 * that method's name and shape.</p>
 */
public final class HudThemeColours {

    public static final int DEFAULT_BACKGROUND = 0xA0101418;
    public static final int DEFAULT_TEXT = 0xFFE8ECF0;

    private HudThemeColours() {
    }

    public static int background() {
        MenuManager m = LdicClient.menu();
        return m == null ? DEFAULT_BACKGROUND : m.themeColour("hud_background", DEFAULT_BACKGROUND);
    }

    public static int text() {
        MenuManager m = LdicClient.menu();
        return (m == null ? DEFAULT_TEXT : m.themeColour("hud_text", DEFAULT_TEXT)) | 0xFF000000;
    }

    public static int accent() {
        int accent = InventoryStyle.accent();
        MenuManager m = LdicClient.menu();
        return (m == null ? accent : m.themeColour("hud_accent", accent)) | 0xFF000000;
    }

    /** ARGB {background, text, accent}. */
    public static int[] colours() {
        return new int[] {background(), text(), accent()};
    }
}
