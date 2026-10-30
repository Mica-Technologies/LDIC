package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** An inventory screen's panel size and position (full-screen scaling, its toggle, themes) and its layer drawing. */
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

    @Invoker("drawGuiContainerBackgroundLayer")
    void ldic$drawBackgroundLayer(float partialTicks, int mouseX, int mouseY);

    @Invoker("drawGuiContainerForegroundLayer")
    void ldic$drawForegroundLayer(int mouseX, int mouseY);
}
