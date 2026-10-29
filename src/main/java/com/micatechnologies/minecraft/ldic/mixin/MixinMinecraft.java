package com.micatechnologies.minecraft.ldic.mixin;

import com.micatechnologies.minecraft.ldic.client.menu.WorldLoading;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Two changes so LDIC's screens animate smoothly.
 *
 * <p>With no world loaded, {@code getLimitFramerate} returns 30 for any open screen. That is
 * fine for a static vanilla menu, but LDIC themes animate (hover easing, slides, panoramas),
 * and at 30 fps a 0.15 s hover ease is four frames and feels sluggish. Theme screens use the
 * player's own Max Framerate setting instead, so the menu is as smooth as the game.</p>
 *
 * <p>While a singleplayer world starts or shuts down, vanilla redraws its progress screen only
 * between sleeps (200 ms while starting). Those sleeps go through LDIC, which keeps drawing
 * the world-loading screen while it waits; without a themed screen they are plain sleeps.</p>
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Shadow
    public GuiScreen currentScreen;

    @Shadow
    public GameSettings gameSettings;

    @Inject(method = "getLimitFramerate", at = @At("HEAD"), cancellable = true)
    private void ldic$themeScreenFramerate(CallbackInfoReturnable<Integer> cir) {
        if (WorldLoading.themed(currentScreen)) {
            cir.setReturnValue(gameSettings.limitFramerate);
        }
    }

    @Redirect(method = "launchIntegratedServer", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;sleep(J)V"))
    private void ldic$drawWhileStarting(long millis) throws InterruptedException {
        WorldLoading.sleep(millis);
    }

    @Redirect(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V",
            at = @At(value = "INVOKE", target = "Ljava/lang/Thread;sleep(J)V"))
    private void ldic$drawWhileStopping(long millis) throws InterruptedException {
        WorldLoading.sleep(millis);
    }
}
