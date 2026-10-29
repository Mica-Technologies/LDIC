package com.micatechnologies.minecraft.ldic.client.loading;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.client.AssetResolver;
import com.micatechnologies.minecraft.ldic.client.ClientThemeSource;
import com.micatechnologies.minecraft.ldic.client.LdicSettings;
import com.micatechnologies.minecraft.ldic.client.WebCache;
import com.micatechnologies.minecraft.ldic.client.audio.MusicPlayer;
import com.micatechnologies.minecraft.ldic.client.render.BitmapFont;
import com.micatechnologies.minecraft.ldic.client.render.Fonts;
import com.micatechnologies.minecraft.ldic.client.render.Gfx;
import com.micatechnologies.minecraft.ldic.client.render.RawGl;
import com.micatechnologies.minecraft.ldic.client.render.ThemeRenderer;
import com.micatechnologies.minecraft.ldic.client.render.Textures;
import com.micatechnologies.minecraft.ldic.loading.ProgressModel;
import com.micatechnologies.minecraft.ldic.theme.Json;
import com.micatechnologies.minecraft.ldic.theme.Problems;
import com.micatechnologies.minecraft.ldic.theme.ThemeCompiler;
import com.micatechnologies.minecraft.ldic.theme.ThemeDef;
import com.micatechnologies.minecraft.ldic.theme.ThemeLoader;
import com.micatechnologies.minecraft.ldic.theme.ThemeSource;
import com.micatechnologies.minecraft.ldic.ui.UiTree;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import net.minecraft.client.Minecraft;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.client.SplashProgress;
import net.minecraftforge.fml.common.ProgressManager;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/**
 * LDIC's loading screen: runs on Forge's splash thread in place of Forge's own drawing.
 *
 * <p>The thread protocol is Forge's, kept exactly (see {@code SplashProgress}): this thread owns
 * the window's GL context and holds {@code SplashProgress.lock} while it does; it takes
 * {@code SplashProgress.mutex} around {@code Display.update()} so the main thread can skip
 * input processing meanwhile; when the main thread sets {@code pause} it hands the context and
 * the lock back until {@code resume}; and it stops when {@code finish()} sets {@code done}.</p>
 *
 * <p>Everything that can fail runs inside a guard. If LDIC is disabled, has no usable theme, or
 * throws before loading is done, the GL context and lock are released and Forge's original
 * runnable runs instead, so a broken loading theme costs the player LDIC's screen, never the
 * game.</p>
 */
public final class LoadingScreen {

    private static final String THEME_FILE = "loading.json";
    public static final String DEFAULT_THEME = "builtin:minimal";

    private final LdicSettings settings;
    private final ThemeDef theme;
    private final LoadingBindings bindings;
    private final File configDir;
    private final Field doneField;
    private final Field pauseField;
    private final Lock lock;
    private final Semaphore mutex;

    private LoadingScreen(LdicSettings settings, ThemeDef theme, LoadingBindings bindings, File configDir) throws ReflectiveOperationException {
        this.settings = settings;
        this.theme = theme;
        this.bindings = bindings;
        this.configDir = configDir;
        this.doneField = SplashProgress.class.getDeclaredField("done");
        this.pauseField = SplashProgress.class.getDeclaredField("pause");
        doneField.setAccessible(true);
        pauseField.setAccessible(true);
        Field lockField = SplashProgress.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        this.lock = (Lock) lockField.get(null);
        Field mutexField = SplashProgress.class.getDeclaredField("mutex");
        mutexField.setAccessible(true);
        this.mutex = (Semaphore) mutexField.get(null);
    }

    /** Wraps Forge's splash runnable; called from {@code MixinSplashProgress}. */
    public static Runnable wrap(Runnable forgeSplash) {
        return () -> {
            LoadingScreen screen = null;
            try {
                screen = create();
            } catch (Throwable t) {
                Ldic.LOGGER.error("LDIC: the loading screen could not start; using Forge's", t);
            }
            if (screen == null || !screen.run()) {
                forgeSplash.run();
            }
        };
    }

