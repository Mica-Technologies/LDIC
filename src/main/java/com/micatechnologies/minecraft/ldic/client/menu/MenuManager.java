package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.client.AssetResolver;
import com.micatechnologies.minecraft.ldic.client.ClientThemeSource;
import com.micatechnologies.minecraft.ldic.client.HostBindings;
import com.micatechnologies.minecraft.ldic.client.LdicSettings;
import com.micatechnologies.minecraft.ldic.client.ServerStatus;
import com.micatechnologies.minecraft.ldic.client.WebCache;
import com.micatechnologies.minecraft.ldic.client.WebText;
import com.micatechnologies.minecraft.ldic.client.audio.MusicPlayer;
import com.micatechnologies.minecraft.ldic.client.render.Fonts;
import com.micatechnologies.minecraft.ldic.client.render.Textures;
import com.micatechnologies.minecraft.ldic.theme.Problems;
import com.micatechnologies.minecraft.ldic.theme.ThemeCompiler;
import com.micatechnologies.minecraft.ldic.theme.ThemeDef;
import com.micatechnologies.minecraft.ldic.theme.ThemeLoader;
import com.micatechnologies.minecraft.ldic.theme.ThemeSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Owns the main-menu theme: loads it, swaps the vanilla title screen for it, navigates between
 * its screens, and reloads it on request.
 *
 * <p>The theme comes from {@code config/ldic/menu.json}, which is created on first run as a
 * one-line file extending {@code builtin:classic}. If it fails to load, the built-in default is
 * used instead, and if even that fails the vanilla menu is left alone, so a broken theme can
 * never lock a player out of the game.</p>
 */
public final class MenuManager {

    public static final String DEFAULT_THEME = "builtin:classic";
    private static final String MENU_FILE = "menu.json";

    private final File configDir;
    private final LdicSettings settings;
    private final ClientThemeSource source;
    private final Textures textures;
    private final Fonts fonts;
    private final AssetResolver assets;
    private final ServerStatus servers = new ServerStatus();
    private final HostBindings bindings;
    private final long startNs = System.nanoTime();
    private final Deque<String> history = new ArrayDeque<>();

    private ThemeDef theme;
    private Problems problems;
    private double loadedAt;
    private boolean loadAttempted;

    public MenuManager(File configDir, LdicSettings settings) {
        this.configDir = configDir;
        this.settings = settings;
        this.source = new ClientThemeSource(configDir);
        this.assets = new AssetResolver(configDir);
        WebCache web = new WebCache(configDir);
        this.textures = new Textures(assets, web);
        this.fonts = new Fonts(assets, com.micatechnologies.minecraft.ldic.client.render.VanillaFont.INSTANCE);
        this.bindings = new HostBindings(settings, servers, new WebText(web));
        writeStarterTheme();
        IResourceManager rm = Minecraft.getMinecraft().getResourceManager();
        if (rm instanceof IReloadableResourceManager) {
            // Resource packs changed: images and fonts from them may have too.
            ((IReloadableResourceManager) rm).registerReloadListener(m -> {
                textures.clear();
                fonts.clear();
            });
        }
    }

    Textures textures() {
        return textures;
    }

    Fonts fonts() {
        return fonts;
    }

    HostBindings bindings() {
        return bindings;
    }

    LdicSettings settings() {
        return settings;
    }

    Problems problems() {
        return problems;
    }

    double loadedAt() {
        return loadedAt;
    }

    /** Seconds since LDIC started; the menu's clock, continuous across screens. */
    double clock() {
        return (System.nanoTime() - startNs) / 1e9;
    }

    boolean hasScreen(String id) {
        return theme != null && theme.screen(id) != null;
    }

