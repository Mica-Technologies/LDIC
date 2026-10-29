package com.micatechnologies.minecraft.ldic;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main mod class for LDIC (Limitless Development: Immersive Client).
 *
 * <p>LDIC owns what a player sees before they reach a world: the loading screen shown while
 * Forge starts the game, and the title screen that follows it. Both are driven by themes, so a
 * modpack can ship its own look or start from one of the built-in ones.</p>
 *
 * <p><b>Client only.</b> {@code clientSideOnly} keeps FML from constructing this mod on a
 * dedicated server, and {@code acceptableRemoteVersions = "*"} lets a client with LDIC join a
 * server without it. The loading screen is reached through {@code LdicCoreMod}'s mixins, which
 * run before this class is constructed; it must never depend on anything set up here.</p>
 */
@Mod(modid = Tags.MODID,
     version = Tags.VERSION,
     name = Tags.MODNAME,
     acceptedMinecraftVersions = "[1.12.2]",
     clientSideOnly = true,
     acceptableRemoteVersions = "*")
public class Ldic {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MODID);

    @Mod.Instance(Tags.MODID)
    public static Ldic instance;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("{} {} loaded", Tags.MODNAME, Tags.VERSION);
    }
}
