package com.micatechnologies.minecraft.ldic.client.render;

import com.micatechnologies.minecraft.ldic.expr.Expr;
import com.micatechnologies.minecraft.ldic.expr.Library;
import com.micatechnologies.minecraft.ldic.theme.ElementDef;
import com.micatechnologies.minecraft.ldic.ui.NodeRenderer;
import com.micatechnologies.minecraft.ldic.ui.UiNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Draws theme elements, for the main menu and the loading screen alike (GL goes through
 * {@link Gfx#gl()}, so the same code is safe on either thread). One instance per open screen; per-node caches (text
 * layout, shuffled slideshow order, panorama framebuffer) live in {@link UiNode#renderState}.
 */
public final class ThemeRenderer implements NodeRenderer {

    private static final String WIDGETS = "minecraft:textures/gui/widgets.png";

    private final Textures textures;
    private final Fonts fonts;
    private final java.util.function.Consumer<UiNode> hoverStarted;
    /** Seconds on the menu's clock (continues across screens), for slideshows and frames. */
    public double now;
    /** Layout units to GUI pixels, and GUI pixels to window pixels. */
    public double uiScale = 1;
    public double guiScale = 2;
    /** The rectangle (layout units) of the theme's foreign_buttons element this frame, or null. */
    public double[] foreignArea;
    public UiNode foreignNode;
    private final List<PanoramaRenderer> panoramas = new ArrayList<>();

    public ThemeRenderer(Textures textures, Fonts fonts, java.util.function.Consumer<UiNode> hoverStarted) {
        this.textures = textures;
        this.fonts = fonts;
        this.hoverStarted = hoverStarted;
    }

    public void dispose() {
        for (PanoramaRenderer p : panoramas) {
            p.delete();
        }
        panoramas.clear();
    }

    private static final class TextState {
        String text;
        double wrapWidth;
        double size;
        List<String> lines;
    }

    private static final class SlideState {
        int[] order;
    }

    // -------------------------------------------------------------------------------------

    @Override
    public void measure(UiNode n) {
        if (!n.def.type.equals("text")) {
            n.width = n.def.width == null ? n.def.locals.parentWidth.getNumber() : 0;
            n.height = n.def.height == null ? n.def.locals.parentHeight.getNumber() : 0;
            return;
        }
        TextState ts = text(n);
        double max = 0;
        LdicFont font = font(n.def);
        for (String line : ts.lines) {
            max = Math.max(max, font.width(line, ts.size));
        }
        n.width = max;
        n.height = ts.lines.size() * ts.size * n.def.num("line_spacing", 1) - (ts.size * n.def.num("line_spacing", 1) - ts.size);
    }

    private LdicFont font(ElementDef d) {
        return fonts.get(d.statik("font", "minecraft"));
    }

    private TextState text(UiNode n) {
        ElementDef d = n.def;
        TextState ts = n.renderState instanceof TextState ? (TextState) n.renderState : new TextState();
        n.renderState = ts;
        String text = d.str("text", "");
        double size = d.num("size", 9);
        double wrap = 0;
        if (Boolean.parseBoolean(d.statik("wrap", "false")) && d.width != null) {
            wrap = d.width.resolve(d.locals.parentWidth.getNumber());
        }
        if (ts.lines == null || !text.equals(ts.text) || wrap != ts.wrapWidth || size != ts.size) {
            ts.text = text;
            ts.wrapWidth = wrap;
            ts.size = size;
            ts.lines = font(d).wrap(text, wrap, size);
        }
        return ts;
    }

    @Override
    public void begin(UiNode n) {
        if (n.rotation != 0 || n.scale != 1) {
            Gfx.gl().pushMatrix();
            Gfx.gl().translate(n.pivotX, n.pivotY, 0);
            if (n.rotation != 0) {
                Gfx.gl().rotate((float) n.rotation, 0, 0, 1);
            }
            if (n.scale != 1) {
                Gfx.gl().scale(n.scale, n.scale, 1);
            }
            Gfx.gl().translate(-n.pivotX, -n.pivotY, 0);
        }
    }

    @Override
    public void end(UiNode n) {
        if (n.rotation != 0 || n.scale != 1) {
            Gfx.gl().popMatrix();
        }
    }

    @Override
    public void hoverStarted(UiNode node) {
        hoverStarted.accept(node);
    }

    @Override
    public void draw(UiNode n) {
        if (n.opacity <= 0) {
            return;
        }
        switch (n.def.type) {
            case "rect":
                drawRect(n);
                break;
            case "image":
                drawImage(n);
                break;
            case "text":
                drawText(n);
                break;
            case "panorama":
                drawPanorama(n);
                break;
            case "button":
                drawButton(n);
                break;
            case "foreign_buttons":
                foreignArea = new double[] {n.x, n.y, n.width, n.height};
                foreignNode = n;
                break;
            default:
                break;
        }
    }

    // -------------------------------------------------------------------------------------

    private void drawRect(UiNode n) {
        ElementDef d = n.def;
        long top = colour(d, "colour", 0);
        long bottom = d.has("colour_bottom") ? colour(d, "colour_bottom", top) : top;
        double radius = d.num("radius", 0);
        if ("horizontal".equals(d.statik("gradient", "vertical")) && radius <= 0) {
            // colour is the left edge, colour_bottom the right edge.
            Gfx.rectHorizontal(n.x, n.y, n.x + n.width, n.y + n.height, top, bottom, n.opacity);
        } else {
            Gfx.roundedRect(n.x, n.y, n.x + n.width, n.y + n.height, radius, top, bottom, n.opacity);
        }
        if (d.has("border_colour")) {
            Gfx.roundedBorder(n.x, n.y, n.x + n.width, n.y + n.height, radius, d.num("border_width", 1),
                    colour(d, "border_colour", 0), n.opacity);
        }
    }

    private void drawImage(UiNode n) {
        ElementDef d = n.def;
        boolean linear = !"nearest".equals(d.statik("filter", "linear"));
        long tint = colour(d, "tint", 0xFFFFFFFFL);
        List<Expr> images = d.list("images");
        if (!images.isEmpty()) {
            drawSlideshow(n, images, linear, tint);
            return;
        }
        String path = n.isHot() && d.has("hover_image") ? d.str("hover_image", null) : d.str("image", null);
        Textures.Texture t = textures.get(path, linear);
        if (t != null) {
            drawTexture(n, t, tint, n.opacity);
        }
    }

    private void drawSlideshow(UiNode n, List<Expr> images, boolean linear, long tint) {
        ElementDef d = n.def;
        SlideState s = n.renderState instanceof SlideState ? (SlideState) n.renderState : null;
        if (s == null || s.order.length != images.size()) {
            s = new SlideState();
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < images.size(); i++) {
                order.add(i);
            }
            if (Boolean.parseBoolean(d.statik("shuffle", "false"))) {
                Collections.shuffle(order, new Random());
            }
            s.order = order.stream().mapToInt(Integer::intValue).toArray();
            n.renderState = s;
        }
        double duration = Math.max(0.1, d.num("duration", 6));
        double fade = Math.max(0, Math.min(duration, d.num("fade", 1)));
        long step = (long) Math.floor(now / duration);
        double phase = now - step * duration;
        int count = s.order.length;
        int cur = s.order[(int) (step % count)];
        int next = s.order[(int) ((step + 1) % count)];
        Textures.Texture a = textures.get(images.get(cur).asString(), linear);
        if (a != null) {
            drawTexture(n, a, tint, n.opacity);
        }
        if (count > 1 && fade > 0 && phase > duration - fade) {
            double t = (phase - (duration - fade)) / fade;
            Textures.Texture b = textures.get(images.get(next).asString(), linear);
            if (b != null) {
                drawTexture(n, b, tint, n.opacity * t);
            }
        }
    }

    /** Draws a texture into the node's box honouring fit, uv and animation frames. */
    private void drawTexture(UiNode n, Textures.Texture t, long tint, double opacity) {
        ElementDef d = n.def;
        double u0 = d.num("uv0", 0);
        double v0 = d.num("uv1", 0);
        double u1 = d.num("uv2", 1);
        double v1 = d.num("uv3", 1);
        int frames = (int) Math.max(1, d.num("frames", 1));
        double texW = t.width * (u1 - u0);
        double texH = t.height * (v1 - v0) / frames;
        if (frames > 1) {
            int f = d.has("frame") ? (int) Math.floor(d.num("frame", 0)) : (int) Math.floor(now * d.num("fps", 10));
            f = ((f % frames) + frames) % frames;
            double fh = (v1 - v0) / frames;
            v0 = v0 + fh * f;
            v1 = v0 + fh;
        }
        double x0 = n.x;
        double y0 = n.y;
        double x1 = n.x + n.width;
        double y1 = n.y + n.height;
        switch (d.statik("fit", "stretch")) {
            case "cover": {
                double s = Math.max(n.width / texW, n.height / texH);
                double cropU = (u1 - u0) * (1 - n.width / (texW * s)) / 2;
                double cropV = (v1 - v0) * (1 - n.height / (texH * s)) / 2;
                u0 += cropU;
                u1 -= cropU;
                v0 += cropV;
                v1 -= cropV;
                break;
            }
            case "contain": {
                double s = Math.min(n.width / texW, n.height / texH);
                double w = texW * s;
                double h = texH * s;
                x0 = n.x + (n.width - w) / 2;
                y0 = n.y + (n.height - h) / 2;
                x1 = x0 + w;
                y1 = y0 + h;
                break;
            }
            case "center": {
                x0 = n.x + (n.width - texW) / 2;
                y0 = n.y + (n.height - texH) / 2;
                x1 = x0 + texW;
                y1 = y0 + texH;
                break;
            }
            case "tile":
                u1 = u0 + (u1 - u0) * n.width / texW;
                v1 = v0 + (v1 - v0) * n.height / texH;
                break;
            default:
                break;
        }
        Gfx.gl().enableTexture();
        t.bind();
        Gfx.texturedQuad(x0, y0, x1, y1, u0, v0, u1, v1, tint, opacity);
    }

    private void drawText(UiNode n) {
        ElementDef d = n.def;
        TextState ts = text(n);
        LdicFont font = font(d);
        long colour = colour(d, "colour", 0xFFFFFFFFL);
        boolean shadow = d.bool("shadow", true);
        String align = d.statik("align", "left");
        double lineH = ts.size * d.num("line_spacing", 1);
        double y = n.y;
        for (String line : ts.lines) {
            double w = font.width(line, ts.size);
            double x = align.equals("center") ? n.x + (n.width - w) / 2 : align.equals("right") ? n.x + n.width - w : n.x;
            font.draw(line, x, y, ts.size, colour, shadow, n.opacity);
            y += lineH;
        }
    }

    private void drawPanorama(UiNode n) {
        ElementDef d = n.def;
        boolean linear = !"nearest".equals(d.statik("filter", "linear"));
        String pattern = d.str("images", "");
        Textures.Texture[] faces = new Textures.Texture[6];
        for (int i = 0; i < 6; i++) {
            faces[i] = textures.get(pattern.replace("#", Integer.toString(i)), linear);
        }
        double speed = d.num("speed", 2);
        double yaw = d.has("angle") ? d.num("angle", 0) : -now * speed;
        double pitch = d.has("pitch") ? d.num("pitch", 0) : 20 + 25 * Math.sin(now * 0.05);
        PanoramaRenderer pr;
        if (n.renderState instanceof PanoramaRenderer) {
            pr = (PanoramaRenderer) n.renderState;
        } else {
            pr = new PanoramaRenderer();
            n.renderState = pr;
            panoramas.add(pr);
        }
        double px = uiScale * guiScale;
        pr.draw(faces, n.x, n.y, n.x + n.width, n.y + n.height,
                (int) Math.round(n.x * px), (int) Math.round(n.y * px),
                (int) Math.round(n.width * px), (int) Math.round(n.height * px),
                yaw, pitch, d.num("fov", 85), (int) Math.max(0, d.num("blur", 0)),
                colour(d, "tint", 0xFFFFFFFFL), n.opacity);
        Gfx.beginUi();
    }

    private void drawButton(UiNode n) {
        ElementDef d = n.def;
        double x0 = n.x;
        double y0 = n.y;
        double x1 = n.x + n.width;
        double y1 = n.y + n.height;
        double ht = n.enabled ? n.hoverT : 0;
        boolean linear = !"nearest".equals(d.statik("filter", "linear"));
        switch (d.statik("style", "vanilla")) {
            case "vanilla": {
                Textures.Texture w = textures.get(WIDGETS, false);
                if (w != null) {
                    Gfx.gl().enableTexture();
                    w.bind();
                    // widgets.png rows: disabled at v=46, normal at 66, hover at 86; 200x20 each.
                    if (!n.enabled) {
                        vanillaRow(x0, y0, x1, y1, 46, n.opacity);
                    } else {
                        vanillaRow(x0, y0, x1, y1, 66, n.opacity * (1 - ht));
                        if (ht > 0) {
                            vanillaRow(x0, y0, x1, y1, 86, n.opacity * ht);
                        }
                    }
                }
                break;
            }
            case "flat": {
                long base = colour(d, "colour", 0xA0000000L);
                long hover = d.has("hover_colour") ? colour(d, "hover_colour", base) : (long) Library.mixColour(base, (base & 0xFF000000L) | 0xFFFFFF, 0.25);
                long fill = (long) Library.mixColour(base, hover, ht);
                if (n.pressed && d.has("pressed_colour")) {
                    fill = colour(d, "pressed_colour", fill);
                }
                if (!n.enabled) {
                    fill = d.has("disabled_colour") ? colour(d, "disabled_colour", base) : (long) Library.mixColour(base, base & 0xFF000000L, 0.5);
                }
                double radius = d.num("radius", 0);
                Gfx.roundedRect(x0, y0, x1, y1, radius, fill, fill, n.opacity);
                if (d.has("border_colour")) {
                    long bc = colour(d, "border_colour", 0);
                    long hb = d.has("hover_border_colour") ? colour(d, "hover_border_colour", bc) : bc;
                    Gfx.roundedBorder(x0, y0, x1, y1, radius, d.num("border_width", 1), (long) Library.mixColour(bc, hb, ht), n.opacity);
                }
                break;
            }
            case "image": {
                Textures.Texture t = textures.get(d.str("image", null), linear);
                if (t != null) {
                    int rows = (int) Math.max(1, d.num("sheet_rows", 2));
                    Gfx.gl().enableTexture();
                    t.bind();
                    double rh = 1.0 / rows;
                    if (!n.enabled && rows >= 3) {
                        Gfx.texturedQuad(x0, y0, x1, y1, 0, 2 * rh, 1, 3 * rh, 0xFFFFFFFFL, n.opacity);
                    } else if (n.pressed && rows >= 4) {
                        Gfx.texturedQuad(x0, y0, x1, y1, 0, 3 * rh, 1, 4 * rh, 0xFFFFFFFFL, n.opacity);
                    } else {
                        Gfx.texturedQuad(x0, y0, x1, y1, 0, 0, 1, rh, 0xFFFFFFFFL, n.opacity * (rows >= 2 ? 1 - ht : 1));
                        if (rows >= 2 && ht > 0) {
                            Gfx.texturedQuad(x0, y0, x1, y1, 0, rh, 1, 2 * rh, 0xFFFFFFFFL, n.opacity * ht);
                        }
                    }
                }
                break;
            }
            default:
                break;
        }

        String text = d.str("text", "");
        double size = d.num("size", 9);
        double pad = d.num("padding", 6);
        LdicFont font = font(d);
        double textW = text.isEmpty() ? 0 : font.width(text, size);
        double iconSize = d.has("icon") ? d.num("icon_size", 16) : 0;
        double gap = iconSize > 0 && textW > 0 ? pad / 2 : 0;
        double contentW = iconSize + gap + textW;
        String align = d.statik("text_align", "center");
        double cx = align.equals("left") ? x0 + pad : align.equals("right") ? x1 - pad - contentW : x0 + (n.width - contentW) / 2;
        if (iconSize > 0) {
            Textures.Texture icon = textures.get(d.str("icon", null), linear);
            if (icon != null) {
                Gfx.gl().enableTexture();
                icon.bind();
                double iy = y0 + (n.height - iconSize) / 2;
                Gfx.texturedQuad(cx, iy, cx + iconSize, iy + iconSize, 0, 0, 1, 1, colour(d, "icon_tint", 0xFFFFFFFFL), n.opacity);
            }
        }
        if (textW > 0) {
            long tc = colour(d, "text_colour", 0xFFE0E0E0L);
            long hc = colour(d, "hover_text_colour", 0xFFFFFFA0L);
            long colour = n.enabled ? (long) Library.mixColour(tc, hc, ht) : colour(d, "disabled_text_colour", 0xFFA0A0A0L);
            double ty = y0 + (n.height - size * 8 / 9) / 2;
            font.draw(text, cx + iconSize + gap, ty, size, colour, d.bool("shadow", true), n.opacity);
        }
    }

    private static void vanillaRow(double x0, double y0, double x1, double y1, double v, double opacity) {
        Gfx.nineSlice(x0, y0, x1, y1, 256, 256, 0, v, 200, 20, 3, 3, 0xFFFFFFFFL, opacity);
    }

    private static long colour(ElementDef d, String prop, long fallback) {
        Expr e = d.prop(prop);
        return e == null ? fallback : (long) e.num();
    }
}
