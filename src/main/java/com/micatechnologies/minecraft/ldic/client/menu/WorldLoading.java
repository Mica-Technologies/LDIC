package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.client.LdicClient;
import com.micatechnologies.minecraft.ldic.client.render.DebugCapture;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiConnecting;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiScreen;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiScreenWorking;
import com.micatechnologies.minecraft.ldic.mixin.AccessorMinecraft;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenWorking;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/**
 * The menu theme's {@code world_loading} screen, shown while a world loads or closes.
 *
 * <p>Vanilla does this in two ways, and LDIC paints over both without taking over the logic:</p>
 * <ul>
 *   <li>Singleplayer startup and shutdown draw through {@code LoadingScreenRenderer}, outside
 *   any screen, from a loop that sleeps 200 ms between frames. A mixin calls
 *   {@link #drawStandalone} instead of the dirt background, and {@link #sleep} keeps drawing
 *   at the frame cap during those sleeps, so the theme animates smoothly.</li>
 *   <li>Connecting to a server, downloading terrain and the "working" screen are real screens
 *   with their own logic and a Cancel button. LDIC cancels only their drawing and paints the
 *   theme, then their buttons where they were, so clicking works exactly as before.</li>
 * </ul>
 *
 * <p>One driver lives for one loading sequence, so {@code screen_time} runs on across the
 * steps; a pause of more than three seconds starts a new one.</p>
 */
public final class WorldLoading {

    static final String SCREEN = "world_loading";
    private static final long NEW_SEQUENCE_NS = 3_000_000_000L;

    private final MenuManager manager;
    private final Standalone standalone = new Standalone();
    private ThemeScreenDriver driver;
    private Object driverFor;
    private long lastDrawNs;
    private long lastStandaloneNs;
    private String lastTitle = "";
    private String lastMessage = "";

    WorldLoading(MenuManager manager) {
        this.manager = manager;
    }

    private static WorldLoading get() {
        MenuManager m = LdicClient.menu();
        return m == null ? null : m.worldLoading();
    }

    private boolean active() {
        return manager.settings().menuEnabled && manager.settings().worldLoadingEnabled && manager.hasScreen(SCREEN);
    }

    /** Whether a screen is drawn by LDIC, so vanilla's 30 fps menu cap should not apply. */
    public static boolean themed(GuiScreen screen) {
        if (screen instanceof ThemeScreenDriver.Host) {
            return true;
        }
        WorldLoading w = get();
        return w != null && w.active() && isLoadingScreen(screen);
    }

    private static boolean isLoadingScreen(GuiScreen s) {
        return s instanceof GuiConnecting || s instanceof GuiDownloadTerrain || s instanceof GuiScreenWorking;
    }

    // -------------------------------------------------------------------------------------
    // Outside any screen: LoadingScreenRenderer

    /**
     * Draws a full frame for {@code LoadingScreenRenderer} and shows it. Returns false to let
     * vanilla draw instead.
     */
    public static boolean drawStandalone(String title, String message, int progress) {
        WorldLoading w = get();
        Minecraft mc = Minecraft.getMinecraft();
        if (w == null || !((AccessorMinecraft) mc).ldic$isRunning() || !w.active()) {
            return false;
        }
        try {
            long now = System.nanoTime();
            boolean sameSequence = now - w.lastStandaloneNs < NEW_SEQUENCE_NS;
            String t = title == null ? "" : title;
            String m = message == null ? "" : message;
            if (!(sameSequence && t.isEmpty() && m.isEmpty())) {
                // Vanilla clears both texts as the client joins the world it just started; keep
                // showing what it was doing rather than an empty screen.
                w.lastTitle = t;
                w.lastMessage = m;
            }
            w.lastStandaloneNs = now;
            // Vanilla calls in at its own times; drawing then as well would land between the
            // paced frames from sleep() and make the animation uneven.
            if (!sameSequence || now - w.lastDrawNs >= w.frameNs() / 2) {
                w.frame();
            }
            return true;
        } catch (RuntimeException e) {
            Ldic.LOGGER.error("LDIC: world-loading screen failed; using vanilla's", e);
            w.manager.settings().worldLoadingEnabled = false;
            return false;
        }
    }

