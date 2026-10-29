package com.micatechnologies.minecraft.ldic.theme;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A point inside a box, as fractions of its width and height: {@code (0,0)} is the top-left
 * corner, {@code (1,1)} the bottom-right.
 *
 * <p>An element's {@code anchor} picks the point in its parent it is positioned from; its
 * {@code pivot} picks the point of the element that sits there (and that rotation and scale act
 * around). The pivot defaults to the anchor, so {@code "anchor": "bottom_right"} with no offset
 * puts the element flush in the corner.</p>
 */
public final class Anchor {

    public static final Anchor TOP_LEFT = new Anchor(0, 0);
    public static final Anchor CENTER = new Anchor(0.5, 0.5);

    private static final Map<String, Anchor> NAMED = new HashMap<>();

    static {
        NAMED.put("top_left", TOP_LEFT);
        NAMED.put("top", new Anchor(0.5, 0));
        NAMED.put("top_center", new Anchor(0.5, 0));
        NAMED.put("top_right", new Anchor(1, 0));
        NAMED.put("left", new Anchor(0, 0.5));
        NAMED.put("center_left", new Anchor(0, 0.5));
        NAMED.put("center", CENTER);
        NAMED.put("right", new Anchor(1, 0.5));
        NAMED.put("center_right", new Anchor(1, 0.5));
        NAMED.put("bottom_left", new Anchor(0, 1));
        NAMED.put("bottom", new Anchor(0.5, 1));
        NAMED.put("bottom_center", new Anchor(0.5, 1));
        NAMED.put("bottom_right", new Anchor(1, 1));
    }

    public final double fx;
    public final double fy;

    public Anchor(double fx, double fy) {
        this.fx = fx;
        this.fy = fy;
    }

    /** A built-in name such as {@code "bottom_right"}, or null. */
    public static Anchor named(String name) {
        return name == null ? null : NAMED.get(name.toLowerCase(Locale.ROOT).replace('-', '_'));
    }

    @Override
    public String toString() {
        return "(" + fx + ", " + fy + ")";
    }
}
