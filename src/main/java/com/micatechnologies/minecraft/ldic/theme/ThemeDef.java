package com.micatechnologies.minecraft.ldic.theme;

import com.micatechnologies.minecraft.ldic.expr.Expr;
import com.micatechnologies.minecraft.ldic.expr.Scope;
import com.micatechnologies.minecraft.ldic.expr.Slot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A fully compiled theme, ready for a runtime to lay out and draw. */
public final class ThemeDef {

    /** How layout units map to GUI pixels. */
    public enum DesignMode { NONE, FIT, FILL, WIDTH, HEIGHT }

    public String name = "Unnamed theme";
    public String author = "";
    public String description = "";
    /** Whether the theme contains flashing or strobing animation (for the photosensitivity notice). */
    public boolean flashing;

    public DesignMode designMode = DesignMode.NONE;
    public double designWidth = 854;
    public double designHeight = 480;
    public double minScale = 0;
    public double maxScale = Double.MAX_VALUE;

    /** What happens to buttons other mods add to the main menu: {@code keep} or {@code hide}. */
    public String foreignButtons = "keep";

    public Scope scope;
    public final Map<String, String> fonts = new HashMap<>();
    public final Map<String, Slot> state = new LinkedHashMap<>();
    /** Per-frame variables, evaluated in declaration order. */
    final List<Slot> variableSlots = new ArrayList<>();
    final List<Expr> variableExprs = new ArrayList<>();
    public final Map<String, ScreenDef> screens = new LinkedHashMap<>();
    public final Problems problems;

    ThemeDef(Problems problems) {
        this.problems = problems;
    }

    /** Re-evaluates the theme's per-frame variables. Call once a frame, after updating host bindings. */
    public void updateVariables() {
        for (int i = 0; i < variableSlots.size(); i++) {
            variableSlots.get(i).assign(variableExprs.get(i));
        }
    }

    /** The layout-unit to GUI-pixel scale for a screen of the given GUI size. */
    public double uiScale(double guiWidth, double guiHeight) {
        double s;
        switch (designMode) {
            case FIT:
                s = Math.min(guiWidth / designWidth, guiHeight / designHeight);
                break;
            case FILL:
                s = Math.max(guiWidth / designWidth, guiHeight / designHeight);
                break;
            case WIDTH:
                s = guiWidth / designWidth;
                break;
            case HEIGHT:
                s = guiHeight / designHeight;
                break;
            default:
                s = 1;
                break;
        }
        return Math.max(minScale, Math.min(maxScale, s));
    }

    public ScreenDef screen(String id) {
        return screens.get(id);
    }
}