    /**
     * Stands in for vanilla's {@code Thread.sleep} in its world startup and shutdown loops:
     * waits as long, but keeps drawing the loading screen meanwhile.
     */
    public static void sleep(long millis) throws InterruptedException {
        WorldLoading w = get();
        long end = System.nanoTime() + millis * 1_000_000L;
        if (w == null || !w.active() || System.nanoTime() - w.lastStandaloneNs > NEW_SEQUENCE_NS) {
            Thread.sleep(millis);
            return;
        }
        long frameNs = w.frameNs();
        while (true) {
            long now = System.nanoTime();
            if (now >= end) {
                return;
            }
            if (now - w.lastDrawNs >= frameNs) {
                drawStandalone(w.lastTitle, w.lastMessage, -1);
            }
            waitUntil(Math.min(end, w.lastDrawNs + frameNs));
        }
    }

    private long frameNs() {
        return 1_000_000_000L / Math.max(10, manager.settings().loadingFps);
    }

    /**
     * Waits until a {@code System.nanoTime()} deadline. Windows rounds {@code Thread.sleep} up
     * to its ~15 ms timer tick, which alone makes 60 fps frames uneven, so the last 2 ms are
     * spent yielding instead.
     */
    private static void waitUntil(long deadline) throws InterruptedException {
        while (true) {
            long left = deadline - System.nanoTime();
            if (left <= 0) {
                return;
            }
            if (left > 2_000_000L) {
                Thread.sleep(1);
            } else {
                Thread.yield();
            }
        }
    }

