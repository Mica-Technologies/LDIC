package com.micatechnologies.minecraft.ldic.client.render;

import com.micatechnologies.minecraft.ldic.Ldic;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/**
 * Saves the frame being drawn to {@code screenshots/<name>.png}. For debug mode on screens
 * nothing else can screenshot: the loading screen, and world loading, where the game thread is
 * busy in vanilla's own loop. Call it before the frame is shown; the file is written in the
 * background.
 */
public final class DebugCapture {

    private DebugCapture() {
    }

    /** Encoding a large PNG takes hundreds of milliseconds: never on the thread that draws. */
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "LDIC debug screenshots");
        t.setDaemon(true);
        return t;
    });

    public static void save(String name) {
        int w = Display.getWidth();
        int h = Display.getHeight();
        ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
        GL11.glReadPixels(0, 0, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
        WRITER.execute(() -> write(name, buf, w, h));
    }

    private static void write(String name, ByteBuffer buf, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = ((h - 1 - y) * w + x) * 4;
                img.setRGB(x, y, ((buf.get(i) & 0xFF) << 16) | ((buf.get(i + 1) & 0xFF) << 8) | (buf.get(i + 2) & 0xFF));
            }
        }
        File dir = new File(Minecraft.getMinecraft().gameDir, "screenshots");
        try {
            if (dir.isDirectory() || dir.mkdirs()) {
                ImageIO.write(img, "png", new File(dir, name + ".png"));
            }
        } catch (IOException e) {
            Ldic.LOGGER.warn("LDIC: could not save a debug screenshot", e);
        }
    }
}
