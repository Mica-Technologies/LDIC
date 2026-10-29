package com.micatechnologies.minecraft.ldic.theme;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The element types a theme can use and the properties each accepts. This table is the single
 * source of truth for the theme format: the compiler validates against it, and the authoring
 * docs must match it.
 */
public final class ElementSchema {

    /** How a property's JSON value is read. */
    public enum Kind {
        /** Number literal or number expression. */
        NUMBER,
        /** Bool literal or bool expression. */
        BOOL,
        /** Number, {@code "#RRGGBB"}/{@code "#AARRGGBB"}, or a number expression. */
        COLOUR,
        /** Text with {@code {expr}} interpolation. */
        TEXT,
        /** Array of TEXT values. */
        TEXT_LIST,
        /** One of a fixed set of words; not an expression. */
        ENUM,
        /** Free-form static string (font names); not an expression. */
        STATIC,
        /** Static true/false; not an expression. */
        FLAG,
        /** Four numbers {@code [u0, v0, u1, v1]}, each a number or expression. */
        UV
    }

    public static final class Prop {
        public final String name;
        public final Kind kind;
        /** Default JSON-ish value (Number, Boolean, String) or null for "optional, no default". */
        public final Object def;
        public final List<String> values;

        Prop(String name, Kind kind, Object def, String... values) {
            this.name = name;
            this.kind = kind;
            this.def = def;
            this.values = Arrays.asList(values);
        }
    }

    /** Keys every element accepts; handled by the compiler directly. */
    public static final Set<String> COMMON = new HashSet<>(Arrays.asList(
            "type", "id", "anchor", "pivot", "x", "y", "width", "height", "visible", "opacity",
            "rotation", "scale", "z", "children", "action", "tooltip", "sound_hover", "sound_press",
            "enabled", "hover_time"));

    /** Keys whose string values are asset paths, resolved relative to the declaring theme file. */
    public static final Set<String> ASSET_KEYS = new HashSet<>(Arrays.asList(
            "image", "hover_image", "images", "icon", "font"));

    private static final Map<String, Map<String, Prop>> TYPES = new LinkedHashMap<>();
    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        type("rect",
                new Prop("colour", Kind.COLOUR, 0x00000000L),
                new Prop("colour_bottom", Kind.COLOUR, null),
                new Prop("gradient", Kind.ENUM, "vertical", "vertical", "horizontal"),
                new Prop("border_colour", Kind.COLOUR, null),
                new Prop("border_width", Kind.NUMBER, 1),
                new Prop("radius", Kind.NUMBER, 0));
        ALIASES.put("group", "rect");
        ALIASES.put("panel", "rect");

        type("image",
                new Prop("image", Kind.TEXT, null),
                new Prop("hover_image", Kind.TEXT, null),
                new Prop("images", Kind.TEXT_LIST, null),
                new Prop("fit", Kind.ENUM, "stretch", "stretch", "cover", "contain", "tile", "center"),
                new Prop("tint", Kind.COLOUR, 0xFFFFFFFFL),
                new Prop("uv", Kind.UV, null),
                new Prop("frames", Kind.NUMBER, 1),
                new Prop("fps", Kind.NUMBER, 10),
                new Prop("frame", Kind.NUMBER, null),
                new Prop("filter", Kind.ENUM, "linear", "linear", "nearest"),
                new Prop("duration", Kind.NUMBER, 6),
                new Prop("fade", Kind.NUMBER, 1),
                new Prop("shuffle", Kind.FLAG, false));

        type("text",
                new Prop("text", Kind.TEXT, ""),
                new Prop("font", Kind.STATIC, "minecraft"),
                new Prop("size", Kind.NUMBER, 9),
                new Prop("colour", Kind.COLOUR, 0xFFFFFFFFL),
                new Prop("shadow", Kind.BOOL, true),
                new Prop("align", Kind.ENUM, "left", "left", "center", "right"),
                new Prop("wrap", Kind.FLAG, false),
                new Prop("line_spacing", Kind.NUMBER, 1));