    private static LoadingScreen create() throws IOException, ReflectiveOperationException {
        File configDir = new File(Minecraft.getMinecraft().gameDir, "config/ldic");
        if (!configDir.isDirectory() && !configDir.mkdirs()) {
            Ldic.LOGGER.warn("LDIC: could not create {}", configDir);
        }
        LdicSettings settings = LdicSettings.load(configDir);
        if (!settings.loadingEnabled) {
            return null;
        }
        writeStarterTheme(configDir);
        LoadingBindings bindings = new LoadingBindings(settings, readTips(configDir));
        ClientThemeSource source = new ClientThemeSource(configDir);
        Problems problems = new Problems();
        ThemeDef theme = null;
        try {
            theme = compile(source, source.openConfigFile(THEME_FILE), bindings, problems);
        } catch (IOException e) {
            problems.error("", "cannot read config/ldic/" + THEME_FILE + ": " + e.getMessage());
        }
        if (theme == null) {
            theme = compile(source, source.open(DEFAULT_THEME), bindings, problems);
        }
        for (Problems.Problem p : problems.all()) {
            Ldic.LOGGER.warn("LDIC loading theme {}", p);
        }
        if (theme == null) {
            return null;
        }
        Ldic.LOGGER.info("LDIC: loading theme '{}' with {} problem(s)", theme.name, problems.size());

        Seen seen = Seen.load(configDir);
        boolean wantNotice = settings.photosensitivityNotice && (settings.photosensitivityAlways || !seen.has(NOTICE_KEY));
        if (theme.flashing && !settings.photosensitivityNotice) {
            Ldic.LOGGER.warn("LDIC: loading theme '{}' declares flashing content but the photosensitivity notice is off "
                    + "(photosensitivity_notice.enabled in config/ldic/settings.json)", theme.name);
        }
        LoadingScreen screen = new LoadingScreen(settings, theme, bindings, configDir);
        if (wantNotice || settings.logoScreen) {
            // config/ldic/notices.json rewords them; a screen it lacks comes from the built-in file.
            Problems np = new Problems();
            ThemeDef builtin = compile(source, source.open(NOTICE_THEME), bindings, np, null);
            ThemeDef custom = new File(configDir, NOTICE_FILE).isFile()
                    ? compile(source, source.openConfigFile(NOTICE_FILE), bindings, np, null) : null;
            for (Problems.Problem p : np.all()) {
                Ldic.LOGGER.warn("LDIC notice theme {}", p);
            }
            screen.logo = settings.logoScreen ? pick(custom, builtin, LOGO_SCREEN) : null;
            screen.notice = wantNotice ? pick(custom, builtin, NOTICE_SCREEN) : null;
        }
        screen.seen = seen;
        if (theme.music != null) {
            Minecraft mc = Minecraft.getMinecraft();
            MusicPlayer.play(theme.music, new AssetResolver(configDir, true), settings.musicVolume,
                    () -> mc.gameSettings.getSoundLevel(SoundCategory.MASTER) * mc.gameSettings.getSoundLevel(SoundCategory.MUSIC));
        }
        return screen;
    }

    private static final String NOTICE_FILE = "notices.json";
    private static final String NOTICE_THEME = "builtin:notices";
    private static final String NOTICE_SCREEN = "photosensitivity";
    private static final String NOTICE_KEY = "notice:photosensitivity";
    private static final String LOGO_SCREEN = "logo";
    /** How quickly the next screen fades in after one is skipped. */
    private static final double SKIP_FADE = 0.35;
    /** How long the screen after the logo card or the notice takes to fade in over it. */
    private static final double INTRO_FADE = 0.6;

    private ThemeDef logo;
    private ThemeDef notice;

    private static ThemeDef pick(ThemeDef custom, ThemeDef builtin, String screen) {
        if (custom != null && custom.screen(screen) != null) {
            return custom;
        }
        return builtin != null && builtin.screen(screen) != null ? builtin : null;
    }
    private Seen seen;

    private static ThemeDef compile(ThemeSource source, ThemeSource.Document doc, LoadingBindings bindings, Problems problems) {
        return compile(source, doc, bindings, problems, "main");
    }

    private static ThemeDef compile(ThemeSource source, ThemeSource.Document doc, LoadingBindings bindings, Problems problems,
                                    String requiredScreen) {
        ThemeLoader.Result r = ThemeLoader.load(source, doc, problems);
        if (r == null) {
            return null;
        }
        ThemeDef t = ThemeCompiler.compile(r.json, bindings.scope, problems);
        if (requiredScreen == null) {
            return t;
        }
        if (!requiredScreen.equals("main")) {
            if (t.screen(requiredScreen) == null) {
                problems.error("screens", doc.name + " has no \"" + requiredScreen + "\" screen");
                return null;
            }
            return t;
        }
        if (t.screen("main") == null) {
            problems.error("screens", doc.name + " has no \"main\" screen (or top-level \"elements\")");
            return null;
        }
        return t;
    }

