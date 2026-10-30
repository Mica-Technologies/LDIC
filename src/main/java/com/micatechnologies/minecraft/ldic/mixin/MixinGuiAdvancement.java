package com.micatechnologies.minecraft.ldic.mixin;

import com.micatechnologies.minecraft.ldic.client.menu.AdvancementMap;
import net.minecraft.client.gui.advancements.GuiAdvancement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** An advancement's hover box flips upward near the bottom of the (skin-sized) view. */
@Mixin(GuiAdvancement.class)
public abstract class MixinGuiAdvancement {

    @ModifyConstant(method = "drawHover", constant = @Constant(intValue = 113))
    private int ldic$viewHeight(int v) {
        return AdvancementMap.height(v);
    }
}
