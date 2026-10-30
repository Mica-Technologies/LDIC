package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.gui.inventory.GuiBrewingStand;
import net.minecraft.inventory.IInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The brewing stand a brewing screen shows (its brew time and fuel), for the inventory theme. */
@Mixin(GuiBrewingStand.class)
public interface AccessorGuiBrewingStand {

    @Accessor("tileBrewingStand")
    IInventory ldic$getStand();
}