    private static void writeStarterTheme(File configDir) {
        File file = new File(configDir, THEME_FILE);
        if (file.exists()) {
            return;
        }
        String starter = "{\n"
                + "  \"_readme\": \"LDIC loading screen theme. This file extends a built-in theme; add or override elements here. Docs: https://mica-technologies.github.io/LDIC/loading/\",\n"
                + "  \"ldic\": 1,\n"
                + "  \"extends\": \"" + DEFAULT_THEME + "\"\n"
                + "}\n";
        try {
            Files.write(file.toPath(), starter.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Ldic.LOGGER.warn("LDIC: could not write {}", file, e);
        }
    }

    /** {@code config/ldic/tips.txt}: one tip per line; blank lines and lines starting # are skipped. */
    private static List<String> readTips(File configDir) {
        List<String> tips = new ArrayList<>();
        File file = new File(configDir, "tips.txt");
        if (!file.isFile()) {
            return tips;
        }
        try {
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                String t = line.trim();
                if (!t.isEmpty() && !t.startsWith("#")) {
                    tips.add(t);
                }
            }
        } catch (IOException e) {
            Ldic.LOGGER.warn("LDIC: could not read {}", file, e);
        }
        return tips;
    }

    // -------------------------------------------------------------------------------------

    private boolean isDone() {
        try {
            return doneField.getBoolean(null);
        } catch (IllegalAccessException e) {
            return true;
        }
    }

