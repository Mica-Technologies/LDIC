package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** An inventory screen's panel size and position, for full-screen scaling and its toggle button. */
@Mixin(GuiContainer.class)
public interface AccessorGuiContainer {

    @Accessor("xSize")
    int ldic$getXSize();

    @Accessor("ySize")
    int ldic$getYSize();

    @Accessor("guiLeft")
    int ldic$getGuiLeft();

    @Accessor("guiTop")
    int ldic$getGuiTop();
}
