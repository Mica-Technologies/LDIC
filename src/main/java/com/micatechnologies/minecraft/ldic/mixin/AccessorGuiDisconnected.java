package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Why the player was disconnected, for its skin. */
@Mixin(GuiDisconnected.class)
public interface AccessorGuiDisconnected {

    @Accessor("reason")
    String ldic$getReason();

    @Accessor("message")
    ITextComponent ldic$getMessage();
}
