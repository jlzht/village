package com.village.mod.village.profession

import com.village.mod.entity.projectile.SimpleFishingBobberEntity
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.entity.ai.goal.FishingGoal

class Fisherman : Profession() {
    override val type = ProfessionType.FISHERMAN
    private var fishHook: SimpleFishingBobberEntity? = null

    public fun getFishHook(): SimpleFishingBobberEntity? {
        return this.fishHook
    }

    public fun setFishHook(fishHook: SimpleFishingBobberEntity?) {
        this.fishHook = fishHook
    }
    override fun castAction(worker: CustomVillagerEntity) {
        if (this.fishHook == null) {
            worker.world.spawnEntity(SimpleFishingBobberEntity(worker, worker.world, 0, 0))
        }
    }
    public fun TryCatch(worker: CustomVillagerEntity) {
        if (this.fishHook != null) {
            (worker.getProfession() as Fisherman).fishHook?.pullBack()
        }
    }

    override fun addProfessionTasks(worker: CustomVillagerEntity) {
        worker.appendGoal(3, FishingGoal(worker))
    }
}
