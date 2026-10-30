package com.micatechnologies.minecraft.ldic.mixin;

import com.micatechnologies.minecraft.ldic.client.inventory.InventoryScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Full-screen (scaled-up) inventories (Decision 14). A scaled screen was given a smaller
 * width and height at init, so its own mouse handling already works in its units; here its
 * drawing is scaled up to match and the mouse position passed to it, and to both draw events,
 * is converted to its units. Converting here rather than in the events keeps other mods'
 * overlays (drawn in {@code DrawScreenEvent.Post}) lined up with the screen.
 */
@Mixin(value = ForgeHooksClient.class, remap = false)
public abstract class MixinForgeHooksClient {

    @ModifyVariable(method = "drawScreen", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int ldic$scaledMouseX(int mouseX) {
        return InventoryScreens.mouseX(Minecraft.getMinecraft().currentScreen, mouseX);
    }

    @ModifyVariable(method = "drawScreen", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private static int ldic$scaledMouseY(int mouseY) {
        return InventoryScreens.mouseY(Minecraft.getMinecraft().currentScreen, mouseY);
    }

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private static void ldic$beginScale(GuiScreen screen, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        float k = InventoryScreens.factor(screen);
        if (k != 1) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(k, k, 1);
        }
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private static void ldic$endScale(GuiScreen screen, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (InventoryScreens.factor(screen) != 1) {
            GlStateManager.popMatrix();
        }
    }
}
