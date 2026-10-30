package com.micatechnologies.minecraft.ldic.client.render;

import com.micatechnologies.minecraft.ldic.ui.UiNode;

/**
 * What {@code vanilla_button} and {@code vanilla_field} elements need from a skinned vanilla
 * screen. {@link ThemeRenderer#skin} is null on every other screen, where they draw nothing.
 */
public interface SkinHooks {

    /**
     * Moves the vanilla button named by the node into its box and sets the node's enabled and
     * hover state from it; returns its label, or null when there is no such (visible) button.
     */
    String vanillaButton(UiNode n);

    /** Moves the vanilla text field named by the node into its box and draws it. */
    void vanillaField(UiNode n);
}
