package com.micatechnologies.minecraft.ldic.mixin;

import com.micatechnologies.minecraft.ldic.client.menu.ScreenSkins;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla buttons in the rows of a skinned list draw in the theme's style (ScreenSkins). */
@Mixin(GuiButton.class)
public abstract class MixinGuiButton {

    @Inject(method = "drawButton", at = @At("HEAD"), cancellable = true)
    private void ldic$skinListButton(Minecraft mc, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (ScreenSkins.drawListButton((GuiButton) (Object) this, mc, mouseX, mouseY)) {
            ci.cancel();
        }
    }
}
