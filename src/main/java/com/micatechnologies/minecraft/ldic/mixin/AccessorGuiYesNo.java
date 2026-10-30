package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.gui.GuiYesNo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A confirmation's question and explanation, for its skin. */
@Mixin(GuiYesNo.class)
public interface AccessorGuiYesNo {

    @Accessor("messageLine1")
    String ldic$getLine1();

    @Accessor("messageLine2")
    String ldic$getLine2();
}
