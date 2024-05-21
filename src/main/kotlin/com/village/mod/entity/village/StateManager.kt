package com.village.mod.entity.village

import com.village.mod.village.villager.State
import com.village.mod.entity.village.CustomVillagerEntity

class StateManager(private val entity: CustomVillagerEntity) {
    private var state: State = State.NONE
    protected var lock = false

    fun lock() {
        this.lock = true
    }
    fun unlock() {
        this.lock = false
    }
    fun isAt(state: State): Boolean {
        return this.state == state
    }

    fun set(state: State) {
        when (this.state) {
            State.SLEEP -> {
                entity.wakeUp()
            }
            State.SWIM -> {
                entity.getNavigation().setCanSwim(false)
            } //
            State.CAST -> {} //
            State.RIDE -> {} // stop mouting
            State.IDLE -> {} // do nothing
            State.SIT -> {
                //entity.setPose(EntityPose.STANDING)
            }
            else -> {}
        }
        this.state = state
    }
    fun get(): State = this.state
}
