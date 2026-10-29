package com.micatechnologies.minecraft.ldic.client;

import com.micatechnologies.minecraft.ldic.client.menu.MenuManager;
import java.io.File;
import net.minecraftforge.common.MinecraftForge;

/**
 * Client start-up. Only {@code Ldic.preInit} on the client side reaches this class, so nothing
 * here is ever loaded on a dedicated server.
 */
public final class LdicClient {

    private static MenuManager menu;

    private LdicClient() {
    }

    public static void preInit(File modConfigDir) {
        File dir = new File(modConfigDir, "ldic");
        LdicSettings settings = LdicSettings.load(ensureDir(dir));
        menu = new MenuManager(dir, settings);
        MinecraftForge.EVENT_BUS.register(menu);
    }

    public static MenuManager menu() {
        return menu;
    }

    private static File ensureDir(File dir) {
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        return dir;
    }
}
