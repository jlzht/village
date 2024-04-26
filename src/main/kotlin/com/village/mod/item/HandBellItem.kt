package com.village.mod.item

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World
import com.village.mod.world.event.HandBellUsageCallback

class HandBellItem(settings: Settings) : Item(settings) {
    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val itemStack = user.getStackInHand(hand)
        user.getItemCooldownManager().set(this, 10)
        if (!world.isClient) {
            itemStack.damage(1, user) { p -> p.sendToolBreakStatus(hand) }
            HandBellUsageCallback.EVENT.invoker().interact(user.getBlockPos(), world)
        }
        return TypedActionResult.success(itemStack, world.isClient)
    }
}
