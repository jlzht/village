package com.village.mod.mixin;

import net.minecraft.block.BellBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.village.mod.world.event.VillageInteractionCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;
import net.minecraft.util.ActionResult;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

@Mixin(BellBlock.class)
public abstract class BellBlockMixin {

  // public final Logger dd = LoggerFactory.getLogger("village");
  @Inject(method = "ring", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;incrementStat(Lnet/minecraft/util/Identifier;)V", shift = At.Shift.AFTER), cancellable = true) 
  public void ring(World world, BlockState state, BlockHitResult hitResult, @Nullable PlayerEntity player, boolean checkHitPos, CallbackInfoReturnable<Boolean> cir) {
      if (player != null) {  
          ItemStack itemStack = player.getMainHandStack();
          if (itemStack.isOf(Items.NAME_TAG) && itemStack.hasCustomName()) {
              ActionResult result = VillageInteractionCallback.EVENT.invoker().interact(player, hitResult.getBlockPos(), itemStack.getName().getString());
              if(result == ActionResult.FAIL) {
                  cir.cancel();
              }
          }
      }
  }
}
