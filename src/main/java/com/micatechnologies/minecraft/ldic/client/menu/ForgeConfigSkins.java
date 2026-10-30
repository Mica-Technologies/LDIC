package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiConfig;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiEditArray;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiEditArrayEntries;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiSelectString;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiEditArray;
import net.minecraftforge.fml.client.config.GuiSelectString;
import net.minecraftforge.fml.client.config.HoverChecker;

/**
 * Forge's config screens for {@link ScreenSkins}: the config screen ({@code forge_config}) and
 * the list editor and value picker it opens ({@code forge_config_values}). Mods subclass these to
 * fill them in, so a subclass is skinned too, unless one of its classes draws the screen itself.
 * A skin replaces their drawScreen, so what it does besides drawing (button states, tooltips) is
 * done here each frame.
 */
final class ForgeConfigSkins {

    private ForgeConfigSkins() {
    }

    /** The theme screen for a Forge config screen, or null (not one, or it draws itself). */
    static String screenFor(GuiScreen gui) {
        Class<?> base = gui instanceof GuiConfig ? GuiConfig.class
                : gui instanceof GuiEditArray ? GuiEditArray.class
                : gui instanceof GuiSelectString ? GuiSelectString.class : null;
        if (base == null) {
            return null;
        }
        for (Class<?> c = gui.getClass(); c != base; c = c.getSuperclass()) {
            for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                String name = m.getName();
                if (name.equals("drawScreen") || name.equals("func_73863_a")) {
                    return null;
                }
            }
        }
        return base == GuiConfig.class ? "forge_config" : "forge_config_values";
    }

    static boolean handles(GuiScreen gui) {
        return gui instanceof GuiConfig || gui instanceof GuiEditArray || gui instanceof GuiSelectString;
    }

    /** The screen's titles, for screen_text: title, title2, title3. */
    static Map<String, Object> extras(GuiScreen gui) {
        Map<String, Object> m = new HashMap<>();
        if (gui instanceof GuiConfig) {
            GuiConfig c = (GuiConfig) gui;
            put(m, c.title, c.titleLine2, null);
        } else if (gui instanceof GuiEditArray) {
            AccessorGuiEditArray a = (AccessorGuiEditArray) gui;
            put(m, a.ldic$getTitle(), a.ldic$getTitle2(), a.ldic$getTitle3());
        } else if (gui instanceof GuiSelectString) {
            AccessorGuiSelectString a = (AccessorGuiSelectString) gui;
            put(m, a.ldic$getTitle(), a.ldic$getTitle2(), a.ldic$getTitle3());
        }
        return m;
    }

    private static void put(Map<String, Object> m, String t1, String t2, String t3) {
        m.put("title", t1 == null ? "" : t1);
        m.put("title2", t2 == null ? "" : t2);
        m.put("title3", t3 == null ? "" : t3);
    }

    /** What the screen's own drawScreen does before drawing: its buttons follow the entries. */
    static void beforeDraw(GuiScreen gui) {
        if (gui instanceof GuiConfig) {
            GuiConfig c = (GuiConfig) gui;
            AccessorGuiConfig a = (AccessorGuiConfig) c;
            if (c.entryList == null || a.ldic$getApplyGlobally() == null) {
                return;
            }
            boolean global = a.ldic$getApplyGlobally().isChecked();
            boolean any = c.entryList.areAnyEntriesEnabled(global);
            a.ldic$getUndoAll().enabled = any && c.entryList.hasChangedEntry(global);
            a.ldic$getDefaultAll().enabled = any && !c.entryList.areAllEntriesDefault(global);
        } else if (gui instanceof GuiEditArray) {
            AccessorGuiEditArray a = (AccessorGuiEditArray) gui;
            if (a.ldic$getEntries() == null || a.ldic$getDone() == null) {
                return;
            }
            a.ldic$getDone().enabled = ((AccessorGuiEditArrayEntries) a.ldic$getEntries()).ldic$isListSavable();
            a.ldic$getDefault().enabled = a.ldic$isEnabled() && !a.ldic$getEntries().isDefault();
            a.ldic$getUndo().enabled = a.ldic$isEnabled() && a.ldic$getEntries().isChanged();
        } else if (gui instanceof GuiSelectString) {
            GuiSelectString s = (GuiSelectString) gui;
            AccessorGuiSelectString a = (AccessorGuiSelectString) gui;
            if (a.ldic$getEntries() == null || a.ldic$getDone() == null) {
                return;
            }
            a.ldic$getDone().enabled = s.currentValue != null;
            a.ldic$getDefault().enabled = a.ldic$isEnabled() && !a.ldic$getEntries().isDefault();
            a.ldic$getUndo().enabled = a.ldic$isEnabled() && a.ldic$getEntries().isChanged();
        }
    }

    /** What the screen's own drawScreen does after drawing: the entries' and buttons' tooltips. */
    static void afterDraw(GuiScreen gui, int mouseX, int mouseY, float partialTicks) {
        if (gui instanceof GuiConfig) {
            GuiConfig c = (GuiConfig) gui;
            AccessorGuiConfig a = (AccessorGuiConfig) c;
            if (c.entryList == null) {
                return;
            }
            c.entryList.drawScreenPost(mouseX, mouseY, partialTicks);
            tooltip(c::drawToolTip, a.ldic$getUndoHover(), I18n.format("fml.configgui.tooltip.undoAll"), mouseX, mouseY);
            tooltip(c::drawToolTip, a.ldic$getResetHover(), I18n.format("fml.configgui.tooltip.resetAll"), mouseX, mouseY);
            tooltip(c::drawToolTip, a.ldic$getCheckBoxHover(), I18n.format("fml.configgui.tooltip.applyGlobally"), mouseX, mouseY);
        } else if (gui instanceof GuiEditArray) {
            GuiEditArray e = (GuiEditArray) gui;
            AccessorGuiEditArray a = (AccessorGuiEditArray) gui;
            if (a.ldic$getEntries() == null) {
                return;
            }
            ((AccessorGuiEditArrayEntries) a.ldic$getEntries()).ldic$drawScreenPost(mouseX, mouseY, partialTicks);
            if (a.ldic$getTooltipHover() != null && a.ldic$getTooltipHover().checkHover(mouseX, mouseY)) {
                e.drawToolTip(a.ldic$getTooltip(), mouseX, mouseY);
            }
        } else if (gui instanceof GuiSelectString) {
            GuiSelectString s = (GuiSelectString) gui;
            AccessorGuiSelectString a = (AccessorGuiSelectString) gui;
            if (a.ldic$getTooltipHover() != null && a.ldic$getTooltipHover().checkHover(mouseX, mouseY)) {
                s.drawToolTip(a.ldic$getTooltip(), mouseX, mouseY);
            }
        }
    }

    private interface TooltipDrawer {
        void draw(List<String> lines, int x, int y);
    }

    private static void tooltip(TooltipDrawer drawer, HoverChecker hover, String text, int mouseX, int mouseY) {
        if (hover != null && hover.checkHover(mouseX, mouseY)) {
            drawer.draw(Arrays.asList(text.split("\n")), mouseX, mouseY);
        }
    }
}
