package com.micatechnologies.minecraft.ldic.expr;

import java.util.Locale;

/**
 * The value types of the theme expression language.
 *
 * <p>Colours are numbers ({@code 0xAARRGGBB}); a double holds every 32-bit colour exactly, and
 * the bitwise operators convert through {@code long}.</p>
 */
public enum Type {
    NUMBER,
    BOOL,
    STRING;

    /** Parses a parameter type name as written in a function signature. */
    public static Type parse(String name) {
        switch (name.toLowerCase(Locale.ROOT)) {
            case "number":
            case "num":
            case "double":
            case "int":
            case "long":
            case "colour":
            case "color":
                return NUMBER;
            case "bool":
            case "boolean":
                return BOOL;
            case "string":
            case "str":
                return STRING;
            default:
                return null;
        }
    }

    public String display() {
        return name().toLowerCase(Locale.ROOT);
    }
}
