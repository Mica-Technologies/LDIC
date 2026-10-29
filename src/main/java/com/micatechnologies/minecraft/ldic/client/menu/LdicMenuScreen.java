package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.client.render.Gfx;
import com.micatechnologies.minecraft.ldic.client.render.ThemeRenderer;
import com.micatechnologies.minecraft.ldic.theme.ElementDef;
import com.micatechnologies.minecraft.ldic.theme.Problems;
import com.micatechnologies.minecraft.ldic.theme.ScreenDef;
import com.micatechnologies.minecraft.ldic.theme.ThemeDef;
import com.micatechnologies.minecraft.ldic.ui.UiNode;
import com.micatechnologies.minecraft.ldic.ui.UiTree;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;

/**
 * A theme screen.
 *
 * <p>It extends {@link GuiMainMenu} on purpose: mods that add buttons to the main menu check
 * {@code gui instanceof GuiMainMenu} in their {@code InitGuiEvent} handlers, and this keeps
 * them working. Everything vanilla about the class is bypassed: {@code initGui} adds no
 * vanilla buttons, {@code drawScreen} draws the theme, {@code actionPerformed} ignores button
 * ids (a mod's button with id 1 must not start singleplayer). Theme elements never go into
 * {@code buttonList}, so other mods only ever see their own buttons there.</p>
 */
public final class LdicMenuScreen extends GuiMainMenu {

    private static final int LINK_CONFIRM_ID = 0x4C44;

    private final MenuManager manager;
    private final String screenId;
    private final ThemeDef theme;
    private final UiTree tree;
    private final ThemeRenderer renderer;
    private final long openedAt = System.nanoTime();
    private long lastFrame = System.nanoTime();
    private String pendingLink;
    private boolean foreignHandled;
    /** Whether this is the first menu after loading, which fades in from black. */
    private final boolean fadeIn;

    LdicMenuScreen(MenuManager manager, ThemeDef theme, String screenId) {
        this.manager = manager;
        this.theme = theme;
        this.screenId = screenId;
        this.fadeIn = manager.consumeFirstOpen() && manager.settings().menuFadeIn > 0;
        ScreenDef def = theme.screen(screenId);
        this.tree = new UiTree(def);
        this.renderer = new ThemeRenderer(manager.textures(), manager.fonts(), this::hoverStarted);
    }

    public String screenId() {
        return screenId;
    }

    // -------------------------------------------------------------------------------------
    // Lifecycle

    @Override
    public void initGui() {
        // No vanilla buttons. Mods add theirs after this, in InitGuiEvent.Post.
        foreignHandled = false;
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        renderer.dispose();
    }

