package com.micatechnologies.minecraft.ldic.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Whether the game is still running. While it shuts down, LDIC leaves the progress screen to
 * vanilla, which uses it to stop work in progress.
 */
@Mixin(Minecraft.class)
public interface AccessorMinecraft {

    @Accessor("running")
    boolean ldic$isRunning();
}
