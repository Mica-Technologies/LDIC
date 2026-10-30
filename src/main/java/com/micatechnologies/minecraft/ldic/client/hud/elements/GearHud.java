package com.micatechnologies.minecraft.ldic.client.hud.elements;

import cc.polyfrost.oneconfig.config.annotations.Switch;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/**
 * Worn armour and the items in both hands as icons, each with a durability bar under it that
 * goes from green to red; a nearly broken item's bar blinks. Stackable items show their count.
 */
public class GearHud extends VisualHud {

    private static final float CELL = 18;

    @Switch(name = "Show Empty Slots")
    public boolean showEmpty = false;

    @Switch(name = "Held Items")
    public boolean showHands = true;

    private static ItemStack[] examples;

    public GearHud() {
        super(4f, 1020f);
    }

    private List<ItemStack> items(Minecraft mc, boolean example) {
        List<ItemStack> out = new ArrayList<>();
        if (example) {
            if (examples == null) {
                examples = new ItemStack[] {new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.IRON_CHESTPLATE),
                        new ItemStack(Items.IRON_LEGGINGS), new ItemStack(Items.DIAMOND_BOOTS), new ItemStack(Items.DIAMOND_PICKAXE)};
                examples[1].setItemDamage(examples[1].getMaxDamage() * 2 / 3);
                examples[4].setItemDamage(examples[4].getMaxDamage() * 19 / 20);
            }
            for (ItemStack s : examples) {
                out.add(s);
            }
            return out;
        }
        EntityPlayer p = mc.player;
        for (int i = 3; i >= 0; i--) {
            ItemStack s = p.inventory.armorInventory.get(i);
            if (!s.isEmpty() || showEmpty) {
                out.add(s);
            }
        }
        if (showHands) {
            for (ItemStack s : new ItemStack[] {p.getHeldItemMainhand(), p.getHeldItemOffhand()}) {
                if (!s.isEmpty() || showEmpty) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    @Override
    protected float width(boolean example) {
        Minecraft mc = Minecraft.getMinecraft();
        int n = mc.player == null && !example ? 0 : items(mc, example || mc.player == null).size();
        return Math.max(1, n * CELL - 2);
    }

    @Override
    protected float height(boolean example) {
        return 20;
    }

    @Override
    protected void render(Minecraft mc, boolean example) {
        FontRenderer font = mc.fontRenderer;
        boolean blinkOn = (System.currentTimeMillis() / 400) % 2 == 0;
        float x = 0;
        for (ItemStack s : items(mc, example)) {
            if (s.isEmpty()) {
                rect(x + 1, 1, x + 15, 15, fade(text(), 0.12f));
            } else {
                RenderHelper.enableGUIStandardItemLighting();
                GlStateManager.enableDepth();
                mc.getRenderItem().renderItemAndEffectIntoGUI(s, (int) x, 0);
                GlStateManager.disableDepth();
                RenderHelper.disableStandardItemLighting();
                GlStateManager.enableBlend();
                if (s.isItemStackDamageable()) {
                    float left = 1 - s.getItemDamage() / (float) s.getMaxDamage();
                    boolean low = left < 0.1f;
                    rect(x + 1, 17, x + 15, 19, 0xA0000000);
                    if (!low || blinkOn) {
                        rect(x + 1, 17, x + 1 + 14 * left, 19, wear(left));
                    }
                } else if (s.getCount() > 1) {
                    String c = String.valueOf(s.getCount());
                    text(font, c, x + 16 - textWidth(font, c, 6), 11, 6, text(), true);
                }
            }
            x += CELL;
        }
    }
}
