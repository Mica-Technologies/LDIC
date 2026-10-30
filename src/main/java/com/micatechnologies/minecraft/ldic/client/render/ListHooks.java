package com.micatechnologies.minecraft.ldic.client.render;

import com.micatechnologies.minecraft.ldic.ui.UiNode;

/**
 * What a {@code list} element needs from its screen: the screen draws the rows (worlds,
 * servers) inside the element's box, styled by the element's properties, and handles the
 * clicks, scrolling and search itself. {@link ThemeRenderer#lists} is null on screens without
 * a list, where the element draws nothing.
 */
public interface ListHooks {

    /** Draws the list into the node's box (layout units; the GL matrix is already scaled). */
    void drawList(UiNode node);
}
