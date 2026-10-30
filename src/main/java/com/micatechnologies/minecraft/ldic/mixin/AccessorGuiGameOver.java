package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** How the player died, for the death screen's skin. */
@Mixin(GuiGameOver.class)
public interface AccessorGuiGameOver {

    @Accessor("causeOfDeath")
    ITextComponent ldic$getCause();
}
