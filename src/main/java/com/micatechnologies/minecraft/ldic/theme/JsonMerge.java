package com.micatechnologies.minecraft.ldic.theme;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Merges a theme over the theme it {@code extends}.
 *
 * <ul>
 *   <li>Objects merge key by key, recursively. A {@code null} value in the child deletes the key.</li>
 *   <li>Arrays whose entries are all objects with an {@code "id"} merge by id: a child entry
 *       with a parent's id deep-merges into it in place, a new id is appended. Any other array
 *       in the child replaces the parent's.</li>
 *   <li>An object may list ids in {@code "remove": [...]}: entries with those ids are dropped
 *       from every id-keyed array directly inside that object.</li>
 * </ul>
 */
public final class JsonMerge {

    private JsonMerge() {
    }

    public static JsonObject merge(JsonObject parent, JsonObject child) {
        JsonObject out = Json.copy(parent);
        for (Map.Entry<String, JsonElement> e : child.entrySet()) {
            String key = e.getKey();
            JsonElement value = e.getValue();
            if (key.equals("remove")) {
                continue;
            }
            if (value.isJsonNull()) {
                out.remove(key);
                continue;
            }
            JsonElement existing = out.get(key);
            if (existing != null && existing.isJsonObject() && value.isJsonObject()) {
                out.add(key, merge(existing.getAsJsonObject(), value.getAsJsonObject()));
            } else if (existing != null && existing.isJsonArray() && value.isJsonArray()
                    && isIdArray(existing.getAsJsonArray()) && isIdArray(value.getAsJsonArray())) {
                out.add(key, mergeById(existing.getAsJsonArray(), value.getAsJsonArray()));
            } else {
                out.add(key, Json.copy(value));
            }
        }
        JsonElement remove = child.get("remove");
        if (remove != null && remove.isJsonArray()) {
            Set<String> ids = new HashSet<>();
            for (JsonElement id : remove.getAsJsonArray()) {
                if (id.isJsonPrimitive()) {
                    ids.add(id.getAsString());
                }
            }
            for (Map.Entry<String, JsonElement> e : out.entrySet()) {
                if (e.getValue().isJsonArray()) {
                    removeIds(e.getValue().getAsJsonArray(), ids);
                }
            }
        }
        return out;
    }

    private static boolean isIdArray(JsonArray array) {
        if (array.size() == 0) {
            return true;
        }
        for (JsonElement e : array) {
            if (!e.isJsonObject() || !e.getAsJsonObject().has("id")) {
                return false;
            }
        }
        return true;
    }

    private static JsonArray mergeById(JsonArray parent, JsonArray child) {
        Map<String, JsonObject> byId = new LinkedHashMap<>();
        for (JsonElement e : parent) {
            JsonObject o = e.getAsJsonObject();
            byId.put(o.get("id").getAsString(), o);
        }
        for (JsonElement e : child) {
            JsonObject o = e.getAsJsonObject();
            String id = o.get("id").getAsString();
            JsonObject existing = byId.get(id);
            byId.put(id, existing == null ? Json.copy(o) : merge(existing, o));
        }
        JsonArray out = new JsonArray();
        for (JsonObject o : byId.values()) {
            out.add(o);
        }
        return out;
    }

    private static void removeIds(JsonArray array, Set<String> ids) {
        for (int i = array.size() - 1; i >= 0; i--) {
            JsonElement e = array.get(i);
            if (e.isJsonObject() && e.getAsJsonObject().has("id")
                    && ids.contains(e.getAsJsonObject().get("id").getAsString())) {
                array.remove(i);
            }
        }
    }
}
