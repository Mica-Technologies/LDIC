package com.micatechnologies.minecraft.ldic.mixin;

import java.util.Map;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.gui.advancements.GuiAdvancementTab;
import net.minecraft.client.gui.advancements.GuiScreenAdvancements;
import net.minecraft.client.multiplayer.ClientAdvancementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The advancement screen's tabs and selection, for its skin. */
@Mixin(GuiScreenAdvancements.class)
public interface AccessorGuiScreenAdvancements {

    @Accessor("tabs")
    Map<Advancement, GuiAdvancementTab> ldic$getTabs();

    @Accessor("selectedTab")
    GuiAdvancementTab ldic$getSelectedTab();

    @Accessor("clientAdvancementManager")
    ClientAdvancementManager ldic$getManager();
}
