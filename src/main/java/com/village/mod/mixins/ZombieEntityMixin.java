package com.village.mod.mixin;

import net.minecraft.entity.ai.goal.GoalSelector;
import com.village.mod.entity.village.CustomVillagerEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.world.World;
import net.minecraft.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.entity.mob.MobEntity;

import net.minecraft.entity.ai.goal.ActiveTargetGoal;

import net.minecraft.entity.mob.HostileEntity;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mixin(ZombieEntity.class)
public abstract class ZombieEntityMixin extends HostileEntity {
        protected ZombieEntityMixin(EntityType<? extends ZombieEntity> entityType, World world) {
                super(entityType, world);
        }
        @Inject(method = "Lnet/minecraft/entity/mob/ZombieEntity;initCustomGoals()V", at = @At("HEAD"), cancellable = true)
	      private void initCustomGoals(CallbackInfo info) {
          this.targetSelector.add(3, new ActiveTargetGoal<CustomVillagerEntity>(this, CustomVillagerEntity.class, true));
        }
}
