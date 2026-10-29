package com.micatechnologies.minecraft.ldic.theme;

import com.micatechnologies.minecraft.ldic.expr.Expr;
import com.micatechnologies.minecraft.ldic.expr.Slot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A compiled action: what a click (or a screen's open/escape hook) does. A JSON array of actions
 * compiles to a {@code chain} that runs each in order.
 */
public final class ActionDef {

    public final String type;
    public final String path;
    final Map<String, Expr> args = new HashMap<>();
    final Map<String, String> statics = new HashMap<>();
    public final List<ActionDef> chain = new ArrayList<>();
    /** For {@code set} / {@code toggle}: the state slot written. */
    public Slot target;
    public Expr value;

    ActionDef(String type, String path) {
        this.type = type;
        this.path = path;
    }

    public String str(String arg, String fallback) {
        Expr e = args.get(arg);
        return e == null ? fallback : e.asString();
    }

    public double num(String arg, double fallback) {
        Expr e = args.get(arg);
        return e == null ? fallback : e.num();
    }

    public boolean flag(String key, boolean fallback) {
        String v = statics.get(key);
        return v == null ? fallback : Boolean.parseBoolean(v);
    }

    @Override
    public String toString() {
        return type + " @ " + path;
    }
}
