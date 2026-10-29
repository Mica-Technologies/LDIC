package com.micatechnologies.minecraft.ldic.client.render;

import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.client.AssetResolver;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves a theme's {@code font} value to a font.
 *
 * <ul>
 *   <li>{@code minecraft} (the default): the vanilla font.</li>
 *   <li>{@code sans}, {@code serif}, {@code mono}: the system's fonts of that kind.</li>
 *   <li>A path to a {@code .ttf} / {@code .otf} file in the theme.</li>
 *   <li>A name from the theme's {@code fonts} table, which maps to any of the above.</li>
 * </ul>
 *
 * Any of them may end in {@code :bold}, {@code :italic} or {@code :bold_italic}. An unknown
 * font is logged once and falls back to the vanilla font.
 */
public final class Fonts {

    private final AssetResolver assets;
    private final Map<String, LdicFont> cache = new HashMap<>();
    private Map<String, String> aliases = new HashMap<>();
    private double pixelScale = 2;

    public Fonts(AssetResolver assets) {
        this.assets = assets;
    }

    public void setAliases(Map<String, String> aliases) {
        this.aliases = aliases;
    }

    /** Real pixels per layout unit this frame, so TTF glyphs rasterise at screen resolution. */
    public void setPixelScale(double pixelScale) {
        this.pixelScale = pixelScale;
        for (LdicFont f : cache.values()) {
            if (f instanceof TtfFont) {
                ((TtfFont) f).setPixelScale(pixelScale);
            }
        }
    }

    public LdicFont get(String spec) {
        if (spec == null || spec.isEmpty() || spec.equals("minecraft")) {
            return VanillaFont.INSTANCE;
        }
        LdicFont f = cache.get(spec);
        if (f == null) {
            f = load(spec, 0);
            cache.put(spec, f);
            if (f instanceof TtfFont) {
                ((TtfFont) f).setPixelScale(pixelScale);
            }
        }
        return f;
    }

    private LdicFont load(String spec, int depth) {
        int style = Font.PLAIN;
        String name = spec;
        String lower = spec.toLowerCase(Locale.ROOT);
        if (lower.endsWith(":bold_italic")) {
            style = Font.BOLD | Font.ITALIC;
            name = spec.substring(0, spec.length() - ":bold_italic".length());
        } else if (lower.endsWith(":bold")) {
            style = Font.BOLD;
            name = spec.substring(0, spec.length() - ":bold".length());
        } else if (lower.endsWith(":italic")) {
            style = Font.ITALIC;
            name = spec.substring(0, spec.length() - ":italic".length());
        }
        String alias = aliases.get(name);
        if (alias != null && depth < 4) {
            String target = style == Font.PLAIN || alias.contains(":b") || alias.contains(":i") ? alias : alias + spec.substring(name.length());
            return load(target, depth + 1);
        }
        switch (name.toLowerCase(Locale.ROOT)) {
            case "minecraft":
                return VanillaFont.INSTANCE;
            case "sans":
                return new TtfFont(new Font(Font.SANS_SERIF, style, 16));
            case "serif":
                return new TtfFont(new Font(Font.SERIF, style, 16));
            case "mono":
                return new TtfFont(new Font(Font.MONOSPACED, style, 16));
            default:
                break;
        }
        String lname = name.toLowerCase(Locale.ROOT);
        if (lname.endsWith(".ttf") || lname.endsWith(".otf")) {
            try (InputStream in = assets.open(name)) {
                Font f = Font.createFont(Font.TRUETYPE_FONT, in);
                return new TtfFont(style == Font.PLAIN ? f : f.deriveFont(style));
            } catch (IOException | FontFormatException | RuntimeException e) {
                Ldic.LOGGER.error("LDIC could not load font '{}': {}; using the Minecraft font", name, e.toString());
                return VanillaFont.INSTANCE;
            }
        }
        Ldic.LOGGER.error("LDIC: unknown font '{}' (use minecraft, sans, serif, mono, a .ttf/.otf file or a theme font name); using the Minecraft font", spec);
        return VanillaFont.INSTANCE;
    }

    public void clear() {
        for (LdicFont f : cache.values()) {
            if (f instanceof TtfFont) {
                ((TtfFont) f).delete();
            }
        }
        cache.clear();
    }
}
