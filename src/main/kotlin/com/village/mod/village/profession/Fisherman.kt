package com.village.mod.village.profession

import com.village.mod.entity.projectile.SimpleFishingBobberEntity
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.structure.StructureType
import net.minecraft.item.FishingRodItem
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.item.ItemStack
import com.village.mod.LOGGER

class Fisherman(villager: CustomVillagerEntity) : Profession(villager) {
    override val type = ProfessionType.FISHERMAN
    private var fishHook: SimpleFishingBobberEntity? = null
    override val desiredItems: (Item) -> Boolean = { item -> item is FishingRodItem }

    public fun getFishHook(): SimpleFishingBobberEntity? {
        return this.fishHook
    }

    public fun setFishHook(fishHook: SimpleFishingBobberEntity?) {
        this.fishHook = fishHook
    }
    override fun canWork(): Boolean {
        if (!villager.isHolding(Items.FISHING_ROD)) {
            val rod = villager.inventory.takeItem({ item -> item is FishingRodItem })
            if (rod != ItemStack.EMPTY) {
                villager.tryEquip(rod)
            }
            return false
        }
        return true
    }

    override fun doWork() {
        if (this.fishHook == null) {
            villager.world.spawnEntity(SimpleFishingBobberEntity(villager, villager.world, 0, 0))
            villager.setActing(true)
        }
    }

    public fun TryCatch(villager: CustomVillagerEntity) {
        if (this.fishHook != null) {
            (villager.getProfession() as Fisherman).fishHook?.pullBack()
        }
    }

    override val structureInterest: StructureType = StructureType.POND
}
