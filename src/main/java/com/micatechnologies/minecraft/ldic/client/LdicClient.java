package com.micatechnologies.minecraft.ldic.client;

import com.micatechnologies.minecraft.ldic.client.hud.HotbarHud;
import com.micatechnologies.minecraft.ldic.client.hud.OneConfigSupport;
import com.micatechnologies.minecraft.ldic.client.inventory.InventoryScreens;
import com.micatechnologies.minecraft.ldic.client.inventory.SurvivalSearch;
import com.micatechnologies.minecraft.ldic.client.menu.MenuManager;
import java.io.File;
import net.minecraftforge.common.MinecraftForge;

/**
 * Client start-up. Only {@code Ldic.preInit} on the client side reaches this class, so nothing
 * here is ever loaded on a dedicated server.
 */
public final class LdicClient {

    private static MenuManager menu;
    private static LdicSettings settings;
    private static File dir;

    private LdicClient() {
    }

    public static void preInit(File modConfigDir) {
        dir = new File(modConfigDir, "ldic");
        settings = LdicSettings.load(ensureDir(dir));
        menu = new MenuManager(dir, settings);
        MinecraftForge.EVENT_BUS.register(menu);
        MinecraftForge.EVENT_BUS.register(menu.worldLoading());
        MinecraftForge.EVENT_BUS.register(new HotbarHud(settings));
        MinecraftForge.EVENT_BUS.register(new InventoryScreens(settings));
        MinecraftForge.EVENT_BUS.register(new SurvivalSearch());
        OneConfigSupport.start();
    }

    public static MenuManager menu() {
        return menu;
    }

    public static LdicSettings settings() {
        return settings;
    }

    /** {@code config/ldic}. */
    public static File configDir() {
        return dir;
    }

    private static File ensureDir(File dir) {
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        return dir;
    }
}
