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
    /** Main menu: let Forge draw its "new version available" notice. Modern themes usually turn it off. */
    public boolean forgeUpdateNotice = true;

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

    /** A screen shown before {@code main} (loading themes): an intro, a logo sting. */
    public static final class Step {
        public final String screen;
        public final double seconds;
        /** Shown only the first time this player loads the game with this theme. */
        public final boolean once;

        Step(String screen, double seconds, boolean once) {
            this.screen = screen;
            this.seconds = seconds;
            this.once = once;
        }
    }

    /** Screens to show, in order, before {@code main}. */
    public final List<Step> sequence = new ArrayList<>();
    /** Seconds each crossfade between sequence screens (and into main) takes. */
    public double crossfade = 0.6;

    /** Background music for this theme, or null. */
    public Music music;

    /** A theme's music: tracks played in order (or shuffled), with fades. */
    public static final class Music {
        public final List<String> tracks = new ArrayList<>();
        public double volume = 1;
        public boolean loop = true;
        public boolean shuffle;
        public double fadeIn = 2;
        public double fadeOut = 3;
        /** Loading themes: keep playing on the main menu instead of fading out when loading ends. */
        public boolean continueIntoMenu;
    }

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
