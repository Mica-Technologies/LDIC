package com.micatechnologies.minecraft.ldic.theme;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import java.io.StringReader;
import java.util.Map;

/**
 * JSON helpers that work on the Gson 2.8.0 bundled with Minecraft 1.12.2, where
 * {@code JsonElement.deepCopy()} is not public yet.
 */
public final class Json {

    private Json() {
    }

    /**
     * Parses theme text leniently (comments and trailing commas are allowed), after stripping a
     * UTF-8 byte-order mark. Throws {@link com.google.gson.JsonParseException} on bad input.
     */
    public static JsonElement parse(String text) {
        if (!text.isEmpty() && text.charAt(0) == '﻿') {
            text = text.substring(1);
        }
        JsonReader reader = new JsonReader(new StringReader(text));
        reader.setLenient(true);
        return new JsonParser().parse(reader);
    }

    public static JsonElement copy(JsonElement e) {
        if (e.isJsonObject()) {
            return copy(e.getAsJsonObject());
        }
        if (e.isJsonArray()) {
            return copy(e.getAsJsonArray());
        }
        // Primitives and JsonNull are immutable.
        return e;
    }

    public static JsonObject copy(JsonObject o) {
        JsonObject out = new JsonObject();
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            out.add(e.getKey(), copy(e.getValue()));
        }
        return out;
    }

    public static JsonArray copy(JsonArray a) {
        JsonArray out = new JsonArray();
        for (JsonElement e : a) {
            out.add(copy(e));
        }
        return out;
    }
}
