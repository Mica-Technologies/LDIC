package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.client.render.Gfx;
import com.micatechnologies.minecraft.ldic.client.render.SkinHooks;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiAdvancementTab;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiScreenAdvancements;
import com.micatechnologies.minecraft.ldic.ui.UiNode;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.advancements.GuiAdvancementTab;
import net.minecraft.client.gui.advancements.GuiScreenAdvancements;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/**
 * The advancement screen's map and tabs inside a skin. The selected tab draws its own contents
 * (background, lines, frames, hover boxes) as in vanilla, but in a view the size of the theme's
 * box rather than vanilla's 234 by 113 window (MixinGuiAdvancementTab reads {@link #width} and
 * {@link #height}). The tabs are a scrolling column of theme buttons with each tab's icon; the
 * map drags as in vanilla and scrolls with the wheel (shift for sideways).
 */
public final class AdvancementMap {

    /** The view size in GUI pixels while a skin shows the map; 0 when vanilla draws it. */
    private static int viewWidth;
    private static int viewHeight;
    /** The map a skin is showing, for clicks that reach the screen itself. */
    private static AdvancementMap shown;

    private final GuiScreenAdvancements gui;
    /** The map box in GUI pixels, from the last frame. */
    private int mapX;
    private int mapY;
    private int mapW;
    private int mapH;
    private boolean dragging;
    private int lastX;
    private int lastY;
    /** The tab column in layout units, and its rows from the last frame. */
    private double tabsX;
    private double tabsY;
    private double tabsW;
    private double tabsH;
    private double tabScroll;
    private double tabsContent;
    private double tabsUi = 1;
    private final List<Object[]> tabRows = new ArrayList<>();
    private final Map<GuiAdvancementTab, Double> hover = new IdentityHashMap<>();
    private Runnable tooltip;

    AdvancementMap(GuiScreenAdvancements gui) {
        this.gui = gui;
    }

    public static boolean active() {
        return viewWidth > 0;
    }

    public static int width(int vanilla) {
        return viewWidth > 0 ? viewWidth : vanilla;
    }

    public static int height(int vanilla) {
        return viewHeight > 0 ? viewHeight : vanilla;
    }

    /** The skin closed: vanilla's sizes again. */
    static void reset() {
        viewWidth = 0;
        viewHeight = 0;
        shown = null;
    }

    private AccessorGuiScreenAdvancements access() {
        return (AccessorGuiScreenAdvancements) gui;
    }

    boolean empty() {
        return access().ldic$getSelectedTab() == null;
    }

    String tabTitle() {
        GuiAdvancementTab tab = access().ldic$getSelectedTab();
        return tab == null ? "" : tab.getTitle();
    }

    /** Vanilla's tab page buttons: every tab is in the column, so they are not needed. */
    void hidePageButtons(List<GuiButton> buttons) {
        for (GuiButton b : buttons) {
            if (b.id == 101 || b.id == 102) {
                b.visible = false;
            }
        }
    }

