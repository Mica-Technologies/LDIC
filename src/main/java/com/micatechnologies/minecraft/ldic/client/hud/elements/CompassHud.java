package com.micatechnologies.minecraft.ldic.client.hud.elements;

import cc.polyfrost.oneconfig.config.annotations.Switch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

/**
 * A compass tape: a strip of ticks and N / NE / E ... labels that slides as the player turns,
 * with the heading under a marker in the middle. Replaces "Facing: North" and a yaw readout in
 * one thin line.
 */
public class CompassHud extends VisualHud {

    private static final String[] NAMES = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
    /** Degrees either side of the heading shown on the tape. */
    private static final float SPAN = 70;
    private static final float W = 140;

    @Switch(name = "Show Degrees")
    public boolean showDegrees = true;

    public CompassHud() {
        super(900f, 4f);
    }

    @Override
    protected float width(boolean example) {
        return W;
    }

    @Override
    protected float height(boolean example) {
        return showDegrees ? 22 : 16;
    }

    /** Compass heading: 0 north, 90 east (Minecraft's yaw is 0 south, 90 west). */
    static float heading(float yaw) {
        float h = (yaw - 180) % 360;
        return h < 0 ? h + 360 : h;
    }

    @Override
    protected void render(Minecraft mc, boolean example) {
        FontRenderer font = mc.fontRenderer;
        float h = example ? 352 : heading(mc.player.rotationYaw);
        int text = text();
        int accent = accent();
        float first = (float) Math.floor((h - SPAN) / 15) * 15;
        for (float d = first; d <= h + SPAN; d += 15) {
            float dx = d - h;
            if (Math.abs(dx) > SPAN) {
                continue;
            }
            float x = W / 2 + dx * (W / 2) / SPAN;
            float a = 1 - Math.abs(dx) / SPAN;
            int deg = Math.round(((d % 360) + 360) % 360);
            boolean cardinal = deg % 90 == 0;
            boolean inter = deg % 45 == 0;
            float top = cardinal ? 10 : inter ? 11 : 13;
            rect(x - 0.5f, top, x + 0.5f, 15, fade(text, a * (cardinal ? 1 : 0.7f)));
            if (inter) {
                String name = NAMES[(deg / 45) % 8];
                float size = cardinal ? 8 : 6;
                int colour = deg == 0 ? accent : text;
                text(font, name, x - textWidth(font, name, size) / 2, cardinal ? 1 : 3, size, fade(colour, a), false);
            }
        }
        // The marker: a small accent wedge pointing at the tape.
        rect(W / 2 - 0.5f, 9, W / 2 + 0.5f, 16, accent);
        rect(W / 2 - 1.5f, 15, W / 2 + 1.5f, 16, accent);
        if (showDegrees) {
            String deg = Math.round(h) % 360 + "°";
            text(font, deg, W / 2 - textWidth(font, deg, 6) / 2, 17, 6, text, false);
        }
    }
}
