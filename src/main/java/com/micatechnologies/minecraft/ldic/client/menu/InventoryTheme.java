package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.client.LdicClient;
import com.micatechnologies.minecraft.ldic.client.inventory.InventoryScreens;
import com.micatechnologies.minecraft.ldic.client.inventory.LdicCreativeContainer;
import com.micatechnologies.minecraft.ldic.client.inventory.LdicCreativeScreen;
import com.micatechnologies.minecraft.ldic.client.render.Gfx;
import com.micatechnologies.minecraft.ldic.client.render.InventoryHooks;
import com.micatechnologies.minecraft.ldic.client.render.ThemeRenderer;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiContainer;
import com.micatechnologies.minecraft.ldic.theme.ThemeDef;
import com.micatechnologies.minecraft.ldic.ui.UiTree;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiHopper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiDispenser;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.gui.inventory.GuiShulkerBox;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;

/**
 * Inventory screens drawn by the menu theme: {@code inventory} (survival), {@code crafting}
 * (crafting table), {@code container} (chests, shulker boxes, hoppers, dispensers and droppers)
 * and {@code creative} (LDIC's full-screen creative inventory).
 *
 * <p>Unlike the menu screens, the vanilla screen keeps all of its logic: slots, clicks, the
 * recipe book, tooltips. The theme screen replaces only its background (and, for vanilla
 * screens, its text labels), drawn at one layout unit per GUI pixel so the theme lines up with
 * the slots. Slots stay where the game puts them; {@code slots} elements decorate them, and
 * {@code area_x("player")} and friends tell a theme where each group is.</p>
 *
 * <p>Other inventories (furnaces, brewing stands, enchanting tables, anvils, mods' screens)
 * draw parts of their working in their background, so they stay as they are.</p>
 */
public final class InventoryTheme implements InventoryHooks {

    private static final Map<GuiScreen, InventoryTheme> OPEN = new WeakHashMap<>();
    private static boolean failed;

    private final MenuManager manager;
    private final ThemeDef theme;
    private final String screenId;
    private final GuiContainer gui;
    private final UiTree tree;
    private final ThemeRenderer renderer;
    private final long openedAt = System.nanoTime();
    private long lastFrame = System.nanoTime();
    private final Map<String, List<Slot>> groups = new HashMap<>();
    private final Map<String, int[]> positions = new HashMap<>();
    private int mouseX;
    private int mouseY;

    private InventoryTheme(MenuManager manager, ThemeDef theme, String screenId, GuiContainer gui) {
        this.manager = manager;
        this.theme = theme;
        this.screenId = screenId;
        this.gui = gui;
        this.tree = new UiTree(theme.screen(screenId));
        this.renderer = new ThemeRenderer(manager.textures(), manager.fonts(), n -> { });
    }

    /** The theme screen for a vanilla (or LDIC) inventory screen, or null when it has none. */
    static String screenFor(GuiScreen gui) {
        if (gui instanceof LdicCreativeScreen) {
            return "creative";
        }
        if (gui instanceof GuiInventory) {
            return "inventory";
        }
        if (gui instanceof GuiCrafting) {
            return "crafting";
        }
        if (gui instanceof GuiChest || gui instanceof GuiShulkerBox || gui instanceof GuiHopper || gui instanceof GuiDispenser) {
            return "container";
        }
        return null;
    }

    /** The theme for this screen, made on first use, or null when it is not themed. */
    public static InventoryTheme of(GuiScreen gui) {
        MenuManager m = LdicClient.menu();
        if (failed || m == null || !m.settings().inventoryThemed || !(gui instanceof GuiContainer)) {
            return null;
        }
        String id = screenFor(gui);
        ThemeDef theme = m.theme();
        if (id == null || theme == null || theme.screen(id) == null) {
            OPEN.remove(gui);
            return null;
        }
        InventoryTheme t = OPEN.get(gui);
        if (t == null || t.theme != theme) {
            t = new InventoryTheme(m, theme, id, (GuiContainer) gui);
            OPEN.put(gui, t);
        }
        return t;
    }

    /**
     * Draws a vanilla inventory's background from the theme; false when the screen is not
     * themed (the creative screen calls {@link #draw} itself, around its own controls).
     */
    public static boolean drawBackground(GuiContainer gui, int mouseX, int mouseY) {
        if (gui instanceof LdicCreativeScreen) {
            return false;
        }
        InventoryTheme t = of(gui);
        if (t == null) {
            return false;
        }
        AccessorGuiContainer a = (AccessorGuiContainer) gui;
        Map<String, double[]> areas = new HashMap<>();
        areas.put("panel", new double[] {a.ldic$getGuiLeft(), a.ldic$getGuiTop(), a.ldic$getXSize(), a.ldic$getYSize()});
        return t.draw(mouseX, mouseY, areas);
    }

    /** Whether to skip a vanilla inventory's own text labels (the theme draws its own). */
    public static boolean hidesLabels(GuiContainer gui) {
        return !(gui instanceof LdicCreativeScreen) && of(gui) != null;
    }

