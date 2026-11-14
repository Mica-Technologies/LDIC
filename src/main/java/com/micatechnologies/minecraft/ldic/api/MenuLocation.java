package com.micatechnologies.minecraft.ldic.api;

import java.util.Locale;

/** The screens a mod can add a {@link MenuEntry} to. */
public enum MenuLocation {

    /** The title screen. */
    MAIN,
    /** The pause menu (Escape in a world). */
    PAUSE,
    /** The options screen, from the title screen or the pause menu. */
    OPTIONS;

    /** The name themes use for this location: {@code main}, {@code pause} or {@code options}. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The location a theme names, or null. */
    public static MenuLocation byKey(String key) {
        for (MenuLocation l : values()) {
            if (l.key().equalsIgnoreCase(key)) {
                return l;
            }
        }
        return null;
    }
}
