package com.village.mod.world

import com.village.mod.MODID
import com.village.mod.screen.Response
import com.village.mod.world.graph.Graph
import com.village.mod.world.graph.Node
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.world.PersistentState
import net.minecraft.world.World
import java.util.UUID

data class StructureNodeRef(val structure: Int = -1, val node: Int = -1)
data class PlayerDataField(var reputation: Int, var uuid: UUID)

class SettlementLoader : PersistentState() {
    // TODO: use HashMap inside of a Manager class instead of MutableList
    // TODO: find better way of gen map key
    val villages = mutableListOf<Settlement>()

    fun addVillage(name: String, pos: BlockPos, player: PlayerEntity): Settlement? {
        val k = Settlement.getAvailableKey(villages.map { it.id })
        Settlement(true, k, name, pos).let {
            if (it.createStructure(pos, player)) {
                it.allies.add(PlayerDataField(50, player.getUuid()))
                villages.add(it)
                Response.NEW_SETTLEMENT.send(player, name)
                return it
            }
        }
        Response.TOO_OBSTRUCTED.send(player)
        return null
    }

    override fun writeNbt(nbt: NbtCompound): NbtCompound {
        nbt.put("Villages", settlementSerialize())
        return nbt
    }

    fun settlementSerialize(): NbtList {
        val nbtList = NbtList()
        for (village in villages) {
            nbtList.add(village.toNbt())
        }
        return nbtList
    }

    companion object {
        fun createFromNbt(tag: NbtCompound): SettlementLoader {
            val state = SettlementLoader()
            val settlement = tag.getList("Villages", NbtElement.COMPOUND_TYPE.toInt())
            for (i in 0 until settlement.size) {
                val data = settlement.getCompound(i)
                val set = Settlement.fromNbt(data)
                nodeSettlementDeserialize(set.graph, data.getList("NodesData", NbtElement.COMPOUND_TYPE.toInt()))
                state.villages.add(set)
            }
            return state
        }

        fun nodeSettlementDeserialize(graph: Graph<StructureNodeRef>, nbtList: NbtList) {
            for (l in 0 until nbtList.size) {
                val data = nbtList.getCompound(l)
                val node = Node<StructureNodeRef>(
                    StructureNodeRef(
                        data.getInt("NodeDataStructure"),
                        data.getInt("NodeDataID"),
                    ),
                    BlockPos(data.getInt("NodePosX"), data.getInt("NodePosY"), data.getInt("NodePosZ")),
                    0.5f,
                    data.getIntArray("NodeConnectionsKeys").toList(),
                )
                graph.loadNode(data.getInt("NodeKey"), node)
            }
        }

        private val type = Type({ SettlementLoader() }, ::createFromNbt, null)

        fun getServerState(server: MinecraftServer): SettlementLoader {
            val persistentStateManager = server.getWorld(World.OVERWORLD)?.persistentStateManager
                ?: throw IllegalStateException("Failed to get PersistentStateManager for OVERWORLD")

            val state = persistentStateManager.getOrCreate(type, MODID)
            state.markDirty()
            return state
        }
    }
}
