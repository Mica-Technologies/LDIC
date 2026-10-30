package com.micatechnologies.minecraft.ldic.mixin;

import com.micatechnologies.minecraft.ldic.client.inventory.InventoryScreens;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The full-screen toggle beside an inventory panel takes its click before the screen does, so
 * the click never counts as one outside the panel (which would throw the held stack). Hooked
 * here rather than on the mouse event so synthetic clicks (Decision 12) reach it too.
 */
@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void ldic$clickFullScreenToggle(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        InventoryScreens screens = InventoryScreens.instance();
        if (screens != null && screens.clickToggle((GuiScreen) (Object) this, mouseX, mouseY, mouseButton)) {
            ci.cancel();
        }
    }
}
