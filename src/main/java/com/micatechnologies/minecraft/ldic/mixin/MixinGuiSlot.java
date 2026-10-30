package com.micatechnologies.minecraft.ldic.mixin;

import com.micatechnologies.minecraft.ldic.client.menu.ScreenSkins;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lists drawn inside a skin (vanilla_list) skip their dirt: the screen background some lists
 * draw, the background inside the list, and the strips vanilla paints over the rest of the
 * screen above and below it. Their scroll bar sits at the right edge of the box the skin gave
 * them. Everything else about the list (rows, scrolling, clicks) is unchanged.
 */
@Mixin(GuiSlot.class)
public abstract class MixinGuiSlot {

    @Redirect(method = "drawScreen", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiSlot;drawBackground()V"))
    private void ldic$skinScreenBackground(GuiSlot list) {
        if (!ScreenSkins.drawingList()) {
            ((AccessorGuiSlot) list).ldic$drawBackground();
        }
    }

    @Redirect(method = "drawScreen", at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraft/client/gui/GuiSlot;drawContainerBackground(Lnet/minecraft/client/renderer/Tessellator;)V"))
    private void ldic$skinListBackground(GuiSlot list, Tessellator tessellator) {
        if (!ScreenSkins.drawingList()) {
            ((AccessorGuiSlot) list).ldic$drawContainerBackground(tessellator);
        }
    }

    @Redirect(method = "drawScreen", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiSlot;overlayBackground(IIII)V"))
    private void ldic$skinListOverlay(GuiSlot list, int startY, int endY, int startAlpha, int endAlpha) {
        if (!ScreenSkins.drawingList()) {
            ((AccessorGuiSlot) list).ldic$overlayBackground(startY, endY, startAlpha, endAlpha);
        }
    }

    @Redirect(method = {"drawScreen", "handleMouseInput"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiSlot;getScrollBarX()I"))
    private int ldic$skinScrollBar(GuiSlot list) {
        return ScreenSkins.skinned(list) ? list.right - 6 : ((AccessorGuiSlot) list).ldic$getScrollBarX();
    }
}
