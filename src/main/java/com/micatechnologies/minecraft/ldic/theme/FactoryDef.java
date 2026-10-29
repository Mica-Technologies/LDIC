package com.micatechnologies.minecraft.ldic.theme;

import com.micatechnologies.minecraft.ldic.expr.Expr;
import com.micatechnologies.minecraft.ldic.expr.Slot;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@code factory} element: it spawns copies of a template element.
 *
 * <ul>
 *   <li>With {@code trigger}, a new instance is created at start and whenever the trigger's
 *       value changes (a scrolling log line per new status).</li>
 *   <li>With {@code count}, there are exactly that many instances (a row of stars, a list).</li>
 * </ul>
 *
 * <p>Inside the template, {@code index} is 0 for the newest instance (trigger mode) or the
 * position (count mode), {@code count} is the number of live instances, {@code age} is seconds
 * since creation, and every {@code keep} name holds the value it had when the instance was
 * created. Instances beyond {@code max}, or whose {@code destroy_when} is true, are removed, so
 * the list is always bounded.</p>
 */
public final class FactoryDef {

    public Expr trigger;
    public Expr count;
    public int max = 32;
    public Expr destroyWhen;
    public ElementDef template;

    public Slot index;
    public Slot countSlot;
    public Slot age;
    public final List<Slot> keepSlots = new ArrayList<>();
    public final List<Expr> keepExprs = new ArrayList<>();
}
