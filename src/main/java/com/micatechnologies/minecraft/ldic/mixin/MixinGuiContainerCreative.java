package com.micatechnologies.minecraft.ldic.mixin;

import com.micatechnologies.minecraft.ldic.client.inventory.CreativeSearch;
import java.util.Collections;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.inventory.Container;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Creative tab sort and search (Decision 13). Every time vanilla rebuilds a tab's item list,
 * LDIC filters it by the tab's query and sorts it by the tab's saved order. Item tabs with no
 * search bar of their own get a search box in place of their title; the sort button sits in the
 * panel's top-right corner on every item tab, Search included.
 */
@Mixin(GuiContainerCreative.class)
public abstract class MixinGuiContainerCreative extends InventoryEffectRenderer implements CreativeSearch.Typing {

    @Shadow
    private static int selectedTabIndex;

    @Shadow
    private GuiTextField searchField;

    @Shadow
    private float currentScroll;

    @Unique
    private GuiTextField ldic$tabSearch;

    private MixinGuiContainerCreative(Container container) {
        super(container);
    }

    @Shadow
    private void updateCreativeSearch() {
    }

    @Override
    public boolean ldic$isTyping() {
        CreativeTabs tab = ldic$tab();
        return tab != null && (tab.hasSearchBar() && searchField != null && searchField.isFocused()
                || ldic$tabSearch != null && ldic$tabSearch.isFocused());
    }

    @Unique
    private static CreativeTabs ldic$tab() {
        return selectedTabIndex >= 0 && selectedTabIndex < CreativeTabs.CREATIVE_TAB_ARRAY.length
                ? CreativeTabs.CREATIVE_TAB_ARRAY[selectedTabIndex] : null;
    }

    @Unique
    private GuiContainerCreative.ContainerCreative ldic$container() {
        return (GuiContainerCreative.ContainerCreative) inventorySlots;
    }

    /** The tab's own box is drawn instead of its title while it is in use. */
    @Unique
    private boolean ldic$boxActive() {
        return ldic$tabSearch != null && CreativeSearch.hasOwnSearch(ldic$tab())
                && (ldic$tabSearch.isFocused() || !ldic$tabSearch.getText().isEmpty());
    }

    @Unique
    private void ldic$apply(CreativeTabs tab, String query) {
        CreativeSearch.apply(tab, ldic$container().itemList, query);
        currentScroll = 0;
        ldic$container().scrollTo(0);
    }

    /** Rebuilds the current tab from scratch with its query and order. */
    @Unique
    private void ldic$refresh() {
        CreativeTabs tab = ldic$tab();
        if (!CreativeSearch.sortable(tab)) {
            return;
        }
        if (tab.hasSearchBar()) {
            updateCreativeSearch();
        } else {
            CreativeSearch.fill(tab, ldic$container().itemList);
            ldic$apply(tab, ldic$tabSearch == null ? "" : ldic$tabSearch.getText());
        }
    }

    @Inject(method = "initGui", at = @At("RETURN"))
    private void ldic$createTabSearch(CallbackInfo ci) {
        if (searchField == null) {
            return;
        }
        ldic$tabSearch = new GuiTextField(1, fontRenderer, guiLeft + CreativeSearch.BOX_X + 3, guiTop + 6,
                CreativeSearch.BOX_W - 40, fontRenderer.FONT_HEIGHT);
        ldic$tabSearch.setMaxStringLength(50);
        ldic$tabSearch.setEnableBackgroundDrawing(false);
        ldic$tabSearch.setTextColor(0xFFFFFF);
    }

    @Inject(method = "updateScreen", at = @At("RETURN"))
    private void ldic$blink(CallbackInfo ci) {
        if (ldic$tabSearch != null) {
            ldic$tabSearch.updateCursorCounter();
        }
    }

    @Inject(method = "setCurrentCreativeTab", at = @At("RETURN"))
    private void ldic$afterTabChange(CreativeTabs tab, CallbackInfo ci) {
        // Tabs with a search bar went through updateCreativeSearch, which already ran the pass.
        if (tab == null || tab.hasSearchBar()) {
            return;
        }
        if (ldic$tabSearch != null) {
            ldic$tabSearch.setText("");
            ldic$tabSearch.setFocused(false);
        }
        ldic$apply(tab, "");
    }

