package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.api.MenuEntry;
import com.micatechnologies.minecraft.ldic.client.inventory.InventoryStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;

/**
 * A mod's {@link MenuEntry} as a button, for every screen whose theme does not draw entries
 * itself: vanilla's look on a vanilla screen, LDIC's flat look (with the theme's accent) on a
 * themed one, where it goes into the theme's area for other mods' buttons.
 */
final class ModEntryButton extends GuiButton {

    final MenuEntry entry;
    private final boolean themed;

    ModEntryButton(int id, int x, int y, MenuEntry entry, boolean themed) {
        super(id, x, y, 150, 20, ModMenus.label(entry));
        this.entry = entry;
        this.themed = themed;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!visible) {
            return;
        }
        displayString = ModMenus.label(entry);
        enabled = ModMenus.enabled(entry);
        if (!themed) {
            super.drawButton(mc, mouseX, mouseY, partialTicks);
            return;
        }
        hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        boolean hot = hovered && enabled;
        int accent = InventoryStyle.accent();
        Gui.drawRect(x, y, x + width, y + height, hot ? 0x60FFFFFF : 0x30FFFFFF);
        Gui.drawRect(x, y, x + 2, y + height, InventoryStyle.alpha(accent, hot ? 0xFF : 0x90));
        String text = mc.fontRenderer.trimStringToWidth(displayString, width - 10);
        int colour = !enabled ? 0xFF808890 : hot ? 0xFFFFFFFF : 0xFFE0E4E8;
        GlStateManager.enableBlend();
        mc.fontRenderer.drawString(text, x + (width - mc.fontRenderer.getStringWidth(text)) / 2, y + (height - 8) / 2, colour);
        GlStateManager.color(1, 1, 1, 1);
    }
}
