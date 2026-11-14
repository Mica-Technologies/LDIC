package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.theme.ThemeDef;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLockIconButton;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.gui.GuiOptionSlider;
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

    /** Every id vanilla's options screen gives a button (its option buttons are told by class). */
    private static final Set<Integer> VANILLA_IDS = new HashSet<>(Arrays.asList(
            100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 200));
    /** Sub-screen names (as the {@code open} action spells them) to vanilla's button ids. */
    private static final Map<String, Integer> VANILLA_OPENERS = new HashMap<>();

    static {
        VANILLA_OPENERS.put("controls", 100);
        VANILLA_OPENERS.put("video", 101);
        VANILLA_OPENERS.put("language", 102);
        VANILLA_OPENERS.put("languages", 102);
        VANILLA_OPENERS.put("chat", 103);
        VANILLA_OPENERS.put("snooper", 104);
        VANILLA_OPENERS.put("resource_packs", 105);
        VANILLA_OPENERS.put("resourcepacks", 105);
        VANILLA_OPENERS.put("sounds", 106);
        VANILLA_OPENERS.put("skin", 110);
    }

    private final GuiScreen parent;
    private final ThemeScreenDriver driver;
    /** Buttons other mods put in by mixin into GuiOptions.initGui; their clicks go to GuiOptions. */
    private final Set<GuiButton> injected = Collections.newSetFromMap(new IdentityHashMap<>());

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
        injected.clear();
        try {
            // Vanilla's own initGui, for what mods add inside it: a mixin at the end of
            // GuiOptions.initGui (a shader mod's "Shader Options...") never runs if only this
            // override does. Vanilla's buttons are dropped again; what is left is theirs.
            super.initGui();
            for (Iterator<GuiButton> it = buttonList.iterator(); it.hasNext();) {
                GuiButton b = it.next();
                if (isVanilla(b)) {
                    it.remove();
                } else {
                    injected.add(b);
                }
            }
        } catch (RuntimeException e) {
            Ldic.LOGGER.error("LDIC: vanilla's options screen failed to set up; buttons other mods inject into it are missing", e);
            buttonList.clear();
            injected.clear();
        }
    }

    private static boolean isVanilla(GuiButton b) {
        return b instanceof GuiOptionButton || b instanceof GuiOptionSlider || b instanceof GuiLockIconButton
                || VANILLA_IDS.contains(b.id);
    }

    /**
     * Opens one of vanilla's sub-screens the way its own button on the options screen would:
     * through {@code GuiOptions.actionPerformed}, where mods hook to put their own screen in its
     * place (Celeritas's video settings). Returns false for names vanilla has no button for.
     */
    boolean openAsVanilla(String name) {
        Integer id = VANILLA_OPENERS.get(name);
        if (id == null) {
            return false;
        }
        driver.saveOptionsIfChanged();
        try {
            super.actionPerformed(new GuiButton(id, 0, 0, ""));
            return true;
        } catch (IOException | RuntimeException e) {
            Ldic.LOGGER.error("LDIC: could not open '{}' through the options screen; opening it directly", name, e);
            return false;
        }
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
    protected void actionPerformed(GuiButton button) throws IOException {
        // Only other mods' buttons are in buttonList. Those added in InitGuiEvent act through
        // ActionPerformedEvent, and GuiOptions' own id handling (100 = controls, 108 = difficulty,
        // ...) must not run for them. Those a mixin added inside GuiOptions.initGui are handled by
        // that mod's mixin in GuiOptions.actionPerformed, so they do go there; none has a vanilla id.
        if (injected.contains(button)) {
            super.actionPerformed(button);
        }
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (!driver.confirmClicked(result, id)) {
            mc.displayGuiScreen(this);
        }
    }
}