    private void frame() {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        standalone.setWorldAndResolution(mc, sr.getScaledWidth(), sr.getScaledHeight());

        String headline = !lastTitle.isEmpty() ? lastTitle : lastMessage;
        String detail = !lastTitle.isEmpty() ? lastMessage : "";
        double progress = -1;
        IntegratedServer server = mc.getIntegratedServer();
        if (server != null && server.getUserMessage() != null && server.percentDone > 0) {
            progress = Math.min(1, server.percentDone / 100.0);
        }

        if (OpenGlHelper.isFramebufferEnabled()) {
            mc.getFramebuffer().unbindFramebuffer();
        }
        GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);
        GlStateManager.clearColor(0, 0, 0, 1);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0, sr.getScaledWidth_double(), sr.getScaledHeight_double(), 0, 1000, 3000);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.loadIdentity();
        GlStateManager.translate(0, 0, -2000);
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();

        int mouseX = Mouse.getX() * sr.getScaledWidth() / Math.max(1, mc.displayWidth);
        int mouseY = sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / Math.max(1, mc.displayHeight) - 1;
        draw(standalone, headline, detail, progress, mouseX, mouseY, 0);
        if (manager.settings().debug && System.nanoTime() - lastShotNs > 1_000_000_000L && shots < 20) {
            lastShotNs = System.nanoTime();
            DebugCapture.save(String.format("ldic-world-loading-%02d", ++shots));
        }
        mc.updateDisplay();
    }

    // -------------------------------------------------------------------------------------
    // Over vanilla's loading screens

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Pre event) {
        GuiScreen gui = event.getGui();
        if (!isLoadingScreen(gui) || !active()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        String headline;
        String detail = "";
        double progress = -1;
        if (gui instanceof GuiConnecting) {
            headline = I18n.format(((AccessorGuiConnecting) gui).ldic$getNetworkManager() == null ? "connect.connecting" : "connect.authorizing");
            ServerData data = mc.getCurrentServerData();
            detail = data == null ? "" : data.serverName;
        } else if (gui instanceof GuiScreenWorking) {
            AccessorGuiScreenWorking w = (AccessorGuiScreenWorking) gui;
            headline = w.ldic$getTitle();
            detail = w.ldic$getStage();
            progress = w.ldic$getProgress() > 0 ? w.ldic$getProgress() / 100.0 : -1;
            if (headline == null || headline.isEmpty()) {
                headline = I18n.format("menu.loadingLevel");
            }
        } else {
            headline = I18n.format("multiplayer.downloadingTerrain");
        }
        try {
            draw(new Overlay(gui), headline, detail, progress, event.getMouseX(), event.getMouseY(), event.getRenderPartialTicks());
            event.setCanceled(true);
        } catch (RuntimeException e) {
            Ldic.LOGGER.error("LDIC: world-loading screen failed; using vanilla's", e);
            manager.settings().worldLoadingEnabled = false;
        }
    }

    // -------------------------------------------------------------------------------------

    private void draw(ThemeScreenDriver.Host host, String headline, String detail, double progress, int mouseX, int mouseY, float partialTicks) {
        long now = System.nanoTime();
        Object key = host instanceof Overlay ? ((Overlay) host).gui : standalone;
        if (driver == null || now - lastDrawNs > NEW_SEQUENCE_NS || manager.theme() != driverTheme) {
            if (driver != null) {
                driver.close();
                logPacing();
            }
            driver = new ThemeScreenDriver(manager, manager.theme(), SCREEN, host, false);
            driverTheme = manager.theme();
            driverFor = null;
            sequenceStartNs = now;
            sequenceFrames = 0;
            longestGapNs = 0;
        } else {
            longestGapNs = Math.max(longestGapNs, now - lastDrawNs);
            if (manager.settings().debug && now - lastDrawNs > 100_000_000L) {
                Ldic.LOGGER.info(String.format("LDIC: world loading: no frame for %.0f ms before '%s' / '%s' (%s)",
                        (now - lastDrawNs) / 1e6, headline, detail, host.gui().getClass().getSimpleName()));
            }
        }
        sequenceFrames++;
        if (driverFor != key) {
            // The same sequence moved to another screen: keep the clock, take the new buttons.
            driver.rehost(host);
            driverFor = key;
        }
        lastDrawNs = now;
        manager.bindings().setStatus(headline, detail, progress);
        driver.draw(mouseX, mouseY, partialTicks);
    }

    private Object driverTheme;
    private long sequenceStartNs;
    private int sequenceFrames;
    private long longestGapNs;

    /** With debug on, how smooth the last loading sequence was, to check frame pacing. */
    private void logPacing() {
        if (!manager.settings().debug || sequenceFrames < 2) {
            return;
        }
        double seconds = (lastDrawNs - sequenceStartNs) / 1e9;
        Ldic.LOGGER.info(String.format("LDIC: world loading drew %d frames in %.1f s (%.0f fps), longest gap %.0f ms",
                sequenceFrames, seconds, sequenceFrames / Math.max(0.001, seconds), longestGapNs / 1e6));
    }
    private long lastShotNs;
    private int shots;

    /** A screen with nothing in it, standing in for the screen the standalone path lacks. */
    private static final class Standalone extends GuiScreen implements ThemeScreenDriver.Host {
        @Override
        public GuiScreen gui() {
            return this;
        }

        @Override
        public List<GuiButton> buttons() {
            return Collections.emptyList();
        }

        @Override
        public void back() {
        }

        @Override
        public boolean opaqueBase() {
            return true;
        }

        @Override
        public boolean mayHideForeignButtons() {
            return false;
        }
    }

    /** One of vanilla's loading screens, whose own buttons (Cancel) are drawn over the theme. */
    private static final class Overlay implements ThemeScreenDriver.Host {
        final GuiScreen gui;

        Overlay(GuiScreen gui) {
            this.gui = gui;
        }

        @Override
        public GuiScreen gui() {
            return gui;
        }

        @Override
        public List<GuiButton> buttons() {
            return ((AccessorGuiScreen) gui).ldic$getButtonList();
        }

        @Override
        public void back() {
        }

        @Override
        public boolean opaqueBase() {
            return true;
        }

        @Override
        public boolean mayHideForeignButtons() {
            return false;
        }
    }
}
