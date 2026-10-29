package com.micatechnologies.minecraft.ldic.mixin;

import com.micatechnologies.minecraft.ldic.client.menu.LdicMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lifts vanilla's 30 fps cap on menu screens for LDIC theme screens.
 *
 * <p>With no world loaded, {@code getLimitFramerate} returns 30 for any open screen. That is
 * fine for a static vanilla menu, but LDIC themes animate (hover easing, slides, panoramas),
 * and at 30 fps a 0.15 s hover ease is four frames and feels sluggish. Theme screens use the
 * player's own Max Framerate setting instead, so the menu is as smooth as the game.</p>
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Shadow
    public GuiScreen currentScreen;

    @Shadow
    public GameSettings gameSettings;

    @Inject(method = "getLimitFramerate", at = @At("HEAD"), cancellable = true)
    private void ldic$themeScreenFramerate(CallbackInfoReturnable<Integer> cir) {
        if (currentScreen instanceof LdicMenuScreen) {
            cir.setReturnValue(gameSettings.limitFramerate);
        }
    }
}
