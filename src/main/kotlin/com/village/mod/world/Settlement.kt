package com.village.mod.world

import com.village.mod.client.render.debug.DebugGraph
import com.village.mod.client.render.debug.Vertex
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.entity.village.Errand
import com.village.mod.network.NodeGraphPacket
import com.village.mod.screen.Response
import com.village.mod.village.structure.Building
import com.village.mod.village.structure.Farm
import com.village.mod.village.structure.Hall
import com.village.mod.village.structure.Pond
import com.village.mod.village.structure.Structure
import com.village.mod.village.structure.StructureType
import com.village.mod.village.villager.Action
import com.village.mod.world.graph.Graph
import com.village.mod.world.graph.Node
import net.minecraft.block.BarrelBlock
import net.minecraft.block.BellBlock
import net.minecraft.block.DoorBlock
import net.minecraft.block.FarmlandBlock
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos

class Settlement(var isLoaded: Boolean, val id: Int, val name: String, val pos: BlockPos) {
    // TODO: make structures,villagers,allies immutable by messing with constructor
    var structures = mutableMapOf<Int, Structure>()
    var villagers = mutableMapOf<Int, CustomVillagerEntity?>()
    var allies = arrayListOf<PlayerDataField>()
    val graph = Graph<StructureNodeRef>()
    constructor(
        isLoaded: Boolean,
        id: Int,
        name: String,
        pos: BlockPos,
        structures: MutableMap<Int, Structure>,
        villagers: MutableMap<Int, CustomVillagerEntity?>,
        allies: ArrayList<PlayerDataField>,
    ) : this(isLoaded, id, name, pos) {
        this.structures = structures
        this.villagers = villagers
        this.allies = allies
    }

    fun createStructure(pos: BlockPos, player: PlayerEntity): Boolean {
        when (player.world.getBlockState(pos).getBlock()) {
            is FarmlandBlock -> {
                if (!this.isStructureInRange(pos, 16.0f)) {
                    Farm.createStructure(pos, player)
                } else Response.ANOTHER_STRUCTURE_CLOSE.send(player).run { null }
            }
            is BarrelBlock -> { // Find a way to interact with water
                if (!this.isStructureInRange(pos, 8.0f)) {
                    Pond.createStructure(pos, player)
                } else Response.ANOTHER_STRUCTURE_CLOSE.send(player).run { null }
            }
            is DoorBlock -> {
                if (!this.isStructureInRegion(pos)) {
                    Building.createStructure(pos, player)
                } else Response.ANOTHER_STRUCTURE_INSIDE.send(player).run { null }
            }
            is BellBlock -> {
                Hall.createStructure(pos, player)
            }
            else -> Response.INVALID_BLOCK.send(player).run { null }
        }?.let {
            this.addStructure(it, player)
            return true
        }
        return false
    }

    // FIXME: player should NOT be passed
    fun addStructure(structure: Structure, player: PlayerEntity) {
        // TODO: create method that finds allies players by UUID and sendMessage to them notifying structure creating
        val key = Settlement.getAvailableKey(this.structures.map { it.key })
        // abstract this better

        structure.graph.getNodes().filter { it.value.cid == 5 }.forEach {
            val f = this.graph.addNode(
                Node(
                    if (structure.type != StructureType.HALL) { StructureNodeRef(key, it.key) } else { StructureNodeRef() },
                    it.value.pos,
                    0.5f,
                    emptyList(),
                ),
            )
            if (structure.type != StructureType.HALL) {
                this.graph.tryMerge(f, player.world, -1, 2)
            } else {
                this.graph.tryMerge(f, player.world, -1, 1)
            }
        }
        this.structures[key] = structure

        // TODO: find apropriette place to put this
        val debugGraph = mutableMapOf<DebugGraph, MutableList<DebugGraph>>()
        debugGraph.put(
            DebugGraph(this.graph.getConnections(), this.graph.getNodes().map { Vertex(it.value.pos.toCenterPos()) }.toSet()),
            this.structures.values.map { st ->
                DebugGraph(st.graph.getConnections(), st.graph.getNodes().map { Vertex(it.value.pos.toCenterPos()) }.toSet())
            }.toMutableList(),
        )
        if (player is ServerPlayerEntity) {
            NodeGraphPacket.sendToClient(player, debugGraph)
        }
    }

    fun removeStructure(id: Int) {
        this.structures.remove(id)
    }

    fun addVillager(entity: CustomVillagerEntity) {
        val key = Settlement.getAvailableKey(this.villagers.map { it.key })
        this.villagers[key] = entity
        entity.key = key
        entity.villageKey = this.id
        entity.errand.push(Errand(this.pos, Action.MOVE))
    }

    fun removeVillager(id: Int) {
        this.villagers.remove(id)
        this.structures.filter { it.value.owners.contains(id) }.forEach { structure ->
            structure.value.owners.remove(id)
        }
    }

    fun getStructureInRegion(pos: BlockPos): StructureType {
        for (structure in structures.values) {
            if (structure.area.contains(pos)) {
                return structure.type
            }
        }
        return StructureType.NONE
    }

    fun isStructureInRegion(pos: BlockPos): Boolean {
        for (structure in structures.values) {
            if (structure.area.contains(pos)) {
                return true
            }
        }
        return false
    }
    fun isStructureInRange(pos: BlockPos, range: Float): Boolean {
        for (structure in structures.values) {
            if (pos.getManhattanDistance(structure.area.center()) < range) {
                return true
            }
        }
        return false
    }

