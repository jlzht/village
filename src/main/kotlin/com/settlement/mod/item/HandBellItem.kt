package com.settlement.mod.item

import com.settlement.mod.world.event.HandBellUsageCallback
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.RaycastContext
import net.minecraft.world.World

class HandBellItem(settings: Settings) : Item(settings) {
    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val itemStack = user.getStackInHand(hand)
        val blockHitResult: BlockHitResult = Item.raycast(world, user, RaycastContext.FluidHandling.NONE)
        if (blockHitResult.getType() == HitResult.Type.MISS) {
            return TypedActionResult.pass(itemStack)
        }
        if (blockHitResult.getType() == HitResult.Type.BLOCK) {
            val pos: BlockPos = blockHitResult.getBlockPos()
            user.getItemCooldownManager().set(this, 45)
            if (!world.isClient) {
                itemStack.damage(1, user) { p -> p.sendToolBreakStatus(hand) }
                HandBellUsageCallback.EVENT.invoker().interact(user, pos)
                return TypedActionResult.success(itemStack, world.isClient)
            }
            return TypedActionResult.fail(itemStack)
        }
        return TypedActionResult.pass(itemStack)
    }
}
