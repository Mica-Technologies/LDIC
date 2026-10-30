package com.micatechnologies.minecraft.ldic.client.inventory;

import com.micatechnologies.minecraft.ldic.client.inventory.LdicCreativeContainer.GridSlot;
import com.micatechnologies.minecraft.ldic.client.inventory.LdicCreativeContainer.PlayerSlot;
import com.micatechnologies.minecraft.ldic.client.menu.InventoryTheme;
import com.micatechnologies.minecraft.ldic.inventory.CreativeLayout;
import com.micatechnologies.minecraft.ldic.inventory.CreativeLayout.Rect;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.CreativeCrafting;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * The full-screen creative inventory (Decision 14): every tab in a sidebar (no tab pages, so
 * modpacks with dozens of tabs stay usable), one search box, an item grid as large as the
 * screen allows, and the whole player inventory beside it, so items go straight where they are
 * wanted. Opened in place of vanilla's screen by {@link InventoryScreens} when full-screen mode
 * is on; falls back to vanilla's when the screen is too small.
 *
 * <p>Clicks on the grid follow vanilla's creative rules (click picks up a stack, middle click a
 * full stack, number keys fill a hotbar slot, Q throws), except that shift-click puts a full
 * stack straight into the inventory. Clicks on player slots go through the player's own
 * container, as vanilla's Inventory tab does.</p>
 */
public final class LdicCreativeScreen extends GuiContainer {

    private static final int ROW_H = 18;
    /** The tab picked last time, by label, so reopening returns to it. */
    private static String lastTab;

    private final LdicCreativeContainer container;
    private final List<CreativeTabs> tabs = new ArrayList<>();
    private CreativeLayout layout;
    private CreativeTabs tab;
    private GuiTextField search;
    private CreativeCrafting listener;
    private int sidebarScroll;
    private boolean draggingScrollbar;

    public LdicCreativeScreen(EntityPlayer player) {
        super(new LdicCreativeContainer(player));
        container = (LdicCreativeContainer) inventorySlots;
        allowUserInput = true;
        for (CreativeTabs t : CreativeTabs.CREATIVE_TAB_ARRAY) {
            if (t != null && t != CreativeTabs.INVENTORY && t != CreativeTabs.SEARCH && t != CreativeTabs.HOTBAR) {
                tabs.add(t);
            }
        }
        tabs.add(0, CreativeTabs.SEARCH);
        tabs.add(CreativeTabs.HOTBAR);
        for (CreativeTabs t : tabs) {
            if (t.getTabLabel().equals(lastTab)) {
                tab = t;
            }
        }
        if (tab == null) {
            tab = tabs.size() > 1 ? tabs.get(1) : CreativeTabs.SEARCH;
        }
    }

    public boolean isTyping() {
        return search != null && search.isFocused();
    }

    // ----------------------------------------------------------------------------- lifecycle

    @Override
    public void initGui() {
        if (!mc.playerController.isInCreativeMode()) {
            mc.displayGuiScreen(new GuiInventory(mc.player));
            return;
        }
        layout = CreativeLayout.compute(width, height);
        if (layout == null) {
            // Too small now (window shrunk): vanilla's screen; GuiOpenEvent will not swap it back.
            mc.displayGuiScreen(new GuiContainerCreative(mc.player));
            return;
        }
        xSize = width;
        ySize = height;
        super.initGui();
        buttonList.clear();
        Keyboard.enableRepeatEvents(true);
        String text = search == null ? "" : search.getText();
        boolean focused = search != null ? search.isFocused() : tab == CreativeTabs.SEARCH;
        Rect s = layout.search;
        search = new GuiTextField(0, fontRenderer, s.x + 4, s.y + 2, s.w - 8 - 30, fontRenderer.FONT_HEIGHT);
        search.setEnableBackgroundDrawing(false);
        search.setMaxStringLength(50);
        search.setTextColor(0xFFFFFF);
        search.setText(text);
        search.setFocused(focused);
        if (listener == null) {
            listener = new CreativeCrafting(mc);
            mc.player.inventoryContainer.addListener(listener);
        }
        container.layout(layout);
        rebuild();
        scrollSidebarTo(tab);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        if (mc.player != null && listener != null) {
            mc.player.inventoryContainer.removeListener(listener);
        }
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        if (!mc.playerController.isInCreativeMode()) {
            mc.displayGuiScreen(new GuiInventory(mc.player));
            return;
        }
        if (search != null) {
            search.updateCursorCounter();
        }
    }

    private void rebuild() {
        CreativeSearch.build(tab, container.items, tab == CreativeTabs.HOTBAR ? "" : search.getText(), container.cols());
        container.scrollTo(0);
    }

