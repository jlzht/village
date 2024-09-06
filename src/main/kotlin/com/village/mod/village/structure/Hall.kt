package com.village.mod.village.structure

import com.village.mod.action.Errand
import com.village.mod.util.Region
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

// TODO: give a purpose to this structure
class Hall(
    val lower: BlockPos,
    val upper: BlockPos,
) : Structure(0) {
    override val MAX_CAPACITY: Int = 3
    override val VOLUME_PER_RESIDENT: Int = 5
    override var type: StructureType = StructureType.HALL
    override var region: Region = Region(lower, upper)
    override val settlers: MutableList<Int> = MutableList(MAX_CAPACITY) { -1 }

    override fun getErrands(vid: Int): List<Errand>? = null

    override fun updateErrands(world: World) {}

    companion object {
        fun createStructure(
            pos: BlockPos,
            player: PlayerEntity,
        ): Structure? {
            val world = player.world
            val hall = Hall(pos, pos)
            return hall
        }
    }
}
