package com.village.mod.village.structure

import com.village.mod.LOGGER
import com.village.mod.action.Action
import com.village.mod.action.Errand
import com.village.mod.screen.Response
import com.village.mod.util.BlockIterator
import com.village.mod.util.Finder
import com.village.mod.util.Region
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class Pond(
    lower: BlockPos,
    upper: BlockPos,
) : Structure() {
    override val maxCapacity: Int = 4
    override val volumePerResident: Int = 32
    override var type: StructureType = StructureType.POND

    override var region: Region = Region(lower, upper)
    override val residents: MutableList<Int> = MutableList(maxCapacity) { -1 }
    override var capacity: Int
        get() = getResidents().size
        set(value) {
        }

    override fun updateErrands(world: World) {
        // add method to get Y height of region
        BlockIterator.CUBOID(region.lower.add(-1, 0, -1), region.upper.add(1, 0, 1)).let { entries ->
            entries.shuffled().take(5).forEach { taken ->
                if (world.getBlockState(taken).isOf(Blocks.WATER)) {
                    errands.add(Errand(Action.Type.FISH, taken))
                    if (region.volume() < 100 && !region.contains(taken)) {
                        region.append(taken)
                    }
                }
            }
        }
        // capacity = (region.volume() / 4).toInt()
    }

    override fun getErrands(vid: Int): List<Errand> {
        if (errands.isEmpty()) {
            return emptyList()
        }
        val taken = errands.take(1)
        errands.remove(taken.first())
        return taken
    }

    companion object {
        fun createStructure(
            pos: BlockPos,
            player: PlayerEntity,
        ): Structure? {
            val world = player.world
            // This check will be removed in the future
            val check = BlockIterator.BOTTOM(pos).all { world.getBlockState(it).isSolid } && world.getBlockState(pos.up()).isAir
            if (!check) {
                LOGGER.info("BLOCKS BELOW MUST BE SOLID")
                // TODO: implement response
                return null
            }

            Finder.findWaterBlock(pos, world)?.let { wpos ->
                BlockIterator.FLOOD_FILL(world, wpos, BlockIterator.RIVER_AVAILABLE_SPACE)?.let { (waterCount, _) ->
                    if (waterCount < 32) {
                        Response.SMALL_BODY_WATER.send(player)
                        return null
                    }
                    val pond = Pond(wpos, wpos)
                    Response.NEW_STRUCTURE.send(player, pond.type.name)
                    return pond
                } ?: run {
                    // flood fill fails and returns null if to many iteration occurs, in this case it means a river was found!
                    val pond = Pond(wpos, wpos)
                    Response.NEW_STRUCTURE.send(player, pond.type.name)
                    return pond
                }
            } ?: run {
                LOGGER.info("NOT ENOUGH WATER")
                // add reponse for NO WATER NEARBY
                return null
            }
        }
    }
}
