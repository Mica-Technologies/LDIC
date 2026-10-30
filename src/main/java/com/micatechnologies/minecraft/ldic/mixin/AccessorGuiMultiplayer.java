package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ServerSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanilla's multiplayer screen state the themed server list reads (its own list is never drawn). */
@Mixin(GuiMultiplayer.class)
public interface AccessorGuiMultiplayer {

    @Accessor("parentScreen")
    GuiScreen ldic$getParentScreen();

    @Accessor("serverListSelector")
    ServerSelectionList ldic$getServerListSelector();
}
