package com.micatechnologies.minecraft.ldic.mixin;

import com.micatechnologies.minecraft.ldic.client.inventory.InventoryScreens;
import com.micatechnologies.minecraft.ldic.client.menu.InventoryTheme;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every inventory screen: theme drawing in place of the background and labels (see
 * {@link InventoryTheme}), and the full-screen toggle beside the panel. The toggle takes its
 * click before the screen does, so the click never counts as one outside the panel (which
 * would throw the held stack); it is hooked here rather than on the mouse event so synthetic
 * clicks (Decision 12) reach it too.
 */
@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer {

    /** A themed inventory draws the menu theme's screen in place of its own background. */
    @Redirect(method = "drawScreen", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawGuiContainerBackgroundLayer(FII)V"))
    private void ldic$themedBackground(GuiContainer gui, float partialTicks, int mouseX, int mouseY) {
        if (!InventoryTheme.drawBackground(gui, mouseX, mouseY)) {
            ((AccessorGuiContainer) gui).ldic$drawBackgroundLayer(partialTicks, mouseX, mouseY);
        }
    }

    /** ... and its own text labels ("Crafting", "Inventory", the chest's name), which the theme replaces. */
    @Redirect(method = "drawScreen", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawGuiContainerForegroundLayer(II)V"))
    private void ldic$themedLabels(GuiContainer gui, int mouseX, int mouseY) {
        if (!InventoryTheme.hidesLabels(gui)) {
            ((AccessorGuiContainer) gui).ldic$drawForegroundLayer(mouseX, mouseY);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void ldic$clickFullScreenToggle(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        InventoryScreens screens = InventoryScreens.instance();
        if (screens != null && screens.clickToggle((GuiScreen) (Object) this, mouseX, mouseY, mouseButton)) {
            ci.cancel();
        }
    }
}
