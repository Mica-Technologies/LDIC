package com.micatechnologies.minecraft.ldic.client.inventory;

import com.micatechnologies.minecraft.ldic.client.LdicClient;
import com.micatechnologies.minecraft.ldic.client.LdicSettings;
import com.micatechnologies.minecraft.ldic.inventory.ItemQuery;
import com.micatechnologies.minecraft.ldic.inventory.ItemSort;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.HotbarSnapshot;
import net.minecraft.client.util.SearchTreeManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * LDIC's pass over a creative tab's item list (Decision 13): vanilla fills the list, then this
 * filters it by the tab's query and sorts it by the tab's chosen order. Also draws the sort
 * button that {@code MixinGuiContainerCreative} places in the panel's top strip.
 */
public final class CreativeSearch {

    /** The sort button, relative to the creative panel's top-left corner. */
    public static final int SORT_X = 173;
    public static final int SORT_Y = 4;
    public static final int SORT_W = 16;
    public static final int SORT_H = 12;

    /** The search box that replaces the title of an item tab, relative to the panel. */
    public static final int BOX_X = 6;
    public static final int BOX_Y = 4;
    public static final int BOX_W = 165;
    public static final int BOX_H = 12;

    private CreativeSearch() {
    }

    /** Tabs that list items: everything except Inventory and Saved Hotbars. */
    public static boolean sortable(CreativeTabs tab) {
        return tab != null && tab != CreativeTabs.INVENTORY && tab != CreativeTabs.HOTBAR;
    }

    /** Item tabs with no search bar of their own get LDIC's search box in place of their title. */
    public static boolean hasOwnSearch(CreativeTabs tab) {
        return sortable(tab) && !tab.hasSearchBar();
    }

    /** Fills {@code list} the way vanilla does for {@code tab}, unfiltered. */
    public static void fill(CreativeTabs tab, NonNullList<ItemStack> list) {
        list.clear();
        if (tab == CreativeTabs.SEARCH) {
            for (Item item : Item.REGISTRY) {
                item.getSubItems(CreativeTabs.SEARCH, list);
            }
        } else {
            tab.displayAllRelevantItems(list);
        }
    }

    /**
     * Filters and sorts a list vanilla has just built for {@code tab}. Tabs with a search bar of
     * their own (Search, and Forge tabs that ask for one) were already filtered by vanilla, which
     * is kept for a single plain word; anything richer is re-run here. LDIC's own boxes always
     * filter here.
     */
    public static void apply(CreativeTabs tab, NonNullList<ItemStack> list, String query) {
        if (!sortable(tab)) {
            return;
        }
        ItemQuery q = ItemQuery.parse(query);
        List<ItemStack> items = new ArrayList<>(list);
        if (!q.isEmpty() && (hasOwnSearch(tab) || !q.isSimple())) {
            if (!hasOwnSearch(tab)) {
                fill(tab, list);
                items = new ArrayList<>(list);
            }
            List<ItemStack> kept = new ArrayList<>(items.size());
            for (ItemStack stack : items) {
                if (!stack.isEmpty() && q.matches(new StackFacts(stack))) {
                    kept.add(stack);
                }
            }
            items = kept;
        }
        items = ItemSort.sort(items, pref(tab), StackFacts::new);
        list.clear();
        list.addAll(items);
    }

    /**
     * Builds a tab's list from scratch for the full-screen inventory: vanilla's contents (its
     * indexed search for a single word on Search Items), then LDIC's filter and order. Saved
     * Toolbars puts each saved hotbar on its own row of {@code cols}.
     */
    public static void build(CreativeTabs tab, NonNullList<ItemStack> list, String query, int cols) {
        list.clear();
        if (tab == CreativeTabs.HOTBAR) {
            savedToolbars(list, cols);
            return;
        }
        ItemQuery q = ItemQuery.parse(query);
        if (tab == CreativeTabs.SEARCH && q.isSimple()) {
            list.addAll(Minecraft.getMinecraft().getSearchTree(SearchTreeManager.ITEMS).search(query.trim().toLowerCase(Locale.ROOT)));
            List<ItemStack> sorted = ItemSort.sort(new ArrayList<>(list), pref(tab), StackFacts::new);
            list.clear();
            list.addAll(sorted);
            return;
        }
        fill(tab, list);
        List<ItemStack> items = new ArrayList<>(list);
        if (!q.isEmpty()) {
            List<ItemStack> kept = new ArrayList<>(items.size());
            for (ItemStack stack : items) {
                if (!stack.isEmpty() && q.matches(new StackFacts(stack))) {
                    kept.add(stack);
                }
            }
            items = kept;
        }
        items = ItemSort.sort(items, pref(tab), StackFacts::new);
        list.clear();
        list.addAll(items);
    }

