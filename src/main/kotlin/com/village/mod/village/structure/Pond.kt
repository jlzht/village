package com.village.mod.village.structure

import com.village.mod.LOGGER
import com.village.mod.action.Action
import com.village.mod.action.Errand
import com.village.mod.screen.Response
import com.village.mod.util.BlockIterator
import com.village.mod.util.Region
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class Pond(
    lower: BlockPos,
    upper: BlockPos,
) : Structure(1) {
    override val MAX_CAPACITY: Int = 4
    override val VOLUME_PER_RESIDENT: Int = 4
    override var type: StructureType = StructureType.POND

    override var region: Region = Region(lower, upper)
    override val settlers: MutableList<Int> = MutableList(MAX_CAPACITY) { -1 }

    override fun updateErrands(world: World) {
        val rand = world.random.nextInt(5) + 2
        for (i in 0 until rand) {
            getWaterBlock(region.center(), world)?.let { wpos ->
                LOGGER.info("+")
                errands.add(Errand(Action.Type.FISH, wpos))
            }
        }
    }

    override fun sortErrands() {}

    override fun getErrands(vid: Int): List<Errand> {
        val amount = errands.size / capacity
        if (amount <= errands.size) return emptyList()
        return errands.take(amount).run { errands.drop(amount) }
    }

    private fun getPondAction(
        pos: BlockPos,
        world: World,
    ): Action.Type? {
        val wpos = getWaterBlock(pos, world)
        if (wpos != null) {
            LOGGER.info("FISSHIN TIME")
            return Action.Type.FISH
        }
        LOGGER.info("UNBALBE")
        return null
    }

    private fun validateWaterBody(
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
    ): Boolean {
        var waterCellCount = 0
        val stack = mutableListOf<BlockPos>()
        val visited = mutableSetOf<BlockPos>()
        stack.add(pos)

        while (stack.isNotEmpty() && waterCellCount < 32) {
            val current = stack.removeLast()
            if (!visited.contains(current)) {
                visited.add(current)
                if (world.getBlockState(current).isOf(Blocks.WATER)) {
                    waterCellCount++
                    stack.addAll(
                        listOf(
                            current.north(),
                            current.south(),
                            current.east(),
                            current.west(),
                            current.up(),
                            current.down(),
                        ).filter { it !in visited && it !in stack },
                    )
                }
            }
        }

        if (waterCellCount < 8) {
            Response.SMALL_BODY_WATER.send(player)
            return false
        }

        return true
    }

    companion object {
        fun getWaterBlock(
            pos: BlockPos,
            world: World,
        ): BlockPos? {
            val water =
                BlockPos.findClosest(pos, 8, 5, { fpos ->
                    world.getBlockState(fpos).isOf(Blocks.WATER) &&
                        world.getBlockState(fpos.up()).isOf(Blocks.AIR) &&
                        world.getBlockState(fpos.north()).isOf(Blocks.WATER) &&
                        world.getBlockState(fpos.south()).isOf(Blocks.WATER) &&
                        world.getBlockState(fpos.west()).isOf(Blocks.WATER) &&
                        world.getBlockState(fpos.east()).isOf(Blocks.WATER) &&
                        world.getBlockState(fpos.down()).isOf(Blocks.WATER)
                })
            return water.orElse(null)
        }

        fun createStructure(
            pos: BlockPos,
            player: PlayerEntity,
        ): Structure? {
            val world = player.world
            // TODO: put on the future general utils object
            val check = BlockIterator.BOTTOM(pos).all { world.getBlockState(it).isSolid }

            if (!check) {
                LOGGER.info("BLOCKS BELOW MUST BE SOLID")
                // TODO: implement response
                return null
            }

            val pond = Pond(pos.south().west().down(), pos.north().east().up())
            val center = pond.region.center()
            val wpos = getWaterBlock(center, world)
            if (wpos != null && !pond.validateWaterBody(world, wpos, player)) {
                return null
            }
            Response.NEW_STRUCTURE.send(player, pond.type.name)
            return pond
        }
    }
}
