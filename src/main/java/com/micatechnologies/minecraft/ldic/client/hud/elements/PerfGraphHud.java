package com.micatechnologies.minecraft.ldic.client.hud.elements;

import cc.polyfrost.oneconfig.config.annotations.Slider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.FrameTimer;

/**
 * FPS and frame time over a sparkline of the last frames: one bar per frame, green under 60 fps
 * (16.7 ms), yellow under 30 fps, red above. Spikes show at a glance where a number alone would
 * hide them.
 */
public class PerfGraphHud extends VisualHud {

    private static final float GRAPH_H = 12;

    @Slider(name = "Frames Shown", min = 30, max = 120, step = 10)
    public float frames = 60;

    public PerfGraphHud() {
        super(1780f, 4f);
    }

    @Override
    protected float width(boolean example) {
        return Math.max(60, Math.round(frames));
    }

    @Override
    protected float height(boolean example) {
        return 9 + GRAPH_H;
    }

    @Override
    protected void render(Minecraft mc, boolean example) {
        FontRenderer font = mc.fontRenderer;
        int n = Math.round(width(example));
        float[] ms = new float[n];
        if (example) {
            for (int i = 0; i < n; i++) {
                ms[i] = 8 + 3 * (float) Math.sin(i / 4.0) + (i % 23 == 0 ? 20 : 0);
            }
        } else {
            FrameTimer timer = mc.getFrameTimer();
            long[] frames = timer.getFrames();
            int idx = timer.getIndex();
            for (int i = 0; i < n; i++) {
                ms[n - 1 - i] = frames[((idx - 1 - i) % frames.length + frames.length) % frames.length] / 1e6f;
            }
        }
        float sum = 0;
        for (float v : ms) {
            sum += v;
        }
        float avg = sum / n;
        String label = (example ? 118 : Minecraft.getDebugFPS()) + " fps  " + String.format("%.1f", avg) + " ms";
        text(font, label, 0, 0, 6.5f, text(), false);
        float base = 9 + GRAPH_H;
        rect(0, 9, n, base, fade(text(), 0.08f));
        for (int i = 0; i < n; i++) {
            float h = Math.min(1, ms[i] / 50f) * GRAPH_H;
            int c = ms[i] < 16.7f ? 0xFF66BB6A : ms[i] < 33.4f ? 0xFFFFCA28 : 0xFFEF5350;
            rect(i, base - Math.max(0.5f, h), i + 1, base, c);
        }
        // The 60 fps line.
        float line = base - 16.7f / 50f * GRAPH_H;
        rect(0, line, n, line + 0.5f, fade(text(), 0.35f));
    }
}