    /** Draws the selected tab's map in the node's box (layout units; ui is the theme's scale). */
    void drawMap(UiNode n, double ui, int mouseX, int mouseY) {
        mapX = (int) Math.round(n.x * ui);
        mapY = (int) Math.round(n.y * ui);
        mapW = Math.max(16, (int) Math.round(n.width * ui));
        mapH = Math.max(16, (int) Math.round(n.height * ui));
        viewWidth = mapW;
        viewHeight = mapH;
        shown = this;
        GuiAdvancementTab tab = access().ldic$getSelectedTab();
        boolean inside = mouseX >= mapX && mouseY >= mapY && mouseX < mapX + mapW && mouseY < mapY + mapH;
        if (Mouse.isButtonDown(0)) {
            if (dragging && tab != null) {
                tab.scroll(mouseX - lastX, mouseY - lastY);
            } else if (!dragging && inside) {
                dragging = true;
            }
            lastX = mouseX;
            lastY = mouseY;
        } else {
            dragging = false;
        }
        tooltip = null;
        if (tab == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        double s = new ScaledResolution(mc).getScaleFactor();
        GlStateManager.pushMatrix();
        GlStateManager.scale(1 / ui, 1 / ui, 1);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) Math.floor(mapX * s), (int) Math.floor(mc.displayHeight - (mapY + mapH) * s),
                (int) Math.ceil(mapW * s), (int) Math.ceil(mapH * s));
        try {
            GlStateManager.pushMatrix();
            GlStateManager.translate(mapX, mapY, -400.0F);
            GlStateManager.enableDepth();
            tab.drawContents();
            GlStateManager.popMatrix();
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.disableDepth();
        } finally {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GlStateManager.popMatrix();
        }
        Gfx.beginUi();
        // The hover box goes over everything the theme draws after the map.
        tooltip = () -> {
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.pushMatrix();
            GlStateManager.enableDepth();
            GlStateManager.translate(mapX, mapY, 400.0F);
            // Vanilla's window sits 9 and 18 pixels up and left of its view.
            tab.drawToolTips(mouseX - mapX, mouseY - mapY, mapX - 9, mapY - 18);
            GlStateManager.disableDepth();
            GlStateManager.popMatrix();
        };
    }

    /** After the theme drew the screen (GUI pixels): the hovered advancement's box. */
    void drawTooltip() {
        if (tooltip != null) {
            tooltip.run();
            tooltip = null;
        }
    }

    /** The tabs as theme buttons (through paint), each with its icon, in a scrolling column. */
    void drawTabs(UiNode n, double ui, int mouseX, int mouseY, Consumer<SkinHooks.Cell> paint) {
        double rowH = n.def.num("row_height", 22);
        double gap = n.def.num("gap", 3);
        double iconSize = n.def.num("icon_size", 16);
        tabsX = n.x;
        tabsY = n.y;
        tabsW = n.width;
        tabsH = n.height;
        tabsUi = ui;
        shown = this;
        tabRows.clear();
        Map<?, GuiAdvancementTab> tabs = access().ldic$getTabs();
        GuiAdvancementTab selected = access().ldic$getSelectedTab();
        tabsContent = tabs.size() * (rowH + gap) - gap;
        tabScroll = Math.max(0, Math.min(tabScroll, tabsContent - tabsH));
        double mx = mouseX / ui;
        double my = mouseY / ui;
        boolean inColumn = mx >= tabsX && my >= tabsY && mx < tabsX + tabsW && my < tabsY + tabsH;
        Minecraft mc = Minecraft.getMinecraft();
        double s = new ScaledResolution(mc).getScaleFactor() * ui;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) Math.floor(n.x * s), (int) Math.floor(mc.displayHeight - (n.y + n.height) * s),
                (int) Math.ceil(n.width * s), (int) Math.ceil(n.height * s));
        try {
            double y = n.y - tabScroll;
            for (GuiAdvancementTab tab : tabs.values()) {
                if (y + rowH >= n.y && y < n.y + n.height) {
                    boolean over = inColumn && my >= y && my < y + rowH;
                    double t = hover.getOrDefault(tab, 0.0);
                    t += ((over || tab == selected ? 1 : 0) - t) * 0.35;
                    hover.put(tab, t);
                    paint.accept(new SkinHooks.Cell(n.x, y, n.width, rowH, tab.getTitle(), true, t, -1));
                    drawIcon(((AccessorGuiAdvancementTab) tab).ldic$getIcon(), n.x + (rowH - iconSize) / 2, y + (rowH - iconSize) / 2, iconSize);
                    Gfx.beginUi();
                }
                tabRows.add(new Object[] {y, rowH, tab});
                y += rowH + gap;
            }
        } finally {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    private static void drawIcon(ItemStack stack, double x, double y, double size) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(size / 16, size / 16, 1);
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }

    /** A left click on the advancement screen (from its mouseClicked); true when a tab took it. */
    public static boolean clicked(GuiScreenAdvancements gui, int mouseX, int mouseY) {
        return shown != null && shown.gui == gui && shown.click(mouseX, mouseY, shown.tabsUi);
    }

    /** A press on a tab selects it; true when it was taken. Mouse in GUI pixels. */
    boolean click(int mouseX, int mouseY, double ui) {
        double mx = mouseX / ui;
        double my = mouseY / ui;
        if (mx < tabsX || my < tabsY || mx >= tabsX + tabsW || my >= tabsY + tabsH) {
            return false;
        }
        for (Object[] row : tabRows) {
            double y = (Double) row[0];
            if (my >= y && my < y + (Double) row[1]) {
                GuiAdvancementTab tab = (GuiAdvancementTab) row[2];
                access().ldic$getManager().setSelectedTab(tab.getAdvancement(), true);
                Minecraft.getMinecraft().getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.getMasterRecord(
                        net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return true;
    }

    /** The wheel scrolls the tab column or the map, whichever is under the mouse. */
    boolean wheel(int mouseX, int mouseY, int dwheel, double ui) {
        double mx = mouseX / ui;
        double my = mouseY / ui;
        double step = Math.signum(dwheel);
        if (mx >= tabsX && my >= tabsY && mx < tabsX + tabsW && my < tabsY + tabsH) {
            tabScroll = Math.max(0, Math.min(tabScroll - step * 24, tabsContent - tabsH));
            return true;
        }
        GuiAdvancementTab tab = access().ldic$getSelectedTab();
        if (tab != null && mouseX >= mapX && mouseY >= mapY && mouseX < mapX + mapW && mouseY < mapY + mapH) {
            int d = (int) (step * 24);
            boolean sideways = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            tab.scroll(sideways ? d : 0, sideways ? 0 : d);
            return true;
        }
        return false;
    }
}
