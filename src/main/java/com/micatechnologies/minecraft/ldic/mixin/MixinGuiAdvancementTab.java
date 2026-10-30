package com.micatechnologies.minecraft.ldic.mixin;

import com.micatechnologies.minecraft.ldic.client.menu.AdvancementMap;
import net.minecraft.client.gui.advancements.GuiAdvancementTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * An advancement tab's view is 234 by 113 pixels in vanilla; while a skin shows the map, it is
 * the size of the skin's box (centring, background tiles, scroll limits and the hover dimming).
 */
@Mixin(GuiAdvancementTab.class)
public abstract class MixinGuiAdvancementTab {

    @ModifyConstant(method = {"drawContents", "drawToolTips", "scroll"}, constant = @Constant(intValue = 234))
    private int ldic$viewWidth(int v) {
        return AdvancementMap.width(v);
    }

    @ModifyConstant(method = {"drawContents", "drawToolTips", "scroll"}, constant = @Constant(intValue = 113))
    private int ldic$viewHeight(int v) {
        return AdvancementMap.height(v);
    }

    @ModifyConstant(method = "drawContents", constant = @Constant(intValue = 117))
    private int ldic$centreX(int v) {
        return AdvancementMap.width(234) / 2;
    }

    @ModifyConstant(method = "drawContents", constant = @Constant(intValue = 56))
    private int ldic$centreY(int v) {
        return AdvancementMap.height(113) / 2;
    }

    /** The last background tile column and row. */
    @ModifyConstant(method = "drawContents", constant = @Constant(intValue = 15))
    private int ldic$tileColumns(int v) {
        return AdvancementMap.active() ? AdvancementMap.width(234) / 16 + 1 : v;
    }

    @ModifyConstant(method = "drawContents", constant = @Constant(intValue = 8))
    private int ldic$tileRows(int v) {
        return AdvancementMap.active() ? AdvancementMap.height(113) / 16 + 1 : v;
    }
}
