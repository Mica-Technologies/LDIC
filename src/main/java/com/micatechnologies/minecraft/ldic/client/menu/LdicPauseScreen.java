package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.theme.ThemeDef;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;

/**
 * The themed pause menu: the menu theme's {@code pause} screen in place of vanilla's.
 *
 * <p>It extends {@link GuiIngameMenu} so mods that add buttons to the pause menu (checking
 * {@code instanceof GuiIngameMenu}) still do. As on the options screen, a theme can never hide
 * them: they go into its {@code foreign_buttons} area, or stay where the mod put them. Vanilla's
 * own buttons are not created; the theme draws its own, and the screens they open (options,
 * advancements, statistics, LAN, mod options) are the usual ones.</p>
 *
 * <p>The {@code classic_pause} action opens vanilla's pause menu instead, once.</p>
 */
public final class LdicPauseScreen extends GuiIngameMenu implements ThemeScreenDriver.Host {

    private final ThemeScreenDriver driver;

    LdicPauseScreen(MenuManager manager, ThemeDef theme) {
        this.driver = new ThemeScreenDriver(manager, theme, "pause", this, false);
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

    /** Back from the pause menu is back to the game. */
    @Override
    public void back() {
        mc.displayGuiScreen(null);
        mc.setIngameFocus();
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
        // GuiIngameMenu's own id handling (1 = disconnect, 4 = resume, ...) must not run for them.
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (!driver.confirmClicked(result, id)) {
            mc.displayGuiScreen(this);
        }
    }
}
