package com.github.zly2006.dungeon.mixin;

import com.github.zly2006.dungeon.Dungeon;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayer {
    @Shadow public ServerPlayNetworkHandler networkHandler;

    @SuppressWarnings("ConstantValue")
    @Inject(
            method = "onDeath",
            at = @At("HEAD")
    )
    private void onDeath(DamageSource source, CallbackInfo ci) {
        if (Dungeon.webSocketServer != null) {
            Dungeon.webSocketServer.onDeath(networkHandler.player, source);
        }
    }
}
