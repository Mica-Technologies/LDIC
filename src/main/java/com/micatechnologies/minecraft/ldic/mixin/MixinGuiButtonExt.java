package com.micatechnologies.minecraft.ldic.mixin;

import com.micatechnologies.minecraft.ldic.client.menu.ScreenSkins;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forge's config buttons (and sliders) in the rows of a skinned list draw in the theme's style,
 * as vanilla's do (MixinGuiButton): GuiButtonExt draws itself without calling GuiButton's.
 * A Forge class overriding a Minecraft method: named for both the dev and the obfuscated game.
 */
@Mixin(value = GuiButtonExt.class, remap = false)
public abstract class MixinGuiButtonExt {

    @Inject(method = {"drawButton", "func_191745_a"}, at = @At("HEAD"), cancellable = true, require = 1)
    private void ldic$skinListButton(Minecraft mc, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (ScreenSkins.drawListButton((GuiButton) (Object) this, mc, mouseX, mouseY)) {
            ci.cancel();
        }
    }
}
