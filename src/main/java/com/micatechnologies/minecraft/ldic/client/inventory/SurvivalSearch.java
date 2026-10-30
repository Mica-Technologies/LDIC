package com.micatechnologies.minecraft.ldic.client.inventory;

import com.micatechnologies.minecraft.ldic.inventory.ItemQuery;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiContainer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiHopper;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiDispenser;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.gui.inventory.GuiShulkerBox;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiContainerEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

/**
 * Search and sort for survival inventories and chests.
 *
 * <p>A small search box sits just above the inventory panel (Ctrl+F or a click to type, right
 * click to clear). While it holds a query, slots whose item does not match are dimmed and
 * matching ones outlined, using the creative search syntax ({@code @mod}, {@code #tooltip},
 * {@code -exclude}). The query stays for the session, so it carries from chest to chest.</p>
 *
 * <p>The sort button beside it orders the player's main inventory (on the inventory screen) or
 * the chest's own slots (chests, shulker boxes, hoppers, dispensers and droppers) by item, and
 * merges part stacks. It does that with ordinary slot clicks, the same packets a player's
 * clicks send, so it is safe on any server; it does nothing while a stack is held.</p>
 */
public final class SurvivalSearch {

    private static final int BOX_W = 96;
    private static final int BOX_H = 12;
    private static final int SORT_W = 14;

    private static SurvivalSearch instance;
    private static String query = "";
    private GuiTextField field;
    private GuiScreen fieldFor;

    public SurvivalSearch() {
        instance = this;
    }

    public static SurvivalSearch instance() {
        return instance;
    }

    private static boolean applies(GuiScreen gui) {
        return gui instanceof GuiContainer && !(gui instanceof GuiContainerCreative) && !(gui instanceof LdicCreativeScreen);
    }

    private static boolean sortable(GuiScreen gui) {
        return gui instanceof GuiInventory || gui instanceof GuiChest || gui instanceof GuiShulkerBox
                || gui instanceof GuiHopper || gui instanceof GuiDispenser;
    }

    /** The box's top-left corner: just above the panel, or just below it when there is no room. */
    private static int[] box(GuiContainer gui) {
        AccessorGuiContainer a = (AccessorGuiContainer) gui;
        int x = a.ldic$getGuiLeft() + a.ldic$getXSize() - BOX_W;
        int top = a.ldic$getGuiTop();
        // 9 clear pixels above the panel, so a themed panel's border never touches it.
        int y = top >= BOX_H + 11 ? top - BOX_H - 9 : top + a.ldic$getYSize() + 9;
        return new int[] {x, y};
    }

    private GuiTextField field(GuiContainer gui) {
        int[] at = box(gui);
        if (field == null || fieldFor != gui) {
            field = new GuiTextField(0, Minecraft.getMinecraft().fontRenderer, 0, 0, BOX_W - 8, BOX_H);
            field.setEnableBackgroundDrawing(false);
            field.setMaxStringLength(50);
            field.setTextColor(0xFFFFFF);
            field.setText(query);
            fieldFor = gui;
        }
        field.x = at[0] + 4;
        field.y = at[1] + 2;
        return field;
    }

    public boolean isTyping(GuiScreen gui) {
        return field != null && fieldFor == gui && field.isFocused();
    }

    // ------------------------------------------------------------------------------ input

    /** From {@code MixinGuiContainer}: true when the click was on the box or the sort button. */
    public boolean click(GuiScreen screen, int mouseX, int mouseY, int button) {
        if (!applies(screen)) {
            return false;
        }
        GuiContainer gui = (GuiContainer) screen;
        int[] at = box(gui);
        GuiTextField f = field(gui);
        if (mouseX >= at[0] && mouseY >= at[1] && mouseX < at[0] + BOX_W && mouseY < at[1] + BOX_H) {
            if (button == 1) {
                f.setText("");
                query = "";
            }
            f.setFocused(true);
            f.mouseClicked(mouseX, mouseY, 0);
            return true;
        }
        f.setFocused(false);
        if (sortable(gui) && button == 0 && mouseX >= at[0] - SORT_W - 2 && mouseY >= at[1]
                && mouseX < at[0] - 2 && mouseY < at[1] + BOX_H) {
            sort(gui);
            return true;
        }
        return false;
    }

