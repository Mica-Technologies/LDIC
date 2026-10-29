package com.micatechnologies.minecraft.ldic.client.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * A TrueType/OpenType (or AWT logical) font rasterised into glyph atlases.
 *
 * <p>Glyphs are rendered at the real screen pixel size of the text, so text is crisp at any GUI
 * scale. Atlases are built lazily, 256 code points per page, and cached per pixel size; only
 * the pages a theme actually uses are ever built (no pre-decoding of every Unicode page).</p>
 */
public final class TtfFont implements LdicFont {

    private static final int[] FORMAT_COLOURS = {
        0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
        0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };
    private static final int MAX_SIZES = 8;

    private final Font base;
    /** Real pixels per layout unit for the frame being drawn; set by the renderer. */
    private double pixelScale = 2;
    private final Map<Integer, Sized> sizes = new HashMap<>();

    public TtfFont(Font base) {
        this.base = base;
    }

    public void setPixelScale(double pixelScale) {
        this.pixelScale = Math.max(0.25, pixelScale);
    }

    private Sized sized(double size) {
        int px = (int) Math.max(4, Math.min(256, Math.round(size * pixelScale)));
        Sized s = sizes.get(px);
        if (s == null) {
            if (sizes.size() >= MAX_SIZES) {
                for (Sized old : sizes.values()) {
                    old.delete();
                }
                sizes.clear();
            }
            s = new Sized(base, px);
            sizes.put(px, s);
        }
        return s;
    }

    public void delete() {
        for (Sized s : sizes.values()) {
            s.delete();
        }
        sizes.clear();
    }

    @Override
    public double width(String text, double size) {
        Sized s = sized(size);
        double w = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                i++;
                continue;
            }
            w += s.advance(c);
        }
        // Layout units: glyphs are px tall for a line of size units.
        return w * size / s.px;
    }

    @Override
    public void draw(String text, double x, double y, double size, long argb, boolean shadow, double opacity) {
        if (Gfx.invisible(argb, opacity)) {
            return;
        }
        if (shadow) {
            double off = size / 9.0;
            drawRun(text, x + off, y + off, size, LdicFont.shadowOf(argb), opacity, true);
        }
        drawRun(text, x, y, size, argb, opacity, false);
    }

    private void drawRun(String text, double x, double y, double size, long argb, double opacity, boolean isShadow) {
        Sized s = sized(size);
        double unit = size / s.px;
        long colour = argb;
        double pen = x;
        Gfx.gl().enableTexture();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(++i));
                int idx = "0123456789abcdef".indexOf(code);
                if (idx >= 0) {
                    long rgb = FORMAT_COLOURS[idx];
                    colour = (argb & 0xFF000000L) | (isShadow ? LdicFont.shadowOf(0xFF000000L | rgb) & 0xFFFFFFL : rgb);
                } else if (code == 'r') {
                    colour = argb;
                }
                continue;
            }
            Page page = s.page(c >> 8);
            int cell = c & 0xFF;
            double adv = s.advance(c);
            if (c != ' ') {
                page.bind();
                double u0 = (cell % 16) * s.cellW / (double) page.size;
                double v0 = (cell / 16) * s.cellH / (double) page.size;
                double u1 = u0 + s.cellW / (double) page.size;
                double v1 = v0 + s.cellH / (double) page.size;
                double gx = pen - Sized.PAD * unit;
                double gy = y - Sized.PAD * unit;
                Gfx.texturedQuad(gx, gy, gx + s.cellW * unit, gy + s.cellH * unit, u0, v0, u1, v1, colour, opacity);
            }
            pen += adv * unit;
        }
    }

    /** One pixel size of the font: metrics plus lazily built atlas pages. */
    private static final class Sized {
        static final int PAD = 2;
        final int px;
        final Font font;
        final FontMetrics metrics;
        final int ascent;
        final int cellW;
        final int cellH;
        final Map<Integer, Page> pages = new HashMap<>();
        final Map<Character, Integer> advances = new HashMap<>();

        Sized(Font base, int px) {
            this.px = px;
            // Size the font so its line height (ascent + descent + leading) is exactly px.
            Font probe = base.deriveFont((float) px);
            FontMetrics pm = metricsFor(probe);
            float fitted = (float) px * px / Math.max(1, pm.getHeight());
            this.font = base.deriveFont(fitted);
            this.metrics = metricsFor(font);
            this.ascent = metrics.getAscent() + metrics.getLeading() / 2;
            int maxAdvance = metrics.getMaxAdvance() > 0 ? metrics.getMaxAdvance() : px * 2;
            this.cellW = Math.min(maxAdvance, px * 2) + PAD * 2;
            this.cellH = px + PAD * 2;
        }

        private static FontMetrics metricsFor(Font f) {
            BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scratch.createGraphics();
            g.setFont(f);
            FontMetrics m = g.getFontMetrics();
            g.dispose();
            return m;
        }

        int advance(char c) {
            Integer a = advances.get(c);
            if (a == null) {
                a = font.canDisplay(c) ? metrics.charWidth(c) : metrics.charWidth('?');
                advances.put(c, a);
            }
            return a;
        }

        Page page(int index) {
            Page p = pages.get(index);
            if (p == null) {
                p = new Page(this, index);
                pages.put(index, p);
            }
            return p;
        }

        void delete() {
            for (Page p : pages.values()) {
                Gfx.gl().deleteTexture(p.texture.id);
            }
            pages.clear();
        }
    }

    private static final class Page {
        final Textures.Texture texture;
        final int size;

        Page(Sized s, int index) {
            int side = Math.max(s.cellW, s.cellH) * 16;
            this.size = side;
            BufferedImage img = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setFont(s.font);
            g.setColor(Color.WHITE);
            for (int cell = 0; cell < 256; cell++) {
                char c = (char) ((index << 8) | cell);
                if (Character.isISOControl(c) || !s.font.canDisplay(c)) {
                    continue;
                }
                int gx = (cell % 16) * s.cellW + Sized.PAD;
                int gy = (cell / 16) * s.cellH + Sized.PAD;
                g.setClip((cell % 16) * s.cellW, (cell / 16) * s.cellH, s.cellW, s.cellH);
                g.drawString(String.valueOf(c), gx, gy + s.ascent);
            }
            g.dispose();
            // A square page of side 16 * max(cellW, cellH); drawRun's uv maths divides by that side.
            this.texture = Textures.upload(img, true, false);
        }

        void bind() {
            texture.bind();
        }
    }
}
