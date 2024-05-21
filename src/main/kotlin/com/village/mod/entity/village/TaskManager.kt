package com.village.mod.entity.village

import com.village.mod.village.villager.Task
import net.minecraft.util.math.BlockPos
import com.village.mod.LOGGER

class TaskManager {
    private var task: Task = Task.NONE 

    fun get(): Task {
        return task
    }

    fun set(t: Task) {
        task = t
    }
}
