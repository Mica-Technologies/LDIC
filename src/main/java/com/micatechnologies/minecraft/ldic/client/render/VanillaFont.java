package com.micatechnologies.minecraft.ldic.client.render;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;

/** Minecraft's own font, scaled. Respects resource packs, the Unicode-font option and font mods. */
public final class VanillaFont implements LdicFont {

    public static final VanillaFont INSTANCE = new VanillaFont();

    private VanillaFont() {
    }

    private static FontRenderer fr() {
        return Minecraft.getMinecraft().fontRenderer;
    }

    @Override
    public double width(String text, double size) {
        return fr().getStringWidth(text) * size / fr().FONT_HEIGHT;
    }

    @Override
    public void draw(String text, double x, double y, double size, long argb, boolean shadow, double opacity) {
        int alpha = Gfx.a(argb, opacity);
        // FontRenderer treats an alpha below 4 as "no alpha given" and draws opaque.
        if (alpha < 4) {
            return;
        }
        int colour = (alpha << 24) | (int) (argb & 0xFFFFFF);
        double s = size / fr().FONT_HEIGHT;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(s, s, 1);
        GlStateManager.enableBlend();
        fr().drawString(text, 0, 0, colour, shadow);
        GlStateManager.popMatrix();
        GlStateManager.color(1, 1, 1, 1);
    }

    @Override
    public List<String> wrap(String text, double maxWidth, double size) {
        if (maxWidth <= 0) {
            return LdicFont.super.wrap(text, maxWidth, size);
        }
        int width = (int) Math.max(1, Math.floor(maxWidth * fr().FONT_HEIGHT / size));
        return fr().listFormattedStringToWidth(text, width);
    }
}
