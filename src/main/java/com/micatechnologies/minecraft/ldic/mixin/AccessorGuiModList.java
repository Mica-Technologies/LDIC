package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.GuiModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The screen Forge's mod list returns to, for the themed one that replaces it. */
@Mixin(value = GuiModList.class, remap = false)
public interface AccessorGuiModList {

    @Accessor("mainMenu")
    GuiScreen ldic$getParent();
}