    /**
     * Draws the theme screen with these named areas (slot-group areas are added). Returns
     * false, and turns inventory theming off for the session, if the theme fails.
     */
    public boolean draw(int mouseX, int mouseY, Map<String, double[]> areas) {
        try {
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            classifySlots(areas);
            Minecraft mc = Minecraft.getMinecraft();
            long nowNs = System.nanoTime();
            double dt = Math.min(0.25, (nowNs - lastFrame) / 1e9);
            lastFrame = nowNs;
            double screenSeconds = (nowNs - openedAt) / 1e9;
            int guiScale = new ScaledResolution(mc).getScaleFactor();
            float k = InventoryScreens.factor(gui);

            manager.textures().tick();
            manager.fonts().setPixelScale(guiScale * k);
            manager.bindings().update(manager.clock(), dt, screenSeconds, screenId, gui.width, gui.height,
                    gui.width, gui.height, guiScale, 1, mouseX, mouseY);
            manager.bindings().setInventory(title(), areas);
            theme.updateVariables();
            renderer.now = manager.clock();
            renderer.uiScale = 1;
            renderer.guiScale = guiScale * k;
            renderer.inventory = this;

            Gfx.beginUi();
            tree.frame(renderer, gui.width, gui.height, mouseX, mouseY, screenSeconds, dt);
            Gfx.endUi();
            GlStateManager.enableDepth();
            GlStateManager.color(1, 1, 1, 1);
            return true;
        } catch (RuntimeException e) {
            Ldic.LOGGER.error("LDIC: inventory theme screen '{}' failed; using the game's own look", screenId, e);
            failed = true;
            Gfx.endUi();
            return false;
        }
    }

    /** The title of the first non-player inventory in the container, e.g. "Large Chest". */
    private String title() {
        for (Slot s : gui.inventorySlots.inventorySlots) {
            IInventory inv = s.inventory;
            if (!(inv instanceof InventoryPlayer) && !(inv instanceof InventoryCrafting) && !(s instanceof SlotCrafting)
                    && !(s instanceof LdicCreativeContainer.GridSlot)) {
                return inv.getDisplayName().getUnformattedText();
            }
        }
        return "";
    }

    /** Sorts the container's slots into groups and adds each group's bounding area. */
    private void classifySlots(Map<String, double[]> areas) {
        groups.clear();
        positions.clear();
        AccessorGuiContainer a = (AccessorGuiContainer) gui;
        int left = a.ldic$getGuiLeft();
        int top = a.ldic$getGuiTop();
        for (Slot s : gui.inventorySlots.inventorySlots) {
            if (!s.isEnabled()) {
                continue;
            }
            String g = group(s);
            add(g, s);
            add("all", s);
            if (isPlayerGroup(g)) {
                add("player", s);
            }
        }
        for (Map.Entry<String, List<Slot>> e : groups.entrySet()) {
            List<Slot> slots = e.getValue();
            int[] pos = new int[slots.size() * 2];
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            for (int i = 0; i < slots.size(); i++) {
                int x = left + slots.get(i).xPos;
                int y = top + slots.get(i).yPos;
                pos[i * 2] = x;
                pos[i * 2 + 1] = y;
                minX = Math.min(minX, x - 1);
                minY = Math.min(minY, y - 1);
                maxX = Math.max(maxX, x + 17);
                maxY = Math.max(maxY, y + 17);
            }
            positions.put(e.getKey(), pos);
            if (!areas.containsKey(e.getKey())) {
                areas.put(e.getKey(), new double[] {minX, minY, maxX - minX, maxY - minY});
            }
        }
    }

    private void add(String group, Slot s) {
        groups.computeIfAbsent(group, g -> new ArrayList<>()).add(s);
    }

    private static boolean isPlayerGroup(String g) {
        return g.equals("main") || g.equals("hotbar") || g.equals("armor") || g.equals("offhand");
    }

    private static String group(Slot s) {
        if (s instanceof LdicCreativeContainer.GridSlot) {
            return "grid";
        }
        if (s instanceof SlotCrafting) {
            return "result";
        }
        if (s.inventory instanceof InventoryCrafting) {
            return "crafting";
        }
        if (s.inventory instanceof InventoryPlayer) {
            int i = s.getSlotIndex();
            return i < 9 ? "hotbar" : i < 36 ? "main" : i < 40 ? "armor" : "offhand";
        }
        return "container";
    }

    // InventoryHooks

    @Override
    public int[] slots(String group) {
        int[] p = positions.get(group);
        return p == null ? new int[0] : p;
    }

    @Override
    public int selected(String group) {
        List<Slot> slots = groups.get(group);
        Minecraft mc = Minecraft.getMinecraft();
        if (slots == null || mc.player == null) {
            return -1;
        }
        int current = mc.player.inventory.currentItem;
        for (int i = 0; i < slots.size(); i++) {
            Slot s = slots.get(i);
            if (s.inventory instanceof InventoryPlayer && s.getSlotIndex() == current) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void drawPlayer(double x, double y, double width, double height, boolean followMouse) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || height <= 0) {
            return;
        }
        int cx = (int) Math.round(x + width / 2);
        int feet = (int) Math.round(y + height - 4);
        int scale = (int) Math.max(4, Math.round(height * 30 / 72));
        float lookX = followMouse ? cx - mouseX : 0;
        float lookY = followMouse ? feet - scale * 1.6f - mouseY : 0;
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableAlpha();
        GuiInventory.drawEntityOnScreen(cx, feet, scale, lookX, lookY, mc.player);
        GlStateManager.disableDepth();
    }
}