    fun toNbt(): NbtCompound {
        return NbtCompound().apply {
            putInt("VillageKey", id)
            putString("VillageName", name)
            putInt("SettlementOriginPosX", pos.x)
            putInt("SettlementOriginPosY", pos.y)
            putInt("SettlementOriginPosZ", pos.z)
            putIntArray("VillagersData", villagers.keys.toIntArray())
            put("StructuresData", structuresSerialize())
            put("AlliesData", alliesSerialize())
            put("NodesData", graph.nodeSerialize(graph.getNode(0)))
        }
    }

    fun alliesSerialize(): NbtList {
        val nbtList = NbtList()
        for (ally in allies) {
            val allyData = NbtCompound()
            allyData.putUuid("AllyUUID", ally.uuid)
            allyData.putInt("AllyReputation", ally.reputation)
            nbtList.add(allyData)
        }
        return nbtList
    }
    fun structuresSerialize(): NbtList {
        val nbtList = NbtList()
        for (structure in structures) {
            val structureData = NbtCompound()
            structureData.putInt("StructureKey", structure.key)
            structureData.putInt("StructureType", structure.value.type.ordinal)
            structureData.putIntArray("StructureOwners", structure.value.owners)
            structureData.putInt("StructureCapacity", structure.value.capacity)
            // TODO: Use only lower block and store work(blocks needed to arrive at upper)  -> two less intagers to store
            structureData.putInt("StructureAreaLowerX", structure.value.area.lower.x)
            structureData.putInt("StructureAreaLowerY", structure.value.area.lower.y)
            structureData.putInt("StructureAreaLowerZ", structure.value.area.lower.z)
            structureData.putInt("StructureAreaUpperX", structure.value.area.upper.x)
            structureData.putInt("StructureAreaUpperY", structure.value.area.upper.y)
            structureData.putInt("StructureAreaUpperZ", structure.value.area.upper.z)
            structureData.put("NodesData", structure.value.graph.nodeSerialize(structure.value.graph.getNode(0)))
            nbtList.add(structureData)
        }
        return nbtList
    }

    companion object {
        fun getAvailableKey(existingNumbers: List<Int>): Int {
            var idNumber: Int = 0
            do {
                idNumber = ++idNumber
            } while (idNumber in existingNumbers)
            return idNumber
        }

        fun alliesDeserialize(nbtList: NbtList): ArrayList<PlayerDataField> {
            val alliesList = arrayListOf<PlayerDataField>()
            for (l in 0 until nbtList.size) {
                val data = nbtList.getCompound(l)
                alliesList.add(PlayerDataField(data.getInt("AllyReputation"), data.getUuid("AllyUUID")))
            }
            return alliesList
        }

        fun nodeStructureDeserialize(graph: Graph<Int>, nbtList: NbtList) {
            for (l in 0 until nbtList.size) {
                val data = nbtList.getCompound(l)
                val node = Node<Int>(
                    data.getInt("NodeDataID"),
                    BlockPos(
                        data.getInt("NodePosX"),
                        data.getInt("NodePosY"),
                        data.getInt("NodePosZ"),
                    ),
                    0.5f,
                    data.getIntArray("NodeConnectionsKeys").toList(),
                )
                graph.loadNode(data.getInt("NodeKey"), node)
            }
        }

        fun structuresDeserialize(nbtList: NbtList): MutableMap<Int, Structure> {
            val structureList = mutableMapOf<Int, Structure>()
            for (l in 0 until nbtList.size) {
                val nbt = nbtList.getCompound(l)
                val lower = BlockPos(nbt.getInt("StructureAreaLowerX"), nbt.getInt("StructureAreaLowerY"), nbt.getInt("StructureAreaLowerZ"))
                val upper = BlockPos(nbt.getInt("StructureAreaUpperX"), nbt.getInt("StructureAreaUpperY"), nbt.getInt("StructureAreaUpperZ"))
                when (nbt.getInt("StructureType")) {
                    StructureType.KITCHEN.ordinal -> {
                        Building(
                            StructureType.KITCHEN,
                            lower,
                            upper,
                        )
                    }
                    StructureType.HOUSE.ordinal -> {
                        Building(
                            StructureType.HOUSE,
                            lower,
                            upper,
                        )
                    }
                    StructureType.FARM.ordinal -> {
                        Farm(lower, upper)
                    }
                    StructureType.POND.ordinal -> {
                        Pond(lower, upper)
                    }
                    StructureType.HALL.ordinal -> {
                        Hall(lower, upper)
                    }
                    else -> null
                }?.let {
                    // unsure
                    it.owners = nbt.getIntArray("StructureOwners").toMutableList()
                    it.capacity = nbt.getInt("StructureCapacity")
                    nodeStructureDeserialize(it.graph, nbt.getList("NodesData", NbtElement.COMPOUND_TYPE.toInt()))
                    structureList[nbt.getInt("StructureKey")] = it
                }
            }
            return structureList
        }

        fun fromNbt(nbt: NbtCompound): Settlement {
            val id = nbt.getInt("VillageKey")
            val name = nbt.getString("VillageName")
            val pos = BlockPos(nbt.getInt("SettlementOriginPosX"), nbt.getInt("SettlementOriginPosY"), nbt.getInt("SettlementOriginPosZ"))
            val villagers = nbt.getIntArray("VillagersData").associateWith { null }.toMutableMap<Int, CustomVillagerEntity?>()
            val structures = structuresDeserialize(nbt.getList("StructuresData", NbtElement.COMPOUND_TYPE.toInt()))
            val allies = alliesDeserialize(nbt.getList("AlliesData", NbtElement.COMPOUND_TYPE.toInt()))
            return Settlement(false, id, name, pos, structures, villagers, allies)
        }
    }
}
