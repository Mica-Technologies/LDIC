package com.micatechnologies.minecraft.ldic.mixin;

import java.util.List;
import net.minecraft.client.gui.ServerListEntryLanDetected;
import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.client.gui.ServerSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The saved and LAN server entries, in vanilla's order (saved, the LAN scan row, LAN). */
@Mixin(ServerSelectionList.class)
public interface AccessorServerSelectionList {

    @Accessor("serverListInternet")
    List<ServerListEntryNormal> ldic$getSaved();

    @Accessor("serverListLan")
    List<ServerListEntryLanDetected> ldic$getLan();
}
