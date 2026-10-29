package com.micatechnologies.minecraft.ldic.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/** Main-thread backend: everything through {@link GlStateManager} and the {@link Tessellator}. */
public final class McGl implements GlBackend {

    public static final McGl INSTANCE = new McGl();

    private McGl() {
    }

    @Override
    public void enableTexture() {
        GlStateManager.enableTexture2D();
    }

    @Override
    public void disableTexture() {
        GlStateManager.disableTexture2D();
    }

    @Override
    public void bindTexture(int id) {
        GlStateManager.bindTexture(id);
    }

    @Override
    public int genTexture() {
        return GlStateManager.generateTexture();
    }

    @Override
    public void deleteTexture(int id) {
        GlStateManager.deleteTexture(id);
    }

    @Override
    public void enableBlend() {
        GlStateManager.enableBlend();
    }

    @Override
    public void pushMatrix() {
        GlStateManager.pushMatrix();
    }

    @Override
    public void popMatrix() {
        GlStateManager.popMatrix();
    }

    @Override
    public void translate(double x, double y, double z) {
        GlStateManager.translate(x, y, z);
    }

    @Override
    public void rotate(float angle, float x, float y, float z) {
        GlStateManager.rotate(angle, x, y, z);
    }

    @Override
    public void scale(double x, double y, double z) {
        GlStateManager.scale(x, y, z);
    }

    @Override
    public void matrixMode(boolean projection) {
        GlStateManager.matrixMode(projection ? GL11.GL_PROJECTION : GL11.GL_MODELVIEW);
    }

    @Override
    public void loadIdentity() {
        GlStateManager.loadIdentity();
    }

    @Override
    public void viewport(int x, int y, int width, int height) {
        GlStateManager.viewport(x, y, width, height);
    }

    @Override
    public void depthMask(boolean on) {
        GlStateManager.depthMask(on);
    }

    @Override
    public void disableCull() {
        GlStateManager.disableCull();
    }

    @Override
    public void disableDepth() {
        GlStateManager.disableDepth();
    }

    private boolean textured;

    @Override
    public void begin(int mode, boolean textured) {
        this.textured = textured;
        Tessellator.getInstance().getBuffer().begin(mode,
                textured ? DefaultVertexFormats.POSITION_TEX_COLOR : DefaultVertexFormats.POSITION_COLOR);
    }

    @Override
    public void vertex(double x, double y, double z, double u, double v, int r, int g, int b, int a) {
        BufferBuilder bb = Tessellator.getInstance().getBuffer();
        bb.pos(x, y, z);
        if (textured) {
            bb.tex(u, v);
        }
        bb.color(r, g, b, a).endVertex();
    }

    @Override
    public void end() {
        Tessellator.getInstance().draw();
    }

    @Override
    public int displayHeight() {
        return Minecraft.getMinecraft().displayHeight;
    }

    @Override
    public int displayWidth() {
        return Minecraft.getMinecraft().displayWidth;
    }

    @Override
    public boolean supportsBlur() {
        return OpenGlHelper.isFramebufferEnabled();
    }
}