        type("panorama",
                new Prop("images", Kind.TEXT, "minecraft:textures/gui/title/background/panorama_#.png"),
                new Prop("speed", Kind.NUMBER, 2),
                new Prop("angle", Kind.NUMBER, null),
                new Prop("pitch", Kind.NUMBER, null),
                new Prop("fov", Kind.NUMBER, 85),
                new Prop("blur", Kind.NUMBER, 0),
                new Prop("tint", Kind.COLOUR, 0xFFFFFFFFL),
                new Prop("filter", Kind.ENUM, "linear", "linear", "nearest"));

        type("button",
                new Prop("text", Kind.TEXT, ""),
                new Prop("font", Kind.STATIC, "minecraft"),
                new Prop("size", Kind.NUMBER, 9),
                new Prop("style", Kind.ENUM, "vanilla", "vanilla", "flat", "image", "none"),
                new Prop("colour", Kind.COLOUR, 0xA0000000L),
                new Prop("hover_colour", Kind.COLOUR, null),
                new Prop("pressed_colour", Kind.COLOUR, null),
                new Prop("disabled_colour", Kind.COLOUR, null),
                new Prop("text_colour", Kind.COLOUR, 0xFFE0E0E0L),
                new Prop("hover_text_colour", Kind.COLOUR, 0xFFFFFFA0L),
                new Prop("disabled_text_colour", Kind.COLOUR, 0xFFA0A0A0L),
                new Prop("border_colour", Kind.COLOUR, null),
                new Prop("hover_border_colour", Kind.COLOUR, null),
                new Prop("border_width", Kind.NUMBER, 1),
                new Prop("radius", Kind.NUMBER, 0),
                new Prop("image", Kind.TEXT, null),
                new Prop("sheet_rows", Kind.NUMBER, 2),
                new Prop("filter", Kind.ENUM, "linear", "linear", "nearest"),
                new Prop("icon", Kind.TEXT, null),
                new Prop("icon_size", Kind.NUMBER, 16),
                new Prop("icon_tint", Kind.COLOUR, 0xFFFFFFFFL),
                new Prop("text_align", Kind.ENUM, "center", "left", "center", "right"),
                new Prop("padding", Kind.NUMBER, 6),
                new Prop("shadow", Kind.BOOL, true),
                new Prop("wrap", Kind.STATIC, null));

        // Factories are compiled specially (trigger/count/keep/template); listed for validation.
        type("factory",
                new Prop("trigger", Kind.STATIC, null),
                new Prop("count", Kind.STATIC, null),
                new Prop("max", Kind.STATIC, null),
                new Prop("keep", Kind.STATIC, null),
                new Prop("destroy_when", Kind.STATIC, null),
                new Prop("template", Kind.STATIC, null));

        type("foreign_buttons",
                new Prop("layout", Kind.ENUM, "column", "keep", "row", "column"),
                new Prop("spacing", Kind.NUMBER, 4),
                new Prop("align", Kind.ENUM, "start", "start", "center", "end"));
    }

    private ElementSchema() {
    }

    private static void type(String name, Prop... props) {
        Map<String, Prop> map = new LinkedHashMap<>();
        for (Prop p : props) {
            map.put(p.name, p);
        }
        TYPES.put(name, map);
    }

    /** The canonical type name ({@code panel} → {@code rect}), or null if unknown. */
    public static String canonical(String type) {
        if (type == null) {
            return null;
        }
        String alias = ALIASES.get(type);
        if (alias != null) {
            return alias;
        }
        return TYPES.containsKey(type) ? type : null;
    }

    public static Map<String, Prop> props(String canonicalType) {
        Map<String, Prop> m = TYPES.get(canonicalType);
        return m == null ? Collections.emptyMap() : m;
    }

    public static Set<String> typeNames() {
        Set<String> names = new HashSet<>(TYPES.keySet());
        names.addAll(ALIASES.keySet());
        return names;
    }
}
