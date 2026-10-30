package com.micatechnologies.minecraft.ldic.client;

import com.micatechnologies.minecraft.ldic.client.hud.OneConfigSupport;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiSlider;

/**
 * LDIC's in-game settings (Mods list, Config): the hotbar and the inventory. Saved to the
 * player's {@code player.json} on Done. Until the OneConfig page (Phase 11) this is the only
 * screen for them.
 */
public final class LdicConfigScreen extends GuiScreen {

    private static final int STYLE = 0;
    private static final int SCALE = 1;
    private static final int AUTO_HIDE = 2;
    private static final int DELAY = 3;
    private static final int FULL = 4;
    private static final int MOD_SCREENS = 5;
    private static final int DONE = 200;

    private final GuiScreen parent;
    private final LdicSettings settings;
    private GuiSlider scale;
    private GuiSlider delay;

    public LdicConfigScreen(GuiScreen parent) {
        this.parent = parent;
        this.settings = LdicClient.settings();
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int left = width / 2 - 155;
        int right = width / 2 + 5;
        int y = height / 6 + 12;
        buttonList.add(new GuiButton(STYLE, left, y, 150, 20, styleLabel()));
        scale = new GuiSlider(SCALE, right, y, 150, 20, I18n.format("ldic.config.hotbar.scale") + ": ", "%",
                50, 200, settings.hotbarScale * 100, false, true);
        buttonList.add(scale);
        y += 24;
        buttonList.add(new GuiButton(AUTO_HIDE, left, y, 150, 20, onOff("ldic.config.hotbar.auto_hide", settings.hotbarAutoHide)));
        delay = new GuiSlider(DELAY, right, y, 150, 20, I18n.format("ldic.config.hotbar.hide_delay") + ": ", "s",
                0.5, 10, settings.hotbarHideDelay, true, true);
        delay.precision = 1;
        delay.updateSlider();
        delay.enabled = settings.hotbarAutoHide;
        buttonList.add(delay);
        y += 36;
        buttonList.add(new GuiButton(FULL, left, y, 150, 20, onOff("ldic.config.inventory.full", settings.inventoryFullScreen)));
        buttonList.add(new GuiButton(MOD_SCREENS, right, y, 150, 20, onOff("ldic.config.inventory.mod_screens", settings.inventoryScaleModScreens)));
        buttonList.add(new GuiButton(DONE, width / 2 - 100, height / 6 + 168, I18n.format("gui.done")));
    }

    private String styleLabel() {
        return I18n.format("ldic.config.hotbar.style") + ": " + I18n.format("ldic.config.hotbar.style." + settings.hotbarStyle);
    }

    private static String onOff(String key, boolean on) {
        return I18n.format(key) + ": " + I18n.format(on ? "options.on" : "options.off");
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case STYLE:
                settings.hotbarStyle = "modern".equals(settings.hotbarStyle) ? "vanilla" : "modern";
                button.displayString = styleLabel();
                break;
            case AUTO_HIDE:
                settings.hotbarAutoHide = !settings.hotbarAutoHide;
                button.displayString = onOff("ldic.config.hotbar.auto_hide", settings.hotbarAutoHide);
                delay.enabled = settings.hotbarAutoHide;
                break;
            case FULL:
                settings.inventoryFullScreen = !settings.inventoryFullScreen;
                button.displayString = onOff("ldic.config.inventory.full", settings.inventoryFullScreen);
                break;
            case MOD_SCREENS:
                settings.inventoryScaleModScreens = !settings.inventoryScaleModScreens;
                button.displayString = onOff("ldic.config.inventory.mod_screens", settings.inventoryScaleModScreens);
                break;
            case DONE:
                mc.displayGuiScreen(parent);
                break;
            default:
                break;
        }
    }

    @Override
    public void onGuiClosed() {
        settings.hotbarScale = Math.round(scale.getValue()) / 100.0;
        settings.hotbarHideDelay = Math.round(delay.getValue() * 10) / 10.0;
        settings.saveHotbarAndInventory(LdicClient.configDir());
        OneConfigSupport.refresh();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, I18n.format("ldic.config.title"), width / 2, 15, 0xFFFFFF);
        drawString(fontRenderer, I18n.format("ldic.config.hotbar"), width / 2 - 155, height / 6, 0xA0A0A0);
        drawString(fontRenderer, I18n.format("ldic.config.inventory"), width / 2 - 155, height / 6 + 72, 0xA0A0A0);
        drawCenteredString(fontRenderer, I18n.format("ldic.config.keys"), width / 2, height / 6 + 132, 0x808080);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
