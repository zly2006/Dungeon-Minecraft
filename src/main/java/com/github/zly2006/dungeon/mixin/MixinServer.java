package com.github.zly2006.dungeon.mixin;

import com.github.zly2006.dungeon.Dungeon;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("ConstantValue")
@Mixin(MinecraftServer.class)
public class MixinServer {
    @Inject(
            method = "tick",
            at = @At("HEAD")
    )
    private void onTick(CallbackInfo ci) {
        if (Dungeon.webSocketServer != null) {
            Dungeon.webSocketServer.tick();
        }
    }
}