    private void selectTab(CreativeTabs t) {
        if (t == tab) {
            return;
        }
        tab = t;
        lastTab = t.getTabLabel();
        if (t == CreativeTabs.SEARCH) {
            search.setFocused(true);
        }
        rebuild();
    }

    private int sidebarRows() {
        return Math.max(1, (layout.sidebar.h - 8) / ROW_H);
    }

    private void scrollSidebarTo(CreativeTabs t) {
        int i = tabs.indexOf(t);
        int rows = sidebarRows();
        if (i < sidebarScroll) {
            sidebarScroll = i;
        } else if (i >= sidebarScroll + rows) {
            sidebarScroll = i - rows + 1;
        }
        sidebarScroll = Math.max(0, Math.min(Math.max(0, tabs.size() - rows), sidebarScroll));
    }

    /** The tab under (x, y) in the sidebar, or null. */
    @Nullable
    private CreativeTabs tabAt(int x, int y) {
        Rect sb = layout.sidebar;
        if (!sb.contains(x, y) || y < sb.y + 4) {
            return null;
        }
        int row = (y - sb.y - 4) / ROW_H;
        int i = sidebarScroll + row;
        return row < sidebarRows() && i < tabs.size() ? tabs.get(i) : null;
    }

    private boolean overTrash(int x, int y) {
        return x >= layout.trashX - 1 && y >= layout.trashY - 1 && x < layout.trashX + 17 && y < layout.trashY + 17;
    }

