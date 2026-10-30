package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiWorldSelection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The screen to return to from vanilla's world selection, so the themed one returns there too. */
@Mixin(GuiWorldSelection.class)
public interface AccessorGuiWorldSelection {

    @Accessor("prevScreen")
    GuiScreen ldic$getPrevScreen();
}
