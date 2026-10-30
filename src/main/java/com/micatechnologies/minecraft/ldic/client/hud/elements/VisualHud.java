package com.micatechnologies.minecraft.ldic.client.hud.elements;

import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.BasicHud;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import com.micatechnologies.minecraft.ldic.client.hud.HudThemeColours;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

/**
 * Base of the drawn (non-text) HUDs: each draws in its own units with the origin at its
 * top-left, and the HUD's scale is applied around it. Colours come from the menu theme's HUD
 * colours ({@link HudThemeColours}), so they follow the theme like the "Theme" style does.
 * Off until the player (or a layout preset) turns them on.
 */
public abstract class VisualHud extends BasicHud {

    protected VisualHud(float x, float y) {
        super(false, x, y, 0.6f, true, true, 3f, 3f, 2f, new OneColor(16, 20, 24, 160),
                false, 1f, new OneColor(0, 0, 0));
    }

    /** Size in the HUD's own units (before scale). */
    protected abstract float width(boolean example);

    protected abstract float height(boolean example);

    /** Draws at (0, 0) in the HUD's own units; {@code example} in the HUD editor. */
    protected abstract void render(Minecraft mc, boolean example);

    @Override
    protected void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null && !example) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(scale, scale, 1);
        render(mc, example);
        GlStateManager.popMatrix();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
    }

    @Override
    protected float getWidth(float scale, boolean example) {
        return width(example) * scale;
    }

    @Override
    protected float getHeight(float scale, boolean example) {
        return height(example) * scale;
    }

    // ----------------------------------------------------------------------- helpers

    protected static int text() {
        return HudThemeColours.text();
    }

    protected static int accent() {
        return HudThemeColours.accent();
    }

    /** {@code colour} with its alpha multiplied by {@code a} (0..1). */
    protected static int fade(int colour, float a) {
        int alpha = Math.round(((colour >>> 24) & 0xFF) * Math.max(0, Math.min(1, a)));
        // FontRenderer treats an alpha below 4 as opaque; keep faded text faint, not solid.
        return (colour & 0xFFFFFF) | (Math.max(alpha, 5) << 24);
    }

    protected static void rect(float x0, float y0, float x1, float y1, int colour) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x0, y0, 0);
        GlStateManager.scale(x1 - x0, y1 - y0, 1);
        Gui.drawRect(0, 0, 1, 1, colour);
        GlStateManager.popMatrix();
    }

    /** Text at {@code size} pixels high (the font is 8), left edge at x. */
    protected static void text(FontRenderer font, String s, float x, float y, float size, int colour, boolean shadow) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        float k = size / 8f;
        GlStateManager.scale(k, k, 1);
        font.drawString(s, 0, 0, colour, shadow);
        GlStateManager.popMatrix();
    }

    protected static float textWidth(FontRenderer font, String s, float size) {
        return font.getStringWidth(s) * size / 8f;
    }

    /** Green through yellow to red as {@code fraction} goes 1 to 0, like vanilla's durability bar. */
    protected static int wear(float fraction) {
        return java.awt.Color.HSBtoRGB(Math.max(0, fraction) / 3f, 1f, 1f) | 0xFF000000;
    }
}