    private boolean isPaused() {
        try {
            return pauseField.getBoolean(null);
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    /**
     * Runs until Forge says loading is done. Returns false if it failed before then, in which
     * case the context and lock have been released and Forge's splash should take over.
     */
    private boolean run() {
        boolean held = false;
        Textures textures = null;
        Fonts fonts = null;
        BitmapFont minecraftFont = new BitmapFont();
        ThemeRenderer renderer = null;
        ProgressModel model = new ProgressModel(Timings.load(configDir));
        try {
            acquire();
            held = true;
            Gfx.useBackend(RawGl.INSTANCE);
            AssetResolver assets = new AssetResolver(configDir, true);
            textures = new Textures(assets, new WebCache(configDir));
            fonts = new Fonts(assets, minecraftFont);
            fonts.setAliases(theme.fonts);
            renderer = new ThemeRenderer(textures, fonts, n -> { });
            List<Segment> segments = buildSegments();
            List<ProgressModel.Bar> bars = new ArrayList<>();

            long start = System.nanoTime();
            long last = start;
            long updateNanos = 0;
            int frames = 0;
            boolean vsyncForced = false;
            // The logo card and the photosensitivity notice are shown in full even if loading
            // finishes first (unless the player skips them): finish() simply waits. With debug on,
            // hold_seconds keeps the screen up after loading ends, so authors can preview a theme
            // on a fast machine.
            double doneAt = -1;
            double skipHeld = 0;
            boolean skipNeedsRelease = false;
            while (true) {
                double t = (System.nanoTime() - start) / 1e9;
                boolean done = isDone();
                if (done) {
                    if (doneAt < 0) {
                        doneAt = t;
                    }
                    double hold = settings.debug ? settings.loadingHoldSeconds : 0;
                    if (t >= doneAt + hold && !introShowing(segments, t)) {
                        break;
                    }
                }
                long now = System.nanoTime();
                double seconds = (now - start) / 1e9;
                double dt = Math.min(0.25, (now - last) / 1e9);
                last = now;

                // Holding Space skips the intro screen showing; the next one needs a fresh press.
                Segment intro = currentIntro(segments, seconds);
                boolean space = SkipKey.spaceDown();
                if (!space) {
                    skipNeedsRelease = false;
                }
                if (intro != null && space && !skipNeedsRelease && settings.introSkipHoldSeconds > 0) {
                    skipHeld += dt;
                    if (skipHeld >= settings.introSkipHoldSeconds) {
                        Ldic.LOGGER.info("LDIC: intro screen skipped at {}s", String.format("%.1f", seconds));
                        skip(segments, intro, seconds);
                        skipHeld = 0;
                        skipNeedsRelease = true;
                    }
                } else {
                    skipHeld = 0;
                }
                bindings.setIntro(settings.introSkipHoldSeconds > 0 ? Math.min(1, skipHeld / settings.introSkipHoldSeconds) : 0,
                        settings.introSkipHoldSeconds, done);

                bars.clear();
                Iterator<ProgressManager.ProgressBar> it = ProgressManager.barIterator();
                while (it.hasNext()) {
                    ProgressManager.ProgressBar b = it.next();
                    bars.add(new ProgressModel.Bar(b.getTitle(), b.getMessage(), b.getStep(), b.getSteps()));
                }
                model.update(bars, seconds, dt);
                drawFrame(renderer, textures, fonts, segments, model, bars, seconds, dt);
                if (settings.debug) {
                    debugShots.maybeCapture(seconds);
                }

                mutex.acquireUninterruptibly();
                long updateStart = System.nanoTime();
                Display.update();
                if (frames < 200) {
                    updateNanos += System.nanoTime() - updateStart;
                }
                mutex.release();
                frames++;

                if (isPaused()) {
                    release();
                    held = false;
                    acquire();
                    held = true;
                }
                // Some drivers force vsync inside Display.update; then sleeping instead of syncing
                // gives the main thread (and mods polling the keyboard) a turn at the display lock.
                if (frames >= 200 && updateNanos > 200L * 5_000_000L) {
                    if (!vsyncForced) {
                        vsyncForced = true;
                        Ldic.LOGGER.info("LDIC: loading screen using sleep timing (Display.update is slow here)");
                    }
                    Thread.sleep(16);
                } else {
                    Display.sync(settings.loadingFps);
                }
            }
            model.finish((System.nanoTime() - start) / 1e9);
            if (model.complete()) {
                Timings.save(configDir, model.stageSeconds());
            }
            markSeen(segments, Double.MAX_VALUE);
            if (theme.music != null && !theme.music.continueIntoMenu) {
                MusicPlayer.stopCurrent();
            }
            return true;
        } catch (Throwable t) {
            boolean finished = isDone();
            Ldic.LOGGER.error("LDIC: the loading screen failed{}", finished ? "" : "; Forge's loading screen takes over", t);
            return finished;
        } finally {
            if (held) {
                try {
                    if (textures != null) {
                        textures.clear();
                    }
                    if (fonts != null) {
                        fonts.clear();
                    }
                    minecraftFont.delete();
                    if (renderer != null) {
                        renderer.dispose();
                    }
                } catch (Throwable t) {
                    Ldic.LOGGER.warn("LDIC: cleaning up loading-screen textures failed", t);
                }
                restoreForMinecraft();
                release();
            }
            Gfx.useBackend(null);
        }
    }

    /**
     * One screen in the loading sequence: the logo card, the notice, a theme intro, or main
     * (which never ends). Times move when the player skips one.
     */
    private static final class Segment {
        final ThemeDef theme;
        final UiTree tree;
        double start;
        double end;
        /** Seconds it fades in over the one before. */
        double fade;
        final String seenKey;
        /** The logo card or the notice: shown in full even after loading ends, and skippable. */
        final boolean intro;

        Segment(ThemeDef theme, String screen, double start, double end, double fade, String seenKey, boolean intro) {
            this.theme = theme;
            this.tree = new UiTree(theme.screen(screen));
            this.start = start;
            this.end = end;
            this.fade = fade;
            this.seenKey = seenKey;
            this.intro = intro;
        }
    }

    /**
     * Lays the sequence out on the clock. Each segment starts {@code crossfade} seconds before
     * the previous one ends and fades in over it; main starts last and runs until loading is done.
     */
    private List<Segment> buildSegments() {
        List<Segment> out = new ArrayList<>();
        double cf = theme.crossfade;
        double t = 0;
        // The intro screens hand over quickly; the theme's own crossfade is meant for its scenes.
        double fadeIn = cf;
        if (logo != null) {
            double len = settings.logoSeconds;
            out.add(new Segment(logo, LOGO_SCREEN, t, t + len, fadeIn, null, true));
            t += len - INTRO_FADE;
            fadeIn = INTRO_FADE;
        }
        if (notice != null) {
            double len = settings.photosensitivitySeconds;
            out.add(new Segment(notice, NOTICE_SCREEN, Math.max(0, t), t + len, fadeIn, NOTICE_KEY, true));
            t += len - INTRO_FADE;
            fadeIn = INTRO_FADE;
        }
        for (ThemeDef.Step step : theme.sequence) {
            String key = "once:" + theme.name + ":" + step.screen;
            if (step.once && seen.has(key)) {
                continue;
            }
            out.add(new Segment(theme, step.screen, Math.max(0, t), t + step.seconds, fadeIn, step.once ? key : null, false));
            t += step.seconds - cf;
            fadeIn = cf;
        }
        out.add(new Segment(theme, "main", Math.max(0, t), Double.MAX_VALUE, fadeIn, null, false));
        return out;
    }

    private static boolean introShowing(List<Segment> segments, double t) {
        for (Segment s : segments) {
            if (s.intro && t < s.end) {
                return true;
            }
        }
        return false;
    }

    /** The intro screen on top now (the latest one started), or null. */
    private static Segment currentIntro(List<Segment> segments, double t) {
        Segment top = null;
        for (Segment s : segments) {
            if (t >= s.start && t < s.end) {
                top = s;
            }
        }
        return top != null && top.intro ? top : null;
    }

    /**
     * Ends {@code s} now: the next screen fades in over it quickly, and everything after moves
     * earlier by the same amount, so the rest of the sequence keeps its timing.
     */
    private static void skip(List<Segment> segments, Segment s, double now) {
        int i = segments.indexOf(s);
        s.end = Math.min(s.end, now + SKIP_FADE);
        if (i + 1 >= segments.size()) {
            return;
        }
        Segment next = segments.get(i + 1);
        double shift = Math.max(0, next.start - now);
        for (int j = i + 1; j < segments.size(); j++) {
            Segment n = segments.get(j);
            n.start = Math.max(now, n.start - shift);
            if (n.end != Double.MAX_VALUE) {
                n.end -= shift;
            }
        }
        next.fade = SKIP_FADE;
    }

    private void markSeen(List<Segment> segments, double t) {
        boolean changed = false;
        for (Segment s : segments) {
            if (s.seenKey != null && s.end != Double.MAX_VALUE && t >= s.end && !seen.has(s.seenKey)) {
                seen.add(s.seenKey);
                changed = true;
            }
        }
        if (changed) {
            seen.save(configDir);
        }
    }

    private void drawFrame(ThemeRenderer renderer, Textures textures, Fonts fonts, List<Segment> segments, ProgressModel model,
                           List<ProgressModel.Bar> bars, double seconds, double dt) {
        int w = Display.getWidth();
        int h = Display.getHeight();
        int scale = guiScale(w, h);
        double gw = w / (double) scale;
        double gh = h / (double) scale;

        GL11.glViewport(0, 0, w, h);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, gw, gh, 0, -100, 100);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glClearColor(0, 0, 0, 1);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        textures.tick();
        renderer.now = seconds;
        renderer.guiScale = scale;
        Gfx.beginUi();
        for (Segment s : segments) {
            if (seconds < s.start || seconds >= s.end) {
                continue;
            }
            // A segment fades in over the one before it; that one stays opaque underneath until
            // it ends, so the crossfade never dips through black.
            double opacity = s.start <= 0 || s.fade <= 0 ? 1 : Math.min(1, (seconds - s.start) / s.fade);
            double ui = s.theme.uiScale(gw, gh);
            fonts.setAliases(s.theme.fonts);
            fonts.setPixelScale(ui * scale);
            bindings.update(seconds, dt, gw / ui, gh / ui, gw, gh, scale, ui, model, bars);
            bindings.setScreenTime(seconds - s.start);
            s.theme.updateVariables();
            renderer.uiScale = ui;
            GL11.glPushMatrix();
            GL11.glScaled(ui, ui, 1);
            s.tree.frame(renderer, gw / ui, gh / ui, -1, -1, seconds - s.start, dt, opacity);
            GL11.glPopMatrix();
        }
        Gfx.endUi();
        markSeen(segments, seconds);
    }

    /** Vanilla's GUI scale rule: the largest scale that keeps at least 320x240 GUI pixels. */
    private static int guiScale(int w, int h) {
        int setting = Minecraft.getMinecraft().gameSettings.guiScale;
        int max = setting == 0 ? 1000 : setting;
        int scale = 1;
        while (scale < max && w / (scale + 1) >= 320 && h / (scale + 1) >= 240) {
            scale++;
        }
        return scale;
    }

    private void acquire() throws Exception {
        lock.lock();
        try {
            Display.getDrawable().makeCurrent();
        } catch (Exception e) {
            lock.unlock();
            throw e;
        }
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void release() {
        try {
            Display.getDrawable().releaseContext();
        } catch (Exception e) {
            Ldic.LOGGER.error("LDIC: could not release the GL context", e);
        } finally {
            lock.unlock();
        }
    }

    /** Leaves GL and Minecraft's window size the way Forge's splash leaves them. */
    private static void restoreForMinecraft() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayWidth = Display.getWidth();
        mc.displayHeight = Display.getHeight();
        mc.resize(mc.displayWidth, mc.displayHeight);
        GL11.glClearColor(1, 1, 1, 1);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, .1f);
    }

