package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.tileentity.TileEntitySign;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The sign being edited, the line being typed and the cursor's blink counter, for its skin. */
@Mixin(GuiEditSign.class)
public interface AccessorGuiEditSign {

    @Accessor("tileSign")
    TileEntitySign ldic$getSign();

    @Accessor("editLine")
    int ldic$getEditLine();

    @Accessor("updateCounter")
    int ldic$getUpdateCounter();
}
