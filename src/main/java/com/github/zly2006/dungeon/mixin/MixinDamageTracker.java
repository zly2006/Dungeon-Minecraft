package com.github.zly2006.dungeon.mixin;

import com.github.zly2006.dungeon.Dungeon;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DamageTracker.class)
public class MixinDamageTracker {
    @Shadow @Final private LivingEntity entity;

    @SuppressWarnings("ConstantValue")
    @Inject(
            method = "onDamage",
            at = @At("HEAD")
    )
    private void onDamage(DamageSource source, float amount, CallbackInfo ci) {
        if (entity instanceof ServerPlayerEntity serverPlayer && Dungeon.webSocketServer != null) {
            Dungeon.webSocketServer.onDamage(serverPlayer, source, amount);
        }
    }
}
