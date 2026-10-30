package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.client.render.Gfx;
import com.micatechnologies.minecraft.ldic.client.render.ListHooks;
import com.micatechnologies.minecraft.ldic.client.render.SkinHooks;
import com.micatechnologies.minecraft.ldic.client.render.ThemeRenderer;
import com.micatechnologies.minecraft.ldic.theme.ElementDef;
import com.micatechnologies.minecraft.ldic.theme.Problems;
import com.micatechnologies.minecraft.ldic.theme.ScreenDef;
import com.micatechnologies.minecraft.ldic.theme.ThemeDef;
import com.micatechnologies.minecraft.ldic.ui.UiNode;
import com.micatechnologies.minecraft.ldic.ui.UiTree;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;

/**
 * Runs one theme screen inside a vanilla screen class: drawing, pointer and keyboard input,
 * slider drags, other mods' buttons, links and the debug overlay.
 *
 * <p>The vanilla class it runs in is chosen for compatibility, not behaviour: the main menu
 * runs in a {@code GuiMainMenu} subclass and the options screen in a {@code GuiOptions}
 * subclass, so other mods that add buttons to "the main menu" or "the options screen" by
 * checking {@code instanceof} keep working. That is also why this is a driver the screens
 * delegate to rather than a base class: the two need different superclasses.</p>
 */
final class ThemeScreenDriver {

    /** What a driver needs from the vanilla screen it runs in. */
    interface Host {
        GuiScreen gui();

        /** The screen's own {@code buttonList}: only other mods' buttons are ever in it. */
        List<GuiButton> buttons();

        /** Escape, or a {@code back} action. */
        void back();

        /** Extra drawing after the theme and other mods' buttons, before tooltips (Forge's notice). */
        default void drawExtras() {
        }

        /** Whether the theme may hide other mods' buttons here. The options screen never does. */
        /** Whether to paint black under the theme first; screens over a live world skip it. */
        default boolean opaqueBase() {
            return Minecraft.getMinecraft().world == null;
        }

        default boolean mayHideForeignButtons() {
            return true;
        }
    }

    static final int LINK_CONFIRM_ID = 0x4C44;

    private final MenuManager manager;
    private final ThemeDef theme;
    private final String screenId;
    private Host host;
    private final UiTree tree;
    private final ThemeRenderer renderer;
    private final long openedAt = System.nanoTime();
    private final boolean fadeIn;
    private long lastFrame = System.nanoTime();
    private boolean foreignHandled;
    private String pendingLink;
    private UiNode dragging;
    private boolean optionsChanged;

    ThemeScreenDriver(MenuManager manager, ThemeDef theme, String screenId, Host host, boolean fadeIn) {
        this.manager = manager;
        this.theme = theme;
        this.screenId = screenId;
        this.host = host;
        this.fadeIn = fadeIn;
        ScreenDef def = theme.screen(screenId);
        this.tree = new UiTree(def);
        this.renderer = new ThemeRenderer(manager.textures(), manager.fonts(), this::hoverStarted);
        if (host instanceof SkinHooks) {
            renderer.skin = (SkinHooks) host;
        }
        if (host instanceof ListHooks) {
            renderer.lists = (ListHooks) host;
        }
    }

    String screenId() {
        return screenId;
    }

    MenuManager manager() {
        return manager;
    }

    GuiScreen gui() {
        return host.gui();
    }

    /** Moves this driver to another vanilla screen, keeping its clock (world loading moves through several). */
    void rehost(Host next) {
        host = next;
        foreignHandled = false;
    }

    Host host() {
        return host;
    }

    // -------------------------------------------------------------------------------------
    // Lifecycle

    void init() {
        // No vanilla buttons. Mods add theirs after this, in InitGuiEvent.Post.
        foreignHandled = false;
        Keyboard.enableRepeatEvents(true);
    }

    void close() {
        Keyboard.enableRepeatEvents(false);
        renderer.dispose();
        saveOptionsIfChanged();
    }

    void markOptionsChanged() {
        optionsChanged = true;
    }

    void saveOptionsIfChanged() {
        if (optionsChanged) {
            optionsChanged = false;
            GameOptions.save();
        }
    }

    /** First frame after init: apply the theme's policy to other mods' buttons. */
    private void handleForeignButtons() {
        if (foreignHandled) {
            return;
        }
        foreignHandled = true;
        if (theme.foreignButtons.equals("hide") && host.mayHideForeignButtons()) {
            for (GuiButton b : host.buttons()) {
                b.visible = false;
            }
        }
        for (UiNode n : allNodes()) {
            String wrap = n.def.wrappedButton();
            GuiButton b = wrap == null ? null : findForeign(wrap);
            if (b != null) {
                b.visible = false;
            }
        }
    }

