package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.client.render.Gfx;
import com.micatechnologies.minecraft.ldic.client.render.LdicFont;
import com.micatechnologies.minecraft.ldic.expr.Expr;
import com.micatechnologies.minecraft.ldic.theme.ElementDef;
import com.micatechnologies.minecraft.ldic.ui.TimeAgo;
import com.micatechnologies.minecraft.ldic.ui.UiNode;
import java.text.DateFormat;
import java.util.Date;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import org.lwjgl.opengl.GL11;

/**
 * Drawing shared by the theme's {@code list} element on the world and server screens: card
 * rows, small badges, the search box, scrolling and clipping. All in layout units, styled by the
 * element's own properties.
 */
final class ListCards {

    static final double SEARCH_H = 16;

    final ElementDef def;
    final LdicFont font;
    final double size;
    final double opacity;

    ListCards(UiNode n, MenuManager manager) {
        this.def = n.def;
        this.font = manager.fonts().get(def.statik("font", "minecraft"));
        this.size = def.num("size", 9);
        this.opacity = n.opacity;
    }

    long colour(String prop, long fallback) {
        Expr e = def.prop(prop);
        return e == null ? fallback : (long) e.num();
    }

    /** A card's background and border for its state. */
    void card(double x, double y, double w, double h, boolean hovered, boolean selected) {
        double r = def.num("radius", 3);
        long fill = selected ? colour("selected_colour", 0x80000000L) : hovered ? colour("hover_colour", 0x60000000L)
                : colour("colour", 0x40000000L);
        Gfx.roundedRect(x, y, x + w, y + h, r, fill, fill, opacity);
        long edge = selected ? colour("selected_border_colour", 0xFF4FC3F7L) : colour("border_colour", 0x18FFFFFFL);
        Gfx.roundedBorder(x, y, x + w, y + h, r, selected ? 1 : 0.5, edge, opacity);
    }

    void text(String s, double x, double y, double size, long colour) {
        font.draw(s, x, y, size, colour, false, opacity);
    }

    double width(String s, double size) {
        return font.width(s, size);
    }

    /** Text cut to {@code max} wide with "...". */
    String fit(String s, double size, double max) {
        if (width(s, size) <= max) {
            return s;
        }
        String dots = "...";
        int end = s.length();
        while (end > 0 && width(s.substring(0, end) + dots, size) > max) {
            end--;
        }
        return s.substring(0, end).trim() + dots;
    }

    /** A small pill with text, its right edge at {@code right}; returns its left edge. */
    double badge(String s, double right, double y, long colour) {
        double bs = size * 0.75;
        double w = width(s, bs) + 6;
        double x = right - w;
        Gfx.roundedRect(x, y, right, y + bs + 4, 2, (colour & 0x00FFFFFFL) | 0x38000000L, (colour & 0x00FFFFFFL) | 0x38000000L, opacity);
        text(s, x + 3, y + 2, bs, colour | 0xFF000000L);
        return x - 3;
    }

    /** The search box across the top of the list. */
    void search(GuiTextField field, double x, double y, double w, boolean hovered, String hint) {
        long accent = colour("accent_colour", 0xFF4FC3F7L);
        Gfx.roundedRect(x, y, x + w, y + SEARCH_H - 3, 2, 0x60000000L, 0x60000000L, opacity);
        Gfx.roundedBorder(x, y, x + w, y + SEARCH_H - 3, 2, 0.5, field.isFocused() ? accent : hovered ? 0x60FFFFFFL : 0x20FFFFFFL, opacity);
        double ts = size * 0.85;
        double ty = y + (SEARCH_H - 3 - ts) / 2;
        String t = field.getText();
        if (t.isEmpty() && !field.isFocused()) {
            text(hint, x + 5, ty, ts, colour("detail_colour", 0xFF9AA3AEL));
        } else {
            String shown = t;
            while (width(shown, ts) > w - 12 && shown.length() > 0) {
                shown = shown.substring(1);
            }
            text(shown, x + 5, ty, ts, colour("text_colour", 0xFFF0F0F0L));
            if (field.isFocused() && (System.currentTimeMillis() / 500) % 2 == 0) {
                double cx = x + 5 + width(shown, ts) + 0.5;
                Gfx.rect(cx, ty - 0.5, cx + 0.75, ty + ts + 0.5, accent, accent, opacity);
            }
        }
    }

    /** Clips drawing to a box given in layout units (needs the screen's ui scale). */
    static void clip(double x, double y, double w, double h, double ui) {
        Minecraft mc = Minecraft.getMinecraft();
        int f = new ScaledResolution(mc).getScaleFactor();
        double s = ui * f;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) Math.floor(x * s), (int) Math.floor(mc.displayHeight - (y + h) * s),
                (int) Math.ceil(w * s), (int) Math.ceil(h * s));
    }

    static void unclip() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    /** A thin scrollbar on the right edge when the content is taller than the view. */
    void scrollbar(double x, double y, double h, double content, double scroll) {
        if (content <= h) {
            return;
        }
        double thumb = Math.max(12, h * h / content);
        double ty = y + (h - thumb) * scroll / (content - h);
        Gfx.rect(x - 2, ty, x, ty + thumb, 0x80FFFFFFL, 0x80FFFFFFL, opacity);
    }

    /** "2 hours ago", or the date past a week. */
    static String ago(long then) {
        TimeAgo.Ago a = TimeAgo.of(then, System.currentTimeMillis());
        if (a == null) {
            return then <= 0 ? "" : DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(then));
        }
        return I18n.format(a.key, a.amount);
    }
}
