package com.village.mod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.google.common.collect.ImmutableList;
import net.minecraft.world.spawner.SpecialSpawner;
import net.minecraft.world.spawner.PatrolSpawner;
import net.minecraft.world.spawner.PhantomSpawner;
import net.minecraft.server.MinecraftServer;

import net.minecraft.server.WorldGenerationProgressListener;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.village.mod.world.spawner.VillagerSpawner;
//(Lnet/minecraft/server/MinecraftServer;)Lcom/google/common/collect/ImmutableList;
//(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Lcom/google/common/collect/ImmutableList;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
  @Redirect(method = "createWorlds", remap = false, at = @At( value = "INVOKE", target = "com/google/common/collect/ImmutableList", ordinal = 0))
  private <T> ImmutableList<SpecialSpawner> coolSpawner(T a, T b, T c, T e, T d) {
      return ImmutableList.of( new PatrolSpawner(), new VillagerSpawner(), new PatrolSpawner(), new PatrolSpawner(), new PatrolSpawner() );
  }
}
