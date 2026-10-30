package com.micatechnologies.minecraft.ldic.client.hud;

import com.micatechnologies.minecraft.ldic.client.LdicSettings;
import com.micatechnologies.minecraft.ldic.client.inventory.InventoryStyle;
import com.micatechnologies.minecraft.ldic.inventory.HotbarVisibility;
import java.util.EnumSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * The hotbar: LDIC's modern look, a size of its own, and auto-hide.
 *
 * <p>Size and the hide slide apply to the whole group at the bottom of the screen (hotbar,
 * health, armour, food, air, experience, mount bars and the held item's name), through one GL
 * transform pushed in each element's {@code Pre} event and popped in its {@code Post}, so the
 * bars stay where they belong relative to the hotbar. Nothing is pushed while the hotbar is at
 * its normal size and fully shown.</p>
 */
public final class HotbarHud {

    private static final EnumSet<ElementType> GROUP = EnumSet.of(ElementType.HOTBAR, ElementType.HEALTH,
            ElementType.ARMOR, ElementType.FOOD, ElementType.AIR, ElementType.EXPERIENCE, ElementType.JUMPBAR,
            ElementType.HEALTHMOUNT);
    /** How far (unscaled GUI pixels) the group slides down when hidden: past the tallest bar stack. */
    private static final int HIDE_DISTANCE = 72;

    private static HotbarHud instance;

    private final LdicSettings settings;
    private final KeyBinding toggleKey = new KeyBinding("key.ldic.hotbar_toggle", KeyConflictContext.IN_GAME,
            Keyboard.KEY_H, "key.categories.ldic");
    private final HotbarVisibility visibility = new HotbarVisibility();
    private final EnumSet<ElementType> pushed = EnumSet.noneOf(ElementType.class);

    private final Item[] lastItems = new Item[10];
    private final int[] lastCounts = new int[10];
    private int lastSlot = -1;
    private float lastHealth = -1;
    private int lastFood = -1;
    private int lastLevel = -1;
    private double lastX;
    private double lastY;
    private double lastZ;
    private boolean moving;
    private long lastFrameNs;
    private double frameDt;
    private float selectionX = Float.NaN;

    public HotbarHud(LdicSettings settings) {
        this.settings = settings;
        instance = this;
        ClientRegistry.registerKeyBinding(toggleKey);
    }

    public static HotbarHud instance() {
        return instance;
    }

    private boolean modern() {
        return !"vanilla".equals(settings.hotbarStyle);
    }

    // ---------------------------------------------------------------------------------- state

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) {
            lastSlot = -1;
            return;
        }
        visibility.configure(settings.hotbarAutoHide, settings.hotbarHideDelay);
        while (toggleKey.isPressed()) {
            visibility.toggle();
        }
        boolean active = mc.currentScreen != null && lastSlot >= 0;
        if (player.inventory.currentItem != lastSlot) {
            active |= lastSlot >= 0;
            lastSlot = player.inventory.currentItem;
        }
        for (int i = 0; i < 10; i++) {
            ItemStack s = i < 9 ? player.inventory.mainInventory.get(i) : player.getHeldItemOffhand();
            Item item = s.isEmpty() ? null : s.getItem();
            int count = s.isEmpty() ? 0 : s.getCount();
            if (item != lastItems[i] || count != lastCounts[i]) {
                active = true;
                lastItems[i] = item;
                lastCounts[i] = count;
            }
        }
        if (player.getHealth() < lastHealth || player.getFoodStats().getFoodLevel() != lastFood
                || player.experienceLevel != lastLevel || player.getAir() < 300) {
            active |= lastHealth >= 0;
        }
        lastHealth = player.getHealth();
        lastFood = player.getFoodStats().getFoodLevel();
        lastLevel = player.experienceLevel;
        double dx = player.posX - lastX;
        double dy = player.posY - lastY;
        double dz = player.posZ - lastZ;
        moving = dx * dx + dy * dy + dz * dz > 0.0004;
        lastX = player.posX;
        lastY = player.posY;
        lastZ = player.posZ;
        if (active) {
            visibility.activity();
        }
    }

    // ------------------------------------------------------------------------------ rendering

    /**
     * Applies the group's size and slide; returns whether a matrix was pushed, which the caller
     * must then pop with {@link #end}. Also used for the held item's name, which has no event.
     */
    public boolean begin(ScaledResolution res) {
        double scale = settings.hotbarScale;
        double vis = visibility.visibility();
        if (scale == 1 && vis >= 1) {
            return false;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.playerController != null && mc.playerController.isSpectator()) {
            return false;
        }
        float cx = res.getScaledWidth() / 2f;
        float h = res.getScaledHeight();
        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, h + (float) ((1 - vis) * HIDE_DISTANCE * scale), 0);
        GlStateManager.scale(scale, scale, 1);
        GlStateManager.translate(-cx, -h, 0);
        return true;
    }

    public void end(boolean pushed) {
        if (pushed) {
            GlStateManager.popMatrix();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onOverlayPre(RenderGameOverlayEvent.Pre event) {
        ElementType type = event.getType();
        if (type == ElementType.ALL) {
            long now = System.nanoTime();
            double dt = lastFrameNs == 0 ? 0 : Math.min(0.1, (now - lastFrameNs) / 1e9);
            lastFrameNs = now;
            frameDt = dt;
            visibility.tick(dt, moving);
            return;
        }
        if (!GROUP.contains(type)) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        boolean spectator = mc.playerController != null && mc.playerController.isSpectator();
        if (!spectator && visibility.visibility() <= 0) {
            event.setCanceled(true);
            return;
        }
        boolean didPush = begin(event.getResolution());
        if (type == ElementType.HOTBAR && modern() && !spectator && mc.getRenderViewEntity() instanceof EntityPlayer) {
            drawModern(mc, event.getResolution(), event.getPartialTicks());
            end(didPush);
            event.setCanceled(true);
            return;
        }
        if (didPush) {
            pushed.add(type);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onOverlayPost(RenderGameOverlayEvent.Post event) {
        if (pushed.remove(event.getType())) {
            GlStateManager.popMatrix();
        }
        if (event.getType() == ElementType.ALL) {
            // An element whose Post never came (another mod cancelled it after us).
            for (int i = pushed.size(); i > 0; i--) {
                GlStateManager.popMatrix();
            }
            pushed.clear();
        }
    }

    private void drawModern(Minecraft mc, ScaledResolution res, float partialTicks) {
        EntityPlayer player = (EntityPlayer) mc.getRenderViewEntity();
        int x0 = res.getScaledWidth() / 2 - 91;
        int y0 = res.getScaledHeight() - 22;
        int accent = InventoryStyle.accent();

        GlStateManager.color(1, 1, 1, 1);
        InventoryStyle.panel(x0, y0, x0 + 182, y0 + 22, 0xA0101418, InventoryStyle.PANEL_EDGE);
        for (int i = 0; i < 9; i++) {
            int cx = x0 + 2 + i * 20;
            Gui.drawRect(cx, y0 + 2, cx + 18, y0 + 20, 0x28FFFFFF);
        }
        float target = x0 + 2 + player.inventory.currentItem * 20;
        if (Float.isNaN(selectionX) || Math.abs(selectionX - target) > 200) {
            selectionX = target;
        } else {
            selectionX += (target - selectionX) * (float) (1 - Math.exp(-frameDt * 25));
            if (Math.abs(target - selectionX) < 0.5f) {
                selectionX = target;
            }
        }
        int sx = Math.round(selectionX);
        Gui.drawRect(sx - 1, y0 + 1, sx + 19, y0 + 21, InventoryStyle.alpha(accent, 0x48));
        InventoryStyle.outline(sx - 1, y0, sx + 19, y0 + 22, accent);

        ItemStack offhand = player.getHeldItemOffhand();
        EnumHandSide offSide = player.getPrimaryHand().opposite();
        int ox = offSide == EnumHandSide.LEFT ? x0 - 26 : x0 + 182 + 4;
        if (!offhand.isEmpty()) {
            InventoryStyle.panel(ox, y0, ox + 22, y0 + 22, 0xA0101418, InventoryStyle.PANEL_EDGE);
        }

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 0; i < 9; i++) {
            renderItem(mc, player, player.inventory.mainInventory.get(i), x0 + 3 + i * 20, y0 + 3, partialTicks);
        }
        if (!offhand.isEmpty()) {
            renderItem(mc, player, offhand, ox + 3, y0 + 3, partialTicks);
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();

        if (mc.gameSettings.attackIndicator == 2) {
            float strength = player.getCooledAttackStrength(0);
            if (strength < 1) {
                int ax = offSide == EnumHandSide.RIGHT ? x0 - 22 : x0 + 182 + 6;
                int ay = y0 + 2;
                mc.getTextureManager().bindTexture(Gui.ICONS);
                int filled = (int) (strength * 19);
                GlStateManager.color(1, 1, 1, 1);
                mc.ingameGUI.drawTexturedModalRect(ax, ay, 0, 94, 18, 18);
                mc.ingameGUI.drawTexturedModalRect(ax, ay + 18 - filled, 18, 112 - filled, 18, filled);
            }
        }
        GlStateManager.disableBlend();
    }

    private static void renderItem(Minecraft mc, EntityPlayer player, ItemStack stack, int x, int y, float partialTicks) {
        if (stack.isEmpty()) {
            return;
        }
        RenderItem items = mc.getRenderItem();
        float pop = stack.getAnimationsToGo() - partialTicks;
        if (pop > 0) {
            GlStateManager.pushMatrix();
            float f = 1 + pop / 5;
            GlStateManager.translate(x + 8, y + 12, 0);
            GlStateManager.scale(1 / f, (f + 1) / 2, 1);
            GlStateManager.translate(-(x + 8), -(y + 12), 0);
        }
        items.renderItemAndEffectIntoGUI(player, stack, x, y);
        if (pop > 0) {
            GlStateManager.popMatrix();
        }
        items.renderItemOverlays(mc.fontRenderer, stack, x, y);
    }
}
