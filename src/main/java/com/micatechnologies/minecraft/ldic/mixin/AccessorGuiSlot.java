package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** GuiSlot's background drawing, called when a list is not inside a skin (see MixinGuiSlot). */
@Mixin(GuiSlot.class)
public interface AccessorGuiSlot {

    @Invoker(value = "drawContainerBackground", remap = false)
    void ldic$drawContainerBackground(Tessellator tessellator);

    @Invoker("drawBackground")
    void ldic$drawBackground();

    @Invoker("getScrollBarX")
    int ldic$getScrollBarX();

    @Invoker("getSize")
    int ldic$getSize();

    @Invoker("overlayBackground")
    void ldic$overlayBackground(int startY, int endY, int startAlpha, int endAlpha);
}