    // -------------------------------------------------------------------------------------
    // Drawing

    void draw(int mouseX, int mouseY, float partialTicks) {
        GuiScreen gui = host.gui();
        Minecraft mc = Minecraft.getMinecraft();
        handleForeignButtons();
        long nowNs = System.nanoTime();
        double dt = Math.min(0.25, (nowNs - lastFrame) / 1e9);
        lastFrame = nowNs;
        double screenSeconds = (nowNs - openedAt) / 1e9;

        int guiScale = new ScaledResolution(mc).getScaleFactor();
        double ui = theme.uiScale(gui.width, gui.height);
        double lw = gui.width / ui;
        double lh = gui.height / ui;
        double mx = mouseX / ui;
        double my = mouseY / ui;

        if (dragging != null) {
            drag(mx);
        }

        manager.textures().tick();
        manager.fonts().setPixelScale(ui * guiScale);
        int foreignCount = 0;
        for (GuiButton b : host.buttons()) {
            if (b.visible) {
                foreignCount++;
            }
        }
        manager.bindings().update(manager.clock(), dt, screenSeconds, screenId, lw, lh, gui.width, gui.height, guiScale, ui, mx, my);
        manager.bindings().setForeignButtons(foreignCount);
        theme.updateVariables();

        renderer.now = manager.clock();
        renderer.uiScale = ui;
        renderer.guiScale = guiScale;
        renderer.foreignArea = null;

        if (host.opaqueBase()) {
            // Opaque base, so a theme with a transparent background never shows last frame's pixels.
            Gui.drawRect(0, 0, gui.width, gui.height, 0xFF000000);
        }
        Gfx.beginUi();
        GlStateManager.pushMatrix();
        GlStateManager.scale(ui, ui, 1);
        tree.frame(renderer, lw, lh, mx, my, screenSeconds, dt);
        GlStateManager.popMatrix();
        Gfx.endUi();

        layoutForeign(ui);
        for (GuiButton b : host.buttons()) {
            b.drawButton(mc, mouseX, mouseY, partialTicks);
        }
        host.drawExtras();
        String tip = tree.tooltip();
        if (tip != null && !tip.isEmpty()) {
            List<String> lines = new ArrayList<>();
            for (String l : tip.split("\n")) {
                lines.add(l);
            }
            gui.drawHoveringText(lines, mouseX, mouseY);
            GlStateManager.disableLighting();
        }
        if (manager.settings().debug) {
            Gfx.beginUi();
            drawDebugOverlay(ui, mouseX, mouseY);
            Gfx.endUi();
        }
        drawProblems();
        if (fadeIn && screenSeconds < manager.settings().menuFadeIn) {
            int alpha = (int) Math.round(255 * (1 - screenSeconds / manager.settings().menuFadeIn));
            Gui.drawRect(0, 0, gui.width, gui.height, alpha << 24);
        }
    }

    /** Moves other mods' visible buttons into the theme's foreign_buttons area, if it has one. */
    private void layoutForeign(double ui) {
        double[] area = renderer.foreignArea;
        if (area == null || renderer.foreignNode == null) {
            return;
        }
        ElementDef d = renderer.foreignNode.def;
        String layout = d.statik("layout", "column");
        if (layout.equals("keep")) {
            return;
        }
        double spacing = d.num("spacing", 4) * ui;
        String align = d.statik("align", "start");
        double ax = area[0] * ui;
        double ay = area[1] * ui;
        double aw = area[2] * ui;
        double ah = area[3] * ui;
        List<GuiButton> shown = new ArrayList<>();
        for (GuiButton b : host.buttons()) {
            if (b.visible) {
                shown.add(b);
            }
        }
        double total = 0;
        for (GuiButton b : shown) {
            total += (layout.equals("row") ? b.width : b.height) + spacing;
        }
        total = Math.max(0, total - spacing);
        double start = align.equals("center") ? ((layout.equals("row") ? aw : ah) - total) / 2
                : align.equals("end") ? (layout.equals("row") ? aw : ah) - total : 0;
        double pos = start;
        for (GuiButton b : shown) {
            if (layout.equals("row")) {
                b.x = (int) Math.round(ax + pos);
                b.y = (int) Math.round(ay + (ah - b.height) / 2);
                pos += b.width + spacing;
            } else {
                b.x = (int) Math.round(ax + (aw - b.width) / 2);
                b.y = (int) Math.round(ay + pos);
                pos += b.height + spacing;
            }
            // A button wider than the area (a 200 px vanilla button at a small UI scale) must still
            // fit on screen, or part of it cannot be clicked.
            GuiScreen gui = host.gui();
            b.x = Math.max(0, Math.min(b.x, gui.width - b.width));
            b.y = Math.max(0, Math.min(b.y, gui.height - b.height));
        }
    }

