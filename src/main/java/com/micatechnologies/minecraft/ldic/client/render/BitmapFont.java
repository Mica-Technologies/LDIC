package com.micatechnologies.minecraft.ldic.client.render;

import com.micatechnologies.minecraft.ldic.Ldic;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

/**
 * The Minecraft font drawn from {@code textures/font/ascii.png} without the vanilla
 * {@code FontRenderer}, for the loading screen (whose thread must not touch vanilla's renderer
 * or resource manager). Glyph widths are measured from the image the way the game does, so
 * text lines up with the vanilla font. Printable ASCII only; anything else draws as {@code ?}.
 */
public final class BitmapFont implements LdicFont {

    private static final int[] FORMAT_COLOURS = {
        0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
        0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    private Textures.Texture texture;
    private final int[] widths = new int[256];
    private boolean loaded;

    private void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        try (InputStream in = Minecraft.getMinecraft().defaultResourcePack.getInputStream(new ResourceLocation("textures/font/ascii.png"))) {
            BufferedImage img = ImageIO.read(in);
            int cell = img.getWidth() / 16;
            for (int c = 0; c < 256; c++) {
                int cx = (c % 16) * cell;
                int cy = (c / 16) * cell;
                int last = -1;
                for (int x = cell - 1; x >= 0 && last < 0; x--) {
                    for (int y = 0; y < cell; y++) {
                        if (((img.getRGB(cx + x, cy + y) >>> 24) & 0xFF) != 0) {
                            last = x;
                            break;
                        }
                    }
                }
                // In 8-pixel glyph units plus one pixel of spacing, as the game measures them.
                widths[c] = c == ' ' ? 4 : (int) (0.5 + (last + 1) * (8.0 / cell)) + 1;
            }
            texture = Textures.upload(img, false, false);
        } catch (Exception e) {
            Ldic.LOGGER.error("LDIC could not load the Minecraft font for the loading screen", e);
        }
    }

    private static int index(char c) {
        return c >= 32 && c < 127 ? c : '?';
    }

    @Override
    public double width(String text, double size) {
        load();
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                i++;
                continue;
            }
            w += widths[index(c)];
        }
        return w * size / 9.0;
    }

    @Override
    public void draw(String text, double x, double y, double size, long argb, boolean shadow, double opacity) {
        load();
        if (texture == null || Gfx.invisible(argb, opacity)) {
            return;
        }
        double s = size / 9.0;
        if (shadow) {
            run(text, x + s, y + s, s, LdicFont.shadowOf(argb), opacity, true);
        }
        run(text, x, y, s, argb, opacity, false);
    }

    private void run(String text, double x, double y, double s, long argb, double opacity, boolean isShadow) {
        GlBackend gl = Gfx.gl();
        gl.enableTexture();
        texture.bind();
        long colour = argb;
        double pen = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                int idx = "0123456789abcdef".indexOf(Character.toLowerCase(text.charAt(++i)));
                if (idx >= 0) {
                    long rgb = 0xFF000000L | FORMAT_COLOURS[idx];
                    colour = (argb & 0xFF000000L) | ((isShadow ? LdicFont.shadowOf(rgb) : rgb) & 0xFFFFFFL);
                } else if (Character.toLowerCase(text.charAt(i)) == 'r') {
                    colour = argb;
                }
                continue;
            }
            int g = index(c);
            if (g != ' ') {
                double u0 = (g % 16) / 16.0;
                double v0 = (g / 16) / 16.0;
                Gfx.texturedQuad(pen, y, pen + 8 * s, y + 8 * s, u0, v0, u0 + 1 / 16.0, v0 + 1 / 16.0, colour, opacity);
            }
            pen += widths[g] * s;
        }
    }

    public void delete() {
        if (texture != null) {
            Gfx.gl().deleteTexture(texture.id);
            texture = null;
        }
        loaded = false;
    }
}
