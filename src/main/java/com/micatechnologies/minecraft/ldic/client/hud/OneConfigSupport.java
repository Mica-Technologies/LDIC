package com.micatechnologies.minecraft.ldic.client.hud;

import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.client.hud.elements.EffectsHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.HudStateTracker;
import com.micatechnologies.minecraft.ldic.client.hud.presets.HudPresets;
import net.minecraftforge.common.MinecraftForge;

/**
 * Starts LDIC's OneConfig page and HUD elements when OneConfig is installed. Nothing here names
 * a OneConfig type except {@link #start}'s call into {@link LdicOneConfig}, which only runs after
 * {@link #available} said yes, so LDIC loads (without HUDs) in a pack that has no OneConfig.
 */
public final class OneConfigSupport {

    private static Runnable refresh = () -> { };

    private OneConfigSupport() {
    }

    public static boolean available() {
        try {
            Class.forName("cc.polyfrost.oneconfig.config.Config", false, OneConfigSupport.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /** Call once at client pre-init. */
    public static void start() {
        if (!available()) {
            Ldic.LOGGER.info("LDIC: OneConfig is not installed, so the HUD elements and the OneConfig page are off");
            return;
        }
        try {
            Hooks.start();
            MinecraftForge.EVENT_BUS.register(new HudStateTracker());
        } catch (RuntimeException | LinkageError e) {
            Ldic.LOGGER.error("LDIC: could not start the OneConfig page and HUD elements", e);
        }
    }

    /** Shows hotbar and inventory changes made elsewhere (LDIC's Config screen, the full-screen toggle). */
    public static void refresh() {
        refresh.run();
    }

    /** After a menu theme load: the "Theme" HUD style takes the new theme's colours, nothing moves. */
    public static void themeChanged() {
        themeChanged.run();
    }

    private static Runnable themeChanged = () -> { };

    /** The only place that touches OneConfig types; loaded on first use. */
    private static final class Hooks {
        static void start() {
            LdicOneConfig config = new LdicOneConfig();
            refresh = config::pullFromSettings;
            MinecraftForge.EVENT_BUS.register(new EffectsHud.VanillaIcons(config.effectsHud));
            themeChanged = () -> {
                if (config.stylePresetIndex == HudPresets.THEME_STYLE) {
                    HudPresets.applyStyle(config.stylePresetIndex, config, false);
                }
            };
        }
    }
}
