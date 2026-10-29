package com.micatechnologies.minecraft.ldic.client.render;

import org.lwjgl.opengl.GL11;

/**
 * Immediate drawing helpers, on whichever {@link GlBackend} the current thread uses: the
 * vanilla-safe one on the main thread, raw GL on the loading-screen thread.
 *
 * <p>Colours are {@code 0xAARRGGBB}; every call takes an extra opacity multiplier so element
 * fades compose without touching the colour values themselves.</p>
 */
public final class Gfx {

    private static final int CORNER_SEGMENTS = 8;
    private static final ThreadLocal<GlBackend> BACKEND = ThreadLocal.withInitial(() -> McGl.INSTANCE);

    private Gfx() {
    }

    /** The GL backend for the current thread. */
    public static GlBackend gl() {
        return BACKEND.get();
    }

    /**
     * Makes the current thread draw with {@code backend} (the loading screen uses
     * {@link RawGl}); null restores the default.
     */
    public static void useBackend(GlBackend backend) {
        if (backend == null) {
            BACKEND.remove();
        } else {
            BACKEND.set(backend);
        }
    }

    /** Standard alpha blending for UI drawing. Call before a batch of draws. */
    public static void beginUi() {
        GlBackend gl = gl();
        if (gl == McGl.INSTANCE) {
            net.minecraft.client.renderer.GlStateManager.enableBlend();
            net.minecraft.client.renderer.GlStateManager.tryBlendFuncSeparate(
                    net.minecraft.client.renderer.GlStateManager.SourceFactor.SRC_ALPHA,
                    net.minecraft.client.renderer.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    net.minecraft.client.renderer.GlStateManager.SourceFactor.ONE,
                    net.minecraft.client.renderer.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            net.minecraft.client.renderer.GlStateManager.disableAlpha();
            net.minecraft.client.renderer.GlStateManager.shadeModel(GL11.GL_SMOOTH);
        } else {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glShadeModel(GL11.GL_SMOOTH);
        }
        gl.disableCull();
        gl.disableDepth();
    }

    public static void endUi() {
        GlBackend gl = gl();
        if (gl == McGl.INSTANCE) {
            net.minecraft.client.renderer.GlStateManager.shadeModel(GL11.GL_FLAT);
            net.minecraft.client.renderer.GlStateManager.enableAlpha();
            net.minecraft.client.renderer.GlStateManager.color(1, 1, 1, 1);
        } else {
            GL11.glShadeModel(GL11.GL_FLAT);
        }
        gl.enableTexture();
    }

    static int a(long c, double opacity) {
        return (int) Math.round(((c >>> 24) & 0xFF) * clamp01(opacity));
    }

    static int r(long c) {
        return (int) ((c >>> 16) & 0xFF);
    }

    static int g(long c) {
        return (int) ((c >>> 8) & 0xFF);
    }

    static int b(long c) {
        return (int) (c & 0xFF);
    }

    static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    public static boolean invisible(long colour, double opacity) {
        return a(colour, opacity) == 0;
    }

    /** A solid or vertically graded rectangle. */
    public static void rect(double x0, double y0, double x1, double y1, long top, long bottom, double opacity) {
        if (invisible(top, opacity) && invisible(bottom, opacity)) {
            return;
        }
        GlBackend gl = gl();
        gl.disableTexture();
        gl.begin(GL11.GL_QUADS, false);
        gl.vertex(x0, y1, 0, 0, 0, r(bottom), g(bottom), b(bottom), a(bottom, opacity));
        gl.vertex(x1, y1, 0, 0, 0, r(bottom), g(bottom), b(bottom), a(bottom, opacity));
        gl.vertex(x1, y0, 0, 0, 0, r(top), g(top), b(top), a(top, opacity));
        gl.vertex(x0, y0, 0, 0, 0, r(top), g(top), b(top), a(top, opacity));
        gl.end();
        gl.enableTexture();
    }

    /** A rectangle graded from {@code left} to {@code right}. */
    public static void rectHorizontal(double x0, double y0, double x1, double y1, long left, long right, double opacity) {
        if (invisible(left, opacity) && invisible(right, opacity)) {
            return;
        }
        GlBackend gl = gl();
        gl.disableTexture();
        gl.begin(GL11.GL_QUADS, false);
        gl.vertex(x0, y1, 0, 0, 0, r(left), g(left), b(left), a(left, opacity));
        gl.vertex(x1, y1, 0, 0, 0, r(right), g(right), b(right), a(right, opacity));
        gl.vertex(x1, y0, 0, 0, 0, r(right), g(right), b(right), a(right, opacity));
        gl.vertex(x0, y0, 0, 0, 0, r(left), g(left), b(left), a(left, opacity));
        gl.end();
        gl.enableTexture();
    }

    /** A rectangle with rounded corners of {@code radius}, optionally graded top to bottom. */
    public static void roundedRect(double x0, double y0, double x1, double y1, double radius, long top, long bottom, double opacity) {
        double rad = Math.min(radius, Math.min(x1 - x0, y1 - y0) / 2);
        if (rad <= 0.01) {
            rect(x0, y0, x1, y1, top, bottom, opacity);
            return;
        }
        if (invisible(top, opacity) && invisible(bottom, opacity)) {
            return;
        }
        GlBackend gl = gl();
        gl.disableTexture();
        gl.begin(GL11.GL_TRIANGLE_FAN, false);
        double cx = (x0 + x1) / 2;
        double cy = (y0 + y1) / 2;
        vertex(gl, cx, cy, y0, y1, top, bottom, opacity);
        corner(gl, x1 - rad, y0 + rad, rad, -90, y0, y1, top, bottom, opacity);
        corner(gl, x1 - rad, y1 - rad, rad, 0, y0, y1, top, bottom, opacity);
        corner(gl, x0 + rad, y1 - rad, rad, 90, y0, y1, top, bottom, opacity);
        corner(gl, x0 + rad, y0 + rad, rad, 180, y0, y1, top, bottom, opacity);
        vertex(gl, x1 - rad, y0, y0, y1, top, bottom, opacity);
        gl.end();
        gl.enableTexture();
    }

    private static void corner(GlBackend gl, double cx, double cy, double rad, double startDeg,
                               double y0, double y1, long top, long bottom, double opacity) {
        for (int i = 0; i <= CORNER_SEGMENTS; i++) {
            double ang = Math.toRadians(startDeg + 90.0 * i / CORNER_SEGMENTS);
            vertex(gl, cx + Math.cos(ang) * rad, cy + Math.sin(ang) * rad, y0, y1, top, bottom, opacity);
        }
    }

    private static void vertex(GlBackend gl, double x, double y, double y0, double y1, long top, long bottom, double opacity) {
        double t = y1 == y0 ? 0 : (y - y0) / (y1 - y0);
        int a = (int) Math.round(a(top, opacity) + (a(bottom, opacity) - a(top, opacity)) * t);
        int r = (int) Math.round(r(top) + (r(bottom) - r(top)) * t);
        int g = (int) Math.round(g(top) + (g(bottom) - g(top)) * t);
        int b = (int) Math.round(b(top) + (b(bottom) - b(top)) * t);
        gl.vertex(x, y, 0, 0, 0, r, g, b, a);
    }

    /** An outline {@code w} thick drawn inside the box (square corners). */
    public static void border(double x0, double y0, double x1, double y1, double w, long colour, double opacity) {
        if (w <= 0 || invisible(colour, opacity)) {
            return;
        }
        rect(x0, y0, x1, y0 + w, colour, colour, opacity);
        rect(x0, y1 - w, x1, y1, colour, colour, opacity);
        rect(x0, y0 + w, x0 + w, y1 - w, colour, colour, opacity);
        rect(x1 - w, y0 + w, x1, y1 - w, colour, colour, opacity);
    }

    /** An outline that follows rounded corners, drawn as a line loop. */
    public static void roundedBorder(double x0, double y0, double x1, double y1, double radius, double w, long colour, double opacity) {
        double rad = Math.min(radius, Math.min(x1 - x0, y1 - y0) / 2);
        if (rad <= 0.01) {
            border(x0, y0, x1, y1, w, colour, opacity);
            return;
        }
        if (w <= 0 || invisible(colour, opacity)) {
            return;
        }
        GlBackend gl = gl();
        gl.disableTexture();
        GL11.glLineWidth((float) Math.max(1, w));
        gl.begin(GL11.GL_LINE_LOOP, false);
        double h = w / 2;
        corner(gl, x1 - rad, y0 + rad, rad - h, -90, y0, y1, colour, colour, opacity);
        corner(gl, x1 - rad, y1 - rad, rad - h, 0, y0, y1, colour, colour, opacity);
        corner(gl, x0 + rad, y1 - rad, rad - h, 90, y0, y1, colour, colour, opacity);
        corner(gl, x0 + rad, y0 + rad, rad - h, 180, y0, y1, colour, colour, opacity);
        gl.end();
        GL11.glLineWidth(1);
        gl.enableTexture();
    }

    /** A textured quad tinted by {@code colour}. The texture must already be bound. */
    public static void texturedQuad(double x0, double y0, double x1, double y1,
                                    double u0, double v0, double u1, double v1, long colour, double opacity) {
        if (invisible(colour, opacity)) {
            return;
        }
        GlBackend gl = gl();
        int r = r(colour);
        int g = g(colour);
        int b = b(colour);
        int a = a(colour, opacity);
        gl.begin(GL11.GL_QUADS, true);
        gl.vertex(x0, y1, 0, u0, v1, r, g, b, a);
        gl.vertex(x1, y1, 0, u1, v1, r, g, b, a);
        gl.vertex(x1, y0, 0, u1, v0, r, g, b, a);
        gl.vertex(x0, y0, 0, u0, v0, r, g, b, a);
        gl.end();
    }

    /**
     * Draws a texture region as a nine-slice: corners of {@code border} texture pixels stay
     * square, edges and centre stretch. Used for vanilla-style buttons of any size.
     */
    public static void nineSlice(double x0, double y0, double x1, double y1, double texW, double texH,
                                 double rx, double ry, double rw, double rh, double border, double drawBorder,
                                 long colour, double opacity) {
        double bw = Math.min(drawBorder, (x1 - x0) / 2);
        double bh = Math.min(drawBorder, (y1 - y0) / 2);
        double[] xs = {x0, x0 + bw, x1 - bw, x1};
        double[] ys = {y0, y0 + bh, y1 - bh, y1};
        double[] us = {rx, rx + border, rx + rw - border, rx + rw};
        double[] vs = {ry, ry + border, ry + rh - border, ry + rh};
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < 3; i++) {
                if (xs[i + 1] <= xs[i] || ys[j + 1] <= ys[j]) {
                    continue;
                }
                texturedQuad(xs[i], ys[j], xs[i + 1], ys[j + 1],
                        us[i] / texW, vs[j] / texH, us[i + 1] / texW, vs[j + 1] / texH, colour, opacity);
            }
        }
    }
}
