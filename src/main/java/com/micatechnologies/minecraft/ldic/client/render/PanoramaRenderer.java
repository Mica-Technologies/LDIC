package com.micatechnologies.minecraft.ldic.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
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
 * sharp pass, not more.</p>
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
        Minecraft mc = Minecraft.getMinecraft();
        boolean useBuffer = blur > 0 && OpenGlHelper.isFramebufferEnabled();
        if (!useBuffer) {
            GlStateManager.viewport(pxLeft, mc.displayHeight - pxTop - pxHeight, pxWidth, pxHeight);
            cube(faces, pxWidth / (double) pxHeight, yaw, pitch, fov, tint, opacity);
            GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);
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
        cube(faces, w / (double) h, yaw, pitch, fov, 0xFFFFFFFFL, 1);
        mc.getFramebuffer().bindFramebuffer(true);

        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(buffer.framebufferTexture);
        double u1 = buffer.framebufferWidth / (double) buffer.framebufferTextureWidth;
        double v1 = buffer.framebufferHeight / (double) buffer.framebufferTextureHeight;
        // Framebuffer textures have a bottom-left origin.
        Gfx.texturedQuad(x0, y0, x1, y1, 0, v1, u1, 0, tint, opacity);
    }

    private static void cube(Textures.Texture[] faces, double aspect, double yaw, double pitch, double fov,
                             long tint, double opacity) {
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        Project.gluPerspective((float) fov, (float) aspect, 0.05F, 10.0F);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate((float) pitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate((float) yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();

        int r = Gfx.r(tint);
        int g = Gfx.g(tint);
        int b = Gfx.b(tint);
        int a = Gfx.a(tint, opacity);
        Tessellator t = Tessellator.getInstance();
        BufferBuilder bb = t.getBuffer();
        for (int k = 0; k < 6; k++) {
            GlStateManager.pushMatrix();
            switch (k) {
                case 1:
                    GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                    break;
                case 2:
                    GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                    break;
                case 3:
                    GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
                    break;
                case 4:
                    GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                    break;
                case 5:
                    GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                    break;
                default:
                    break;
            }
            faces[k].bind();
            bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            bb.pos(-1, -1, 1).tex(0, 0).color(r, g, b, a).endVertex();
            bb.pos(1, -1, 1).tex(1, 0).color(r, g, b, a).endVertex();
            bb.pos(1, 1, 1).tex(1, 1).color(r, g, b, a).endVertex();
            bb.pos(-1, 1, 1).tex(0, 1).color(r, g, b, a).endVertex();
            t.draw();
            GlStateManager.popMatrix();
        }

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
        GlStateManager.depthMask(true);
    }

    public void delete() {
        if (buffer != null) {
            buffer.deleteFramebuffer();
            buffer = null;
        }
    }
}
