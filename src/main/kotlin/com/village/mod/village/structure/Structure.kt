package com.village.mod.village.structure

import com.village.mod.LOGGER
import com.village.mod.action.Errand
import com.village.mod.util.Region
import net.minecraft.world.World

// TODO:
// - make errands field a set?
sealed class Structure(
    var capacity: Int,
) {
    abstract val MAX_CAPACITY: Int
    abstract val VOLUME_PER_RESIDENT: Int
    abstract val type: StructureType
    abstract var region: Region

    val errands = mutableListOf<Errand>()
    abstract val settlers: MutableList<Int>

    fun hasErrands(): Boolean = errands.isNotEmpty()

    abstract fun updateErrands(world: World)

    abstract fun getErrands(vid: Int): List<Errand>?

    fun showErrands() {
        errands.forEach { e ->
            LOGGER.info("Action:{} - Pos:{}", e.cid, e.pos)
        }
    }

    fun isAvailable(): Boolean = settlers.count { it != -1 } <= capacity

    fun addResident(vid: Int) {
        settlers.set(settlers.indexOfFirst { it == -1 }, vid)
    }

    fun getResidentIndex(vid: Int): Int {
        var i: Int = 1
        for (settler in settlers) {
            if (settler == vid) {
                return i
            } else {
                i++
            }
        }
        return 0
    }

    fun removeResident(vid: Int) {
        settlers.set(settlers.indexOf(vid), -1)
    }

    fun getResidents(): List<Int> = settlers.filter { it != -1 }
}