    private final DebugShots debugShots = new DebugShots();

    /**
     * With {@code "debug": true} in settings.json, saves {@code screenshots/ldic-loading-NN.png}
     * every 2 seconds (up to 12), so a loading theme can be checked (and documented)
     * without a screen recorder: nothing else can screenshot the game this early.
     */
    private static final class DebugShots {
        private static final double EVERY = 2;
        private static final int MAX = 12;
        private int next;

        void maybeCapture(double seconds) {
            if (next >= MAX || seconds < EVERY * (next + 1)) {
                return;
            }
            int at = (int) Math.round(EVERY * (next + 1));
            next++;
            com.micatechnologies.minecraft.ldic.client.render.DebugCapture.save(String.format("ldic-loading-%02ds", at));
        }
    }

    /** {@code config/ldic/cache/seen.json}: notices and once-only intros this player has seen. */
    static final class Seen {
        private final java.util.Set<String> keys = new java.util.HashSet<>();

        boolean has(String key) {
            return keys.contains(key);
        }

        void add(String key) {
            keys.add(key);
        }

        static Seen load(File configDir) {
            Seen s = new Seen();
            File f = new File(configDir, "cache/seen.json");
            if (f.isFile()) {
                try {
                    for (com.google.gson.JsonElement e : Json.parse(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8))
                            .getAsJsonObject().getAsJsonArray("seen")) {
                        s.keys.add(e.getAsString());
                    }
                } catch (Exception e) {
                    Ldic.LOGGER.warn("LDIC: could not read {}", f);
                }
            }
            return s;
        }

