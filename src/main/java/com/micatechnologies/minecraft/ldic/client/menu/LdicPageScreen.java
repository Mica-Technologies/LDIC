package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.api.MenuPage;
import com.micatechnologies.minecraft.ldic.theme.ThemeDef;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

/**
 * A page another mod described through the API ({@link MenuPage}), drawn by the menu theme's
 * {@code mod_page} screen. Every theme has that screen: LDIC's base layer supplies one, and a
 * theme restyles it by overriding its elements.
 *
 * <p>A plain {@code GuiScreen}, unlike the title, pause and options screens: no other mod adds
 * buttons to a screen it has never heard of, so there is nothing to stay compatible with.</p>
 */
public final class LdicPageScreen extends GuiScreen implements ThemeScreenDriver.Host {

    static final String SCREEN = "mod_page";

    private final MenuManager manager;
    private final GuiScreen parent;
    private final PageView view;
    private final ThemeScreenDriver driver;

    LdicPageScreen(MenuManager manager, ThemeDef theme, MenuPage page, GuiScreen parent) {
        this.manager = manager;
        this.parent = parent;
        this.view = new PageView(page);
        this.driver = new ThemeScreenDriver(manager, theme, SCREEN, this, false);
    }

    PageView view() {
        return view;
    }

    // Host

    @Override
    public GuiScreen gui() {
        return this;
    }

    @Override
    public List<GuiButton> buttons() {
        return buttonList;
    }

    @Override
    public void back() {
        mc.displayGuiScreen(parent);
        if (parent == null && mc.world != null) {
            mc.setIngameFocus();
        }
    }

    // Vanilla screen

    @Override
    public void initGui() {
        driver.init();
    }

    @Override
    public void onGuiClosed() {
        driver.close();
        // Also when a row opens another screen over the page: saving twice is harmless, and a
        // page that never came back would otherwise never be saved.
        view.closed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        manager.bindings().setPage(view);
        driver.draw(mouseX, mouseY, partialTicks);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            view.scrollBy(wheel > 0 ? -1 : 1);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        manager.bindings().setPage(view);
        if (!driver.mouseClicked(mouseX, mouseY, mouseButton)) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        driver.mouseDragged(mouseX, mouseY);
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        manager.bindings().setPage(view);
        if (!driver.mouseReleased(mouseX, mouseY, state)) {
            super.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        manager.bindings().setPage(view);
        driver.keyTyped(typedChar, keyCode);
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (!driver.confirmClicked(result, id)) {
            mc.displayGuiScreen(this);
        }
    }
}
