package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptionsRowList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** An options row's two buttons, re-centred when a skin moves the list (see ScreenSkins). */
@Mixin(GuiOptionsRowList.Row.class)
public interface AccessorGuiOptionsRowListRow {

    @Accessor("buttonA")
    GuiButton ldic$getButtonA();

    @Accessor("buttonB")
    GuiButton ldic$getButtonB();
}
