package com.micatechnologies.minecraft.ldic.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.theme.Json;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * {@code config/ldic/settings.json}: switches that are not part of a theme. Missing keys keep
 * their defaults, and the file is rewritten with every key so packs and players can see what
 * exists. Read once on the loading-screen thread and again at pre-init.
 */
public final class LdicSettings {

    /** Replace the vanilla main menu with the LDIC theme. */
    public boolean menuEnabled = true;
    /** Seconds the first menu after loading fades in from black; 0 for none. */
    public double menuFadeIn = 0.6;
    /** Replace Forge's loading screen with the LDIC theme. */
    public boolean loadingEnabled = true;
    /** Loading-screen frame cap. Lower leaves more CPU for loading itself. */
    public int loadingFps = 60;
    /** With debug on: keep the loading screen up this long after loading ends, to preview a theme. */
    public double loadingHoldSeconds = 0;

    /** Show the photosensitivity notice before the loading screen. Packs turn this on. */
    public boolean photosensitivityNotice = false;
    /** Show it every launch instead of only until the player has seen it once. */
    public boolean photosensitivityAlways = false;
    /** Seconds the notice stays up. */
    public double photosensitivitySeconds = 7;

    /** Player choice: themes should keep motion to a minimum (built-in themes honour it). */
    public boolean reducedMotion = false;
    /** Player choice: theme music volume, 0..1, on top of the game's Master and Music sliders. */
    public double musicVolume = 1;

    /** Shown to themes as {@code pack_name} / {@code pack_version}; launchers can set these. */
    public String packName = "";
    public String packVersion = "";
    /** Lists every theme problem on screen, not just a count; saves loading screenshots. */
    public boolean debug = false;

    public static LdicSettings load(File dir) {
        LdicSettings s = new LdicSettings();
        File file = new File(dir, "settings.json");
        if (file.isFile()) {
            try {
                JsonElement root = Json.parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
                if (root.isJsonObject()) {
                    s.read(root.getAsJsonObject());
                }
            } catch (IOException | RuntimeException e) {
                Ldic.LOGGER.error("Could not read {}; using defaults", file, e);
                return s;
            }
        }
        s.save(file);
        return s;
    }

    private void read(JsonObject o) {
        JsonObject menu = obj(o, "menu");
        if (menu != null) {
            menuEnabled = bool(menu, "enabled", menuEnabled);
            menuFadeIn = Math.max(0, num(menu, "fade_in", menuFadeIn));
        }
        JsonObject loading = obj(o, "loading");
        if (loading != null) {
            loadingEnabled = bool(loading, "enabled", loadingEnabled);
            loadingFps = (int) Math.max(10, Math.min(240, num(loading, "fps", loadingFps)));
            loadingHoldSeconds = Math.max(0, Math.min(120, num(loading, "hold_seconds", loadingHoldSeconds)));
        }
        JsonObject notice = obj(o, "photosensitivity_notice");
        if (notice != null) {
            photosensitivityNotice = bool(notice, "enabled", photosensitivityNotice);
            photosensitivityAlways = bool(notice, "always", photosensitivityAlways);
            photosensitivitySeconds = Math.max(2, Math.min(60, num(notice, "seconds", photosensitivitySeconds)));
        }
        JsonObject player = obj(o, "player");
        if (player != null) {
            reducedMotion = bool(player, "reduced_motion", reducedMotion);
            musicVolume = Math.max(0, Math.min(1, num(player, "music_volume", musicVolume)));
        }
        JsonObject pack = obj(o, "pack");
        if (pack != null) {
            packName = str(pack, "name", packName);
            packVersion = str(pack, "version", packVersion);
        }
        debug = bool(o, "debug", debug);
    }

    private void save(File file) {
        JsonObject o = new JsonObject();
        JsonObject menu = new JsonObject();
        menu.addProperty("enabled", menuEnabled);
        menu.addProperty("fade_in", menuFadeIn);
        o.add("menu", menu);
        JsonObject loading = new JsonObject();
        loading.addProperty("enabled", loadingEnabled);
        loading.addProperty("fps", loadingFps);
        loading.addProperty("hold_seconds", loadingHoldSeconds);
        o.add("loading", loading);
        JsonObject notice = new JsonObject();
        notice.addProperty("enabled", photosensitivityNotice);
        notice.addProperty("always", photosensitivityAlways);
        notice.addProperty("seconds", photosensitivitySeconds);
        o.add("photosensitivity_notice", notice);
        JsonObject player = new JsonObject();
        player.addProperty("reduced_motion", reducedMotion);
        player.addProperty("music_volume", musicVolume);
        o.add("player", player);
        JsonObject pack = new JsonObject();
        pack.addProperty("name", packName);
        pack.addProperty("version", packVersion);
        o.add("pack", pack);
        o.addProperty("debug", debug);
        try {
            Files.write(file.toPath(), new GsonBuilder().setPrettyPrinting().create().toJson(o).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Ldic.LOGGER.warn("Could not write {}", file, e);
        }
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
