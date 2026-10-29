package com.micatechnologies.minecraft.ldic.client.render;

/**
 * The GL calls LDIC's drawing code makes, behind an interface so the same code can run on two
 * threads with different rules.
 *
 * <ul>
 *   <li>{@link McGl}, on the main thread: goes through {@code GlStateManager} and the
 *       {@code Tessellator}, so vanilla's GL state cache stays truthful.</li>
 *   <li>{@link RawGl}, on the loading-screen thread: plain GL11 immediate mode. That thread has
 *       its own GL context, and touching {@code GlStateManager}'s static cache or the shared
 *       {@code Tessellator} from it would corrupt the main thread's state.</li>
 * </ul>
 *
 * {@link Gfx#gl()} returns the backend for the current thread.
 */
public interface GlBackend {

    void enableTexture();

    void disableTexture();

    void bindTexture(int id);

    int genTexture();

    void deleteTexture(int id);

    void enableBlend();

    void pushMatrix();

    void popMatrix();

    void translate(double x, double y, double z);

    void rotate(float angle, float x, float y, float z);

    void scale(double x, double y, double z);

    /** Switches to the projection (true) or modelview (false) matrix. */
    void matrixMode(boolean projection);

    void loadIdentity();

    void viewport(int x, int y, int width, int height);

    void depthMask(boolean on);

    void disableCull();

    void disableDepth();

    /** Starts a primitive of GL {@code mode}; vertices carry texture coords when {@code textured}. */
    void begin(int mode, boolean textured);

    void vertex(double x, double y, double z, double u, double v, int r, int g, int b, int a);

    void end();

    /** The window's height in pixels, for converting top-left coordinates to GL viewports. */
    int displayHeight();

    int displayWidth();

    /** Whether {@link PanoramaRenderer} may use a framebuffer for blur on this thread. */
    boolean supportsBlur();
}
