package com.micatechnologies.minecraft.ldic.client.render;

import java.util.ArrayList;
import java.util.List;

/**
 * A font that draws at any size. {@code size} is the line height in layout units (the vanilla
 * font is 9). Text may contain {@code §} formatting codes.
 */
public interface LdicFont {

    double width(String text, double size);

    void draw(String text, double x, double y, double size, long argb, boolean shadow, double opacity);

    /** Splits on newlines and wraps each line to {@code maxWidth} at spaces. */
    default List<String> wrap(String text, double maxWidth, double size) {
        List<String> out = new ArrayList<>();
        for (String para : text.split("\n", -1)) {
            if (maxWidth <= 0 || width(para, size) <= maxWidth) {
                out.add(para);
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (String word : para.split(" ")) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (line.length() > 0 && width(candidate, size) > maxWidth) {
                    out.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                } else {
                    line.setLength(0);
                    line.append(candidate);
                }
            }
            out.add(line.toString());
        }
        return out;
    }

    /** The vanilla shadow colour: a quarter of the brightness, same alpha. */
    static long shadowOf(long argb) {
        return (argb & 0xFF000000L) | ((argb & 0xFCFCFCL) >> 2);
    }
}