    /** From {@code MixinGuiContainer} before the screen sees a key; true when the box took it. */
    public boolean key(GuiScreen gui, char c, int key) {
        if (!applies(gui)) {
            return false;
        }
        GuiTextField f = field((GuiContainer) gui);
        if (f.isFocused()) {
            if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_RETURN) {
                f.setFocused(false);
            } else {
                f.textboxKeyTyped(c, key);
                query = f.getText();
            }
            return true;
        }
        if (key == Keyboard.KEY_F && GuiScreen.isCtrlKeyDown()) {
            f.setFocused(true);
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------------------- drawing

    /** Dims slots whose item does not match the query, outlines those that do. */
    @SubscribeEvent
    public void onForeground(GuiContainerEvent.DrawForeground event) {
        GuiContainer gui = event.getGuiContainer();
        if (!applies(gui) || query.trim().isEmpty()) {
            return;
        }
        ItemQuery q = ItemQuery.parse(query);
        int accent = InventoryStyle.accent();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0, 300);
        for (Slot s : gui.inventorySlots.inventorySlots) {
            if (!s.isEnabled()) {
                continue;
            }
            ItemStack stack = s.getStack();
            boolean match = !stack.isEmpty() && q.matches(new StackFacts(stack));
            if (match) {
                InventoryStyle.outline(s.xPos - 1, s.yPos - 1, s.xPos + 17, s.yPos + 17, accent);
            } else {
                Gui.drawRect(s.xPos, s.yPos, s.xPos + 16, s.yPos + 16, 0xB0101418);
            }
        }
        GlStateManager.popMatrix();
        GlStateManager.enableDepth();
    }

