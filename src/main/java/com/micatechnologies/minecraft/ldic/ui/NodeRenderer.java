package com.micatechnologies.minecraft.ldic.ui;

/**
 * Draws nodes. The client implements this with OpenGL; the runtime ({@link UiTree}) decides
 * what to draw, where, and in what order, and calls in here.
 *
 * <p>Calls arrive as {@code begin(n)}, {@code draw(n)}, the children's calls, then
 * {@code end(n)}, so a renderer can push a transform in {@code begin} that the children
 * inherit. All coordinates are layout units with the origin at the top-left of the screen.</p>
 */
public interface NodeRenderer {

    /**
     * Sets {@link UiNode#width} / {@link UiNode#height} for a node with automatic size (text).
     * Called before the node's position is computed.
     */
    void measure(UiNode node);

    /** Applies the node's rotation and scale (around its pivot) for it and its children. */
    void begin(UiNode node);

    void draw(UiNode node);

    void end(UiNode node);

    /** The pointer started hovering over (or keyboard focus reached) an interactive node. */
    default void hoverStarted(UiNode node) {
    }
}
