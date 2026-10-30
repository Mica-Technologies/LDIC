package com.micatechnologies.minecraft.ldic.mixin;

import java.util.List;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.client.config.GuiSelectString;
import net.minecraftforge.fml.client.config.GuiSelectStringEntries;
import net.minecraftforge.fml.client.config.HoverChecker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A Forge value picker's titles, entries, buttons and tooltip, which its own drawScreen uses; the skin does the same. */
@Mixin(value = GuiSelectString.class, remap = false)
public interface AccessorGuiSelectString {

    @Accessor("entryList")
    GuiSelectStringEntries ldic$getEntries();

    @Accessor("btnDone")
    GuiButtonExt ldic$getDone();

    @Accessor("btnDefault")
    GuiButtonExt ldic$getDefault();

    @Accessor("btnUndoChanges")
    GuiButtonExt ldic$getUndo();

    @Accessor("title")
    String ldic$getTitle();

    @Accessor("titleLine2")
    String ldic$getTitle2();

    @Accessor("titleLine3")
    String ldic$getTitle3();

    @Accessor("tooltipHoverChecker")
    HoverChecker ldic$getTooltipHover();

    @Accessor("toolTip")
    List<String> ldic$getTooltip();

    @Accessor("enabled")
    boolean ldic$isEnabled();
}
