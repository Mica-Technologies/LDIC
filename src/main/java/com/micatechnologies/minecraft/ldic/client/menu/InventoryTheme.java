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
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiBrewingStand;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiFurnace;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiRepair;
import com.micatechnologies.minecraft.ldic.theme.ThemeDef;
import com.micatechnologies.minecraft.ldic.ui.UiTree;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiHopper;
import net.minecraft.client.gui.GuiRepair;
import net.minecraft.client.gui.inventory.GuiBrewingStand;
import net.minecraft.client.gui.inventory.GuiFurnace;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiDispenser;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.gui.inventory.GuiShulkerBox;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.InventoryCraftResult;
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
 * <p>Furnaces ({@code furnace}), brewing stands ({@code brewing}) and anvils ({@code anvil}) draw
 * part of their working in their background (progress, fuel, the anvil's cost and name box): that
 * reaches the theme as values and areas (see {@link #machine}). Other inventories (enchanting
 * tables, mods' screens)
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
        // Exactly these: a mod's screen built on one may draw more of its working in its background.
        if (gui.getClass() == GuiFurnace.class) {
            return "furnace";
        }
        if (gui.getClass() == GuiBrewingStand.class) {
            return "brewing";
        }
        if (gui.getClass() == GuiRepair.class) {
            return "anvil";
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
        t.machine(a.ldic$getGuiLeft(), a.ldic$getGuiTop(), areas);
        return t.draw(mouseX, mouseY, areas);
    }

    /**
     * What a furnace, brewing stand or anvil drew in its background, as values and areas for the
     * theme: progress, fuel, the anvil's cost and name box. Areas are where vanilla draws each
     * part, so a theme can draw them in place.
     */
    private void machine(int left, int top, Map<String, double[]> areas) {
        Map<String, String> text = new HashMap<>();
        Map<String, Boolean> flags = new HashMap<>();
        Map<String, Double> values = new HashMap<>();
        if (gui instanceof GuiFurnace) {
            IInventory f = ((AccessorGuiFurnace) gui).ldic$getFurnace();
            int burn = f.getField(0);
            int burnTotal = f.getField(1) == 0 ? 200 : f.getField(1);
            int cook = f.getField(2);
            int cookTotal = f.getField(3);
            values.put("burn", burn <= 0 ? 0 : Math.min(1, burn / (double) burnTotal));
            values.put("cook", cookTotal <= 0 ? 0 : Math.min(1, cook / (double) cookTotal));
            flags.put("burning", burn > 0);
            areas.put("flame", new double[] {left + 56, top + 36, 14, 14});
            areas.put("arrow", new double[] {left + 79, top + 34, 24, 17});
        } else if (gui instanceof GuiBrewingStand) {
            IInventory s = ((AccessorGuiBrewingStand) gui).ldic$getStand();
            int brew = s.getField(0);
            values.put("fuel", Math.max(0, Math.min(1, s.getField(1) / 20.0)));
            values.put("brew", brew <= 0 ? 0 : 1 - brew / 400.0);
            flags.put("brewing", brew > 0);
            areas.put("fuel_bar", new double[] {left + 60, top + 44, 18, 4});
            areas.put("arrow", new double[] {left + 97, top + 16, 9, 28});
            areas.put("bubbles", new double[] {left + 63, top + 14, 12, 29});
        } else if (gui instanceof GuiRepair) {
            AccessorGuiRepair r = (AccessorGuiRepair) gui;
            ContainerRepair anvil = r.ldic$getAnvil();
            Minecraft mc = Minecraft.getMinecraft();
            boolean result = anvil.getSlot(2).getHasStack();
            boolean expensive = anvil.maximumCost >= 40 && mc.player != null && !mc.player.capabilities.isCreativeMode;
            // As vanilla's label: the cost when there is a result (or "Too Expensive!"), red when
            // the player cannot take it.
            flags.put("show_cost", anvil.maximumCost > 0 && (expensive || result));
            flags.put("affordable", !expensive && result && mc.player != null && anvil.getSlot(2).canTakeStack(mc.player));
            text.put("cost", expensive ? I18n.format("container.repair.expensive") : I18n.format("container.repair.cost", anvil.maximumCost));
            values.put("cost", (double) anvil.maximumCost);
            flags.put("has_item", anvil.getSlot(0).getHasStack());
            flags.put("blocked", (anvil.getSlot(0).getHasStack() || anvil.getSlot(1).getHasStack()) && !result);
            areas.put("name", new double[] {left + 59, top + 20, 110, 16});
            areas.put("plus", new double[] {left + 52, top + 47, 16, 16});
            areas.put("arrow", new double[] {left + 99, top + 45, 28, 21});
        }
        manager.bindings().setScreenValues(text, flags, values);
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
            String g = machineGroup(s);
            if (g == null) {
                g = group(s);
            }
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

    /** A furnace's, brewing stand's or anvil's own slots by what they hold; null for others. */
    private String machineGroup(Slot s) {
        if (s.inventory instanceof InventoryPlayer) {
            return null;
        }
        int i = s.getSlotIndex();
        switch (screenId) {
            case "furnace":
                return i == 0 ? "input" : i == 1 ? "fuel" : "result";
            case "brewing":
                return i < 3 ? "bottles" : i == 3 ? "ingredient" : "fuel";
            case "anvil":
                return s.inventory instanceof InventoryCraftResult ? "result" : "input";
            default:
                return null;
        }
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
