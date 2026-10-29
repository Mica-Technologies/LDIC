package com.micatechnologies.minecraft.ldic.expr;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * A compiled, statically typed expression node.
 *
 * <p>Each node has one {@link #type()} fixed at compile time and implements the matching
 * accessor; calling another accessor is a compiler bug and throws. Nodes read their inputs from
 * {@link Slot}s, so evaluating a number or boolean expression allocates nothing.</p>
 */
public abstract class Expr {

    public abstract Type type();

    public double num() {
        throw new IllegalStateException("not a number expression: " + type());
    }

    public boolean bool() {
        throw new IllegalStateException("not a bool expression: " + type());
    }

    public String str() {
        throw new IllegalStateException("not a string expression: " + type());
    }

    /** True when the value can never change, so the compiler may fold it. */
    public boolean isConstant() {
        return false;
    }

    /** Evaluates as a string whatever the type, the way {@code +} concatenation formats values. */
    public String asString() {
        switch (type()) {
            case NUMBER:
                return formatNumber(num());
            case BOOL:
                return bool() ? "true" : "false";
            default:
                return str();
        }
    }

    /** Evaluates and boxes the value. */
    public Object value() {
        switch (type()) {
            case NUMBER:
                return num();
            case BOOL:
                return bool();
            default:
                return str();
        }
    }

    private static final ThreadLocal<DecimalFormat> FORMAT = ThreadLocal.withInitial(
            () -> new DecimalFormat("0.####", DecimalFormatSymbols.getInstance(Locale.ROOT)));

    /** Whole numbers print without a fraction; others with up to four decimals. */
    public static String formatNumber(double n) {
        if (Double.isNaN(n)) {
            return "NaN";
        }
        if (Double.isInfinite(n)) {
            return n > 0 ? "Infinity" : "-Infinity";
        }
        if (n == Math.rint(n) && Math.abs(n) < 1e15) {
            return Long.toString((long) n);
        }
        return FORMAT.get().format(n);
    }

    // ---------------------------------------------------------------------------------------
    // Leaf nodes

    public static Expr constant(double value) {
        return new NumConst(value);
    }

    public static Expr constant(boolean value) {
        return value ? BoolConst.TRUE : BoolConst.FALSE;
    }

    public static Expr constant(String value) {
        return new StrConst(value);
    }

    /** A constant of whatever type {@code value} is (Double, Boolean or String). */
    public static Expr constantOf(Object value) {
        if (value instanceof Boolean) {
            return constant((Boolean) value);
        }
        if (value instanceof Number) {
            return constant(((Number) value).doubleValue());
        }
        return constant(String.valueOf(value));
    }

    /** Folds {@code e} to a constant node of the same type. */
    public static Expr fold(Expr e) {
        if (e instanceof NumConst || e instanceof BoolConst || e instanceof StrConst) {
            return e;
        }
        switch (e.type()) {
            case NUMBER:
                return constant(e.num());
            case BOOL:
                return constant(e.bool());
            default:
                return constant(e.str());
        }
    }

    public static Expr read(Slot slot) {
        return new SlotRead(slot);
    }

    static final class NumConst extends Expr {
        private final double value;

        NumConst(double value) {
            this.value = value;
        }

        @Override
        public Type type() {
            return Type.NUMBER;
        }

        @Override
        public double num() {
            return value;
        }

        @Override
        public boolean isConstant() {
            return true;
        }
    }

    static final class BoolConst extends Expr {
        static final BoolConst TRUE = new BoolConst(true);
        static final BoolConst FALSE = new BoolConst(false);
        private final boolean value;

        private BoolConst(boolean value) {
            this.value = value;
        }

        @Override
        public Type type() {
            return Type.BOOL;
        }

        @Override
        public boolean bool() {
            return value;
        }

        @Override
        public boolean isConstant() {
            return true;
        }
    }

    static final class StrConst extends Expr {
        private final String value;

        StrConst(String value) {
            this.value = value;
        }

        @Override
        public Type type() {
            return Type.STRING;
        }

        @Override
        public String str() {
            return value;
        }

        @Override
        public boolean isConstant() {
            return true;
        }
    }

    static final class SlotRead extends Expr {
        private final Slot slot;

        SlotRead(Slot slot) {
            this.slot = slot;
        }

        @Override
        public Type type() {
            return slot.type();
        }

        @Override
        public double num() {
            return slot.getNumber();
        }

        @Override
        public boolean bool() {
            return slot.getBool();
        }

        @Override
        public String str() {
            return slot.getString();
        }
    }
}
