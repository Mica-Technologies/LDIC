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
 *   <li>Objects merge key by key, recursively. A {@code null} value in the child deletes the key.
 *       An object whose {@code "type"} differs from the parent's (an action changing from
 *       {@code open} to {@code connect}) replaces it instead.</li>
 *   <li>Arrays whose entries are all objects with an {@code "id"} merge by id: a child entry
 *       with a parent's id deep-merges into it in place, even when that element is nested in a
 *       group's {@code children} or a factory's {@code template}; a new id is appended. Any
 *       other array in the child replaces the parent's.</li>
 *   <li>An object may list ids in {@code "remove": [...]}: elements with those ids are dropped
 *       from the arrays inside that object, at any depth of {@code children}.</li>
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
            if (existing != null && existing.isJsonObject() && value.isJsonObject() && typeChanges(existing.getAsJsonObject(), value.getAsJsonObject())) {
                // A different kind of thing (an "open" action becoming "connect"): the old keys mean
                // nothing for the new type, so replace instead of merging.
                out.add(key, Json.copy(value));
            } else if (existing != null && existing.isJsonObject() && value.isJsonObject()) {
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

    private static boolean typeChanges(JsonObject parent, JsonObject child) {
        JsonElement a = parent.get("type");
        JsonElement b = child.get("type");
        return a != null && b != null && a.isJsonPrimitive() && b.isJsonPrimitive() && !a.getAsString().equals(b.getAsString());
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
            if (existing != null) {
                byId.put(id, typeChanges(existing, o) ? Json.copy(o) : merge(existing, o));
                continue;
            }
            // Not at this level: an id names an element anywhere in the tree, so look inside
            // groups and factory templates before treating it as a new element.
            boolean merged = false;
            for (JsonObject candidate : byId.values()) {
                if (mergeNested(candidate, id, o)) {
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                byId.put(id, Json.copy(o));
            }
        }
        JsonArray out = new JsonArray();
        for (JsonObject o : byId.values()) {
            out.add(o);
        }
        return out;
    }

    /** Merges {@code child} into the element with {@code id} under {@code node}; false if there is none. */
    private static boolean mergeNested(JsonObject node, String id, JsonObject child) {
        JsonElement children = node.get("children");
        if (children != null && children.isJsonArray()) {
            JsonArray a = children.getAsJsonArray();
            for (int i = 0; i < a.size(); i++) {
                if (!a.get(i).isJsonObject()) {
                    continue;
                }
                JsonObject c = a.get(i).getAsJsonObject();
                if (c.has("id") && c.get("id").getAsString().equals(id)) {
                    a.set(i, typeChanges(c, child) ? Json.copy(child) : merge(c, child));
                    return true;
                }
                if (mergeNested(c, id, child)) {
                    return true;
                }
            }
        }
        JsonElement template = node.get("template");
        if (template != null && template.isJsonObject()) {
            JsonObject t = template.getAsJsonObject();
            if (t.has("id") && t.get("id").getAsString().equals(id)) {
                node.add("template", typeChanges(t, child) ? Json.copy(child) : merge(t, child));
                return true;
            }
            return mergeNested(t, id, child);
        }
        return false;
    }

    /** Removes elements with these ids from the array and, recursively, from their children. */
    private static void removeIds(JsonArray array, Set<String> ids) {
        for (int i = array.size() - 1; i >= 0; i--) {
            JsonElement e = array.get(i);
            if (!e.isJsonObject()) {
                continue;
            }
            JsonObject o = e.getAsJsonObject();
            if (o.has("id") && ids.contains(o.get("id").getAsString())) {
                array.remove(i);
                continue;
            }
            JsonElement children = o.get("children");
            if (children != null && children.isJsonArray()) {
                removeIds(children.getAsJsonArray(), ids);
            }
        }
    }
}