    /**
     * With debug on: outlines every visible element's box (interactive ones in cyan) and names
     * the element under the pointer with its JSON path, so a theme author can find what to edit.
     */
    private void drawDebugOverlay(double ui, int mouseX, int mouseY) {
        GuiScreen gui = host.gui();
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        UiNode under = null;
        for (UiNode n : allNodes()) {
            if (!n.visible) {
                continue;
            }
            long colour = n.def.isInteractive() ? 0xC000E5FFL : 0x50FFFFFFL;
            Gfx.border(n.x * ui, n.y * ui, (n.x + n.width) * ui, (n.y + n.height) * ui, 1, colour, 1);
            if (n.contains(mouseX / ui, mouseY / ui)) {
                under = n;
            }
        }
        if (under != null) {
            String label = (under.def.id != null ? under.def.id + " " : "") + "(" + under.def.type + ")  " + under.def.path
                    + String.format("  [%.0f, %.0f  %.0f x %.0f]", under.x, under.y, under.width, under.height);
            label = fr.trimStringToWidth(label, gui.width - 8);
            int w = fr.getStringWidth(label);
            Gui.drawRect(gui.width / 2 - w / 2 - 3, gui.height - 14, gui.width / 2 + w / 2 + 3, gui.height - 2, 0xC0000000);
            fr.drawStringWithShadow(label, gui.width / 2 - w / 2, gui.height - 12, 0x00E5FF);
        }
    }

    private void drawProblems() {
        Problems p = manager.problems();
        if (p == null || p.isEmpty()) {
            return;
        }
        boolean debug = manager.settings().debug;
        if (!debug && manager.clock() - manager.loadedAt() > 10) {
            return;
        }
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        int y = 2;
        String head = "LDIC: " + p.size() + " theme problem" + (p.size() == 1 ? "" : "s") + " (see the log"
                + (debug ? "" : "; set debug in config/ldic/settings.json to list them here") + ")";
        fr.drawStringWithShadow(head, 2, y, p.hasErrors() ? 0xFF5555 : 0xFFFF55);
        if (debug) {
            for (Problems.Problem pr : p.all()) {
                y += 10;
                if (y > host.gui().height - 10) {
                    break;
                }
                fr.drawStringWithShadow(fr.trimStringToWidth(pr.toString(), host.gui().width - 4), 2, y, 0xFFCCCC);
            }
        }
    }

    private void hoverStarted(UiNode n) {
        if (n.def.soundHover != null) {
            Actions.playSound(n.def.soundHover.asString(), 1, 1);
        }
    }

    // -------------------------------------------------------------------------------------
    // Input

    private double ui() {
        return theme.uiScale(host.gui().width, host.gui().height);
    }