    // -------------------------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onGuiOpen(GuiOpenEvent event) {
        if (!settings.menuEnabled) {
            return;
        }
        if (event.getGui() instanceof GuiMainMenu && !(event.getGui() instanceof LdicMenuScreen)) {
            if (!loadAttempted) {
                load();
            }
            if (theme != null) {
                history.clear();
                event.setGui(new LdicMenuScreen(this, theme, "main"));
                startMusic();
            }
        }
    }

    private boolean firstMenuShown;

    /** True exactly once: for the first theme screen after the game finished loading. */
    boolean consumeFirstOpen() {
        boolean first = !firstMenuShown;
        firstMenuShown = true;
        return first;
    }

    /**
     * Plays the menu theme's music unless LDIC music is already playing (loading music that
     * continues into the menu, or this theme's own from an earlier visit).
     */
    private void startMusic() {
        MusicPlayer playing = MusicPlayer.current();
        if (playing != null || theme.music == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        MusicPlayer.play(theme.music, assets, settings.musicVolume,
                () -> mc.gameSettings.getSoundLevel(SoundCategory.MASTER) * mc.gameSettings.getSoundLevel(SoundCategory.MUSIC));
    }

    /** While LDIC music plays, vanilla's menu music stays quiet so the two never overlap. */
    @SubscribeEvent
    public void onPlaySound(PlaySoundEvent event) {
        if (event.getSound() != null && event.getSound().getCategory() == SoundCategory.MUSIC && MusicPlayer.current() != null) {
            event.setResultSound(null);
        }
    }

    /** Menu and loading music end when the player enters a world; server pings are processed on the menu. */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (Minecraft.getMinecraft().world != null) {
            if (MusicPlayer.current() != null) {
                MusicPlayer.stopCurrent();
            }
        } else {
            // Process replies to the theme's server-status pings.
            servers.tick();
        }
    }

    /** Opens one of the theme's screens, remembering the current one for {@code back}. */
    void open(String id) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen instanceof LdicMenuScreen) {
            history.push(((LdicMenuScreen) mc.currentScreen).screenId());
        }
        mc.displayGuiScreen(new LdicMenuScreen(this, theme, id));
    }

    void back() {
        String previous = history.isEmpty() ? "main" : history.pop();
        Minecraft.getMinecraft().displayGuiScreen(new LdicMenuScreen(this, theme, hasScreen(previous) ? previous : "main"));
    }

    private long watchedStamp = -1;
    private double lastWatch;

    /**
     * Called every tick while a theme screen is open: once a second, reloads the theme if
     * {@code menu.json} or anything under {@code themes/} changed, so authors see edits live.
     */
    void checkForChanges() {
        double now = clock();
        if (now - lastWatch < 1) {
            return;
        }
        lastWatch = now;
        long stamp = newest(new File(configDir, MENU_FILE), 0);
        stamp = Math.max(stamp, newest(new File(configDir, "themes"), 0));
        if (watchedStamp >= 0 && stamp != watchedStamp) {
            watchedStamp = stamp;
            reload();
            return;
        }
        watchedStamp = stamp;
    }

    private static long newest(File f, int depth) {
        if (!f.exists()) {
            return 0;
        }
        long t = f.lastModified();
        File[] children = depth < 4 ? f.listFiles() : null;
        if (children != null) {
            for (File c : children) {
                t = Math.max(t, newest(c, depth + 1));
            }
        }
        return t;
    }

    /** Re-reads the theme from disk and reopens the current screen (Ctrl+R, or the reload action). */
    public void reload() {
        Minecraft mc = Minecraft.getMinecraft();
        String current = mc.currentScreen instanceof LdicMenuScreen ? ((LdicMenuScreen) mc.currentScreen).screenId() : "main";
        textures.clear();
        fonts.clear();
        load();
        history.clear();
        if (mc.currentScreen instanceof LdicMenuScreen || mc.currentScreen instanceof GuiMainMenu) {
            if (theme != null) {
                mc.displayGuiScreen(new LdicMenuScreen(this, theme, hasScreen(current) ? current : "main"));
            } else {
                mc.displayGuiScreen(new GuiMainMenu());
            }
        }
        Ldic.LOGGER.info("LDIC: menu theme reloaded");
    }

    private void load() {
        loadAttempted = true;
        loadedAt = clock();
        bindings.setSplash(pickSplash());
        Problems p = new Problems();
        ThemeDef t = null;
        try {
            t = loadFrom(source.openConfigFile(MENU_FILE), p);
        } catch (IOException e) {
            p.error("", "cannot read config/ldic/" + MENU_FILE + ": " + e.getMessage());
        }
        if (t == null) {
            Ldic.LOGGER.error("LDIC: the menu theme failed to load; falling back to {}", DEFAULT_THEME);
            try {
                t = loadFrom(source.open(DEFAULT_THEME), p);
            } catch (IOException e) {
                p.error("", "cannot open " + DEFAULT_THEME + ": " + e.getMessage());
            }
        }
        for (Problems.Problem pr : p.all()) {
            Ldic.LOGGER.warn("LDIC theme {}", pr);
        }
        if (t == null) {
            Ldic.LOGGER.error("LDIC: no usable menu theme; keeping the vanilla main menu");
        } else {
            fonts.setAliases(t.fonts);
            Ldic.LOGGER.info("LDIC: menu theme '{}' loaded with {} problem(s)", t.name, p.size());
        }
        theme = t;
        problems = p;
    }

    /** Loads and compiles one document; null if it is unusable (bad JSON or no main screen). */
    private ThemeDef loadFrom(ThemeSource.Document doc, Problems p) {
        ThemeLoader.Result r = ThemeLoader.load(source, doc, p);
        if (r == null) {
            return null;
        }
        ThemeDef t = ThemeCompiler.compile(r.json, bindings.scope, p);
        if (t.screen("main") == null) {
            p.error("screens", doc.name + " has no \"main\" screen (or top-level \"elements\")");
            return null;
        }
        return t;
    }

    private void writeStarterTheme() {
        File file = new File(configDir, MENU_FILE);
        if (file.exists()) {
            return;
        }
        String starter = "{\n"
                + "  \"_readme\": \"LDIC main menu theme. This file extends a built-in theme; add or override elements here. Press Ctrl+R on the menu to reload. Docs: https://mica-technologies.github.io/LDIC/\",\n"
                + "  \"ldic\": 1,\n"
                + "  \"extends\": \"" + DEFAULT_THEME + "\"\n"
                + "}\n";
        try {
            if (!configDir.isDirectory() && !configDir.mkdirs()) {
                Ldic.LOGGER.warn("LDIC: could not create {}", configDir);
                return;
            }
            Files.write(file.toPath(), starter.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Ldic.LOGGER.warn("LDIC: could not write {}", file, e);
        }
    }

    /** A random vanilla splash, with vanilla's date overrides. Read once per theme load. */
    private static String pickSplash() {
        Calendar c = Calendar.getInstance();
        int month = c.get(Calendar.MONTH) + 1;
        int day = c.get(Calendar.DAY_OF_MONTH);
        if (month == 12 && day == 24) {
            return "Merry X-mas!";
        }
        if (month == 1 && day == 1) {
            return "Happy new year!";
        }
        if (month == 10 && day == 31) {
            return "OOoooOOOoooo! Spooky!";
        }
        List<String> lines = new ArrayList<>();
        try (IResource res = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation("texts/splashes.txt"));
             BufferedReader r = new BufferedReader(new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            return "";
        }
        return lines.isEmpty() ? "" : lines.get(new Random().nextInt(lines.size()));
    }
}
