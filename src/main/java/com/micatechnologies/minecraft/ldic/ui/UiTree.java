package com.micatechnologies.minecraft.ldic.ui;

import com.micatechnologies.minecraft.ldic.theme.ElementDef;
import com.micatechnologies.minecraft.ldic.theme.FactoryDef;
import com.micatechnologies.minecraft.ldic.theme.ScreenDef;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The live element tree of one theme screen.
 *
 * <p>Each frame, {@link #frame} walks the tree once. For every node it writes the node's local
 * slots ({@code pw}, {@code hover}, factory {@code index}, ...), evaluates its layout and
 * properties, and has the renderer draw it, before moving to its children. Evaluating and
 * drawing in the same pass matters: factory instances share one template definition, so a
 * node's expressions are only valid while its own values are in the slots.</p>
 *
 * <p>Hover is resolved against the previous frame's layout, so it lags by one frame. Nobody can
 * see that, and it keeps the pass single.</p>
 */
public final class UiTree {

    private final ScreenDef screen;
    private final List<UiNode> roots = new ArrayList<>();
    private final List<UiNode> interactive = new ArrayList<>();
    private UiNode hot;
    private UiNode focused;
    private UiNode pressed;
    private double now;
    private double delta;
    private double mouseX = -1;
    private double mouseY = -1;
    private double screenWidth;
    private double screenHeight;

    public UiTree(ScreenDef screen) {
        this.screen = screen;
        for (ElementDef d : screen.elements) {
            roots.add(new UiNode(d, null, null));
        }
    }

    public ScreenDef screen() {
        return screen;
    }

    public List<UiNode> roots() {
        return roots;
    }

    /**
     * Lays out and draws one frame.
     *
     * @param width   screen width in layout units
     * @param height  screen height in layout units
     * @param mx      pointer x in layout units (negative when there is no pointer)
     * @param my      pointer y in layout units
     * @param time    seconds since the screen opened (drives factory ages)
     * @param seconds seconds since the previous frame
     */
    public void frame(NodeRenderer renderer, double width, double height, double mx, double my,
                      double time, double seconds) {
        this.screenWidth = width;
        this.screenHeight = height;
        this.mouseX = mx;
        this.mouseY = my;
        this.now = time;
        this.delta = Math.max(0, seconds);
        interactive.clear();
        for (UiNode n : roots) {
            visit(n, renderer, 0, 0, width, height, 1, 1, 0, 0);
        }
        // Topmost interactive node under the pointer, for the next frame's hover state.
        UiNode top = null;
        for (int i = interactive.size() - 1; i >= 0; i--) {
            UiNode n = interactive.get(i);
            if (n.enabled && mx >= 0 && n.contains(mx, my)) {
                top = n;
                break;
            }
        }
        hot = top;
        if (focused != null && !interactive.contains(focused)) {
            focused = null;
        }
    }

    private void visit(UiNode n, NodeRenderer r, double px, double py, double pw, double ph,
                       double parentOpacity, double sScale, double sx, double sy) {
        ElementDef d = n.def;
        if (n.instance != null) {
            restoreInstance(n);
        }
        writeLocals(n, pw, ph);
        n.visible = d.visible.bool();
        if (!n.visible) {
            clearInteraction(n);
            return;
        }
        n.enabled = d.enabled.bool();

        if (d.width == null || d.height == null) {
            r.measure(n);
            if (d.width != null) {
                n.width = d.width.resolve(pw);
            }
            if (d.height != null) {
                n.height = d.height.resolve(ph);
            }
        } else {
            n.width = d.width.resolve(pw);
            n.height = d.height.resolve(ph);
        }
        d.locals.width.set(n.width);
        d.locals.height.set(n.height);
        n.x = px + pw * d.anchor.fx + d.x.resolve(pw) - n.width * d.pivot.fx;
        n.y = py + ph * d.anchor.fy + d.y.resolve(ph) - n.height * d.pivot.fy;
        n.pivotX = n.x + n.width * d.pivot.fx;
        n.pivotY = n.y + n.height * d.pivot.fy;
        n.rotation = d.rotation.num();
        n.scale = d.scale.num();
        n.opacity = parentOpacity * clamp01(d.opacity.num());

        // This node's screen affine: the parent's, then a scale around our pivot.
        n.screenScale = sScale * n.scale;
        n.screenX = sx + sScale * (n.pivotX - n.pivotX * n.scale);
        n.screenY = sy + sScale * (n.pivotY - n.pivotY * n.scale);

        if (d.isInteractive()) {
            interactive.add(n);
            updateInteraction(n, r);
        }

        r.begin(n);
        r.draw(n);
        if (d.factory != null) {
            visitFactory(n, r);
        }
        for (UiNode c : n.children) {
            visit(c, r, n.x, n.y, n.width, n.height, n.opacity, n.screenScale, n.screenX, n.screenY);
        }
        r.end(n);
    }

    private void writeLocals(UiNode n, double pw, double ph) {
        ElementDef.Locals l = n.def.locals;
        l.parentWidth.set(pw);
        l.parentHeight.set(ph);
        l.hover.set(n.hover);
        l.hoverT.set(n.hoverT);
        l.pressed.set(n.pressed);
        l.focused.set(n.focused);
        l.width.set(n.width);
        l.height.set(n.height);
    }

    private void updateInteraction(UiNode n, NodeRenderer r) {
        n.hover = n == hot;
        n.focused = n == focused;
        n.pressed = n == pressed && n.hover;
        boolean isHot = n.isHot() && n.enabled;
        if (isHot && !n.wasHot) {
            r.hoverStarted(n);
        }
        n.wasHot = isHot;
        double target = isHot ? 1 : 0;
        double t = n.def.hoverTime;
        if (t <= 0) {
            n.hoverT = target;
        } else {
            double step = delta / t;
            n.hoverT = target > n.hoverT ? Math.min(target, n.hoverT + step) : Math.max(target, n.hoverT - step);
        }
        // Re-write the locals so this frame's expressions see the fresh state.
        n.def.locals.hover.set(n.hover);
        n.def.locals.hoverT.set(n.hoverT);
        n.def.locals.pressed.set(n.pressed);
        n.def.locals.focused.set(n.focused);
        n.tooltip = n.hover && n.def.tooltip != null ? n.def.tooltip.asString() : null;
    }

    private static void clearInteraction(UiNode n) {
        n.hover = false;
        n.pressed = false;
        n.hoverT = 0;
        n.wasHot = false;
        n.tooltip = null;
    }

    // -------------------------------------------------------------------------------------
    // Factories

    private void visitFactory(UiNode n, NodeRenderer r) {
        FactoryDef f = n.def.factory;
        if (f.trigger != null) {
            Object value = f.trigger.value();
            if (!n.triggered || !Objects.equals(value, n.lastTrigger)) {
                n.triggered = true;
                n.lastTrigger = value;
                n.instances.add(0, spawn(n, f));
            }
        } else {
            int want = (int) Math.max(0, Math.min(f.max, Math.floor(f.count.num())));
            while (n.instances.size() < want) {
                n.instances.add(spawn(n, f));
            }
            while (n.instances.size() > want) {
                n.instances.remove(n.instances.size() - 1);
            }
        }
        while (n.instances.size() > f.max) {
            n.instances.remove(n.instances.size() - 1);
        }
        if (f.destroyWhen != null) {
            for (int i = n.instances.size() - 1; i >= 0; i--) {
                UiNode.FactoryInstance inst = n.instances.get(i);
                writeInstance(f, inst, i, n.instances.size());
                if (f.destroyWhen.bool()) {
                    n.instances.remove(i);
                }
            }
        }
        int count = n.instances.size();
        for (int i = 0; i < count; i++) {
            UiNode.FactoryInstance inst = n.instances.get(i);
            writeInstance(f, inst, i, count);
            visit(inst.node, r, n.x, n.y, n.width, n.height, n.opacity, n.screenScale, n.screenX, n.screenY);
        }
    }

    private UiNode.FactoryInstance spawn(UiNode factoryNode, FactoryDef f) {
        Object[] kept = new Object[f.keepExprs.size()];
        for (int i = 0; i < kept.length; i++) {
            kept[i] = f.keepExprs.get(i).value();
        }
        UiNode.FactoryInstance inst = new UiNode.FactoryInstance(kept, now);
        inst.node = new UiNode(f.template, factoryNode, inst);
        return inst;
    }

    private void writeInstance(FactoryDef f, UiNode.FactoryInstance inst, int index, int count) {
        f.index.set(index);
        f.countSlot.set(count);
        f.age.set(now - inst.born);
        for (int k = 0; k < f.keepSlots.size(); k++) {
            f.keepSlots.get(k).restore(inst.kept[k]);
        }
    }

    private void restoreInstance(UiNode n) {
        FactoryDef f = n.parent.def.factory;
        int index = n.parent.instances.indexOf(n.instance);
        writeInstance(f, n.instance, Math.max(0, index), n.parent.instances.size());
    }

    // -------------------------------------------------------------------------------------
    // Input

    /** The topmost enabled interactive node under the pointer (as of the last frame), or null. */
    public UiNode hot() {
        return hot;
    }

    public UiNode focused() {
        return focused;
    }

    /** The tooltip to show this frame, or null. */
    public String tooltip() {
        return hot == null ? null : hot.tooltip;
    }

    /** Pointer pressed: returns the node that will receive the click on release, or null. */
    public UiNode press() {
        pressed = hot;
        return pressed;
    }

    /** Pointer released: returns the node clicked (pressed and released over the same node), or null. */
    public UiNode release() {
        UiNode p = pressed;
        pressed = null;
        return p != null && p == hot ? p : null;
    }

    /** Moves keyboard focus to the next (or previous) interactive node in draw order. */
    public UiNode moveFocus(boolean forward) {
        List<UiNode> candidates = new ArrayList<>();
        for (UiNode n : interactive) {
            if (n.enabled && n.def.action != null) {
                candidates.add(n);
            }
        }
        if (candidates.isEmpty()) {
            focused = null;
            return null;
        }
        int i = focused == null ? -1 : candidates.indexOf(focused);
        if (i < 0) {
            focused = forward ? candidates.get(0) : candidates.get(candidates.size() - 1);
        } else {
            int next = (i + (forward ? 1 : -1) + candidates.size()) % candidates.size();
            focused = candidates.get(next);
        }
        return focused;
    }

    /**
     * Re-writes the slots a node's expressions read (its ancestors' factory instances and its own
     * locals), so an action on it evaluates with that node's values even outside the frame pass.
     */
    public void prepare(UiNode n) {
        List<UiNode> chain = new ArrayList<>();
        for (UiNode c = n; c != null; c = c.parent) {
            chain.add(0, c);
        }
        double pw = screenWidth;
        double ph = screenHeight;
        for (UiNode c : chain) {
            if (c.instance != null) {
                restoreInstance(c);
            }
            writeLocals(c, pw, ph);
            pw = c.width;
            ph = c.height;
        }
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
