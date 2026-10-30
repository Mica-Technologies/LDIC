package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** A button's per-frame drag update (sliders), run when a skin draws the button instead. */
@Mixin(GuiButton.class)
public interface AccessorGuiButton {

    @Invoker("mouseDragged")
    void ldic$mouseDragged(Minecraft mc, int mouseX, int mouseY);
}