    @Inject(method = "updateCreativeSearch", at = @At("RETURN"))
    private void ldic$afterSearch(CallbackInfo ci) {
        CreativeTabs tab = ldic$tab();
        if (searchField != null && tab != null) {
            ldic$apply(tab, searchField.getText());
        }
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void ldic$typeInTabSearch(char typedChar, int keyCode, CallbackInfo ci) {
        if (ldic$tabSearch == null || !CreativeSearch.hasOwnSearch(ldic$tab())) {
            return;
        }
        if (ldic$tabSearch.isFocused()) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                ldic$tabSearch.setFocused(false);
                ci.cancel();
                return;
            }
            String before = ldic$tabSearch.getText();
            if (ldic$tabSearch.textboxKeyTyped(typedChar, keyCode)) {
                if (!before.equals(ldic$tabSearch.getText())) {
                    ldic$refresh();
                }
                ci.cancel();
            }
        } else if (keyCode == Keyboard.KEY_F && GuiScreen.isCtrlKeyDown()) {
            ldic$tabSearch.setFocused(true);
            ci.cancel();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void ldic$clickSortOrSearch(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        CreativeTabs tab = ldic$tab();
        if (searchField == null || !CreativeSearch.sortable(tab)) {
            return;
        }
        if ((mouseButton == 0 || mouseButton == 1) && CreativeSearch.overSort(mouseX, mouseY, guiLeft, guiTop)) {
            CreativeSearch.clickSort(tab, mouseButton);
            ldic$refresh();
            ci.cancel();
            return;
        }
        if (ldic$tabSearch == null || !CreativeSearch.hasOwnSearch(tab)) {
            return;
        }
        if (CreativeSearch.overBox(mouseX, mouseY, guiLeft, guiTop)) {
            if (mouseButton == 1 && !ldic$tabSearch.getText().isEmpty()) {
                ldic$tabSearch.setText("");
                ldic$refresh();
            }
            ldic$tabSearch.setFocused(true);
            ldic$tabSearch.mouseClicked(mouseX, mouseY, 0);
            ci.cancel();
        } else {
            ldic$tabSearch.setFocused(false);
        }
    }

    @Redirect(method = "drawGuiContainerForegroundLayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/creativetab/CreativeTabs;drawInForegroundOfTab()Z"))
    private boolean ldic$hideTitleUnderBox(CreativeTabs tab) {
        return tab.drawInForegroundOfTab() && !ldic$boxActive();
    }

    @Inject(method = "drawGuiContainerBackgroundLayer", at = @At("RETURN"))
    private void ldic$drawBoxAndSort(float partialTicks, int mouseX, int mouseY, CallbackInfo ci) {
        CreativeTabs tab = ldic$tab();
        if (searchField == null || !CreativeSearch.sortable(tab)) {
            return;
        }
        if (ldic$tabSearch != null && CreativeSearch.hasOwnSearch(tab)) {
            int x = guiLeft + CreativeSearch.BOX_X;
            int y = guiTop + CreativeSearch.BOX_Y;
            int r = x + CreativeSearch.BOX_W;
            int b = y + CreativeSearch.BOX_H;
            if (ldic$boxActive()) {
                drawRect(x, y, r, b, ldic$tabSearch.isFocused() ? 0xFFE0E0E0 : 0xFF6F6F6F);
                drawRect(x + 1, y + 1, r - 1, b - 1, 0xFF000000);
                ldic$tabSearch.x = x + 3;
                ldic$tabSearch.y = y + 2;
                if (ldic$tabSearch.getText().isEmpty()) {
                    fontRenderer.drawString(I18n.format("ldic.creative.search.syntax"), x + 3, y + 2, 0xFF6F6F6F);
                }
                ldic$tabSearch.drawTextBox();
                String count = String.valueOf(ldic$container().itemList.size());
                fontRenderer.drawString(count, r - 3 - fontRenderer.getStringWidth(count), y + 2, 0xFF8F8F8F);
            } else if (CreativeSearch.overBox(mouseX, mouseY, guiLeft, guiTop)) {
                drawRect(x, y, r, b, 0x30000000);
            }
        } else if (tab == CreativeTabs.SEARCH && searchField.getText().isEmpty()) {
            fontRenderer.drawString(I18n.format("ldic.creative.search.syntax"), searchField.x, searchField.y, 0xFF6F6F6F);
        }
        CreativeSearch.drawSortButton(fontRenderer, tab, guiLeft, guiTop,
                CreativeSearch.overSort(mouseX, mouseY, guiLeft, guiTop));
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void ldic$sortTooltip(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        CreativeTabs tab = ldic$tab();
        if (searchField == null || !CreativeSearch.sortable(tab)) {
            return;
        }
        if (CreativeSearch.overSort(mouseX, mouseY, guiLeft, guiTop)) {
            drawHoveringText(CreativeSearch.sortTooltip(tab, ldic$container().itemList.size()), mouseX, mouseY);
        } else if (ldic$tabSearch != null && CreativeSearch.hasOwnSearch(tab) && !ldic$boxActive()
                && CreativeSearch.overBox(mouseX, mouseY, guiLeft, guiTop)) {
            drawHoveringText(Collections.singletonList(I18n.format("ldic.creative.search.tip")), mouseX, mouseY);
        }
    }
}