    @SubscribeEvent
    public void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        GuiScreen screen = event.getGui();
        if (!applies(screen)) {
            return;
        }
        GuiContainer gui = (GuiContainer) screen;
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRenderer;
        int[] at = box(gui);
        GuiTextField f = field(gui);
        int mx = event.getMouseX();
        int my = event.getMouseY();
        boolean hover = mx >= at[0] && my >= at[1] && mx < at[0] + BOX_W && my < at[1] + BOX_H;
        int accent = InventoryStyle.accent();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        boolean active = f.isFocused() || !f.getText().isEmpty();
        InventoryStyle.panel(at[0], at[1], at[0] + BOX_W, at[1] + BOX_H, active ? 0xD0000000 : 0x90000000,
                f.isFocused() ? accent : hover ? 0x60FFFFFF : InventoryStyle.PANEL_EDGE);
        if (f.getText().isEmpty() && !f.isFocused()) {
            font.drawString(I18n.format("ldic.survival.search"), at[0] + 4, at[1] + 2, InventoryStyle.TEXT_DIM);
        }
        f.drawTextBox();
        if (sortable(gui)) {
            int sx = at[0] - SORT_W - 2;
            boolean sortHover = mx >= sx && my >= at[1] && mx < sx + SORT_W && my < at[1] + BOX_H;
            InventoryStyle.panel(sx, at[1], sx + SORT_W, at[1] + BOX_H, sortHover ? 0xD0202830 : 0x90000000,
                    sortHover ? accent : InventoryStyle.PANEL_EDGE);
            int c = sortHover ? accent : InventoryStyle.TEXT;
            // Three bars, shortest last: "sort".
            Gui.drawRect(sx + 3, at[1] + 3, sx + 11, at[1] + 4, c);
            Gui.drawRect(sx + 3, at[1] + 6, sx + 9, at[1] + 7, c);
            Gui.drawRect(sx + 3, at[1] + 9, sx + 7, at[1] + 10, c);
            if (sortHover) {
                gui.drawHoveringText(Arrays.asList(I18n.format("ldic.survival.sort"),
                        "§7" + I18n.format(gui instanceof GuiInventory ? "ldic.survival.sort.inventory" : "ldic.survival.sort.container")),
                        mx, my);
            }
        }
        if (hover && !active) {
            gui.drawHoveringText(Arrays.asList(I18n.format("ldic.survival.search.tip"),
                    "§7" + I18n.format("ldic.creative.search.syntax")), mx, my);
        }
        GlStateManager.enableDepth();
    }

    // ------------------------------------------------------------------------------- sort

    /** The slots the sort button orders: the main inventory, or the chest's own slots. */
    private static List<Slot> sortSlots(GuiContainer gui) {
        List<Slot> out = new ArrayList<>();
        for (Slot s : gui.inventorySlots.inventorySlots) {
            boolean player = s.inventory instanceof InventoryPlayer;
            if (gui instanceof GuiInventory) {
                if (player && s.getSlotIndex() >= 9 && s.getSlotIndex() < 36) {
                    out.add(s);
                }
            } else if (!player) {
                out.add(s);
            }
        }
        return out;
    }

    private static final Comparator<ItemStack> ORDER = Comparator
            .comparing((ItemStack s) -> {
                ResourceLocation id = s.getItem().getRegistryName();
                return id == null ? "" : id.toString();
            })
            .thenComparingInt(ItemStack::getMetadata)
            .thenComparing(s -> s.hasTagCompound() ? s.getTagCompound().toString() : "")
            .thenComparing(Comparator.comparingInt(ItemStack::getCount).reversed());

    /** Merges part stacks, then orders the slots, one ordinary click at a time. */
    static void sort(GuiContainer gui) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null || !player.inventory.getItemStack().isEmpty()) {
            return;
        }
        Container c = gui.inventorySlots;
        List<Slot> slots = sortSlots(gui);
        // Merge: fold each later part stack into the first earlier one of the same item.
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                ItemStack a = slots.get(i).getStack();
                ItemStack b = slots.get(j).getStack();
                if (a.isEmpty() || b.isEmpty() || !a.isStackable() || a.getCount() >= a.getMaxStackSize()
                        || !ItemStack.areItemsEqual(a, b) || !ItemStack.areItemStackTagsEqual(a, b)) {
                    continue;
                }
                click(mc, c, slots.get(j));
                click(mc, c, slots.get(i));
                if (!player.inventory.getItemStack().isEmpty()) {
                    click(mc, c, slots.get(j));
                }
                if (!player.inventory.getItemStack().isEmpty()) {
                    return; // the server disagreed with what we expected; stop rather than guess
                }
            }
        }
        // Order: a selection sort by swaps, so each move is three plain clicks.
        for (int i = 0; i < slots.size(); i++) {
            int best = -1;
            for (int j = i; j < slots.size(); j++) {
                ItemStack s = slots.get(j).getStack();
                if (s.isEmpty()) {
                    continue;
                }
                if (best < 0 || ORDER.compare(s, slots.get(best).getStack()) < 0) {
                    best = j;
                }
            }
            if (best < 0) {
                break;
            }
            if (best == i || sameKind(slots.get(i).getStack(), slots.get(best).getStack())) {
                continue;
            }
            boolean targetEmpty = slots.get(i).getStack().isEmpty();
            click(mc, c, slots.get(best));
            click(mc, c, slots.get(i));
            if (!targetEmpty) {
                click(mc, c, slots.get(best));
            }
            if (!player.inventory.getItemStack().isEmpty()) {
                return;
            }
        }
        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private static boolean sameKind(ItemStack a, ItemStack b) {
        return !a.isEmpty() && ItemStack.areItemsEqual(a, b) && ItemStack.areItemStackTagsEqual(a, b);
    }

    private static void click(Minecraft mc, Container c, Slot s) {
        mc.playerController.windowClick(c.windowId, s.slotNumber, 0, ClickType.PICKUP, mc.player);
    }
}
