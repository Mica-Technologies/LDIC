package com.micatechnologies.minecraft.ldic.client.menu;

import com.google.common.util.concurrent.Runnables;
import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.expr.Type;
import com.micatechnologies.minecraft.ldic.theme.ActionDef;
import java.io.File;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiCustomizeSkin;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.GuiScreenOptionsSounds;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.gui.GuiShareToLan;
import net.minecraft.client.gui.GuiSnooper;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.GuiWinGame;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.client.gui.advancements.GuiScreenAdvancements;
import net.minecraft.client.gui.ScreenChatOptions;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.realms.RealmsBridge;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.storage.WorldSummary;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.GuiModList;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

/** Runs theme actions. Problems at run time (an unknown screen, a missing world) are logged, never thrown. */
final class Actions {

    private Actions() {
    }

    static void run(ActionDef a, ThemeScreenDriver driver) {
        if (a == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen screen = driver.gui();
        MenuManager manager = driver.manager();
        try {
            switch (a.type) {
                case "chain":
                    for (ActionDef sub : a.chain) {
                        run(sub, driver);
                    }
                    break;
                case "open_link":
                    driver.openLink(a.str("url", ""), a.flag("confirm", true));
                    break;
                case "option": {
                    String name = a.str("name", "");
                    if (!GameOptions.exists(name)) {
                        Ldic.LOGGER.warn("LDIC: '{}' is not a game option ({})", name, a.path);
                        break;
                    }
                    GameOptions.cycle(name);
                    driver.markOptionsChanged();
                    if (name.equalsIgnoreCase("GUI_SCALE")) {
                        // As vanilla's video settings do: lay the screen out again at the new scale.
                        ScaledResolution sr = new ScaledResolution(mc);
                        screen.setWorldAndResolution(mc, sr.getScaledWidth(), sr.getScaledHeight());
                    }
                    break;
                }
                case "difficulty":
                    GameOptions.cycleDifficulty();
                    break;
                case "classic_options":
                    manager.openClassicOptions(driver.host() instanceof LdicOptionsScreen ? ((LdicOptionsScreen) driver.host()).parent() : screen);
                    break;
                case "classic_pause":
                    if (mc.world != null) {
                        manager.openClassicPause();
                    }
                    break;
                case "resume":
                    mc.displayGuiScreen(null);
                    mc.setIngameFocus();
                    break;
                case "disconnect":
                    disconnect();
                    break;
                case "connect": {
                    String address = a.str("address", "");
                    ServerData data = new ServerData(a.str("name", address), address, false);
                    FMLClientHandler.instance().connectToServer(screen, data);
                    break;
                }
                case "load_world":
                    loadWorld(a.str("folder", ""), screen);
                    break;
                case "open":
                    open(a.str("screen", ""), screen, manager, a);
                    break;
                case "back":
                    driver.host().back();
                    break;
                case "quit":
                    mc.shutdown();
                    break;
                case "reload":
                    manager.reload();
                    break;
                case "open_folder": {
                    File dir = new File(mc.gameDir, a.str("path", ""));
                    if (!dir.getCanonicalPath().startsWith(mc.gameDir.getCanonicalPath())) {
                        Ldic.LOGGER.warn("LDIC: refusing to open a folder outside the game directory: {}", dir);
                        break;
                    }
                    if (!dir.exists() && !dir.mkdirs()) {
                        Ldic.LOGGER.warn("LDIC: could not create {}", dir);
                    }
                    OpenGlHelper.openFile(dir);
                    break;
                }
                case "mod_config":
                    modConfig(a.str("modid", ""), screen);
                    break;
                case "sound":
                    playSound(a.str("sound", ""), (float) a.num("volume", 1), (float) a.num("pitch", 1));
                    break;
                case "set":
                    a.target.assign(a.value);
                    break;
                case "toggle":
                    if (a.target.type() == Type.BOOL) {
                        a.target.set(!a.target.getBool());
                    }
                    break;
                default:
                    Ldic.LOGGER.warn("LDIC: action '{}' is not implemented ({})", a.type, a.path);
                    break;
            }
        } catch (Exception e) {
            Ldic.LOGGER.error("LDIC: action {} failed", a, e);
        }
    }

    private static void open(String id, GuiScreen screen, MenuManager manager, ActionDef a) {
        Minecraft mc = Minecraft.getMinecraft();
        // "options" always goes through vanilla's options screen, even when the theme has an "options"
        // screen: MenuManager swaps it for the themed one there, as a GuiOptions subclass, so other
        // mods still add their settings buttons to it.
        if (!id.equalsIgnoreCase("options") && manager.hasScreen(id)) {
            manager.open(id);
            return;
        }
        GuiScreen next;
        switch (id.toLowerCase(Locale.ROOT)) {
            case "singleplayer":
                next = new GuiWorldSelection(screen);
                break;
            case "create_world":
                next = new GuiCreateWorld(screen);
                break;
            case "multiplayer":
                next = new GuiMultiplayer(screen);
                break;
            case "options":
                next = new GuiOptions(screen, mc.gameSettings);
                break;
            case "language":
            case "languages":
                next = new GuiLanguage(screen, mc.gameSettings, mc.getLanguageManager());
                break;
            case "mods":
                next = new GuiModList(screen);
                break;
            case "resource_packs":
            case "resourcepacks":
                next = new GuiScreenResourcePacks(screen);
                break;
            case "video":
                next = new GuiVideoSettings(screen, mc.gameSettings);
                break;
            case "controls":
                next = new GuiControls(screen, mc.gameSettings);
                break;
            case "sounds":
                next = new GuiScreenOptionsSounds(screen, mc.gameSettings);
                break;
            case "skin":
                next = new GuiCustomizeSkin(screen);
                break;
            case "chat":
                next = new ScreenChatOptions(screen, mc.gameSettings);
                break;
            case "snooper":
                next = new GuiSnooper(screen, mc.gameSettings);
                break;
            case "credits":
                next = new GuiWinGame(false, Runnables.doNothing());
                break;
            case "realms":
                new RealmsBridge().switchToRealms(screen);
                return;
            case "advancements":
                if (mc.player == null) {
                    return;
                }
                next = new GuiScreenAdvancements(mc.player.connection.getAdvancementManager());
                break;
            case "statistics":
            case "stats":
                if (mc.player == null) {
                    return;
                }
                next = new GuiStats(screen, mc.player.getStatFileWriter());
                break;
            case "lan":
                if (!mc.isSingleplayer()) {
                    return;
                }
                next = new GuiShareToLan(screen);
                break;
            case "mod_options":
                if (mc.world != null) {
                    FMLClientHandler.instance().showInGameModOptions(screen instanceof GuiIngameMenu ? (GuiIngameMenu) screen : new GuiIngameMenu());
                } else {
                    next = new GuiModList(screen);
                    break;
                }
                return;
            default:
                Ldic.LOGGER.warn("LDIC: '{}' is neither a screen in this theme nor a built-in screen ({})", id, a.path);
                return;
        }
        mc.displayGuiScreen(next);
    }

    /**
     * Leaves the world the way vanilla's "Save and Quit to Title" / "Disconnect" does: to the
     * title screen from singleplayer, to Realms from a Realms server, else to the server list.
     */
    private static void disconnect() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            // Already leaving: a second click must not disconnect twice.
            return;
        }
        boolean integrated = mc.isIntegratedServerRunning();
        boolean realms = mc.isConnectedToRealms();
        mc.world.sendQuittingDisconnectingPacket();
        mc.loadWorld(null);
        if (integrated) {
            mc.displayGuiScreen(new GuiMainMenu());
        } else if (realms) {
            new RealmsBridge().switchToRealms(new GuiMainMenu());
        } else {
            mc.displayGuiScreen(new GuiMultiplayer(new GuiMainMenu()));
        }
    }

    private static void loadWorld(String folder, GuiScreen screen) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        for (WorldSummary s : mc.getSaveLoader().getSaveList()) {
            if (s.getFileName().equals(folder)) {
                FMLClientHandler.instance().tryLoadExistingWorld(new GuiWorldSelection(screen), s);
                return;
            }
        }
        Ldic.LOGGER.warn("LDIC: no world folder '{}' in saves", folder);
        mc.displayGuiScreen(new GuiWorldSelection(screen));
    }

    private static void modConfig(String modid, GuiScreen screen) {
        ModContainer mod = Loader.instance().getIndexedModList().get(modid);
        if (mod == null) {
            Ldic.LOGGER.warn("LDIC: mod_config: no mod '{}' is loaded", modid);
            return;
        }
        IModGuiFactory factory = FMLClientHandler.instance().getGuiFactoryFor(mod);
        if (factory == null || !factory.hasConfigGui()) {
            Ldic.LOGGER.warn("LDIC: mod_config: '{}' has no config screen", modid);
            return;
        }
        Minecraft.getMinecraft().displayGuiScreen(factory.createConfigGui(screen));
    }

    /** Plays a sound event by name ({@code minecraft:ui.button.click}) on the master channel. */
    static void playSound(String name, float volume, float pitch) {
        if (name == null || name.isEmpty()) {
            return;
        }
        Minecraft.getMinecraft().getSoundHandler().playSound(new PositionedSoundRecord(new ResourceLocation(name),
                SoundCategory.MASTER, volume, pitch, false, 0, ISound.AttenuationType.NONE, 0, 0, 0));
    }
}
