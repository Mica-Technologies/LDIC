package com.micatechnologies.minecraft.ldic.mixin;

import java.util.List;
import net.minecraft.client.gui.GuiListWorldSelection;
import net.minecraft.client.gui.GuiListWorldSelectionEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanilla's loaded world entries, which the themed world list draws and acts through. */
@Mixin(GuiListWorldSelection.class)
public interface AccessorGuiListWorldSelection {

    @Accessor("entries")
    List<GuiListWorldSelectionEntry> ldic$getEntries();
}
