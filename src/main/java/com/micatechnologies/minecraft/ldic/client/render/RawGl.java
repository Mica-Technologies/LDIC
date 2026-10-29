package com.micatechnologies.minecraft.ldic.client.render;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/**
 * Loading-screen backend: plain GL11 immediate mode, for the thread that owns the window's GL
 * context while Forge loads mods on another. Nothing here touches vanilla's state cache.
 */
public final class RawGl implements GlBackend {

    public static final RawGl INSTANCE = new RawGl();

    private RawGl() {
    }

    @Override
    public void enableTexture() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    @Override
    public void disableTexture() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    @Override
    public void bindTexture(int id) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
    }

    @Override
    public int genTexture() {
        return GL11.glGenTextures();
    }

    @Override
    public void deleteTexture(int id) {
        GL11.glDeleteTextures(id);
    }

    @Override
    public void enableBlend() {
        GL11.glEnable(GL11.GL_BLEND);
    }

    @Override
    public void pushMatrix() {
        GL11.glPushMatrix();
    }

    @Override
    public void popMatrix() {
        GL11.glPopMatrix();
    }

    @Override
    public void translate(double x, double y, double z) {
        GL11.glTranslated(x, y, z);
    }

    @Override
    public void rotate(float angle, float x, float y, float z) {
        GL11.glRotatef(angle, x, y, z);
    }

    @Override
    public void scale(double x, double y, double z) {
        GL11.glScaled(x, y, z);
    }

    @Override
    public void matrixMode(boolean projection) {
        GL11.glMatrixMode(projection ? GL11.GL_PROJECTION : GL11.GL_MODELVIEW);
    }

    @Override
    public void loadIdentity() {
        GL11.glLoadIdentity();
    }

    @Override
    public void viewport(int x, int y, int width, int height) {
        GL11.glViewport(x, y, width, height);
    }

    @Override
    public void depthMask(boolean on) {
        GL11.glDepthMask(on);
    }

    @Override
    public void disableCull() {
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    @Override
    public void disableDepth() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    private boolean textured;

    @Override
    public void begin(int mode, boolean textured) {
        this.textured = textured;
        GL11.glBegin(mode);
    }

    @Override
    public void vertex(double x, double y, double z, double u, double v, int r, int g, int b, int a) {
        GL11.glColor4ub((byte) r, (byte) g, (byte) b, (byte) a);
        if (textured) {
            GL11.glTexCoord2d(u, v);
        }
        GL11.glVertex3d(x, y, z);
    }

    @Override
    public void end() {
        GL11.glEnd();
    }

    @Override
    public int displayHeight() {
        return Display.getHeight();
    }

    @Override
    public int displayWidth() {
        return Display.getWidth();
    }

    @Override
    public boolean supportsBlur() {
        return false;
    }
}