    private static void savedToolbars(NonNullList<ItemStack> list, int cols) {
        Minecraft mc = Minecraft.getMinecraft();
        for (int j = 0; j < 9; j++) {
            HotbarSnapshot snapshot = mc.creativeSettings.getHotbarSnapshot(j);
            for (int k = 0; k < cols; k++) {
                ItemStack stack = ItemStack.EMPTY;
                if (k < 9) {
                    if (!snapshot.isEmpty()) {
                        stack = snapshot.get(k);
                    } else if (k == j) {
                        stack = new ItemStack(Items.PAPER);
                        stack.getOrCreateSubCompound("CustomCreativeLock");
                        String hotbarKey = GameSettings.getKeyDisplayString(mc.gameSettings.keyBindsHotbar[j].getKeyCode());
                        String saveKey = GameSettings.getKeyDisplayString(mc.gameSettings.keyBindSaveToolbar.getKeyCode());
                        stack.setStackDisplayName(new TextComponentTranslation("inventory.hotbarInfo", saveKey, hotbarKey).getUnformattedText());
                    }
                }
                list.add(stack);
            }
        }
    }

    /** Implemented by the creative screen (mixin): its search box has the keyboard. */
    public interface Typing {
        boolean ldic$isTyping();
    }

    public static boolean isTyping(Object screen) {
        return screen instanceof Typing && ((Typing) screen).ldic$isTyping();
    }

    public static ItemSort.Pref pref(CreativeTabs tab) {
        LdicSettings s = LdicClient.settings();
        ItemSort.Pref p = s == null ? null : s.creativeSort.get(tab.getTabLabel());
        return p == null ? ItemSort.Pref.DEFAULT : p;
    }

    /** Left click picks the next order, right click reverses; the choice is saved at once. */
    public static void clickSort(CreativeTabs tab, int button) {
        ItemSort.Pref p = pref(tab);
        p = button == 1 ? p.flipped() : p.nextOrder();
        LdicSettings s = LdicClient.settings();
        if (s != null) {
            s.saveCreativeSort(LdicClient.configDir(), tab.getTabLabel(), p);
        }
        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public static void drawSortButton(FontRenderer font, CreativeTabs tab, int left, int top, boolean hovered) {
        drawSortButtonAt(font, tab, left + SORT_X, top + SORT_Y, hovered);
    }

    /** The sort button with its top-left corner at (x, y), {@link #SORT_W} by {@link #SORT_H}. */
    public static void drawSortButtonAt(FontRenderer font, CreativeTabs tab, int x, int y, boolean hovered) {
        ItemSort.Pref p = pref(tab);
        Gui.drawRect(x, y, x + SORT_W, y + SORT_H, 0xFF373737);
        Gui.drawRect(x + 1, y + 1, x + SORT_W - 1, y + SORT_H - 1, hovered ? 0xFF9C9C9C : 0xFF8B8B8B);
        String label = label(p.order);
        int tx = x + 2 + (8 - font.getStringWidth(label)) / 2;
        font.drawStringWithShadow(label, tx, y + 2, p.isDefault() ? 0xFFE0E0E0 : 0xFFFFFF55);
        // A small triangle: up for ascending, down for descending.
        int ax = x + SORT_W - 6;
        int ay = y + 4;
        for (int row = 0; row < 3; row++) {
            int r = p.descending ? row : 2 - row;
            Gui.drawRect(ax + 2 - r, ay + row, ax + 3 + r, ay + row + 1, 0xFFFFFFFF);
        }
    }

    public static boolean overSort(int mouseX, int mouseY, int left, int top) {
        int x = mouseX - left - SORT_X;
        int y = mouseY - top - SORT_Y;
        return x >= 0 && y >= 0 && x < SORT_W && y < SORT_H;
    }

    public static boolean overBox(int mouseX, int mouseY, int left, int top) {
        int x = mouseX - left - BOX_X;
        int y = mouseY - top - BOX_Y;
        return x >= 0 && y >= 0 && x < BOX_W && y < BOX_H;
    }

    public static List<String> sortTooltip(CreativeTabs tab, int count) {
        ItemSort.Pref p = pref(tab);
        String order = I18n.format("ldic.creative.sort." + p.order.key);
        String dir = I18n.format(p.descending ? "ldic.creative.sort.descending" : "ldic.creative.sort.ascending");
        return Arrays.asList(
                I18n.format("ldic.creative.sort.title", order + " (" + dir + ")"),
                "§7" + I18n.format("ldic.creative.search.count", count),
                "§8" + I18n.format("ldic.creative.sort.left"),
                "§8" + I18n.format("ldic.creative.sort.right"));
    }

    private static String label(ItemSort.Order order) {
        switch (order) {
            case NAME:
                return "A";
            case MOD:
                return "@";
            case ID:
                return "i";
            default:
                return "#";
        }
    }
}
