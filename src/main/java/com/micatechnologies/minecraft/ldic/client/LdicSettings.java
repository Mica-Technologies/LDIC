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
 * their defaults, and the file is rewritten with every key so packs can see what exists.
 */
public final class LdicSettings {

    /** Replace the vanilla main menu with the LDIC theme. */
    public boolean menuEnabled = true;
    /** Shown to themes as {@code pack_name} / {@code pack_version}; launchers can set these. */
    public String packName = "";
    public String packVersion = "";
    /** Lists every theme problem on screen, not just a count. */
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
        if (menu != null && menu.has("enabled")) {
            menuEnabled = menu.get("enabled").getAsBoolean();
        }
        JsonObject pack = obj(o, "pack");
        if (pack != null) {
            packName = str(pack, "name", packName);
            packVersion = str(pack, "version", packVersion);
        }
        if (o.has("debug")) {
            debug = o.get("debug").getAsBoolean();
        }
    }

    private void save(File file) {
        JsonObject o = new JsonObject();
        JsonObject menu = new JsonObject();
        menu.addProperty("enabled", menuEnabled);
        o.add("menu", menu);
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
}
