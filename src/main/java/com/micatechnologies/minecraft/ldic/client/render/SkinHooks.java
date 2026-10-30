package com.micatechnologies.minecraft.ldic.client.render;

import com.micatechnologies.minecraft.ldic.ui.UiNode;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleToIntFunction;

/**
 * What the {@code vanilla_*} elements need from a skinned vanilla screen. {@link ThemeRenderer#skin}
 * is null on every other screen, where they draw nothing.
 */
public interface SkinHooks {

    /** One vanilla button placed by a {@code vanilla_grid}: its box (layout units), label and state. */
    final class Cell {
        public final double x;
        public final double y;
        public final double width;
        public final double height;
        public final String label;
        public final boolean enabled;
        public final double hoverT;
        /** A slider's position (0 to 1), or -1 for a plain button. */
        public final double value;

        public Cell(double x, double y, double width, double height, String label, boolean enabled, double hoverT, double value) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.label = label;
            this.enabled = enabled;
            this.hoverT = hoverT;
            this.value = value;
        }
    }

    /**
     * Moves the vanilla button named by the node into its box and sets the node's enabled and
     * hover state from it; returns its label, or null when there is no such (visible) button.
     */
    String vanillaButton(UiNode n);

    /** Whether the check box named by a vanilla_button node is ticked (1) or not (0); -1 for other buttons. */
    int vanillaChecked(UiNode n);

    /** The position (0 to 1) of the slider named by a vanilla_button node, or -1. */
    double vanillaSlider(UiNode n);

    /** Lays the remaining vanilla buttons out in the node's box; the cells to draw. */
    List<Cell> vanillaGrid(UiNode n);

    /**
     * A vanilla text field as the theme draws it. One per field, kept while the screen is open;
     * the renderer sets {@link #hit} each frame so a click places the cursor by the theme's font.
     */
    final class Field {
        public String text = "";
        public int cursor;
        public int selection;
        public boolean focused;
        /** First character shown (the renderer keeps the cursor in view). */
        public int start;
        /** The text's box in layout units, for clicks. */
        public double x;
        public double y;
        public double width;
        public double height;
        /** Layout x to character index, from the last draw; null before it. */
        public DoubleToIntFunction hit;
    }

    /**
     * Moves the vanilla text field named by the node into its box (vanilla still takes the focus
     * click); its text, cursor and selection, or null when there is no such (visible) field.
     */
    Field vanillaField(UiNode n);

    /**
     * Moves the vanilla list named by the node into its box and draws it; when {@code buttons} is
     * not null, the vanilla buttons in its rows are drawn through it instead of vanilla's look.
     */
    void vanillaList(UiNode n, Consumer<Cell> buttons);

    /** The screen's own drawing (at GUI scale, where vanilla put it), minus the buttons the theme draws. */
    void vanillaScreen(UiNode n);

    /** The advancement screen's selected tab, drawn in the node's box. */
    void advancementMap(UiNode n);

    /** The advancement tabs, each painted as a button cell (then given its icon). */
    void advancementTabs(UiNode n, Consumer<Cell> paint);
}
