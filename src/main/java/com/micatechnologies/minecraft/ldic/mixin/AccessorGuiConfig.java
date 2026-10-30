package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.client.config.GuiCheckBox;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.HoverChecker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * A Forge config screen's bottom buttons and their hover checks, which its own drawScreen updates;
 * the skin does the same each frame in its place.
 */
@Mixin(value = GuiConfig.class, remap = false)
public interface AccessorGuiConfig {

    @Accessor("btnDefaultAll")
    GuiButtonExt ldic$getDefaultAll();

    @Accessor("btnUndoAll")
    GuiButtonExt ldic$getUndoAll();

    @Accessor("chkApplyGlobally")
    GuiCheckBox ldic$getApplyGlobally();

    @Accessor("undoHoverChecker")
    HoverChecker ldic$getUndoHover();

    @Accessor("resetHoverChecker")
    HoverChecker ldic$getResetHover();

    @Accessor("checkBoxHoverChecker")
    HoverChecker ldic$getCheckBoxHover();
}
