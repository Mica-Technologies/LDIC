package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.api.LdicApi;
import com.micatechnologies.minecraft.ldic.api.MenuEntry;
import com.micatechnologies.minecraft.ldic.api.MenuLocation;
import com.micatechnologies.minecraft.ldic.api.MenuPage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Puts the entries other mods registered through {@link LdicApi} on the title, pause and
 * options screens, and runs them.
 *
 * <p>A theme screen that draws entries itself (it has a {@code menu_entry} action) gets them
 * through the {@code menu_entry_*} values. Every other screen, vanilla ones included, gets a
 * {@link ModEntryButton} per entry, added the way any mod adds a button, so an entry is
 * reachable whether or not the theme is in play.</p>
 *
 * <p>Entries are another mod's code: every call into one is guarded, and a failure is logged
 * once per entry instead of breaking the menu.</p>
 */
public final class ModMenus {

    /** First id of the entry buttons; far from vanilla's and from the ids mods usually pick. */
    private static final int BUTTON_ID = 0x4C4500;

    private static final Set<String> FAILED = new HashSet<>();

    private final MenuManager manager;

    ModMenus(MenuManager manager) {
        this.manager = manager;
        if (manager.settings().debug) {
            registerSamples();
        }
    }

    // -------------------------------------------------------------------------------------
    // Reading entries safely

    /** The entries showing at a location right now, in display order. */
    public static List<MenuEntry> shown(MenuLocation location) {
        List<MenuEntry> out = new ArrayList<>();
        if (location == null) {
            return out;
        }
        for (MenuEntry e : LdicApi.menus().entries(location)) {
            try {
                if (e.visible().getAsBoolean()) {
                    out.add(e);
                }
            } catch (RuntimeException ex) {
                failed(e, ex);
            }
        }
        return out;
    }

    /** The entry at a position among those showing at a location (by theme name), or null. */
    public static MenuEntry shownAt(String location, int index) {
        List<MenuEntry> list = shown(MenuLocation.byKey(location));
        return index >= 0 && index < list.size() ? list.get(index) : null;
    }

    public static String label(MenuEntry e) {
        try {
            String s = e.label().get();
            return s == null ? "" : s;
        } catch (RuntimeException ex) {
            failed(e, ex);
            return e.id();
        }
    }

    public static String tooltip(MenuEntry e) {
        try {
            String s = e.tooltip() == null ? null : e.tooltip().get();
            return s == null ? "" : s;
        } catch (RuntimeException ex) {
            failed(e, ex);
            return "";
        }
    }

    public static boolean enabled(MenuEntry e) {
        try {
            return e.enabled().getAsBoolean();
        } catch (RuntimeException ex) {
            failed(e, ex);
            return false;
        }
    }

    private static void failed(MenuEntry e, Throwable t) {
        if (FAILED.add(e.id())) {
            Ldic.LOGGER.error("LDIC: menu entry '{}' failed (logged once per entry)", e.id(), t);
        }
    }

    // -------------------------------------------------------------------------------------
    // Running entries

    /** Runs an entry from the screen it is on. */
    void activate(MenuEntry e, GuiScreen on) {
        if (e == null || !enabled(e)) {
            return;
        }
        try {
            if (e.page() != null) {
                manager.openPage(e.page(), on);
            } else if (e.screen() != null) {
                GuiScreen next = e.screen().apply(on);
                if (next != null) {
                    Minecraft.getMinecraft().displayGuiScreen(next);
                }
            } else if (e.action() != null) {
                e.action().run();
            }
        } catch (RuntimeException | LinkageError ex) {
            Ldic.LOGGER.error("LDIC: menu entry '{}' failed when clicked", e.id(), ex);
        }
    }

    // -------------------------------------------------------------------------------------
    // Buttons on screens whose theme does not draw entries itself