    @Override
    public void updateScreen() {
        manager.checkForChanges();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /** First frame after init: apply the theme's policy to other mods' buttons. */
    private void handleForeignButtons() {
        if (foreignHandled) {
            return;
        }
        foreignHandled = true;
        if (theme.foreignButtons.equals("hide")) {
            for (GuiButton b : buttonList) {
                b.visible = false;
            }
        }
        for (UiNode n : allNodes()) {
            String wrap = n.def.statik("wrap", null);
            GuiButton b = wrap == null ? null : findForeign(wrap);
            if (b != null) {
                b.visible = false;
            }
        }
    }

    // -------------------------------------------------------------------------------------
    // Drawing

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        handleForeignButtons();
        long nowNs = System.nanoTime();
        double dt = Math.min(0.25, (nowNs - lastFrame) / 1e9);
        lastFrame = nowNs;
        double screenSeconds = (nowNs - openedAt) / 1e9;

        int guiScale = new ScaledResolution(mc).getScaleFactor();
        double ui = theme.uiScale(width, height);
        double lw = width / ui;
        double lh = height / ui;
        double mx = mouseX / ui;
        double my = mouseY / ui;

        manager.textures().tick();
        manager.fonts().setPixelScale(ui * guiScale);
        manager.bindings().update(manager.clock(), dt, screenSeconds, screenId, lw, lh, width, height, guiScale, ui, mx, my);
        theme.updateVariables();

        renderer.now = manager.clock();
        renderer.uiScale = ui;
        renderer.guiScale = guiScale;
        renderer.foreignArea = null;

        // Opaque base, so a theme with a transparent background never shows last frame's pixels.
        drawRect(0, 0, width, height, 0xFF000000);
        Gfx.beginUi();
        GlStateManager.pushMatrix();
        GlStateManager.scale(ui, ui, 1);
        tree.frame(renderer, lw, lh, mx, my, screenSeconds, dt);
        GlStateManager.popMatrix();
        Gfx.endUi();

        layoutForeign(ui);
        for (GuiButton b : buttonList) {
            b.drawButton(mc, mouseX, mouseY, partialTicks);
        }
        if (screenId.equals("main")) {
            ForgeHooksClient.renderMainMenu(this, fontRenderer, width, height, "");
        }
        String tip = tree.tooltip();
        if (tip != null && !tip.isEmpty()) {
            List<String> lines = new ArrayList<>();
            for (String l : tip.split("\n")) {
                lines.add(l);
            }
            drawHoveringText(lines, mouseX, mouseY);
            GlStateManager.disableLighting();
        }
        drawProblems();
        if (fadeIn && screenSeconds < manager.settings().menuFadeIn) {
            int alpha = (int) Math.round(255 * (1 - screenSeconds / manager.settings().menuFadeIn));
            drawRect(0, 0, width, height, alpha << 24);
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
        for (GuiButton b : buttonList) {
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
        int y = 2;
        String head = "LDIC: " + p.size() + " theme problem" + (p.size() == 1 ? "" : "s") + " (see the log" + (debug ? "" : "; set debug in config/ldic/settings.json to list them here") + ")";
        drawString(fontRenderer, head, 2, y, p.hasErrors() ? 0xFF5555 : 0xFFFF55);
        if (debug) {
            for (Problems.Problem pr : p.all()) {
                y += 10;
                if (y > height - 10) {
                    break;
                }
                drawString(fontRenderer, fontRenderer.trimStringToWidth(pr.toString(), width - 4), 2, y, 0xFFCCCC);
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

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        double ui = theme.uiScale(width, height);
        if (mouseButton == 0 && tree.press(mouseX / ui, mouseY / ui) != null) {
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            double ui = theme.uiScale(width, height);
            UiNode clicked = tree.release(mouseX / ui, mouseY / ui);
            if (clicked != null) {
                activate(clicked);
                return;
            }
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_R && isCtrlKeyDown()) {
            manager.reload();
            return;
        }
        switch (keyCode) {
            case Keyboard.KEY_ESCAPE: {
                ScreenDef def = tree.screen();
                if (def.onEscape != null) {
                    Actions.run(def.onEscape, this, manager);
                } else if (!screenId.equals("main")) {
                    manager.back();
                }
                return;
            }
            case Keyboard.KEY_TAB:
                tree.moveFocus(!isShiftKeyDown());
                return;
            case Keyboard.KEY_UP:
            case Keyboard.KEY_LEFT:
                tree.moveFocus(false);
                return;
            case Keyboard.KEY_DOWN:
            case Keyboard.KEY_RIGHT:
                tree.moveFocus(true);
                return;
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
            case Keyboard.KEY_SPACE:
                if (tree.focused() != null) {
                    activate(tree.focused());
                }
                return;
            default:
                break;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        // Other mods' buttons act through ActionPerformedEvent; vanilla id meanings do not apply here.
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
        String wrap = d.statik("wrap", null);
        if (wrap != null) {
            GuiButton b = findForeign(wrap);
            if (b == null) {
                Ldic.LOGGER.warn("LDIC: {} wraps button '{}', but no mod added a button with that id or class", d, wrap);
            } else {
                clickForeign(b);
            }
        }
        Actions.run(d.action, this, manager);
    }

    /** Clicks another mod's button the way GuiScreen would, so its event handlers run exactly once. */
    private void clickForeign(GuiButton b) {
        GuiScreenEvent.ActionPerformedEvent.Pre pre = new GuiScreenEvent.ActionPerformedEvent.Pre(this, b, buttonList);
        if (MinecraftForge.EVENT_BUS.post(pre)) {
            return;
        }
        actionPerformed(pre.getButton());
        if (equals(mc.currentScreen)) {
            MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.ActionPerformedEvent.Post(this, pre.getButton(), buttonList));
        }
    }

    /** A foreign button by numeric id, or by (simple or full) class name. */
    private GuiButton findForeign(String key) {
        for (GuiButton b : buttonList) {
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
        mc.displayGuiScreen(new GuiConfirmOpenLink(this, url, LINK_CONFIRM_ID, true));
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (id == LINK_CONFIRM_ID) {
            if (result && pendingLink != null) {
                browse(pendingLink);
            }
            pendingLink = null;
            mc.displayGuiScreen(this);
            return;
        }
        mc.displayGuiScreen(this);
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
