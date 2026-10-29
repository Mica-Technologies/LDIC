package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.theme.ThemeDef;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.ForgeHooksClient;

/**
 * A theme screen shown as (or instead of) the title screen.
 *
 * <p>It extends {@link GuiMainMenu} on purpose: mods that add buttons to the main menu check
 * {@code gui instanceof GuiMainMenu} in their {@code InitGuiEvent} handlers, and this keeps
 * them working. Everything vanilla about the class is bypassed: {@code initGui} adds no
 * vanilla buttons, drawing and input go to the {@link ThemeScreenDriver}, and
 * {@code actionPerformed} ignores button ids (a mod's button with id 1 must not start
 * singleplayer). Theme elements never go into {@code buttonList}, so other mods only ever see
 * their own buttons there.</p>
 */
public final class LdicMenuScreen extends GuiMainMenu implements ThemeScreenDriver.Host {

    private final MenuManager manager;
    private final ThemeScreenDriver driver;

    LdicMenuScreen(MenuManager manager, ThemeDef theme, String screenId) {
        this.manager = manager;
        boolean fadeIn = manager.consumeFirstOpen() && manager.settings().menuFadeIn > 0;
        this.driver = new ThemeScreenDriver(manager, theme, screenId, this, fadeIn);
    }

    public String screenId() {
        return driver.screenId();
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
        if (!driver.screenId().equals("main")) {
            manager.back();
        }
    }

    @Override
    public void drawExtras() {
        if (driver.screenId().equals("main") && manager.theme() != null && manager.theme().forgeUpdateNotice) {
            ForgeHooksClient.renderMainMenu(this, fontRenderer, width, height, "");
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
    }

    @Override
    public void updateScreen() {
        manager.checkForChanges();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        driver.draw(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
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
        if (!driver.mouseReleased(mouseX, mouseY, state)) {
            super.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        driver.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        // Other mods' buttons act through ActionPerformedEvent; vanilla id meanings do not apply here.
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (!driver.confirmClicked(result, id)) {
            mc.displayGuiScreen(this);
        }
    }
}
