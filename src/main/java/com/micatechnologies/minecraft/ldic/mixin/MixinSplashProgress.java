package com.micatechnologies.minecraft.ldic.mixin;

import com.micatechnologies.minecraft.ldic.client.loading.LoadingScreen;
import java.lang.reflect.Method;
import net.minecraftforge.fml.client.SplashProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Hands Forge's loading-screen thread to LDIC.
 *
 * <p>{@code SplashProgress.start()} creates one {@code new Thread(runnable)} whose runnable
 * draws Forge's splash on the window's GL context while mods load on the main thread. We wrap
 * that runnable: LDIC's loading screen runs in its place, honouring the same lock, mutex and
 * pause protocol, and if LDIC is disabled, has no usable theme, or fails at any point, Forge's
 * own runnable runs instead. Forge's threading, context sharing and {@code finish()} are left
 * as they are, with one exception below.</p>
 *
 * <p>{@code finish()} deletes the font, logo and Forge textures that Forge's runnable creates.
 * When LDIC drew the screen, that runnable never ran and those fields are null; the NPE would
 * make Forge disable the splash in {@code config/splash.properties} and crash the game. The
 * deletes therefore go through a null check.</p>
 *
 * <p>If {@code config/splash.properties} has {@code enabled=false}, Forge never creates the
 * thread and this does nothing; LDIC respects that, since Forge writes it after a splash crash.</p>
 */
@Mixin(value = SplashProgress.class, remap = false)
public abstract class MixinSplashProgress {

    @Redirect(method = "start", at = @At(value = "NEW", target = "java/lang/Thread"), remap = false)
    private static Thread ldic$wrapSplashThread(Runnable forgeSplash) {
        return new Thread(LoadingScreen.wrap(forgeSplash), "LDIC loading screen");
    }

    @Redirect(method = "finish",
              at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/client/SplashProgress$Texture;delete()V"),
              remap = false)
    private static void ldic$deleteIfCreated(@Coerce Object texture) {
        if (texture == null) {
            return;
        }
        try {
            Method delete = texture.getClass().getDeclaredMethod("delete");
            delete.setAccessible(true);
            delete.invoke(texture);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("could not delete a Forge splash texture", e);
        }
    }
}
