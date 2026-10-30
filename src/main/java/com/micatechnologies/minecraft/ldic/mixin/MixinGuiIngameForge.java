package com.micatechnologies.minecraft.ldic.mixin;

import com.micatechnologies.minecraft.ldic.client.hud.HotbarHud;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The held item's name sits just above the hotbar but has no overlay event, so it gets the
 * hotbar group's size and slide here (see {@link HotbarHud}).
 */
@Mixin(value = GuiIngameForge.class, remap = false)
public abstract class MixinGuiIngameForge {

    @Unique
    private boolean ldic$highlightPushed;

    @Inject(method = "renderToolHighlight", at = @At("HEAD"))
    private void ldic$beginHighlight(ScaledResolution res, CallbackInfo ci) {
        HotbarHud hud = HotbarHud.instance();
        ldic$highlightPushed = hud != null && hud.begin(res);
    }

    @Inject(method = "renderToolHighlight", at = @At("RETURN"))
    private void ldic$endHighlight(ScaledResolution res, CallbackInfo ci) {
        HotbarHud hud = HotbarHud.instance();
        if (hud != null) {
            hud.end(ldic$highlightPushed);
        }
        ldic$highlightPushed = false;
    }
}
