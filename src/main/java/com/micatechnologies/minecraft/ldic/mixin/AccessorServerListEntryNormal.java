package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A saved server's icon texture, which vanilla's entry decodes from the server's favicon. */
@Mixin(ServerListEntryNormal.class)
public interface AccessorServerListEntryNormal {

    @Accessor("serverIcon")
    ResourceLocation ldic$getIconLocation();

    /** Null when the server sent no icon. */
    @Accessor("icon")
    DynamicTexture ldic$getIcon();
}
