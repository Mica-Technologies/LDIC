package com.micatechnologies.minecraft.ldic.client.loading;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.client.AssetResolver;
import com.micatechnologies.minecraft.ldic.client.ClientThemeSource;
import com.micatechnologies.minecraft.ldic.client.LdicSettings;
import com.micatechnologies.minecraft.ldic.client.WebCache;
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
        return new LoadingScreen(settings, theme, bindings, configDir);
    }

    private static ThemeDef compile(ThemeSource source, ThemeSource.Document doc, LoadingBindings bindings, Problems problems) {
        ThemeLoader.Result r = ThemeLoader.load(source, doc, problems);
        if (r == null) {
            return null;
        }
        ThemeDef t = ThemeCompiler.compile(r.json, bindings.scope, problems);
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
            UiTree tree = new UiTree(theme.screen("main"));
            List<ProgressModel.Bar> bars = new ArrayList<>();

            long start = System.nanoTime();
            long last = start;
            long updateNanos = 0;
            int frames = 0;
            boolean vsyncForced = false;
            while (!isDone()) {
                long now = System.nanoTime();
                double seconds = (now - start) / 1e9;
                double dt = Math.min(0.25, (now - last) / 1e9);
                last = now;

                bars.clear();
                Iterator<ProgressManager.ProgressBar> it = ProgressManager.barIterator();
                while (it.hasNext()) {
                    ProgressManager.ProgressBar b = it.next();
                    bars.add(new ProgressModel.Bar(b.getTitle(), b.getMessage(), b.getStep(), b.getSteps()));
                }
                model.update(bars, seconds, dt);
                drawFrame(renderer, textures, fonts, tree, model, bars, seconds, dt);
                if (settings.debug) {
                    debugShots.maybeCapture(model.progress());
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

    private void drawFrame(ThemeRenderer renderer, Textures textures, Fonts fonts, UiTree tree, ProgressModel model,
                           List<ProgressModel.Bar> bars, double seconds, double dt) {
        int w = Display.getWidth();
        int h = Display.getHeight();
        int scale = guiScale(w, h);
        double gw = w / (double) scale;
        double gh = h / (double) scale;
        double ui = theme.uiScale(gw, gh);

        GL11.glViewport(0, 0, w, h);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, gw, gh, 0, -100, 100);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glClearColor(0, 0, 0, 1);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        textures.tick();
        fonts.setPixelScale(ui * scale);
        bindings.update(seconds, dt, gw / ui, gh / ui, gw, gh, scale, ui, model, bars);
        theme.updateVariables();
        renderer.now = seconds;
        renderer.uiScale = ui;
        renderer.guiScale = scale;

        Gfx.beginUi();
        GL11.glPushMatrix();
        GL11.glScaled(ui, ui, 1);
        tree.frame(renderer, gw / ui, gh / ui, -1, -1, seconds, dt);
        GL11.glPopMatrix();
        Gfx.endUi();
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
     * as progress passes 10%, 50% and 85%, so a loading theme can be checked (and documented)
     * without a screen recorder: nothing else can screenshot the game this early.
     */
    private static final class DebugShots {
        private static final double[] AT = {0.10, 0.50, 0.85};
        private int next;

        void maybeCapture(double progress) {
            if (next >= AT.length || progress < AT[next]) {
                return;
            }
            int pct = (int) Math.round(AT[next] * 100);
            next++;
            int w = Display.getWidth();
            int h = Display.getHeight();
            java.nio.ByteBuffer buf = org.lwjgl.BufferUtils.createByteBuffer(w * h * 4);
            GL11.glReadPixels(0, 0, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int i = ((h - 1 - y) * w + x) * 4;
                    img.setRGB(x, y, ((buf.get(i) & 0xFF) << 16) | ((buf.get(i + 1) & 0xFF) << 8) | (buf.get(i + 2) & 0xFF));
                }
            }
            File dir = new File(Minecraft.getMinecraft().gameDir, "screenshots");
            try {
                if (dir.isDirectory() || dir.mkdirs()) {
                    javax.imageio.ImageIO.write(img, "png", new File(dir, String.format("ldic-loading-%02d.png", pct)));
                }
            } catch (IOException e) {
                Ldic.LOGGER.warn("LDIC: could not save a loading screenshot", e);
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
