package com.micatechnologies.minecraft.ldic.api;

/**
 * The entry point for other mods.
 *
 * <p>LDIC is a client-only mod, so call this from client-side code only (a client proxy, or
 * behind {@code event.getSide().isClient()}), and only when {@code Loader.isModLoaded("ldic")}
 * if LDIC is an optional dependency. Registration works at any time, in any load order: before
 * LDIC's own pre-init, during init, or later while the game runs.</p>
 *
 * <pre>{@code
 * LdicApi.menus().add(MenuLocation.PAUSE,
 *     MenuEntry.builder("mymod:settings")
 *         .label(() -> I18n.format("mymod.settings"))
 *         .opens(parent -> new MyModScreen(parent))
 *         .build());
 * }</pre>
 *
 * <p>Everything in this package is the supported API. Classes outside it are LDIC's internals
 * and change without notice.</p>
 */
public final class LdicApi {

    /** Raised when something is added to the API; a mod can check it before using newer parts. */
    public static final int API_VERSION = 1;

    private static final MenuRegistry MENUS = new MenuRegistry();

    private LdicApi() {
    }

    /** Entries on the title screen, the pause menu and the options screen. */
    public static MenuRegistry menus() {
        return MENUS;
    }
}
