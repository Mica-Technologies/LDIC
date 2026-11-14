package com.micatechnologies.minecraft.ldic.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * A page of settings a mod describes as rows, which LDIC draws: in the active menu theme's
 * style when there is one, as a plain vanilla-style screen otherwise. The mod writes no GUI
 * code and the page matches whatever theme a pack uses.
 *
 * <pre>{@code
 * MenuPage page = MenuPage.builder("My Mod")
 *     .section("Display")
 *     .toggle("Fancy mode", cfg::fancy, cfg::setFancy)
 *     .slider("Range", 1, 32, 1, cfg::range, v -> cfg.setRange((int) v))
 *     .choice("Quality", Arrays.asList("Low", "Medium", "High"), cfg::quality, cfg::setQuality)
 *     .button("Reset to defaults", cfg::reset)
 *     .onClose(cfg::save)
 *     .build();
 * }</pre>
 *
 * <p>Getters are read every frame and setters run on the client thread, as soon as the player
 * changes a row. Save in {@link Builder#onClose}, which runs once each time the page closes.</p>
 */
public final class MenuPage {

    /** What a row is. */
    public enum Kind {
        /** A heading over the rows that follow. */
        SECTION,
        /** A line of text. */
        TEXT,
        /** On or off. */
        TOGGLE,
        /** A number in a range. */
        SLIDER,
        /** One of a list of named options; a click moves to the next. */
        CHOICE,
        /** Runs code. */
        BUTTON;

        /** The name themes use: {@code section}, {@code text}, {@code toggle}, ... */
        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** One row of a page. */
    public static final class Row {

        private final Kind kind;
        private final Supplier<String> label;
        private Supplier<String> tooltip;
        private BooleanSupplier enabled = () -> true;
        private BooleanSupplier boolGet;
        private Consumer<Boolean> boolSet;
        private double min;
        private double max = 1;
        private double step;
        private DoubleSupplier numGet;
        private DoubleConsumer numSet;
        private DoubleFunction<String> format;
        private List<String> options;
        private IntSupplier indexGet;
        private IntConsumer indexSet;
        private Runnable action;

        private Row(Kind kind, Supplier<String> label) {
            this.kind = kind;
            this.label = label;
        }

        public Kind kind() {
            return kind;
        }

        public String label() {
            String s = label.get();
            return s == null ? "" : s;
        }

        /** Hover text; empty for none. */
        public String tooltip() {
            String s = tooltip == null ? null : tooltip.get();
            return s == null ? "" : s;
        }

        /** Whether the player can change this row right now. Headings and text never can. */
        public boolean enabled() {
            return kind != Kind.SECTION && kind != Kind.TEXT && enabled.getAsBoolean();
        }

        /** A toggle's state; false for other rows. */
        public boolean on() {
            return kind == Kind.TOGGLE && boolGet.getAsBoolean();
        }

        /** A slider's value in its own range; 0 for other rows. */
        public double value() {
            return kind == Kind.SLIDER ? clamp(numGet.getAsDouble()) : 0;
        }

        /** A slider's position from 0 (its minimum) to 1 (its maximum); 0 for other rows. */
        public double fraction() {
            if (kind != Kind.SLIDER || max <= min) {
                return 0;
            }
            return (value() - min) / (max - min);
        }

        /**
         * Moves a slider to a position from 0 to 1, snapped to its step. The setter only runs
         * when the snapped value differs from the current one, so a drag does not flood it.
         */
        public void setFraction(double fraction) {
            if (kind != Kind.SLIDER || !enabled()) {
                return;
            }
            double f = fraction < 0 ? 0 : (fraction > 1 ? 1 : fraction);
            double v = min + f * (max - min);
            if (step > 0) {
                v = min + Math.round((v - min) / step) * step;
            }
            v = clamp(v);
            if (Math.abs(v - numGet.getAsDouble()) > 1e-9) {
                numSet.accept(v);
            }
        }

        /** How far one arrow-key press moves a slider, as a fraction of its range. */
        public double keyStep() {
            if (kind != Kind.SLIDER || max <= min) {
                return 0;
            }
            return step > 0 ? step / (max - min) : 0.02;
        }

        /**
         * The row's current value as text: a slider's formatted number or a choice's option.
         * Null for rows without one; a toggle's is left to LDIC, which translates On and Off.
         */
        public String valueText() {
            switch (kind) {
                case SLIDER:
                    return format.apply(value());
                case CHOICE: {
                    int i = indexGet.getAsInt();
                    return i >= 0 && i < options.size() ? options.get(i) : "";
                }
                default:
                    return null;
            }
        }

        /** A click: flips a toggle, moves a choice to its next option, runs a button. */
        public void activate() {
            if (!enabled()) {
                return;
            }
            switch (kind) {
                case TOGGLE:
                    boolSet.accept(!boolGet.getAsBoolean());
                    break;
                case CHOICE: {
                    int n = options.size();
                    int i = indexGet.getAsInt();
                    indexSet.accept(i < 0 || i >= n ? 0 : (i + 1) % n);
                    break;
                }
                case BUTTON:
                    action.run();
                    break;
                default:
                    break;
            }
        }

        private double clamp(double v) {
            return v < min ? min : (v > max ? max : v);
        }
    }

    private final Supplier<String> title;
    private final List<Row> rows;
    private final Runnable onClose;

    private MenuPage(Builder b) {
        this.title = b.title;
        this.rows = Collections.unmodifiableList(new ArrayList<>(b.rows));
        this.onClose = b.onClose;
    }

    public static Builder builder(String title) {
        Objects.requireNonNull(title, "title");
        return new Builder(() -> title);
    }

    /** With a title read every frame, for translations. */
    public static Builder builder(Supplier<String> title) {
        return new Builder(Objects.requireNonNull(title, "title"));
    }

    public String title() {
        String s = title.get();
        return s == null ? "" : s;
    }

    public List<Row> rows() {
        return rows;
    }

    /** Runs the page's close hook, if it has one. LDIC calls this; a mod does not need to. */
    public void closed() {
        if (onClose != null) {
            onClose.run();
        }
    }

    /** The default number format: whole numbers for a step of 1 or more, two decimals otherwise. */
    static String defaultFormat(double step, double value) {
        if (step >= 1 && step == Math.rint(step)) {
            return String.valueOf(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    /** Builds a {@link MenuPage}; rows appear in the order they are added. */
    public static final class Builder {

        private final Supplier<String> title;
        private final List<Row> rows = new ArrayList<>();
        private Runnable onClose;

        private Builder(Supplier<String> title) {
            this.title = title;
        }

        /** A heading over the rows that follow. */
        public Builder section(String label) {
            return add(new Row(Kind.SECTION, fixed(label)));
        }

        /** A line of fixed text. */
        public Builder text(String text) {
            return add(new Row(Kind.TEXT, fixed(text)));
        }

        /** A line of text read every frame (a status, a count). */
        public Builder text(Supplier<String> text) {
            return add(new Row(Kind.TEXT, Objects.requireNonNull(text, "text")));
        }

        /** An on/off switch. */
        public Builder toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
            Row r = new Row(Kind.TOGGLE, fixed(label));
            r.boolGet = Objects.requireNonNull(get, "get");
            r.boolSet = Objects.requireNonNull(set, "set");
            return add(r);
        }

        /**
         * A number from {@code min} to {@code max}. A {@code step} above 0 snaps the value to
         * multiples of it from {@code min}; 0 leaves it continuous.
         */
        public Builder slider(String label, double min, double max, double step, DoubleSupplier get, DoubleConsumer set) {
            return slider(label, min, max, step, get, set, v -> defaultFormat(step, v));
        }

        /** As above, with the mod's own text for a value ({@code v -> (int) v + " chunks"}). */
        public Builder slider(String label, double min, double max, double step, DoubleSupplier get, DoubleConsumer set,
                              DoubleFunction<String> format) {
            if (!(max > min)) {
                throw new IllegalArgumentException("slider '" + label + "': max must be above min");
            }
            if (step < 0) {
                throw new IllegalArgumentException("slider '" + label + "': step must be 0 or more");
            }
            Row r = new Row(Kind.SLIDER, fixed(label));
            r.min = min;
            r.max = max;
            r.step = step;
            r.numGet = Objects.requireNonNull(get, "get");
            r.numSet = Objects.requireNonNull(set, "set");
            r.format = Objects.requireNonNull(format, "format");
            return add(r);
        }

        /** One of a list of named options, by position; a click moves to the next. */
        public Builder choice(String label, List<String> options, IntSupplier get, IntConsumer set) {
            if (options == null || options.isEmpty()) {
                throw new IllegalArgumentException("choice '" + label + "' needs at least one option");
            }
            Row r = new Row(Kind.CHOICE, fixed(label));
            r.options = Collections.unmodifiableList(new ArrayList<>(options));
            r.indexGet = Objects.requireNonNull(get, "get");
            r.indexSet = Objects.requireNonNull(set, "set");
            return add(r);
        }

        /** A button that runs code. */
        public Builder button(String label, Runnable action) {
            Row r = new Row(Kind.BUTTON, fixed(label));
            r.action = Objects.requireNonNull(action, "action");
            return add(r);
        }

        /** Hover text for the row added last. */
        public Builder tooltip(String text) {
            Objects.requireNonNull(text, "text");
            last().tooltip = () -> text;
            return this;
        }

        /** Greys out the row added last while this is false. */
        public Builder enabledWhen(BooleanSupplier enabled) {
            last().enabled = Objects.requireNonNull(enabled, "enabled");
            return this;
        }

        /** Runs once each time the page closes: the place to save a config. */
        public Builder onClose(Runnable onClose) {
            this.onClose = Objects.requireNonNull(onClose, "onClose");
            return this;
        }

        public MenuPage build() {
            return new MenuPage(this);
        }

        private Builder add(Row r) {
            rows.add(r);
            return this;
        }

        private Row last() {
            if (rows.isEmpty()) {
                throw new IllegalStateException("add a row first");
            }
            return rows.get(rows.size() - 1);
        }

        private static Supplier<String> fixed(String text) {
            Objects.requireNonNull(text, "label");
            return () -> text;
        }
    }
}
