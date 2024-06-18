package com.village.mod.village.structure

import com.village.mod.utils.Utils
import com.village.mod.LOGGER
import com.village.mod.world.graph.Node
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.Heightmap
import net.minecraft.world.World

class Hall(val lower: BlockPos, val upper: BlockPos) : Structure() {
    override var type: StructureType = StructureType.HALL
    override var capacity: Int = 1
    override var area: Region = Region(lower, upper)

    companion object {
        private fun isWalkable(pos: BlockPos, world: World): Boolean {
            return world.getBlockState(pos.add(0, 1, 0)).isAir &&
                world.getBlockState(pos).isAir &&
                world.getBlockState(pos.add(0, -1, 0)).isSolid
        }

        fun createStructure(pos: BlockPos, player: PlayerEntity): Structure? {
            val world = player.world
            val listD = mutableListOf<BlockPos>()
            var k = 0
            for (df in Utils.iterateInCircuference(pos, 6)) {
                val sfd = world.getTopPosition(Heightmap.Type.WORLD_SURFACE, df)
                k = sfd.y
                if (isWalkable(sfd, world)) {
                    if (listD.isEmpty()) { listD.add(sfd); continue }
                    listD.map { it.getManhattanDistance(sfd) }.min().let { d ->
                        if (d >= 4) {
                            listD.addLast(sfd)
                        }
                    }
                }
            }
            if (!listD.all { it.y == k }) {
                return null
            }

            if (!listD.isEmpty()) {
                val upos = BlockPos(pos.x - 8, pos.y - 3, pos.z - 8)
                val kpos = BlockPos(pos.x + 8, pos.y + 3, pos.z + 8)
                LOGGER.info("{} --- {}", kpos, upos)
                val hall = Hall(upos, kpos)
                for (i in listD) {
                    if (i.getManhattanDistance(pos) > 7) {
                        hall.graph.addNode(Node(5, i, 0.5f, emptyList()))
                    } else {
                        hall.graph.addNode(Node(0, i, 0.5f, emptyList()))
                    }
                }
                return hall
            }
            return null
        }
    }
}
