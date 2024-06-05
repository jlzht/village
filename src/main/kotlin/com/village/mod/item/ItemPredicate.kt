package com.village.mod.item

import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.item.ArmorItem
import net.minecraft.item.BowItem
import net.minecraft.item.CrossbowItem
import net.minecraft.item.HoeItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.RangedWeaponItem
import net.minecraft.item.SwordItem
import net.minecraft.item.ArrowItem

object ItemPredicate {
    val HOE: (Item) -> Boolean = { item -> item is HoeItem }
    val BOW: (Item) -> Boolean = { item -> item is BowItem }
    val ARROW: (Item) -> Boolean = { item -> item is ArrowItem }
    val SWORD: (Item) -> Boolean = { item -> item is SwordItem }
    val ARMOR: (Item) -> Boolean = { item -> item is ArmorItem }
    val CROSSBOW: (Item) -> Boolean = { item -> item is CrossbowItem }
    val WEAPON: (Item) -> Boolean = { item -> item is ArmorItem || item is SwordItem || item is BowItem || item is CrossbowItem || item is ArrowItem }
    val RANGED_WEAPON: (Item) -> Boolean = { item -> item is RangedWeaponItem }

    fun prefersNewEquipment(newStack: ItemStack, oldStack: ItemStack): Boolean {
        if (oldStack.isEmpty) return true
        val newItem = newStack.item
        val oldItem = oldStack.item
        if (newItem is ArmorItem) {
            if (EnchantmentHelper.hasBindingCurse(oldStack)) {
                return false
            } else {
                val newArmorItem = newItem
                val oldArmorItem = oldItem as ArmorItem
                if (newArmorItem.protection != oldArmorItem.protection) {
                    newArmorItem.protection > oldArmorItem.protection
                } else if (newArmorItem.toughness != oldArmorItem.toughness) {
                    newArmorItem.toughness > oldArmorItem.toughness
                } else {
                    prefersNewDamageableItem(newStack, oldStack)
                }
            }
        }
        return false
    }

    fun prefersNewDamageableItem(newStack: ItemStack, oldStack: ItemStack): Boolean {
        return if (newStack.damage < oldStack.damage || newStack.hasNbt() && !oldStack.hasNbt()) {
            true
        } else if (newStack.hasNbt() && oldStack.hasNbt()) {
            newStack.nbt!!.keys.any { it != "Damage" } && oldStack.nbt!!.keys.none { it != "Damage" }
        } else {
            false
        }
    }
}
