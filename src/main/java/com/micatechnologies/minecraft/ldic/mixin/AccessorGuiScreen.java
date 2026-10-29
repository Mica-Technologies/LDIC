package com.micatechnologies.minecraft.ldic.mixin;

import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A vanilla screen's buttons, so LDIC can draw them over its world-loading screen. */
@Mixin(GuiScreen.class)
public interface AccessorGuiScreen {

    @Accessor("buttonList")
    List<GuiButton> ldic$getButtonList();
}
