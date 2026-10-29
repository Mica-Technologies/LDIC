package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Whether a connection is open yet: "Connecting to the server" or "Logging in". */
@Mixin(GuiConnecting.class)
public interface AccessorGuiConnecting {

    @Accessor("networkManager")
    NetworkManager ldic$getNetworkManager();
}
