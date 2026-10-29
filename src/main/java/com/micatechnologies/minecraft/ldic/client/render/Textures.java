package com.micatechnologies.minecraft.ldic.client.render;

import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.client.AssetResolver;
import com.micatechnologies.minecraft.ldic.client.WebCache;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

/**
 * LDIC's own texture cache. It decodes with ImageIO and uploads with plain GL, never through
 * vanilla's {@code TextureUtil} (whose shared upload buffer is not safe to touch from another
 * thread, which the loading screen will need).
 *
 * <p>Files and resource-pack assets load on first use. Web assets download and decode on a
 * background thread; {@link #get} returns null until {@link #tick} has uploaded them. A missing
 * or broken asset is logged once and drawn as a magenta/black checker, so it is obvious on
 * screen rather than silently blank.</p>
 */
public final class Textures {

    public static final class Texture {
        public final int id;
        public final int width;
        public final int height;
        public final boolean missing;

        Texture(int id, int width, int height, boolean missing) {
            this.id = id;
            this.width = width;
            this.height = height;
            this.missing = missing;
        }

        public void bind() {
            Gfx.gl().bindTexture(id);
        }
    }

    private static final class Decoded {
        final String key;
        final BufferedImage image;
        final boolean linear;

        Decoded(String key, BufferedImage image, boolean linear) {
            this.key = key;
            this.image = image;
            this.linear = linear;
        }
    }

    private final AssetResolver assets;
    private final WebCache web;
    private final Map<String, Texture> loaded = new HashMap<>();
    private final Set<String> pending = new HashSet<>();
    private final Set<String> reported = new HashSet<>();
    private final ConcurrentLinkedQueue<Decoded> ready = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> failed = new ConcurrentLinkedQueue<>();
    private Texture missing;

    public Textures(AssetResolver assets, WebCache web) {
        this.assets = assets;
        this.web = web;
    }

    /** The texture for {@code path}, or null while a web download is still in flight. */
    public Texture get(String path, boolean linear) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        String key = (linear ? "L|" : "N|") + path;
        Texture t = loaded.get(key);
        if (t != null) {
            return t;
        }
        if (AssetResolver.isWeb(path)) {
            if (pending.add(key)) {
                web.pool().submit(() -> {
                    try {
                        BufferedImage img = decode(new ByteArrayInputStream(web.fetch(path)), path);
                        ready.add(new Decoded(key, img, linear));
                    } catch (IOException | RuntimeException e) {
                        report(path, e);
                        failed.add(key);
                    }
                });
            }
            return null;
        }
        try (InputStream in = assets.open(path)) {
            t = upload(decode(in, path), linear, false);
        } catch (IOException | RuntimeException e) {
            report(path, e);
            t = missing();
        }
        loaded.put(key, t);
        return t;
    }

    /** Uploads finished background decodes. Call once a frame on the render thread. */
    public void tick() {
        Decoded d;
        while ((d = ready.poll()) != null) {
            pending.remove(d.key);
            loaded.put(d.key, upload(d.image, d.linear, false));
        }
        String key;
        while ((key = failed.poll()) != null) {
            pending.remove(key);
            loaded.put(key, missing());
        }
    }

    /** Frees every texture; the next {@link #get} reloads. Call after a theme or resource reload. */
    public void clear() {
        for (Texture t : loaded.values()) {
            if (t != missing) {
                Gfx.gl().deleteTexture(t.id);
            }
        }
        loaded.clear();
        reported.clear();
    }

    private synchronized void report(String path, Exception e) {
        if (reported.add(path)) {
            Ldic.LOGGER.error("LDIC could not load image '{}': {}", path, e.toString());
        }
    }

    private static BufferedImage decode(InputStream in, String path) throws IOException {
        BufferedImage img = ImageIO.read(in);
        if (img == null) {
            throw new IOException("not a supported image format: " + path);
        }
        return img;
    }

    private Texture missing() {
        if (missing == null) {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    img.setRGB(x, y, ((x / 8 + y / 8) % 2 == 0) ? 0xFFF800F8 : 0xFF000000);
                }
            }
            missing = upload(img, false, true);
        }
        return missing;
    }

    static Texture upload(BufferedImage img, boolean linear, boolean isMissing) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[] argb = img.getRGB(0, 0, w, h, null, 0, w);
        IntBuffer buf = BufferUtils.createIntBuffer(argb.length);
        buf.put(argb).flip();
        int id = Gfx.gl().genTexture();
        Gfx.gl().bindTexture(id);
        boolean mipmaps = linear && GLContext.getCapabilities().OpenGL30;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
                linear ? (mipmaps ? GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_LINEAR) : GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, linear ? GL11.GL_LINEAR : GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buf);
        if (mipmaps) {
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        }
        return new Texture(id, w, h, isMissing);
    }
}
