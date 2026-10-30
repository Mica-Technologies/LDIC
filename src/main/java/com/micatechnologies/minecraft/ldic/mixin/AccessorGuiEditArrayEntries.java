package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraftforge.fml.client.config.GuiEditArrayEntries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** A Forge list editor's entries: whether the list can be saved, and their tooltips. */
@Mixin(value = GuiEditArrayEntries.class, remap = false)
public interface AccessorGuiEditArrayEntries {

    @Invoker("isListSavable")
    boolean ldic$isListSavable();

    @Invoker("drawScreenPost")
    void ldic$drawScreenPost(int mouseX, int mouseY, float partialTicks);
}
