package com.micatechnologies.minecraft.ldic.mixin;

import java.util.List;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.client.config.GuiEditArray;
import net.minecraftforge.fml.client.config.GuiEditArrayEntries;
import net.minecraftforge.fml.client.config.HoverChecker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A Forge list editor's titles, entries, buttons and tooltip, which its own drawScreen uses; the skin does the same. */
@Mixin(value = GuiEditArray.class, remap = false)
public interface AccessorGuiEditArray {

    @Accessor("entryList")
    GuiEditArrayEntries ldic$getEntries();

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
