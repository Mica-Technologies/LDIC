package com.micatechnologies.minecraft.ldic.client.hud.elements;

import cc.polyfrost.oneconfig.config.annotations.Switch;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Health, food, armour and experience as slim bars with their vanilla icons and a number: four
 * readable lines in the height of two text HUDs.
 */
public class VitalsHud extends VisualHud {

    private static final float ROW = 10;
    private static final float BAR_X = 12;
    private static final float BAR_W = 60;
    private static final float W = 92;

    @Switch(name = "Health")
    public boolean showHealth = true;

    @Switch(name = "Food")
    public boolean showFood = true;

    @Switch(name = "Armour")
    public boolean showArmour = true;

    @Switch(name = "Experience")
    public boolean showXp = true;

    @Switch(name = "Hide Armour When None Worn")
    public boolean hideEmptyArmour = true;

    public VitalsHud() {
        super(4f, 4f);
    }

    /** One bar: an icon from icons.png (u, v), fill 0..1, an extra overlay fill, colour and label. */
    private static final class Row {
        final int u;
        final int v;
        final float fill;
        final float extra;
        final int colour;
        final String label;

        Row(int u, int v, float fill, float extra, int colour, String label) {
            this.u = u;
            this.v = v;
            this.fill = fill;
            this.extra = extra;
            this.colour = colour;
            this.label = label;
        }
    }

    private List<Row> rows(Minecraft mc, boolean example) {
        List<Row> rows = new ArrayList<>();
        EntityPlayer p = mc.player;
        float health = example ? 18 : p.getHealth();
        float max = example ? 20 : Math.max(1, p.getMaxHealth());
        float absorb = example ? 0 : p.getAbsorptionAmount();
        int food = example ? 14 : p.getFoodStats().getFoodLevel();
        int armour = example ? 8 : p.getTotalArmorValue();
        float xp = example ? 0.4f : p.experience;
        int level = example ? 12 : p.experienceLevel;
        if (showHealth) {
            rows.add(new Row(52, 0, health / max, absorb / max, 0xFFE53935,
                    Math.round(health) + (absorb > 0 ? "+" + Math.round(absorb) : "")));
        }
        if (showFood) {
            rows.add(new Row(52, 27, food / 20f, 0, 0xFFD9822B, String.valueOf(food)));
        }
        if (showArmour && !(hideEmptyArmour && armour == 0)) {
            rows.add(new Row(34, 9, armour / 20f, 0, 0xFFA8BCCB, String.valueOf(armour)));
        }
        if (showXp) {
            rows.add(new Row(-1, -1, xp, 0, 0xFF8BC34A, "L" + level));
        }
        return rows;
    }

    @Override
    protected float width(boolean example) {
        return W;
    }

    @Override
    protected float height(boolean example) {
        Minecraft mc = Minecraft.getMinecraft();
        int n = mc.player == null && !example ? 0 : rows(mc, example || mc.player == null).size();
        return Math.max(1, n * ROW - 1);
    }

    @Override
    protected void render(Minecraft mc, boolean example) {
        FontRenderer font = mc.fontRenderer;
        int text = text();
        float y = 0;
        for (Row r : rows(mc, example)) {
            if (r.u >= 0) {
                mc.getTextureManager().bindTexture(Gui.ICONS);
                GlStateManager.color(1, 1, 1, 1);
                GlStateManager.enableBlend();
                mc.ingameGUI.drawTexturedModalRect(0, (int) y, 16, r.v, 9, 9);
                mc.ingameGUI.drawTexturedModalRect(0, (int) y, r.u, r.v, 9, 9);
            } else {
                // Experience has no icon in icons.png small enough; a green orb dot instead.
                rect(2, y + 2, 7, y + 7, 0xFF8BC34A);
                rect(3, y + 3, 6, y + 6, 0xFFC5E1A5);
            }
            float by = y + 3;
            rect(BAR_X, by, BAR_X + BAR_W, by + 3, fade(text, 0.18f));
            rect(BAR_X, by, BAR_X + BAR_W * Math.min(1, r.fill), by + 3, r.colour);
            if (r.extra > 0) {
                rect(BAR_X, by, BAR_X + BAR_W * Math.min(1, r.extra), by + 1, 0xFFFFD54F);
            }
            text(font, r.label, BAR_X + BAR_W + 4, y + 1.5f, 6.5f, text, false);
            y += ROW;
        }
    }
}
