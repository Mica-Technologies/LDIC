package com.micatechnologies.minecraft.ldic.client.inventory;

import com.micatechnologies.minecraft.ldic.inventory.CreativeLayout;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

/**
 * The full-screen creative inventory's slots: an item grid of whatever size the screen allows,
 * and the player's armour, offhand, main inventory and hotbar. Player slots read and write the
 * real inventory, and every click on them goes through the player's own container (as vanilla's
 * Inventory tab does), whose creative listener sends each change to the server.
 */
public final class LdicCreativeContainer extends Container {

    /** A grid cell: shows one of {@link #items}, never holds anything put into it. */
    public static final class GridSlot extends Slot {
        GridSlot(InventoryBasic inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            ItemStack s = getStack();
            // Saved Toolbars' "how to save" placeholders.
            return s.isEmpty() || s.getSubCompound("CustomCreativeLock") == null;
        }
    }

    /** One of the player container's slots, placed somewhere else on screen. */
    public static final class PlayerSlot extends Slot {
        public final Slot target;

        PlayerSlot(Slot target, int x, int y) {
            super(target.inventory, target.getSlotIndex(), x, y);
            this.target = target;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return target.isItemValid(stack);
        }

        @Override
        public int getSlotStackLimit() {
            return target.getSlotStackLimit();
        }

        @Override
        public int getItemStackLimit(ItemStack stack) {
            return target.getItemStackLimit(stack);
        }

        @Nullable
        @Override
        public String getSlotTexture() {
            return target.getSlotTexture();
        }

        @Nullable
        @Override
        public TextureAtlasSprite getBackgroundSprite() {
            return target.getBackgroundSprite();
        }

        @Override
        public ResourceLocation getBackgroundLocation() {
            return target.getBackgroundLocation();
        }

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            return target.canTakeStack(player);
        }
    }

    // Player container slot numbers (ContainerPlayer): 5-8 armour, 9-35 main, 36-44 hotbar, 45 offhand.
    private static final int ARMOR = 5;
    private static final int MAIN = 9;
    private static final int HOTBAR = 36;
    private static final int OFFHAND = 45;

    private final EntityPlayer player;
    public final NonNullList<ItemStack> items = NonNullList.create();
    private InventoryBasic grid = new InventoryBasic("ldic_creative", false, 1);
    private int cols = 9;
    private int rows = 5;
    private int scrollRow;

    public LdicCreativeContainer(EntityPlayer player) {
        this.player = player;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    /** Lays the slots out again for a new screen size. */
    public void layout(CreativeLayout l) {
        inventorySlots.clear();
        inventoryItemStacks.clear();
        cols = l.cols;
        rows = l.rows;
        grid = new InventoryBasic("ldic_creative", false, cols * rows);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                addSlotToContainer(new GridSlot(grid, r * cols + c, l.gridX + c * CreativeLayout.CELL, l.gridY + r * CreativeLayout.CELL));
            }
        }
        Container p = player.inventoryContainer;
        for (int i = 0; i < 4; i++) {
            addSlotToContainer(new PlayerSlot(p.getSlot(ARMOR + i), l.armorX, l.armorY + i * CreativeLayout.CELL));
        }
        addSlotToContainer(new PlayerSlot(p.getSlot(OFFHAND), l.offhandX, l.offhandY));
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlotToContainer(new PlayerSlot(p.getSlot(MAIN + r * 9 + c), l.mainX + c * CreativeLayout.CELL, l.mainY + r * CreativeLayout.CELL));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlotToContainer(new PlayerSlot(p.getSlot(HOTBAR + c), l.hotbarX + c * CreativeLayout.CELL, l.hotbarY));
        }
        scrollTo(scrollRow);
    }

    public int cols() {
        return cols;
    }

    public int rows() {
        return rows;
    }

    public int scrollRow() {
        return scrollRow;
    }

    /** Rows past the first screenful; 0 when everything fits. */
    public int maxScroll() {
        return Math.max(0, (items.size() + cols - 1) / cols - rows);
    }

    public void scrollTo(int row) {
        scrollRow = Math.max(0, Math.min(maxScroll(), row));
        for (int i = 0; i < cols * rows; i++) {
            int index = scrollRow * cols + i;
            grid.setInventorySlotContents(i, index < items.size() ? items.get(index) : ItemStack.EMPTY);
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slot) {
        return !(slot instanceof GridSlot);
    }

    @Override
    public boolean canDragIntoSlot(Slot slot) {
        return !(slot instanceof GridSlot);
    }
}
