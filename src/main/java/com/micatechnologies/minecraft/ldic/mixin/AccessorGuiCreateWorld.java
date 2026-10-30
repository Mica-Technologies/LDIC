package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.gui.GuiCreateWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The strings Create New World draws itself, for its skin to show (see ScreenSkins). */
@Mixin(GuiCreateWorld.class)
public interface AccessorGuiCreateWorld {

    @Accessor("saveDirName")
    String ldic$getSaveDirName();

    @Accessor("gameModeDesc1")
    String ldic$getGameModeDesc1();

    @Accessor("gameModeDesc2")
    String ldic$getGameModeDesc2();

    @Accessor("inMoreWorldOptionsDisplay")
    boolean ldic$isMoreOptions();

    @Accessor("selectedIndex")
    int ldic$getWorldTypeIndex();
}