    private MenuLocation locationOf(GuiScreen gui) {
        if (gui instanceof LdicMenuScreen) {
            // The same class runs every screen of the theme; only its title screen is the title screen.
            return "main".equals(((LdicMenuScreen) gui).screenId()) ? MenuLocation.MAIN : null;
        }
        if (gui instanceof GuiMainMenu) {
            return MenuLocation.MAIN;
        }
        if (gui instanceof GuiIngameMenu) {
            return MenuLocation.PAUSE;
        }
        if (gui instanceof GuiOptions) {
            return MenuLocation.OPTIONS;
        }
        return null;
    }

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.getGui();
        MenuLocation location = locationOf(gui);
        if (location == null) {
            return;
        }
        boolean themed = gui instanceof ThemeScreenDriver.Host;
        // The first entries are the theme's when it draws entries itself; the rest get buttons.
        int drawnByTheme = themed ? ThemeScreenDriver.entryCapacity(manager.theme(), location.key()) : 0;
        // Top right, one under another: clear of vanilla's own buttons on all three screens. A
        // themed screen then moves them into its area for other mods' buttons.
        List<MenuEntry> entries = shown(location);
        for (int i = drawnByTheme, row = 0; i < entries.size(); i++, row++) {
            event.getButtonList().add(new ModEntryButton(BUTTON_ID + i, gui.width - 154, 4 + row * 24, entries.get(i), themed));
        }
    }

    @SubscribeEvent
    public void onAction(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.getButton() instanceof ModEntryButton) {
            // Cancelled so the screen's own id handling never sees an entry's button.
            event.setCanceled(true);
            Actions.playSound("minecraft:ui.button.click", 1, 1);
            activate(((ModEntryButton) event.getButton()).entry, event.getGui());
        }
    }

    // -------------------------------------------------------------------------------------
    // Debug samples

    private static boolean sampleFancy = true;
    private static double sampleRange = 12;
    private static double sampleOpacity = 0.75;
    private static int sampleQuality = 1;
    private static int sampleClicks;

    /**
     * With debug on, registers a sample entry and page on every location the way another mod
     * would, so a theme author can see where entries go and how a page looks in their theme.
     */
    private static void registerSamples() {
        MenuPage.Builder page = MenuPage.builder("Sample mod page")
                .section("Display")
                .toggle("Fancy mode", () -> sampleFancy, v -> sampleFancy = v).tooltip("A sample switch")
                .slider("Range", 2, 32, 1, () -> sampleRange, v -> sampleRange = v, v -> (int) v + " chunks")
                .slider("Opacity", 0, 1, 0, () -> sampleOpacity, v -> sampleOpacity = v)
                .enabledWhen(() -> sampleFancy)
                .choice("Quality", Arrays.asList("Low", "Medium", "High"), () -> sampleQuality, v -> sampleQuality = v)
                .section("Actions")
                .button("Count a click", () -> sampleClicks++)
                .text(() -> "Clicked " + sampleClicks + " time" + (sampleClicks == 1 ? "" : "s"))
                .section("More rows, for scrolling");
        for (int i = 1; i <= 12; i++) {
            boolean[] state = new boolean[1];
            page.toggle("Extra switch " + i, () -> state[0], v -> state[0] = v);
        }
        page.onClose(() -> Ldic.LOGGER.info("LDIC: sample mod page closed (a mod would save its config here)"));
        MenuEntry entry = MenuEntry.builder("ldic:sample_page").label("Sample mod page")
                .tooltip("A sample entry, shown because debug is on").page(page.build()).build();
        MenuEntry action = MenuEntry.builder("ldic:sample_action").label("Sample mod action").order(1001)
                .runs(() -> Ldic.LOGGER.info("LDIC: sample mod action ran")).build();
        for (MenuLocation l : MenuLocation.values()) {
            LdicApi.menus().add(l, entry);
        }
        LdicApi.menus().add(MenuLocation.PAUSE, action);
    }
}
