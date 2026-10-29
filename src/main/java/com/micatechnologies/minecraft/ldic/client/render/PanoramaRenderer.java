package com.micatechnologies.minecraft.ldic.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

/**
 * Draws a six-face cube panorama into a rectangle with a real perspective projection.
 *
 * <p>Unlike the vanilla title screen, which renders 64 jittered passes into a 256-pixel
 * viewport and blurs that, this draws one sharp pass at the rectangle's real resolution and
 * the real aspect ratio. Blur is opt-in: level {@code n} renders at 1/2^n resolution into a
 * framebuffer and scales it up with linear filtering, which is soft and costs less than the
 * sharp pass, not more. Blur needs a framebuffer, so it is main-thread only; on the loading
 * screen the panorama is always sharp.</p>
 *
 * <p>Face order follows the vanilla files {@code panorama_0..5}: front, right, back, left,
 * top, bottom.</p>
 */
public final class PanoramaRenderer {

    private Framebuffer buffer;

    /**
     * @param faces   six textures (any may be null while loading; nothing is drawn then)
     * @param x0      rectangle in the current GL coordinates (layout units)
     * @param pxLeft  the same rectangle in real window pixels, top-left origin
     */
    public void draw(Textures.Texture[] faces, double x0, double y0, double x1, double y1,
                     int pxLeft, int pxTop, int pxWidth, int pxHeight,
                     double yaw, double pitch, double fov, int blur, long tint, double opacity) {
        for (Textures.Texture t : faces) {
            if (t == null) {
                return;
            }
        }
        if (pxWidth <= 0 || pxHeight <= 0) {
            return;
        }
        GlBackend gl = Gfx.gl();
        if (blur <= 0 || !gl.supportsBlur()) {
            gl.viewport(pxLeft, gl.displayHeight() - pxTop - pxHeight, pxWidth, pxHeight);
            cube(gl, faces, pxWidth / (double) pxHeight, yaw, pitch, fov, tint, opacity);
            gl.viewport(0, 0, gl.displayWidth(), gl.displayHeight());
            return;
        }
        int level = Math.min(blur, 6);
        int w = Math.max(1, pxWidth >> level);
        int h = Math.max(1, pxHeight >> level);
        if (buffer == null) {
            buffer = new Framebuffer(w, h, false);
            buffer.setFramebufferFilter(GL11.GL_LINEAR);
        } else if (buffer.framebufferWidth != w || buffer.framebufferHeight != h) {
            buffer.createBindFramebuffer(w, h);
            buffer.setFramebufferFilter(GL11.GL_LINEAR);
        }
        buffer.setFramebufferColor(0, 0, 0, 1);
        buffer.framebufferClear();
        buffer.bindFramebuffer(true);
        cube(gl, faces, w / (double) h, yaw, pitch, fov, 0xFFFFFFFFL, 1);
        Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);

        gl.enableTexture();
        gl.bindTexture(buffer.framebufferTexture);
        double u1 = buffer.framebufferWidth / (double) buffer.framebufferTextureWidth;
        double v1 = buffer.framebufferHeight / (double) buffer.framebufferTextureHeight;
        // Framebuffer textures have a bottom-left origin.
        Gfx.texturedQuad(x0, y0, x1, y1, 0, v1, u1, 0, tint, opacity);
    }

    private static void cube(GlBackend gl, Textures.Texture[] faces, double aspect, double yaw, double pitch,
                             double fov, long tint, double opacity) {
        gl.matrixMode(true);
        gl.pushMatrix();
        gl.loadIdentity();
        Project.gluPerspective((float) fov, (float) aspect, 0.05F, 10.0F);
        gl.matrixMode(false);
        gl.pushMatrix();
        gl.loadIdentity();
        gl.rotate(180.0F, 1.0F, 0.0F, 0.0F);
        gl.rotate((float) pitch, 1.0F, 0.0F, 0.0F);
        gl.rotate((float) yaw, 0.0F, 1.0F, 0.0F);
        gl.enableTexture();
        gl.disableCull();
        gl.disableDepth();
        gl.depthMask(false);
        gl.enableBlend();

        int r = Gfx.r(tint);
        int g = Gfx.g(tint);
        int b = Gfx.b(tint);
        int a = Gfx.a(tint, opacity);
        for (int k = 0; k < 6; k++) {
            gl.pushMatrix();
            switch (k) {
                case 1:
                    gl.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                    break;
                case 2:
                    gl.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                    break;
                case 3:
                    gl.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
                    break;
                case 4:
                    gl.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                    break;
                case 5:
                    gl.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                    break;
                default:
                    break;
            }
            faces[k].bind();
            gl.begin(GL11.GL_QUADS, true);
            gl.vertex(-1, -1, 1, 0, 0, r, g, b, a);
            gl.vertex(1, -1, 1, 1, 0, r, g, b, a);
            gl.vertex(1, 1, 1, 1, 1, r, g, b, a);
            gl.vertex(-1, 1, 1, 0, 1, r, g, b, a);
            gl.end();
            gl.popMatrix();
        }

        gl.matrixMode(true);
        gl.popMatrix();
        gl.matrixMode(false);
        gl.popMatrix();
        gl.depthMask(true);
    }

    public void delete() {
        if (buffer != null) {
            buffer.deleteFramebuffer();
            buffer = null;
        }
    }
}
