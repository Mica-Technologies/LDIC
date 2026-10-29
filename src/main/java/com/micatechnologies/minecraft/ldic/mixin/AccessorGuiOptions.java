package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the screen a vanilla options screen returns to, so LDIC's themed options screen can
 * replace it and still go back to the same place (the title screen or the pause menu).
 */
@Mixin(GuiOptions.class)
public interface AccessorGuiOptions {

    @Accessor("lastScreen")
    GuiScreen ldic$getLastScreen();
}
