package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.gui.advancements.GuiAdvancementTab;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** An advancement tab's icon, for the skin's tab list. */
@Mixin(GuiAdvancementTab.class)
public interface AccessorGuiAdvancementTab {

    @Accessor("icon")
    ItemStack ldic$getIcon();
}
