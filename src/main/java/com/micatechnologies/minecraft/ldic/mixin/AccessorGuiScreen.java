package com.micatechnologies.minecraft.ldic.mixin;

import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * A vanilla screen's buttons, so LDIC can draw them over its world-loading screen, and its hover
 * box for chat text (the chat skin draws it as vanilla's chat screen does).
 */
@Mixin(GuiScreen.class)
public interface AccessorGuiScreen {

    @Accessor("buttonList")
    List<GuiButton> ldic$getButtonList();

    @Invoker("handleComponentHover")
    void ldic$handleComponentHover(ITextComponent component, int x, int y);
}
