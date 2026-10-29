package com.micatechnologies.minecraft.ldic.ui;

import com.micatechnologies.minecraft.ldic.theme.ElementDef;
import java.util.ArrayList;
import java.util.List;

/**
 * A live instance of an {@link ElementDef}: its layout this frame and its interaction state.
 * Factory templates have one node per instance, all sharing the template's definition.
 */
public final class UiNode {

    public final ElementDef def;
    public final UiNode parent;
    public final List<UiNode> children = new ArrayList<>();
    /** Set when this node is the root of a factory instance. */
    final FactoryInstance instance;

    /** Layout box in the parent's (untransformed) coordinates, layout units. */
    public double x;
    public double y;
    public double width;
    public double height;
    /** Pivot point in the same coordinates; rotation and scale act around it. */
    public double pivotX;
    public double pivotY;
    public double rotation;
    public double scale = 1;
    /** Opacity including every ancestor's. */
    public double opacity = 1;
    public boolean visible;
    public boolean enabled = true;

    /** Screen-space affine (uniform scale + translation, rotation ignored) for hit testing. */
    double screenScale = 1;
    double screenX;
    double screenY;

    public boolean hover;
    public boolean pressed;
    public boolean focused;
    public double hoverT;
    boolean wasHot;
    /** Tooltip text evaluated this frame while hovered, else null. */
    String tooltip;

    /** Free for the renderer to cache per-node state (text layout, loaded textures, ...). */
    public Object renderState;

    /** Factory state, for factory nodes only. */
    final List<FactoryInstance> instances = new ArrayList<>();
    Object lastTrigger;
    boolean triggered;

    UiNode(ElementDef def, UiNode parent, FactoryInstance instance) {
        this.def = def;
        this.parent = parent;
        this.instance = instance;
        for (ElementDef child : def.children) {
            children.add(new UiNode(child, this, null));
        }
    }

    /** Whether a screen-space point falls inside this node's (scaled) box. */
    public boolean contains(double sx, double sy) {
        double left = screenX + x * screenScale;
        double top = screenY + y * screenScale;
        return sx >= left && sy >= top && sx < left + width * screenScale && sy < top + height * screenScale;
    }

    /** True when hovered or focused: the state an author styles as "highlighted". */
    public boolean isHot() {
        return hover || focused;
    }

    /** One spawned copy of a factory's template. */
    static final class FactoryInstance {
        final Object[] kept;
        final double born;
        UiNode node;

        FactoryInstance(Object[] kept, double born) {
            this.kept = kept;
            this.born = born;
        }
    }
}
