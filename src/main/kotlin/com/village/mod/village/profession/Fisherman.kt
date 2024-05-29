package com.village.mod.village.profession

import com.village.mod.entity.projectile.SimpleFishingBobberEntity
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.structure.StructureType
import net.minecraft.item.FishingRodItem
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.item.ItemStack
import com.village.mod.LOGGER

class Fisherman : Profession() {
    override val type = ProfessionType.FISHERMAN
    private var fishHook: SimpleFishingBobberEntity? = null
    override val desiredItems: (Item) -> Boolean = { item -> item is FishingRodItem }

    public fun getFishHook(): SimpleFishingBobberEntity? {
        return this.fishHook
    }

    public fun setFishHook(fishHook: SimpleFishingBobberEntity?) {
        this.fishHook = fishHook
    }
    override fun canWork(worker: CustomVillagerEntity): Boolean {
        if (!worker.isHolding(Items.FISHING_ROD)) {
            val rod = worker.takeItem(worker) { item -> item is FishingRodItem }
            if (rod != ItemStack.EMPTY) {
                worker.tryEquip(rod)
            }
            return false
        }
        return true
    }

    override fun doWork(worker: CustomVillagerEntity) {
        if (this.fishHook == null) {
            worker.world.spawnEntity(SimpleFishingBobberEntity(worker, worker.world, 0, 0))
            worker.setActing(true)
        }
    }

    public fun TryCatch(worker: CustomVillagerEntity) {
        if (this.fishHook != null) {
            (worker.getProfession() as Fisherman).fishHook?.pullBack()
        }
    }

    override fun addProfessionTasks(worker: CustomVillagerEntity) {}
    override val structureInterest: StructureType = StructureType.POND
}
