package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.achievement.GuiStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The statistics screen's shown list and whether it still waits for the server, for its skin. */
@Mixin(GuiStats.class)
public interface AccessorGuiStats {

    @Accessor("displaySlot")
    GuiSlot ldic$getShownList();

    /** True while the stats are downloading (vanilla's misnamed flag). */
    @Accessor("doesGuiPauseGame")
    boolean ldic$isLoading();
}
