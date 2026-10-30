package com.micatechnologies.minecraft.ldic.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.expr.Library;
import com.micatechnologies.minecraft.ldic.inventory.ItemSort;
import com.micatechnologies.minecraft.ldic.theme.Json;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code config/ldic/settings.json} (the pack's switches) and {@code config/ldic/player.json}
 * (the player's own preferences). Missing keys keep their defaults.
 * Read once on the loading-screen thread and again at pre-init.
 */
public final class LdicSettings {

    /** Replace the vanilla main menu with the LDIC theme. */
    public boolean menuEnabled = true;
    /** Replace the vanilla options screen with the menu theme's "options" screen, when it has one. */
    public boolean optionsEnabled = true;
    /** Replace the vanilla pause menu with the menu theme's "pause" screen, when it has one. */
    public boolean pauseEnabled = true;
    /** Draw the menu theme's "world_loading" screen while a world loads, when it has one. */
    public boolean worldLoadingEnabled = true;
    /** Replace vanilla's world selection with the menu theme's "worlds" screen, when it has one. */
    public boolean worldsEnabled = true;
    /** Replace vanilla's server list with the menu theme's "servers" screen, when it has one. */
    public boolean serversEnabled = true;
    /** Seconds the first menu after loading fades in from black; 0 for none. */
    public double menuFadeIn = 0.6;
    /** Replace Forge's loading screen with the LDIC theme. */
    public boolean loadingEnabled = true;
    /** Loading-screen frame cap. Lower leaves more CPU for loading itself. */
    public int loadingFps = 60;
    /** With debug on: keep the loading screen up this long after loading ends, to preview a theme. */
    public double loadingHoldSeconds = 0;

    /** Show a logo card (Mojang's, or the pack's own) first, while loading starts behind it. */
    public boolean logoScreen = true;
    /** Seconds the logo card stays up. */
    public double logoSeconds = 3;

    /** Show the photosensitivity notice before the loading screen. On unless a pack opts out. */
    public boolean photosensitivityNotice = true;
    /** Show it every launch instead of only until the player has seen it once. */
    public boolean photosensitivityAlways = true;
    /** Seconds the notice stays up. */
    public double photosensitivitySeconds = 5;

    /** Holding Space this long skips the logo card or the notice; 0 turns skipping off. */
    public double introSkipHoldSeconds = 1.5;

    /** Player choice: themes should keep motion to a minimum (built-in themes honour it). */
    public boolean reducedMotion = false;
    /** Player choice: theme music volume, 0..1, on top of the game's Master and Music sliders. */
    public double musicVolume = 1;
    /** Player choice: the loading screen's F3 details panel is open (remembered between launches). */
    public boolean loadingDetails = false;
    /** Player choice: each creative tab's item order, by tab label (remembered between launches). */
    public final Map<String, ItemSort.Pref> creativeSort = new LinkedHashMap<>();

    // Hotbar and inventory: the pack sets defaults in settings.json; the player's player.json wins.
    /** "modern" (LDIC's hotbar) or "vanilla". */
    public String hotbarStyle = "modern";
    /** Size of the hotbar and the bars above it, relative to the GUI scale (0.5 to 2). */
    public double hotbarScale = 1;
    /** Slide the hotbar away once the player has been moving for {@link #hotbarHideDelay} seconds. */
    public boolean hotbarAutoHide = false;
    public double hotbarHideDelay = 2;
    /** Open inventories full screen: creative's grid fills the screen, other inventories scale up. */
    public boolean inventoryFullScreen = false;
    /** Also scale up other mods' inventory screens in full-screen mode (off: some draw their own overlays). */
    public boolean inventoryScaleModScreens = false;
    /** Draw inventories with the menu theme's inventory screens, when it has them. */
    public boolean inventoryThemed = true;
    /** Accent for LDIC's inventory and hotbar when the menu theme has no "accent" constant. */
    public int inventoryAccent = 0xFF4FC3F7;

    /** Shown to themes as {@code pack_name} / {@code pack_version}; launchers can set these. */
    public String packName = "";
    public String packVersion = "";
    /** Lists every theme problem on screen, not just a count; saves loading screenshots. */
    public boolean debug = false;

    /**
     * Reads {@code settings.json} (the pack's) and then {@code player.json} (the player's own
     * choices, which win). Each is written with defaults only when it does not exist yet: a
     * launcher-managed pack ships settings.json, and rewriting it would make the launcher think
     * it changed; player.json is never shipped, so pack updates cannot reset a player's choices.
     */
    public static LdicSettings load(File dir) {
        LdicSettings s = new LdicSettings();
        File file = new File(dir, "settings.json");
        if (file.isFile()) {
            JsonObject root = readObject(file);
            if (root != null) {
                s.read(root);
            }
        } else {
            s.save(file);
        }
        File player = new File(dir, "player.json");
        if (player.isFile()) {
            JsonObject root = readObject(player);
            if (root != null) {
                s.readPlayer(root);
            }
        } else {
            s.savePlayer(player);
        }
        return s;
    }

    private static JsonObject readObject(File file) {
        try {
            JsonElement root = Json.parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            return root.isJsonObject() ? root.getAsJsonObject() : null;
        } catch (IOException | RuntimeException e) {
            Ldic.LOGGER.error("Could not read {}; using defaults", file, e);
            return null;
        }
    }

    private void readPlayer(JsonObject player) {
        reducedMotion = bool(player, "reduced_motion", reducedMotion);
        musicVolume = Math.max(0, Math.min(1, num(player, "music_volume", musicVolume)));
        loadingDetails = bool(player, "loading_details", loadingDetails);
        readHotbarAndInventory(player);
        JsonObject sorts = obj(player, "creative_sort");
        for (Map.Entry<String, JsonElement> e : sorts == null ? new JsonObject().entrySet() : sorts.entrySet()) {
            if (e.getValue().isJsonObject()) {
                JsonObject s = e.getValue().getAsJsonObject();
                creativeSort.put(e.getKey(), new ItemSort.Pref(ItemSort.Order.byKey(str(s, "order", "default")),
                        bool(s, "descending", false)));
            }
        }
    }

    /**
     * Remembers the player's F3 choice on the loading screen in {@code player.json}, keeping
     * whatever else the file holds.
     */
    public void saveLoadingDetails(File dir, boolean on) {
        loadingDetails = on;
        File file = new File(dir, "player.json");
        JsonObject o = file.isFile() ? readObject(file) : null;
        if (o == null) {
            o = new JsonObject();
        }
        o.addProperty("loading_details", on);
        write(file, o);
    }

    /** Remembers a creative tab's item order in {@code player.json}, keeping whatever else it holds. */
    public void saveCreativeSort(File dir, String tab, ItemSort.Pref pref) {
        if (pref.isDefault()) {
            creativeSort.remove(tab);
        } else {
            creativeSort.put(tab, pref);
        }
        File file = new File(dir, "player.json");
        JsonObject o = file.isFile() ? readObject(file) : null;
        if (o == null) {
            o = new JsonObject();
        }
        JsonObject sorts = new JsonObject();
        for (Map.Entry<String, ItemSort.Pref> e : creativeSort.entrySet()) {
            JsonObject s = new JsonObject();
            s.addProperty("order", e.getValue().order.key);
            s.addProperty("descending", e.getValue().descending);
            sorts.add(e.getKey(), s);
        }
        o.add("creative_sort", sorts);
        write(file, o);
    }

    private void savePlayer(File file) {
        JsonObject o = new JsonObject();
        o.addProperty("_readme", "Your own LDIC preferences. Modpacks do not overwrite this file.");
        o.addProperty("reduced_motion", reducedMotion);
        o.addProperty("music_volume", musicVolume);
        o.addProperty("loading_details", loadingDetails);
        write(file, o);
    }

    private static void write(File file, JsonObject o) {
        try {
            Files.write(file.toPath(), new GsonBuilder().setPrettyPrinting().create().toJson(o).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Ldic.LOGGER.warn("Could not write {}", file, e);
        }
    }

    private void read(JsonObject o) {
        JsonObject menu = obj(o, "menu");
        if (menu != null) {
            menuEnabled = bool(menu, "enabled", menuEnabled);
            menuFadeIn = Math.max(0, num(menu, "fade_in", menuFadeIn));
            optionsEnabled = bool(menu, "themed_options", optionsEnabled);
            pauseEnabled = bool(menu, "themed_pause", pauseEnabled);
            worldLoadingEnabled = bool(menu, "themed_world_loading", worldLoadingEnabled);
            worldsEnabled = bool(menu, "themed_worlds", worldsEnabled);
            serversEnabled = bool(menu, "themed_servers", serversEnabled);
        }
        JsonObject loading = obj(o, "loading");
        if (loading != null) {
            loadingEnabled = bool(loading, "enabled", loadingEnabled);
            loadingFps = (int) Math.max(10, Math.min(240, num(loading, "fps", loadingFps)));
            loadingHoldSeconds = Math.max(0, Math.min(120, num(loading, "hold_seconds", loadingHoldSeconds)));
        }
        JsonObject logo = obj(o, "logo_screen");
        if (logo != null) {
            logoScreen = bool(logo, "enabled", logoScreen);
            logoSeconds = Math.max(1, Math.min(30, num(logo, "seconds", logoSeconds)));
        }
        JsonObject skip = obj(o, "intro_skip");
        if (skip != null) {
            introSkipHoldSeconds = Math.max(0, Math.min(10, num(skip, "hold_seconds", introSkipHoldSeconds)));
        }
        JsonObject notice = obj(o, "photosensitivity_notice");
        if (notice != null) {
            photosensitivityNotice = bool(notice, "enabled", photosensitivityNotice);
            photosensitivityAlways = bool(notice, "always", photosensitivityAlways);
            photosensitivitySeconds = Math.max(2, Math.min(60, num(notice, "seconds", photosensitivitySeconds)));
        }
        JsonObject pack = obj(o, "pack");
        if (pack != null) {
            packName = str(pack, "name", packName);
            packVersion = str(pack, "version", packVersion);
        }
        debug = bool(o, "debug", debug);
        readHotbarAndInventory(o);
    }

    private void readHotbarAndInventory(JsonObject o) {
        JsonObject hotbar = obj(o, "hotbar");
        if (hotbar != null) {
            String style = str(hotbar, "style", hotbarStyle);
            hotbarStyle = "vanilla".equalsIgnoreCase(style) ? "vanilla" : "modern";
            hotbarScale = Math.max(0.5, Math.min(2, num(hotbar, "scale", hotbarScale)));
            hotbarAutoHide = bool(hotbar, "auto_hide", hotbarAutoHide);
            hotbarHideDelay = Math.max(0, Math.min(30, num(hotbar, "hide_delay", hotbarHideDelay)));
        }
        JsonObject inventory = obj(o, "inventory");
        if (inventory != null) {
            inventoryFullScreen = bool(inventory, "full_screen", inventoryFullScreen);
            inventoryScaleModScreens = bool(inventory, "scale_mod_screens", inventoryScaleModScreens);
            inventoryThemed = bool(inventory, "themed", inventoryThemed);
            Long accent = Library.parseHexColour(str(inventory, "accent", null));
            if (accent != null) {
                inventoryAccent = (int) (long) accent;
            }
        }
    }

    /** Saves the player's hotbar and inventory choices in {@code player.json}, keeping the rest. */
    public void saveHotbarAndInventory(File dir) {
        File file = new File(dir, "player.json");
        JsonObject o = file.isFile() ? readObject(file) : null;
        if (o == null) {
            o = new JsonObject();
        }
        JsonObject hotbar = new JsonObject();
        hotbar.addProperty("style", hotbarStyle);
        hotbar.addProperty("scale", hotbarScale);
        hotbar.addProperty("auto_hide", hotbarAutoHide);
        hotbar.addProperty("hide_delay", hotbarHideDelay);
        o.add("hotbar", hotbar);
        JsonObject inventory = obj(o, "inventory");
        if (inventory == null) {
            inventory = new JsonObject();
        }
        inventory.addProperty("full_screen", inventoryFullScreen);
        inventory.addProperty("scale_mod_screens", inventoryScaleModScreens);
        o.add("inventory", inventory);
        write(file, o);
    }

    private void save(File file) {
        JsonObject o = new JsonObject();
        JsonObject menu = new JsonObject();
        menu.addProperty("enabled", menuEnabled);
        menu.addProperty("fade_in", menuFadeIn);
        menu.addProperty("themed_options", optionsEnabled);
        menu.addProperty("themed_pause", pauseEnabled);
        menu.addProperty("themed_world_loading", worldLoadingEnabled);
        menu.addProperty("themed_worlds", worldsEnabled);
        menu.addProperty("themed_servers", serversEnabled);
        o.add("menu", menu);
        JsonObject loading = new JsonObject();
        loading.addProperty("enabled", loadingEnabled);
        loading.addProperty("fps", loadingFps);
        loading.addProperty("hold_seconds", loadingHoldSeconds);
        o.add("loading", loading);
        JsonObject logo = new JsonObject();
        logo.addProperty("enabled", logoScreen);
        logo.addProperty("seconds", logoSeconds);
        o.add("logo_screen", logo);
        JsonObject notice = new JsonObject();
        notice.addProperty("enabled", photosensitivityNotice);
        notice.addProperty("always", photosensitivityAlways);
        notice.addProperty("seconds", photosensitivitySeconds);
        o.add("photosensitivity_notice", notice);
        JsonObject skip = new JsonObject();
        skip.addProperty("hold_seconds", introSkipHoldSeconds);
        o.add("intro_skip", skip);
        JsonObject pack = new JsonObject();
        pack.addProperty("name", packName);
        pack.addProperty("version", packVersion);
        o.add("pack", pack);
        JsonObject hotbar = new JsonObject();
        hotbar.addProperty("style", hotbarStyle);
        hotbar.addProperty("scale", hotbarScale);
        hotbar.addProperty("auto_hide", hotbarAutoHide);
        hotbar.addProperty("hide_delay", hotbarHideDelay);
        o.add("hotbar", hotbar);
        JsonObject inventory = new JsonObject();
        inventory.addProperty("full_screen", inventoryFullScreen);
        inventory.addProperty("scale_mod_screens", inventoryScaleModScreens);
        inventory.addProperty("themed", inventoryThemed);
        inventory.addProperty("accent", String.format("#%06X", inventoryAccent & 0xFFFFFF));
        o.add("inventory", inventory);
        o.addProperty("debug", debug);
        write(file, o);
    }

    private static JsonObject obj(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    private static String str(JsonObject o, String key, String fallback) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : fallback;
    }

    private static boolean bool(JsonObject o, String key, boolean fallback) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() ? e.getAsBoolean() : fallback;
    }

    private static double num(JsonObject o, String key, double fallback) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsDouble() : fallback;
    }
}
