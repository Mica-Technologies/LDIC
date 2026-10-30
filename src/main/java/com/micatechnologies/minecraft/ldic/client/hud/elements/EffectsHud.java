package com.micatechnologies.minecraft.ldic.client.hud.elements;

import cc.polyfrost.oneconfig.config.annotations.Switch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Active effects as their icons, each in a ring that drains as the effect runs out (ambient and
 * endless effects keep a full ring), with the level in the corner when it is above I.
 *
 * <p>The client is told how long an effect has left, not how long it lasted, so the ring's full
 * length is the longest time this effect has been seen with; it resets when the effect is
 * renewed for longer.</p>
 */
public class EffectsHud extends VisualHud {

    private static final float CELL = 21;

    @Switch(name = "Show Level")
    public boolean showLevel = true;

    @Switch(name = "Hide Vanilla Effect Icons", description = "Vanilla's own effect icons in the top-right corner, while this HUD is on.")
    public boolean hideVanillaIcons = true;

    /** Hides vanilla's effect icons while an effects HUD that asks for it is on. */
    public static final class VanillaIcons {
        private final EffectsHud hud;

        public VanillaIcons(EffectsHud hud) {
            this.hud = hud;
        }

        @SubscribeEvent
        public void onOverlay(RenderGameOverlayEvent.Pre event) {
            if (event.getType() == RenderGameOverlayEvent.ElementType.POTION_ICONS && hud.isEnabled() && hud.hideVanillaIcons) {
                event.setCanceled(true);
            }
        }
    }

    private final transient Map<Potion, Integer> longest = new HashMap<>();

    public EffectsHud() {
        super(1780f, 40f);
    }

    private List<PotionEffect> effects(Minecraft mc, boolean example) {
        List<PotionEffect> out = new ArrayList<>();
        if (example) {
            out.add(new PotionEffect(MobEffects.SPEED, 600, 1));
            out.add(new PotionEffect(MobEffects.NIGHT_VISION, 3600, 0));
            return out;
        }
        for (PotionEffect e : mc.player.getActivePotionEffects()) {
            if (e.getPotion().shouldRenderHUD(e)) {
                out.add(e);
            }
        }
        out.sort((a, b) -> Integer.compare(b.getDuration(), a.getDuration()));
        return out;
    }

    @Override
    protected float width(boolean example) {
        Minecraft mc = Minecraft.getMinecraft();
        int n = mc.player == null && !example ? 0 : effects(mc, example || mc.player == null).size();
        return Math.max(1, n * CELL - 1);
    }

    @Override
    protected float height(boolean example) {
        return 20;
    }

    @Override
    protected void render(Minecraft mc, boolean example) {
        FontRenderer font = mc.fontRenderer;
        int accent = accent();
        float x = 0;
        for (PotionEffect e : effects(mc, example)) {
            Potion potion = e.getPotion();
            float fraction;
            if (example) {
                fraction = e.getPotion() == MobEffects.SPEED ? 0.35f : 0.8f;
            } else if (e.getIsPotionDurationMax() || e.getIsAmbient()) {
                fraction = 1;
            } else {
                int seen = Math.max(e.getDuration(), longest.getOrDefault(potion, 0));
                longest.put(potion, seen);
                fraction = e.getDuration() / (float) seen;
            }
            ring(x + 10, 10, 10, 8.6f, 1, fade(text(), 0.2f));
            ring(x + 10, 10, 10, 8.6f, fraction, fraction < 0.15f ? 0xFFE57373 : accent);
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.enableBlend();
            if (potion.hasStatusIcon()) {
                mc.getTextureManager().bindTexture(GuiContainer.INVENTORY_BACKGROUND);
                int i = potion.getStatusIconIndex();
                GlStateManager.pushMatrix();
                GlStateManager.translate(x + 3, 3, 0);
                GlStateManager.scale(14 / 18f, 14 / 18f, 1);
                mc.ingameGUI.drawTexturedModalRect(0, 0, i % 8 * 18, 198 + i / 8 * 18, 18, 18);
                GlStateManager.popMatrix();
            } else {
                potion.renderHUDEffect((int) x + 2, 2, e, mc, 1);
            }
            if (showLevel && e.getAmplifier() > 0) {
                String lvl = I18n.format("enchantment.level." + (e.getAmplifier() + 1));
                text(font, lvl, x + 20 - textWidth(font, lvl, 5.5f), 14.5f, 5.5f, text(), true);
            }
            x += CELL;
        }
        if (!example) {
            longest.keySet().removeIf(p -> mc.player.getActivePotionEffect(p) == null);
        }
    }

    /** An arc ring clockwise from the top, {@code fraction} of the way round. */
    private static void ring(float cx, float cy, float outer, float inner, float fraction, int colour) {
        if (fraction <= 0) {
            return;
        }
        int segments = Math.max(2, Math.round(48 * Math.min(1, fraction)));
        float a = ((colour >>> 24) & 0xFF) / 255f;
        float r = ((colour >> 16) & 0xFF) / 255f;
        float g = ((colour >> 8) & 0xFF) / 255f;
        float b = (colour & 0xFF) / 255f;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        Tessellator t = Tessellator.getInstance();
        BufferBuilder buf = t.getBuffer();
        buf.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double ang = -Math.PI / 2 + 2 * Math.PI * Math.min(1, fraction) * i / segments;
            double cos = Math.cos(ang);
            double sin = Math.sin(ang);
            buf.pos(cx + cos * outer, cy + sin * outer, 0).color(r, g, b, a).endVertex();
            buf.pos(cx + cos * inner, cy + sin * inner, 0).color(r, g, b, a).endVertex();
        }
        t.draw();
        GlStateManager.enableTexture2D();
    }
}
