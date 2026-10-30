package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.gui.inventory.GuiFurnace;
import net.minecraft.inventory.IInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The furnace a furnace screen shows (its burn and cook times), for the inventory theme. */
@Mixin(GuiFurnace.class)
public interface AccessorGuiFurnace {

    @Accessor("tileFurnace")
    IInventory ldic$getFurnace();
}
