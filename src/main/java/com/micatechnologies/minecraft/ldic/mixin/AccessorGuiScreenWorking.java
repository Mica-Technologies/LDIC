package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.gui.GuiScreenWorking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** What vanilla's "working" screen is showing, for LDIC's world-loading screen. */
@Mixin(GuiScreenWorking.class)
public interface AccessorGuiScreenWorking {

    @Accessor("title")
    String ldic$getTitle();

    @Accessor("stage")
    String ldic$getStage();

    @Accessor("progress")
    int ldic$getProgress();
}
