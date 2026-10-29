package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.theme.ThemeDef;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;

/**
 * The themed options screen: the menu theme's {@code options} screen in place of vanilla's.
 *
 * <p>It extends {@link GuiOptions} for the same reason the menu screen extends
 * {@code GuiMainMenu}: mods add their settings buttons to "the options screen" by checking
 * {@code instanceof GuiOptions}, and those buttons must keep appearing. Unlike the main menu,
 * a theme can never hide them here; they go into the theme's {@code foreign_buttons} area, or
 * stay where the mod put them. Vanilla's own buttons are not created at all: the theme draws
 * its own, and its sub-screens (video, controls, sounds, ...) are vanilla's, opened as-is.</p>
 *
 * <p>The {@code classic_options} action opens vanilla's screen instead, once, as a way out.</p>
 */
public final class LdicOptionsScreen extends GuiOptions implements ThemeScreenDriver.Host {

    private final GuiScreen parent;
    private final ThemeScreenDriver driver;

    LdicOptionsScreen(MenuManager manager, ThemeDef theme, GuiScreen parent) {
        super(parent, Minecraft.getMinecraft().gameSettings);
        this.parent = parent;
        this.driver = new ThemeScreenDriver(manager, theme, "options", this, false);
    }

    GuiScreen parent() {
        return parent;
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
        driver.saveOptionsIfChanged();
        mc.gameSettings.saveOptions();
        mc.displayGuiScreen(parent);
    }

    @Override
    public boolean mayHideForeignButtons() {
        return false;
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
    public boolean doesGuiPauseGame() {
        return true;
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
        // Only other mods' buttons are in buttonList; they act through ActionPerformedEvent.
        // GuiOptions' own id handling (100 = controls, 108 = difficulty, ...) must not run for them.
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (!driver.confirmClicked(result, id)) {
            mc.displayGuiScreen(this);
        }
    }
}
