package com.micatechnologies.minecraft.ldic.theme;

import com.micatechnologies.minecraft.ldic.expr.Expr;

/**
 * A size or offset along one axis: either layout units (an expression) or a percentage of the
 * parent's size along that axis ({@code "50%"}). Anything more involved is an expression over
 * {@code pw} / {@code ph}, the parent's width and height.
 */
public final class Length {

    private final Expr value;
    private final boolean percent;

    private Length(Expr value, boolean percent) {
        this.value = value;
        this.percent = percent;
    }

    public static Length units(Expr value) {
        return new Length(value, false);
    }

    public static Length percent(double percent) {
        return new Length(Expr.constant(percent), true);
    }

    public static Length fill() {
        return percent(100);
    }

    /** Resolves against the parent's size along this axis. */
    public double resolve(double parentSize) {
        double v = value.num();
        return percent ? parentSize * v / 100.0 : v;
    }

    public boolean isPercent() {
        return percent;
    }
}
