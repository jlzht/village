package com.village.mod.village.structure

import com.village.mod.action.Errand
import com.village.mod.util.Region
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

// TODO: give a purpose to this structure
class Campfire(
    val lower: BlockPos,
    val upper: BlockPos,
) : Structure() {
    override val maxCapacity: Int = 4
    override val volumePerResident: Int = 32
    override var type: StructureType = StructureType.CAMPFIRE
    override var region: Region = Region(lower, upper)
    override val residents: MutableList<Int> = MutableList(maxCapacity) { -1 }
    override var capacity: Int
        get() = getResidents().size
        set(value) {
        }

    override fun getErrands(vid: Int): List<Errand>? = null

    override fun updateErrands(world: World) {}

    companion object {
        fun createStructure(
            pos: BlockPos,
            player: PlayerEntity,
        ): Structure? {
            val world = player.world
            val campfire = Campfire(pos.down().south(5).west(5), pos.up().north(5).east(5))
            return campfire
        }
    }
}
