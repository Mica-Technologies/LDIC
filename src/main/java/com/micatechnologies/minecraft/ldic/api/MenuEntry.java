package com.micatechnologies.minecraft.ldic.api;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiScreen;

/**
 * One thing a mod adds to a menu: a labelled entry that opens the mod's own screen, opens a
 * {@link MenuPage} LDIC draws, or runs some code.
 *
 * <p>With a menu theme in play, LDIC draws the entry itself, in the theme's style, where the
 * theme puts mod entries. On a vanilla screen (the theme is off, failed to load, or the player
 * opened the classic screen) it is an ordinary button, so an entry is always reachable.</p>
 *
 * <p>Build one with {@link #builder(String)}. Exactly one of {@code opens}, {@code page} or
 * {@code runs} must be set.</p>
 */
public final class MenuEntry {

    private final String id;
    private final Supplier<String> label;
    private final Supplier<String> tooltip;
    private final int order;
    private final BooleanSupplier visible;
    private final BooleanSupplier enabled;
    private final Function<GuiScreen, GuiScreen> screen;
    private final MenuPage page;
    private final Runnable action;

    private MenuEntry(Builder b) {
        this.id = b.id;
        this.label = b.label;
        this.tooltip = b.tooltip;
        this.order = b.order;
        this.visible = b.visible;
        this.enabled = b.enabled;
        this.screen = b.screen;
        this.page = b.page;
        this.action = b.action;
    }

    /**
     * Starts an entry. The id names it for replacing and removing; use {@code modid:name} so
     * it cannot collide with another mod's.
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String id() {
        return id;
    }

    /** The text shown, read every frame (so it can follow the language or a setting). */
    public Supplier<String> label() {
        return label;
    }

    /** Hover text, or null for none. */
    public Supplier<String> tooltip() {
        return tooltip;
    }

    /** Lower numbers come first; entries with the same order keep the order they were added in. */
    public int order() {
        return order;
    }

    /** Whether the entry shows at all right now. */
    public BooleanSupplier visible() {
        return visible;
    }

    /** Whether the entry can be clicked right now; a disabled entry is drawn greyed out. */
    public BooleanSupplier enabled() {
        return enabled;
    }

    /** The screen to open, given the screen the entry is on (to return to); null if not that kind. */
    public Function<GuiScreen, GuiScreen> screen() {
        return screen;
    }

    /** The page to open; null if not that kind. */
    public MenuPage page() {
        return page;
    }

    /** The code to run; null if not that kind. */
    public Runnable action() {
        return action;
    }

    /** Builds a {@link MenuEntry}. */
    public static final class Builder {

        private final String id;
        private Supplier<String> label;
        private Supplier<String> tooltip;
        private int order = 1000;
        private BooleanSupplier visible = () -> true;
        private BooleanSupplier enabled = () -> true;
        private Function<GuiScreen, GuiScreen> screen;
        private MenuPage page;
        private Runnable action;

        private Builder(String id) {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("a menu entry needs an id such as \"mymod:settings\"");
            }
            this.id = id;
        }

        /** Fixed text. */
        public Builder label(String text) {
            Objects.requireNonNull(text, "text");
            return label(() -> text);
        }

        /** Text read every frame, for translations ({@code () -> I18n.format("mymod.settings")}). */
        public Builder label(Supplier<String> text) {
            this.label = Objects.requireNonNull(text, "text");
            return this;
        }

        public Builder tooltip(String text) {
            Objects.requireNonNull(text, "text");
            return tooltip(() -> text);
        }

        public Builder tooltip(Supplier<String> text) {
            this.tooltip = Objects.requireNonNull(text, "text");
            return this;
        }

        /** Position among other mods' entries; the default is 1000. */
        public Builder order(int order) {
            this.order = order;
            return this;
        }

        /** Shows the entry only while this is true (only in a world, only for operators, ...). */
        public Builder visibleWhen(BooleanSupplier visible) {
            this.visible = Objects.requireNonNull(visible, "visible");
            return this;
        }

        /** Greys the entry out while this is false. */
        public Builder enabledWhen(BooleanSupplier enabled) {
            this.enabled = Objects.requireNonNull(enabled, "enabled");
            return this;
        }

        /**
         * Opens the mod's own screen. The function gets the screen the entry is on, to go back
         * to when the mod's screen closes.
         */
        public Builder opens(Function<GuiScreen, GuiScreen> screen) {
            this.screen = Objects.requireNonNull(screen, "screen");
            return this;
        }

        /** Opens a page LDIC draws in the theme's style (see {@link MenuPage}). */
        public Builder page(MenuPage page) {
            this.page = Objects.requireNonNull(page, "page");
            return this;
        }

        /** Runs code on the client thread; the menu stays open unless the code changes screen. */
        public Builder runs(Runnable action) {
            this.action = Objects.requireNonNull(action, "action");
            return this;
        }

        public MenuEntry build() {
            if (label == null) {
                throw new IllegalStateException("menu entry '" + id + "' needs a label");
            }
            int kinds = (screen != null ? 1 : 0) + (page != null ? 1 : 0) + (action != null ? 1 : 0);
            if (kinds != 1) {
                throw new IllegalStateException("menu entry '" + id + "' needs exactly one of opens(...), page(...) or runs(...)");
            }
            return new MenuEntry(this);
        }
    }
}
