package com.micatechnologies.minecraft.ldic.mixin;

import com.micatechnologies.minecraft.ldic.client.menu.ScreenSkins;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.GuiSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * A list inside a skin is laid out in the theme's units, so the clicks its screen forwards (in
 * GUI pixels) are converted to them first. Lists outside a skin are untouched.
 */
@Mixin(GuiListExtended.class)
public abstract class MixinGuiListExtended {

    @ModifyVariable(method = "mouseClicked", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int ldic$clickX(int x) {
        return ScreenSkins.toList((GuiSlot) (Object) this, x);
    }

    @ModifyVariable(method = "mouseClicked", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int ldic$clickY(int y) {
        return ScreenSkins.toList((GuiSlot) (Object) this, y);
    }

    @ModifyVariable(method = "mouseReleased", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int ldic$releaseX(int x) {
        return ScreenSkins.toList((GuiSlot) (Object) this, x);
    }

    @ModifyVariable(method = "mouseReleased", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int ldic$releaseY(int y) {
        return ScreenSkins.toList((GuiSlot) (Object) this, y);
    }
}
