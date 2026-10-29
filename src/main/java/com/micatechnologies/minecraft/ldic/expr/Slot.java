package com.micatechnologies.minecraft.ldic.expr;

/**
 * A named, typed storage cell that expressions read from.
 *
 * <p>Bindings the host updates every frame ({@code time}, {@code screen_width}, ...), theme
 * variables, function parameters and element-local values ({@code hover}, {@code index}) are all
 * slots. A compiled expression holds direct references to the slots it reads, so evaluation does
 * no name lookups. Slots are not thread-safe: each compiled theme is owned by one thread.</p>
 */
public final class Slot {

    private final String name;
    private final Type type;
    private double number;
    private boolean bool;
    private String string = "";

    public Slot(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public String name() {
        return name;
    }

    public Type type() {
        return type;
    }

    public double getNumber() {
        return number;
    }

    public boolean getBool() {
        return bool;
    }

    public String getString() {
        return string;
    }

    public void set(double value) {
        this.number = value;
    }

    public void set(boolean value) {
        this.bool = value;
    }

    public void set(String value) {
        this.string = value == null ? "" : value;
    }

    /** Evaluates {@code expr} (which must have this slot's type) and stores the result. */
    public void assign(Expr expr) {
        switch (type) {
            case NUMBER:
                number = expr.num();
                break;
            case BOOL:
                bool = expr.bool();
                break;
            default:
                set(expr.str());
                break;
        }
    }

    /** The current value, boxed; used to snapshot per-instance factory state. */
    public Object snapshot() {
        switch (type) {
            case NUMBER:
                return number;
            case BOOL:
                return bool;
            default:
                return string;
        }
    }

    public void restore(Object value) {
        switch (type) {
            case NUMBER:
                number = (Double) value;
                break;
            case BOOL:
                bool = (Boolean) value;
                break;
            default:
                string = (String) value;
                break;
        }
    }

    @Override
    public String toString() {
        return name + ":" + type.display();
    }
}
