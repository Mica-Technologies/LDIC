package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.gui.GuiRepair;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.inventory.ContainerRepair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** An anvil screen's container (its cost) and name box, for the inventory theme. */
@Mixin(GuiRepair.class)
public interface AccessorGuiRepair {

    @Accessor("anvil")
    ContainerRepair ldic$getAnvil();

    @Accessor("nameField")
    GuiTextField ldic$getNameField();
}
