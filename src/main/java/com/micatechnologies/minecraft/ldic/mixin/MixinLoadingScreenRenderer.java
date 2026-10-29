package com.micatechnologies.minecraft.ldic.mixin;

import com.micatechnologies.minecraft.ldic.client.menu.WorldLoading;
import net.minecraft.client.LoadingScreenRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws LDIC's world-loading screen in place of the dirt-background progress screen vanilla
 * shows while a singleplayer world starts or shuts down. Vanilla's own drawing runs whenever
 * LDIC declines (no theme screen, feature switched off, or the game is still starting).
 */
@Mixin(LoadingScreenRenderer.class)
public abstract class MixinLoadingScreenRenderer {

    @Shadow
    private String message;

    @Shadow
    private String currentlyDisplayedText;

    @Inject(method = "setLoadingProgress", at = @At("HEAD"), cancellable = true)
    private void ldic$drawWorldLoading(int progress, CallbackInfo ci) {
        if (WorldLoading.drawStandalone(currentlyDisplayedText, message, progress)) {
            ci.cancel();
        }
    }
}