    /** Returns true when the theme took the click (so the vanilla screen must not). */
    boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return false;
        }
        double ui = ui();
        UiNode n = tree.press(mouseX / ui, mouseY / ui);
        if (n == null) {
            return false;
        }
        if (n.def.type.equals("slider") && n.enabled) {
            dragging = n;
            drag(mouseX / ui);
        }
        return true;
    }

    void mouseDragged(int mouseX, int mouseY) {
        if (dragging != null) {
            drag(mouseX / ui());
        }
    }

    /** Returns true when the theme took the release. */
    boolean mouseReleased(int mouseX, int mouseY, int state) {
        if (state != 0) {
            return false;
        }
        if (dragging != null) {
            drag(mouseX / ui());
            dragging = null;
            tree.release(mouseX / ui(), mouseY / ui());
            Actions.playSound("minecraft:ui.button.click", 1, 1);
            saveOptionsIfChanged();
            return true;
        }
        double ui = ui();
        UiNode clicked = tree.release(mouseX / ui, mouseY / ui);
        if (clicked != null) {
            activate(clicked);
            return true;
        }
        return false;
    }

    /** Sets a dragged slider's option from the pointer's position across it. */
    private void drag(double layoutX) {
        String option = dragging.def.statik("option", null);
        if (option != null) {
            GameOptions.setFraction(option, dragging.fractionX(layoutX));
            optionsChanged = true;
        }
    }

    /** Returns true when the key was handled. */
    boolean keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_R && GuiScreen.isCtrlKeyDown()) {
            manager.reload();
            return true;
        }
        UiNode focused = tree.focused();
        if (focused != null && focused.def.type.equals("slider") && (keyCode == Keyboard.KEY_LEFT || keyCode == Keyboard.KEY_RIGHT)) {
            String option = focused.def.statik("option", null);
            if (option != null) {
                double step = GuiScreen.isShiftKeyDown() ? 0.1 : 0.02;
                GameOptions.setFraction(option, GameOptions.value(option) + (keyCode == Keyboard.KEY_RIGHT ? step : -step));
                optionsChanged = true;
            }
            return true;
        }
        switch (keyCode) {
            case Keyboard.KEY_ESCAPE: {
                ScreenDef def = tree.screen();
                if (def.onEscape != null) {
                    Actions.run(def.onEscape, this);
                } else {
                    host.back();
                }
                return true;
            }
            case Keyboard.KEY_TAB:
                tree.moveFocus(!GuiScreen.isShiftKeyDown());
                return true;
            case Keyboard.KEY_UP:
            case Keyboard.KEY_LEFT:
                tree.moveFocus(false);
                return true;
            case Keyboard.KEY_DOWN:
            case Keyboard.KEY_RIGHT:
                tree.moveFocus(true);
                return true;
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
            case Keyboard.KEY_SPACE:
                if (tree.focused() != null) {
                    activate(tree.focused());
                }
                return true;
            default:
                return false;
        }
    }

    private void activate(UiNode n) {
        if (!n.enabled) {
            return;
        }
        tree.prepare(n);
        ElementDef d = n.def;
        if (d.soundPress != null) {
            Actions.playSound(d.soundPress.asString(), 1, 1);
        } else if (d.type.equals("button")) {
            Actions.playSound("minecraft:ui.button.click", 1, 1);
        }
        String wrap = d.wrappedButton();
        if (wrap != null) {
            GuiButton b = findForeign(wrap);
            if (b == null) {
                Ldic.LOGGER.warn("LDIC: {} wraps button '{}', but no mod added a button with that id or class", d, wrap);
            } else {
                clickForeign(b);
            }
        }
        Actions.run(d.action, this);
    }

    /** Clicks another mod's button the way GuiScreen would, so its event handlers run exactly once. */
    private void clickForeign(GuiButton b) {
        GuiScreen gui = host.gui();
        GuiScreenEvent.ActionPerformedEvent.Pre pre = new GuiScreenEvent.ActionPerformedEvent.Pre(gui, b, host.buttons());
        if (MinecraftForge.EVENT_BUS.post(pre)) {
            return;
        }
        if (gui.equals(Minecraft.getMinecraft().currentScreen)) {
            MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.ActionPerformedEvent.Post(gui, pre.getButton(), host.buttons()));
        }
    }

    /** A foreign button by numeric id, or by (simple or full) class name. */
    private GuiButton findForeign(String key) {
        for (GuiButton b : host.buttons()) {
            if (String.valueOf(b.id).equals(key) || b.getClass().getSimpleName().equals(key) || b.getClass().getName().equals(key)) {
                return b;
            }
        }
        return null;
    }

    private List<UiNode> allNodes() {
        List<UiNode> out = new ArrayList<>();
        for (UiNode n : tree.roots()) {
            collect(n, out);
        }
        return out;
    }

    private static void collect(UiNode n, List<UiNode> out) {
        out.add(n);
        for (UiNode c : n.children) {
            collect(c, out);
        }
    }

    // -------------------------------------------------------------------------------------
    // Links

    void openLink(String url, boolean confirm) {
        if (url.isEmpty()) {
            return;
        }
        if (!confirm) {
            browse(url);
            return;
        }
        pendingLink = url;
        GuiScreen gui = host.gui();
        Minecraft.getMinecraft().displayGuiScreen(new GuiConfirmOpenLink(gui, url, LINK_CONFIRM_ID, true));
    }

    /** Handles the link confirmation; returns false for confirmations that are not ours. */
    boolean confirmClicked(boolean result, int id) {
        if (id != LINK_CONFIRM_ID) {
            return false;
        }
        if (result && pendingLink != null) {
            browse(pendingLink);
        }
        pendingLink = null;
        Minecraft.getMinecraft().displayGuiScreen(host.gui());
        return true;
    }

    private static void browse(String url) {
        try {
            Class<?> desktop = Class.forName("java.awt.Desktop");
            Object d = desktop.getMethod("getDesktop").invoke(null);
            desktop.getMethod("browse", URI.class).invoke(d, new URI(url));
        } catch (Throwable t) {
            Ldic.LOGGER.error("LDIC could not open link {}", url, t);
        }
    }
}
