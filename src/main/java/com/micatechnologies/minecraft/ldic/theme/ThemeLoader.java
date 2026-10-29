package com.micatechnologies.minecraft.ldic.theme;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Loads a theme document and its {@code extends} ancestors, and merges them into one JSON
 * object (child over parent, see {@link JsonMerge}).
 *
 * <p>Before merging, relative asset paths in each document are rewritten to
 * {@code theme://<location>/<path>}, so a path keeps pointing at the folder of the file that
 * wrote it even after a child theme inherits it.</p>
 */
public final class ThemeLoader {

    /** Prefix for asset paths that resolve against a theme document's location. */
    public static final String THEME_SCHEME = "theme://";

    private static final int MAX_DEPTH = 8;

    private ThemeLoader() {
    }

    /** The merged document plus the names of every file it was built from, child first. */
    public static final class Result {
        public final JsonObject json;
        public final List<String> chain;

        Result(JsonObject json, List<String> chain) {
            this.json = json;
            this.chain = chain;
        }
    }

    /**
     * Loads {@code document} (already opened) and its ancestors. Returns null, with an error in
     * {@code problems}, when the document itself cannot be parsed. A broken ancestor is reported
     * and skipped, so the child still loads over whatever ancestors did load.
     */
    public static Result load(ThemeSource source, ThemeSource.Document document, Problems problems) {
        List<JsonObject> docs = new ArrayList<>();
        List<String> chain = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        ThemeSource.Document current = document;
        String ref = null;
        while (true) {
            problems.setSource(current.name);
            JsonObject json = parse(current, problems);
            if (json == null) {
                if (docs.isEmpty()) {
                    return null;
                }
                break;
            }
            rewriteAssets(json, current.location);
            docs.add(json);
            chain.add(current.name);
            JsonElement ext = json.get("extends");
            if (ext == null || ext.isJsonNull()) {
                break;
            }
            if (!ext.isJsonPrimitive()) {
                problems.error("extends", "must be a theme name such as \"builtin:classic\"");
                break;
            }
            ref = ext.getAsString();
            if (!seen.add(ref)) {
                problems.error("extends", "theme '" + ref + "' extends itself (cycle: " + chain + ")");
                break;
            }
            if (docs.size() >= MAX_DEPTH) {
                problems.error("extends", "more than " + MAX_DEPTH + " levels of extends");
                break;
            }
            try {
                current = source.open(ref);
            } catch (IOException e) {
                problems.error("extends", "cannot open theme '" + ref + "': " + e.getMessage());
                break;
            }
        }
        problems.setSource(document.name);
        JsonObject merged = docs.get(docs.size() - 1);
        for (int i = docs.size() - 2; i >= 0; i--) {
            merged = JsonMerge.merge(merged, docs.get(i));
        }
        merged.remove("extends");
        return new Result(merged, chain);
    }

    private static JsonObject parse(ThemeSource.Document doc, Problems problems) {
        try {
            JsonElement e = Json.parse(doc.text);
            if (!e.isJsonObject()) {
                problems.error("", "a theme file must contain a JSON object");
                return null;
            }
            return e.getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            String msg = e.getMessage();
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                msg = e.getCause().getMessage();
            }
            problems.error("", "invalid JSON: " + msg);
            return null;
        }
    }

    /** True for paths that already say where they live (URLs, resource locations, theme://). */
    public static boolean isQualified(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith(THEME_SCHEME)) {
            return true;
        }
        int colon = path.indexOf(':');
        int slash = path.indexOf('/');
        // "minecraft:textures/x.png" is a resource location; "C:/x" would be a drive, but theme
        // paths are never absolute, so any colon before the first slash means a namespace.
        return colon > 0 && (slash < 0 || colon < slash);
    }

    static void rewriteAssets(JsonObject o, String location) {
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            String key = e.getKey();
            JsonElement v = e.getValue();
            if (key.equals("fonts") && v.isJsonObject()) {
                for (Map.Entry<String, JsonElement> f : v.getAsJsonObject().entrySet()) {
                    JsonElement nv = rewritePath("font", f.getValue(), location);
                    if (nv != f.getValue()) {
                        f.setValue(nv);
                    }
                }
                continue;
            }
            if (ElementSchema.ASSET_KEYS.contains(key)) {
                if (v.isJsonArray()) {
                    JsonArray a = v.getAsJsonArray();
                    for (int i = 0; i < a.size(); i++) {
                        a.set(i, rewritePath(key, a.get(i), location));
                    }
                } else {
                    JsonElement nv = rewritePath(key, v, location);
                    if (nv != v) {
                        e.setValue(nv);
                    }
                }
                continue;
            }
            if (v.isJsonObject()) {
                rewriteAssets(v.getAsJsonObject(), location);
            } else if (v.isJsonArray()) {
                for (JsonElement item : v.getAsJsonArray()) {
                    if (item.isJsonObject()) {
                        rewriteAssets(item.getAsJsonObject(), location);
                    }
                }
            }
        }
    }

    private static JsonElement rewritePath(String key, JsonElement v, String location) {
        if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isString()) {
            return v;
        }
        String path = v.getAsString().trim();
        if (path.isEmpty() || isQualified(path)) {
            return v;
        }
        if (key.equals("font")) {
            String lower = path.toLowerCase(Locale.ROOT);
            // Font names (minecraft, sans, an alias) stay names; only font files are paths.
            if (!lower.endsWith(".ttf") && !lower.endsWith(".otf")) {
                return v;
            }
        }
        while (path.startsWith("./")) {
            path = path.substring(2);
        }
        return new JsonPrimitive(THEME_SCHEME + location + "/" + path);
    }
}
