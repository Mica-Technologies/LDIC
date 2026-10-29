package com.micatechnologies.minecraft.ldic.theme;

import com.micatechnologies.minecraft.ldic.expr.Expr;
import com.micatechnologies.minecraft.ldic.expr.Slot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One compiled element of a theme screen. Layout and common properties are fields; the
 * type-specific ones (colour, text, image, ...) live in {@link #props}, already compiled to
 * expressions of the type {@link ElementSchema} declares, with defaults filled in.
 */
public final class ElementDef {

    /** Values the runtime writes before evaluating this element's expressions. */
    public static final class Locals {
        public Slot hover;
        public Slot hoverT;
        public Slot pressed;
        public Slot focused;
        public Slot parentHover;
        public Slot parentHoverT;
        public Slot parentWidth;
        public Slot parentHeight;
        public Slot width;
        public Slot height;
    }

    public final String type;
    public final String id;
    /** JSON path, for problem reports. */
    public final String path;

    public Anchor anchor = Anchor.TOP_LEFT;
    public Anchor pivot = Anchor.TOP_LEFT;
    public Length x;
    public Length y;
    /** Null means automatic: measured for text, filled otherwise. */
    public Length width;
    public Length height;
    public Expr visible;
    public Expr opacity;
    public Expr rotation;
    public Expr scale;
    public Expr enabled;
    /** String expression, or null for no tooltip. */
    public Expr tooltip;
    public Expr soundHover;
    public Expr soundPress;
    public double z;
    /** Seconds for {@code hover_t} to ease from 0 to 1. */
    public double hoverTime = 0.15;

    final Map<String, Expr> props = new HashMap<>();
    final Map<String, String> statics = new HashMap<>();
    final Map<String, List<Expr>> lists = new HashMap<>();
    public final List<ElementDef> children = new ArrayList<>();
    public ActionDef action;
    public final Locals locals = new Locals();
    public FactoryDef factory;

    ElementDef(String type, String id, String path) {
        this.type = type;
        this.id = id;
        this.path = path;
    }

    public boolean has(String prop) {
        return props.containsKey(prop);
    }

    /** The compiled property, or null when it was not set and has no default. */
    public Expr prop(String prop) {
        return props.get(prop);
    }

    public double num(String prop, double fallback) {
        Expr e = props.get(prop);
        return e == null ? fallback : e.num();
    }

    public boolean bool(String prop, boolean fallback) {
        Expr e = props.get(prop);
        return e == null ? fallback : e.bool();
    }

    public String str(String prop, String fallback) {
        Expr e = props.get(prop);
        return e == null ? fallback : e.str();
    }

    /** A static (non-expression) setting such as {@code fit} or {@code font}. */
    public String statik(String key, String fallback) {
        String v = statics.get(key);
        return v == null ? fallback : v;
    }

    public List<Expr> list(String key) {
        List<Expr> l = lists.get(key);
        return l == null ? Collections.emptyList() : l;
    }

    /** Whether the element reacts to the pointer: it has an action, a tooltip or wraps a button. */
    public boolean isInteractive() {
        return action != null || tooltip != null || wrappedButton() != null || type.equals("slider");
    }

    /**
     * The other mod's button this button stands in for, or null. Only buttons wrap: on a text
     * element {@code wrap} means word wrapping.
     */
    public String wrappedButton() {
        return type.equals("button") ? statics.get("wrap") : null;
    }

    @Override
    public String toString() {
        return type + (id != null ? "#" + id : "") + " @ " + path;
    }
}