    // ----------------------------------------------------------------------------- drawing

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (layout == null) {
            return;
        }
        if (draggingScrollbar) {
            if (Mouse.isButtonDown(0)) {
                scrollFromMouse(mouseY);
            } else {
                draggingScrollbar = false;
            }
        }
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        GlStateManager.disableLighting();
        List<String> tip = tooltipAt(mouseX, mouseY);
        if (tip != null) {
            drawHoveringText(tip, mouseX, mouseY);
        } else {
            renderHoveredToolTip(mouseX, mouseY);
        }
    }

    @Nullable
    private List<String> tooltipAt(int x, int y) {
        if (layout.sort.contains(x, y)) {
            return CreativeSearch.sortTooltip(tab, container.items.size());
        }
        if (layout.toggle.contains(x, y)) {
            return Arrays.asList(I18n.format("ldic.inventory.full.exit"), "§7" + InventoryScreens.instance().keyHint());
        }
        if (overTrash(x, y) && mc.player.inventory.getItemStack().isEmpty()) {
            return Arrays.asList(I18n.format("inventory.binSlot"), "§7" + I18n.format("ldic.creative.trash.clear"));
        }
        CreativeTabs t = tabAt(x, y);
        if (t != null && layout.compact) {
            return Collections.singletonList(I18n.format(t.getTranslationKey()));
        }
        return null;
    }

    @Override
    protected boolean hasClickedOutside(int x, int y, int left, int top) {
        CreativeLayout l = layout;
        return !(l.top.contains(x, y) || l.sidebar.contains(x, y) || l.grid.contains(x, y) || l.player.contains(x, y));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        CreativeLayout l = layout;
        int accent = InventoryStyle.accent();
        GlStateManager.color(1, 1, 1, 1);
        InventoryTheme theme = InventoryTheme.of(this);
        if (theme == null || !theme.draw(mouseX, mouseY, areas())) {
            drawChrome(mouseX, mouseY, accent);
        }
        drawControls(mouseX, mouseY, accent);
    }

    /** The named areas a {@code creative} theme screen lays itself out by. */
    private Map<String, double[]> areas() {
        CreativeLayout l = layout;
        Map<String, double[]> areas = new HashMap<>();
        areas.put("panel", new double[] {0, 0, width, height});
        for (Map.Entry<String, Rect> e : namedRects().entrySet()) {
            Rect r = e.getValue();
            areas.put(e.getKey(), new double[] {r.x, r.y, r.w, r.h});
        }
        areas.put("model", new double[] {l.armorX + 19, l.armorY - 1, l.offhandX - 3 - (l.armorX + 19), 4 * CreativeLayout.CELL - 2});
        areas.put("trash", new double[] {l.trashX - 1, l.trashY - 1, 18, 18});
        return areas;
    }

    private Map<String, Rect> namedRects() {
        Map<String, Rect> m = new HashMap<>();
        m.put("top", layout.top);
        m.put("sidebar", layout.sidebar);
        m.put("browser", layout.grid);
        m.put("inventory", layout.player);
        m.put("search", layout.search);
        m.put("scrollbar", layout.scrollbar);
        return m;
    }

    /** LDIC's own look, when the theme has no {@code creative} screen: panels, cells, labels, model. */
    private void drawChrome(int mouseX, int mouseY, int accent) {
        CreativeLayout l = layout;
        panel(l.top);
        panel(l.sidebar);
        panel(l.grid);
        panel(l.player);
        if (!l.compact) {
            String title = fontRenderer.trimStringToWidth(I18n.format(tab.getTranslationKey()), l.sidebar.w - 10);
            fontRenderer.drawString(title, l.top.x + 6, l.top.y + 6, InventoryStyle.TEXT);
        }
        for (Slot slot : container.inventorySlots) {
            InventoryStyle.cell(slot.xPos, slot.yPos);
        }
        fontRenderer.drawString(I18n.format("container.inventory"), l.player.x + 6, l.player.y + 5, InventoryStyle.TEXT_DIM);
        int selected = mc.player.inventory.currentItem;
        int hx = l.hotbarX + selected * CreativeLayout.CELL;
        InventoryStyle.outline(hx - 1, l.hotbarY - 1, hx + 17, l.hotbarY + 17, accent);
        Gui.drawRect(l.armorX + 19, l.armorY - 1, l.offhandX - 3, l.armorY + 4 * CreativeLayout.CELL - 3, 0x30000000);
        GlStateManager.color(1, 1, 1, 1);
        GuiInventory.drawEntityOnScreen(l.modelX, l.modelY, 30, l.modelX - mouseX, l.modelY - 50 - mouseY, mc.player);
    }

    /** What works the screen, themed or not: search, sort, toggle, tabs, scrollbar, trash. */
    private void drawControls(int mouseX, int mouseY, int accent) {
        CreativeLayout l = layout;
        Rect s = l.search;
        boolean searchHover = s.contains(mouseX, mouseY);
        InventoryStyle.panel(s.x, s.y, s.right(), s.bottom(), 0xA0000000,
                search.isFocused() ? accent : searchHover ? 0x60FFFFFF : InventoryStyle.PANEL_EDGE);
        if (search.getText().isEmpty() && !search.isFocused()) {
            String hint = I18n.format("ldic.creative.search.hint", I18n.format(tab.getTranslationKey()))
                    + "   " + I18n.format("ldic.creative.search.syntax");
            fontRenderer.drawString(fontRenderer.trimStringToWidth(hint, s.w - 40), s.x + 4, s.y + 2, InventoryStyle.TEXT_DIM);
        }
        search.drawTextBox();
        String count = String.valueOf(container.items.size());
        fontRenderer.drawString(count, s.right() - 4 - fontRenderer.getStringWidth(count), s.y + 2, InventoryStyle.TEXT_DIM);
        CreativeSearch.drawSortButtonAt(fontRenderer, tab, l.sort.x, l.sort.y, l.sort.contains(mouseX, mouseY));
        InventoryScreens.drawToggle(l.toggle.x, l.toggle.y, l.toggle.w, l.toggle.contains(mouseX, mouseY), true);

        drawSidebar(mouseX, mouseY, accent);
        if (container.items.isEmpty()) {
            String none = I18n.format("ldic.creative.search.none");
            fontRenderer.drawString(none, l.grid.x + (l.grid.w - fontRenderer.getStringWidth(none)) / 2, l.grid.y + 30, InventoryStyle.TEXT_DIM);
        }
        drawScrollbar(mouseX, mouseY, accent);
        drawTrash(mouseX, mouseY);
    }

    private static void panel(Rect r) {
        InventoryStyle.panel(r.x, r.y, r.right(), r.bottom());
    }

    private void drawSidebar(int mouseX, int mouseY, int accent) {
        Rect sb = layout.sidebar;
        int rows = sidebarRows();
        RenderHelper.enableGUIStandardItemLighting();
        for (int row = 0; row < rows && sidebarScroll + row < tabs.size(); row++) {
            CreativeTabs t = tabs.get(sidebarScroll + row);
            int y = sb.y + 4 + row * ROW_H;
            boolean hover = mouseX >= sb.x && mouseX < sb.right() && mouseY >= y && mouseY < y + ROW_H;
            if (t == tab) {
                Gui.drawRect(sb.x + 2, y, sb.right() - 2, y + ROW_H, InventoryStyle.alpha(accent, 0x40));
                Gui.drawRect(sb.x + 2, y, sb.x + 4, y + ROW_H, accent);
            } else if (hover) {
                Gui.drawRect(sb.x + 2, y, sb.right() - 2, y + ROW_H, 0x20FFFFFF);
            }
            ItemStack icon = t.getIcon();
            if (!icon.isEmpty()) {
                RenderHelper.enableGUIStandardItemLighting();
                itemRender.renderItemAndEffectIntoGUI(icon, sb.x + 5, y + 1);
                RenderHelper.disableStandardItemLighting();
            }
            if (!layout.compact) {
                String name = fontRenderer.trimStringToWidth(I18n.format(t.getTranslationKey()), sb.w - 30);
                fontRenderer.drawString(name, sb.x + 25, y + 5, t == tab ? InventoryStyle.TEXT : 0xFFC0C8D0);
            }
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        if (tabs.size() > rows) {
            // A thin bar on the sidebar's right edge shows where the list is scrolled to.
            int trackH = rows * ROW_H;
            int thumbH = Math.max(8, trackH * rows / tabs.size());
            int ty = sb.y + 4 + (trackH - thumbH) * sidebarScroll / Math.max(1, tabs.size() - rows);
            Gui.drawRect(sb.right() - 3, ty, sb.right() - 1, ty + thumbH, 0x60FFFFFF);
        }
    }

    private void drawScrollbar(int mouseX, int mouseY, int accent) {
        Rect sb = layout.scrollbar;
        Gui.drawRect(sb.x, sb.y, sb.right(), sb.bottom(), 0x40000000);
        int max = container.maxScroll();
        if (max == 0) {
            return;
        }
        int total = max + container.rows();
        int thumbH = Math.max(12, sb.h * container.rows() / total);
        int ty = sb.y + (sb.h - thumbH) * container.scrollRow() / max;
        boolean hover = draggingScrollbar || sb.contains(mouseX, mouseY);
        Gui.drawRect(sb.x, ty, sb.right(), ty + thumbH, hover ? accent : InventoryStyle.alpha(accent, 0xB0));
    }

    private void drawTrash(int mouseX, int mouseY) {
        int x = layout.trashX;
        int y = layout.trashY;
        boolean hover = overTrash(mouseX, mouseY);
        Gui.drawRect(x - 1, y - 1, x + 17, y + 17, hover ? 0x60C04040 : 0x40C04040);
        int c = hover ? 0xFFFF8080 : 0xFFD07070;
        for (int i = 0; i < 8; i++) {
            Gui.drawRect(x + 4 + i, y + 4 + i, x + 5 + i, y + 5 + i, c);
            Gui.drawRect(x + 11 - i, y + 4 + i, x + 12 - i, y + 5 + i, c);
        }
    }

    // ----------------------------------------------------------------------------- input

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        CreativeLayout l = layout;
        if (l.search.contains(mouseX, mouseY)) {
            if (button == 1 && !search.getText().isEmpty()) {
                search.setText("");
                rebuild();
            }
            search.setFocused(true);
            search.mouseClicked(mouseX, mouseY, 0);
            return;
        }
        search.setFocused(false);
        if (l.sort.contains(mouseX, mouseY) && (button == 0 || button == 1)) {
            CreativeSearch.clickSort(tab, button);
            rebuild();
            return;
        }
        if (l.toggle.contains(mouseX, mouseY) && button == 0) {
            InventoryScreens.instance().toggle(this);
            return;
        }
        CreativeTabs t = tabAt(mouseX, mouseY);
        if (t != null) {
            if (t != tab) {
                mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                selectTab(t);
            }
            return;
        }
        if (l.scrollbar.contains(mouseX, mouseY) && button == 0) {
            draggingScrollbar = true;
            scrollFromMouse(mouseY);
            return;
        }
        if (overTrash(mouseX, mouseY)) {
            clickTrash();
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    private void clickTrash() {
        InventoryPlayer inv = mc.player.inventory;
        if (!inv.getItemStack().isEmpty()) {
            inv.setItemStack(ItemStack.EMPTY);
        } else if (isShiftKeyDown()) {
            for (int i = 0; i < mc.player.inventoryContainer.getInventory().size(); i++) {
                mc.playerController.sendSlotPacket(ItemStack.EMPTY, i);
            }
        }
    }

    private void scrollFromMouse(int mouseY) {
        Rect sb = layout.scrollbar;
        double t = (mouseY - sb.y - 6) / (double) Math.max(1, sb.h - 12);
        container.scrollTo((int) Math.round(Math.max(0, Math.min(1, t)) * container.maxScroll()));
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0 || layout == null) {
            return;
        }
        int x = Mouse.getEventX() * width / mc.displayWidth;
        int y = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        int step = wheel > 0 ? -1 : 1;
        if (layout.sidebar.contains(x, y)) {
            sidebarScroll = Math.max(0, Math.min(Math.max(0, tabs.size() - sidebarRows()), sidebarScroll + step));
        } else {
            container.scrollTo(container.scrollRow() + step * (isShiftKeyDown() ? container.rows() : 1));
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (search.isFocused()) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
                search.setFocused(false);
                return;
            }
            String before = search.getText();
            if (search.textboxKeyTyped(typedChar, keyCode)) {
                if (!before.equals(search.getText())) {
                    rebuild();
                }
                return;
            }
        } else if ((keyCode == Keyboard.KEY_F && isCtrlKeyDown()) || mc.gameSettings.keyBindChat.isActiveAndMatches(keyCode)) {
            search.setFocused(true);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void handleMouseClick(@Nullable Slot slot, int slotId, int button, ClickType type) {
        InventoryPlayer inv = mc.player.inventory;
        if (slot == null) {
            if (slotId == -999 && (type == ClickType.PICKUP || type == ClickType.THROW) && !inv.getItemStack().isEmpty()) {
                if (button == 0) {
                    mc.player.dropItem(inv.getItemStack(), true);
                    mc.playerController.sendPacketDropItem(inv.getItemStack());
                    inv.setItemStack(ItemStack.EMPTY);
                } else if (button == 1) {
                    ItemStack one = inv.getItemStack().splitStack(1);
                    mc.player.dropItem(one, true);
                    mc.playerController.sendPacketDropItem(one);
                }
            } else if (type == ClickType.QUICK_CRAFT) {
                mc.player.inventoryContainer.slotClick(slotId, button, type, mc.player);
                mc.player.inventoryContainer.detectAndSendChanges();
            }
            return;
        }
        if (!slot.canTakeStack(mc.player)) {
            return;
        }
        if (slot instanceof GridSlot) {
            clickGrid(slot, button, type, inv);
        } else if (slot instanceof PlayerSlot) {
            Slot target = ((PlayerSlot) slot).target;
            if (type == ClickType.THROW && slot.getHasStack()) {
                ItemStack thrown = target.decrStackSize(button == 0 ? 1 : target.getStack().getMaxStackSize());
                mc.player.dropItem(thrown, true);
                mc.playerController.sendPacketDropItem(thrown);
                mc.playerController.sendSlotPacket(target.getStack(), target.slotNumber);
            } else {
                mc.player.inventoryContainer.slotClick(target.slotNumber, button, type, mc.player);
                mc.player.inventoryContainer.detectAndSendChanges();
            }
        }
    }

    /** Vanilla's creative rules for its item grid, but shift-click fills the inventory. */
    private void clickGrid(Slot slot, int button, ClickType type, InventoryPlayer inv) {
        ItemStack held = inv.getItemStack();
        ItemStack shown = slot.getStack();
        switch (type) {
            case QUICK_CRAFT:
                return;
            case SWAP:
                if (!shown.isEmpty() && button >= 0 && button < 9) {
                    inv.setInventorySlotContents(button, fullStack(shown));
                    mc.player.inventoryContainer.detectAndSendChanges();
                }
                return;
            case CLONE:
                if (held.isEmpty() && !shown.isEmpty()) {
                    inv.setItemStack(fullStack(shown));
                }
                return;
            case THROW:
                if (!shown.isEmpty()) {
                    ItemStack thrown = shown.copy();
                    thrown.setCount(button == 0 ? 1 : thrown.getMaxStackSize());
                    mc.player.dropItem(thrown, true);
                    mc.playerController.sendPacketDropItem(thrown);
                }
                return;
            case QUICK_MOVE:
                if (!shown.isEmpty() && held.isEmpty()) {
                    inv.addItemStackToInventory(fullStack(shown));
                    mc.player.inventoryContainer.detectAndSendChanges();
                    return;
                }
                break;
            default:
                break;
        }
        if (!held.isEmpty() && !shown.isEmpty() && held.isItemEqual(shown) && ItemStack.areItemStackTagsEqual(held, shown)) {
            if (button == 0) {
                if (held.getCount() < held.getMaxStackSize()) {
                    held.grow(1);
                }
            } else {
                held.shrink(1);
            }
        } else if (!shown.isEmpty() && held.isEmpty()) {
            inv.setItemStack(shown.copy());
        } else if (button == 0) {
            inv.setItemStack(ItemStack.EMPTY);
        } else if (!held.isEmpty()) {
            held.shrink(1);
        }
    }

    private static ItemStack fullStack(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(copy.getMaxStackSize());
        return copy;
    }
}
