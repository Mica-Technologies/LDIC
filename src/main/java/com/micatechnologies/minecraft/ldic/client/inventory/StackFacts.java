package com.micatechnologies.minecraft.ldic.client.inventory;

import com.micatechnologies.minecraft.ldic.inventory.ItemQuery;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

/** {@link ItemQuery.Facts} for one stack, each fact computed on first use. */
final class StackFacts implements ItemQuery.Facts {

    private static final Map<String, String> MOD_NAMES = new HashMap<>();

    private final ItemStack stack;
    private String name;
    private String id;
    private List<String> tooltip;

    StackFacts(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public String name() {
        if (name == null) {
            name = safe(() -> stack.getDisplayName());
        }
        return name;
    }

    @Override
    public String id() {
        if (id == null) {
            ResourceLocation rl = stack.getItem().getRegistryName();
            id = rl == null ? "" : rl.toString();
        }
        return id;
    }

    @Override
    public String modId() {
        String full = id();
        int colon = full.indexOf(':');
        return colon < 0 ? "minecraft" : full.substring(0, colon);
    }

    @Override
    public String modName() {
        return MOD_NAMES.computeIfAbsent(modId(), StackFacts::lookUpModName);
    }

    @Override
    public List<String> tooltip() {
        if (tooltip == null) {
            Minecraft mc = Minecraft.getMinecraft();
            try {
                tooltip = stack.getTooltip(mc.player, mc.gameSettings.advancedItemTooltips
                        ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
            } catch (RuntimeException e) {
                // A mod's tooltip code can throw for a stack it never expected to see out of context.
                tooltip = Collections.singletonList(name());
            }
        }
        return tooltip;
    }

    private static String lookUpModName(String modId) {
        if ("minecraft".equals(modId)) {
            return "Minecraft";
        }
        ModContainer mod = Loader.instance().getIndexedModList().get(modId);
        return mod == null ? modId : mod.getName();
    }

    private static String safe(Supplier<String> s) {
        try {
            String v = s.get();
            return v == null ? "" : v;
        } catch (RuntimeException e) {
            return "";
        }
    }
}
