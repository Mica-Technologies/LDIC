package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.gui.GuiListWorldSelectionEntry;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.WorldSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** One vanilla world entry: its summary and the icon texture vanilla already loaded. */
@Mixin(GuiListWorldSelectionEntry.class)
public interface AccessorGuiListWorldSelectionEntry {

    @Accessor("worldSummary")
    WorldSummary ldic$getSummary();

    @Accessor("iconLocation")
    ResourceLocation ldic$getIconLocation();

    /** Null when the world has no (valid) icon. */
    @Accessor("icon")
    DynamicTexture ldic$getIcon();
}