        void save(File configDir) {
            File dir = new File(configDir, "cache");
            JsonObject o = new JsonObject();
            JsonArray a = new JsonArray();
            for (String k : keys) {
                a.add(k);
            }
            o.add("seen", a);
            try {
                if (dir.isDirectory() || dir.mkdirs()) {
                    Files.write(new File(dir, "seen.json").toPath(), o.toString().getBytes(StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                Ldic.LOGGER.warn("LDIC: could not save {}", dir, e);
            }
        }
    }

    /** {@code config/ldic/cache/loading-timings.json}: seconds per stage last launch. */
    static final class Timings {
        private Timings() {
        }

        static double[] load(File configDir) {
            File f = new File(configDir, "cache/loading-timings.json");
            if (!f.isFile()) {
                return null;
            }
            try {
                JsonArray a = Json.parse(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8))
                        .getAsJsonObject().getAsJsonArray("stages");
                double[] t = new double[a.size()];
                for (int i = 0; i < t.length; i++) {
                    t[i] = a.get(i).getAsDouble();
                }
                return t;
            } catch (Exception e) {
                return null;
            }
        }

        static void save(File configDir, double[] stages) {
            File dir = new File(configDir, "cache");
            JsonObject o = new JsonObject();
            JsonArray a = new JsonArray();
            for (double s : stages) {
                a.add(Math.round(s * 1000) / 1000.0);
            }
            o.add("stages", a);
            try {
                if (!dir.isDirectory() && !dir.mkdirs()) {
                    return;
                }
                Files.write(new File(dir, "loading-timings.json").toPath(), o.toString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                Ldic.LOGGER.warn("LDIC: could not save loading timings", e);
            }
        }
    }
}
