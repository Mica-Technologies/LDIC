package com.micatechnologies.minecraft.ldic.client;

import com.micatechnologies.minecraft.ldic.theme.ThemeLoader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

/**
 * Turns a theme asset path into bytes.
 *
 * <ul>
 *   <li>{@code theme://builtin/<id>/<path>}: a file inside the LDIC jar.</li>
 *   <li>{@code theme://config/<path>}: a file under {@code config/ldic/}; paths that would
 *       escape that folder are refused.</li>
 *   <li>{@code namespace:path}: a resource-pack resource, e.g. {@code minecraft:textures/gui/widgets.png}.</li>
 *   <li>{@code http://} / {@code https://}: see {@link WebCache}; {@link #isWeb} tells callers to go there.</li>
 * </ul>
 */
public final class AssetResolver {

    private final File configDir;
    private final boolean early;

    public AssetResolver(File configDir) {
        this(configDir, false);
    }

    /**
     * @param early true on the loading screen: the resource manager is not ready (and not
     *              thread-safe), so {@code namespace:path} reads straight from the game's
     *              built-in resources, without resource packs
     */
    public AssetResolver(File configDir, boolean early) {
        this.configDir = configDir;
        this.early = early;
    }

    public static boolean isWeb(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    /** Opens a non-web asset. The caller closes the stream. */
    public InputStream open(String path) throws IOException {
        if (path.startsWith(ThemeLoader.THEME_SCHEME)) {
            String rest = path.substring(ThemeLoader.THEME_SCHEME.length());
            if (rest.startsWith("builtin/")) {
                String inJar = "/assets/ldic/themes/" + rest.substring("builtin/".length());
                if (inJar.contains("..")) {
                    throw new FileNotFoundException("refusing path outside the theme: " + path);
                }
                InputStream in = AssetResolver.class.getResourceAsStream(inJar);
                if (in == null) {
                    throw new FileNotFoundException("not in the LDIC jar: " + inJar);
                }
                return in;
            }
            if (rest.startsWith("config/")) {
                File file = new File(configDir, rest.substring("config/".length()));
                String root = configDir.getCanonicalPath() + File.separator;
                if (!file.getCanonicalPath().startsWith(root)) {
                    throw new FileNotFoundException("refusing path outside config/ldic: " + path);
                }
                return new FileInputStream(file);
            }
            throw new FileNotFoundException("unknown theme location: " + path);
        }
        if (isWeb(path)) {
            throw new FileNotFoundException("web assets load through WebCache: " + path);
        }
        ResourceLocation rl = new ResourceLocation(path);
        if (early) {
            return Minecraft.getMinecraft().defaultResourcePack.getInputStream(rl);
        }
        return Minecraft.getMinecraft().getResourceManager().getResource(rl).getInputStream();
    }
}
