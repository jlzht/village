//package com.village.mod.item
//
//import com.village.mod.entity.projectile.SimpleFishingBobberEntity
//import com.village.mod.entity.village.VillagerEntity
//import net.minecraft.item.ItemStack
//import net.minecraft.item.Item
//import net.minecraft.util.TypedActionResult
//import net.minecraft.util.Hand
//import net.minecraft.world.World
//
//class FishingRodItem(settings: Item.Settings) : Item(settings) {
//    public fun use(
//        world: World,
//        user: VillagerEntity,
//        hand: Hand,
//    ): TypedActionResult<ItemStack> {
//        var itemStack: ItemStack = user.getStackInHand(hand)
//        if (user.FishHook != null) {
//            if (!world.isClient) {
//                val i: Int = user.FishHook!!.use(itemStack)
//                itemStack.damage(i, user, { p -> p.sendToolBreakStatus(hand) })
//            }
//        } else {
//            if (!world.isClient) {
//                world.spawnEntity(SimpleFishingBobberEntity(user, world, 0, 0))
//            }
//        }
//        return TypedActionResult.success(itemStack, world.isClient())
//    }
//}
