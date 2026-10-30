package com.micatechnologies.minecraft.ldic.mixin;

import com.micatechnologies.minecraft.ldic.client.menu.AdvancementMap;
import net.minecraft.client.gui.advancements.GuiAdvancementTab;
import net.minecraft.client.gui.advancements.GuiScreenAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While a skin lists the tabs, a click on its tab column selects that tab, and vanilla's (unseen)
 * tabs above its window take no clicks.
 */
@Mixin(GuiScreenAdvancements.class)
public abstract class MixinGuiScreenAdvancements {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void ldic$skinTabClick(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (mouseButton == 0 && AdvancementMap.clicked((GuiScreenAdvancements) (Object) this, mouseX, mouseY)) {
            ci.cancel();
        }
    }

    @Redirect(method = "mouseClicked", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/advancements/GuiAdvancementTab;isMouseOver(IIII)Z"))
    private boolean ldic$skinTabs(GuiAdvancementTab tab, int x, int y, int mouseX, int mouseY) {
        return !AdvancementMap.active() && tab.isMouseOver(x, y, mouseX, mouseY);
    }
}
