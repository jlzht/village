package com.settlement.mod.world

import com.settlement.mod.action.Errand
import com.settlement.mod.entity.mob.AbstractVillagerEntity
import com.settlement.mod.network.SettlementDebugData
import com.settlement.mod.screen.Response
import com.settlement.mod.profession.ProfessionType
import com.settlement.mod.structure.Building
import com.settlement.mod.structure.Farm
import com.settlement.mod.structure.Pond
import com.settlement.mod.structure.Structure
import com.settlement.mod.structure.StructureType
import net.minecraft.block.BarrelBlock
import net.minecraft.block.Blocks
import net.minecraft.block.DoorBlock
import net.minecraft.block.FarmlandBlock
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.util.math.BlockPos
import java.util.UUID

// TODO:
// - implement leveling system
// - make pos the median center point (relative to structure centers)
class Settlement(
    val id: Int,
    val name: String,
    val pos: BlockPos,
    val dim: Byte,
) {
    var structures = mutableMapOf<Int, Structure>()
    var settlers = mutableListOf<Int>()
    var allies = mutableMapOf<UUID, Int>()
    private val errands = mutableListOf<Errand>()
    var level: Int = 1

    constructor(
        id: Int,
        name: String,
        pos: BlockPos,
        dim: Byte,
        structures: MutableMap<Int, Structure>,
        settlers: MutableList<Int>,
        allies: MutableMap<UUID, Int>,
    ) : this(id, name, pos, dim) {
        this.structures = structures
        this.settlers = settlers
        this.allies = allies
    }

    fun createStructure(
        pos: BlockPos,
        player: PlayerEntity,
    ) {
        when (player.world.getBlockState(pos).getBlock()) {
            is FarmlandBlock -> {
                if (
                    player.world.getBlockState(pos.north().west()).isOf(Blocks.FARMLAND) ||
                    player.world.getBlockState(pos.north().east()).isOf(Blocks.FARMLAND) ||
                    player.world.getBlockState(pos.south().east()).isOf(Blocks.FARMLAND) ||
                    player.world.getBlockState(pos.south().west()).isOf(Blocks.FARMLAND)
                ) {
                    Farm.createStructure(pos, player)
                } else {
                    Response.ANOTHER_STRUCTURE_CLOSE.send(player).run { null }
                }
            }
            is BarrelBlock -> { // Find a way to interact with water
                if (!this.isStructureInRange(pos, 8.0f)) {
                    Pond.createStructure(pos, player)
                } else {
                    Response.ANOTHER_STRUCTURE_CLOSE.send(player).run { null }
                }
            }
            is DoorBlock -> {
                if (!this.isStructureInRegion(pos)) {
                    Building.createStructure(pos, player)
                } else {
                    Response.ANOTHER_STRUCTURE_INSIDE.send(player).run { null }
                }
            }
            else -> Response.INVALID_BLOCK.send(player).run { null }
        }?.let {
            this.addStructure(it)
        }
    }

    fun addStructure(structure: Structure) {
        // TODO: create method that finds allies players by UUID and sendMessage to them notifying structure creating
        val key = Settlement.getAvailableKey(this.structures.map { it.key })
        this.structures[key] = structure
    }

    fun getStructure(id: Int): Structure? = structures[id]

    fun getStructureByType(type: StructureType): Pair<Int, Structure>? =
        this.structures
            .filter {
                it.value.type == type && it.value.isAvailable()
            }.entries
            .shuffled()
            .firstOrNull()
            ?.toPair()

    fun removeStructure(id: Int) {
        this.structures.remove(id)
    }

    fun getErrands(vid: Int): List<Errand> = emptyList()

    fun addVillager(entity: AbstractVillagerEntity) {
        val key = Settlement.getAvailableKey(this.settlers.map { it })
        entity.errandProvider.assignProvider(id, { v -> this.getErrands(v) }, false, key)
        this.settlers.add(key)
    }

    fun removeVillager(id: Int) {
        this.settlers.removeIf { it == id }
        this.structures.entries
            .filter { id in it.value.residents }
            .forEach { it.value.removeResident(id) }
    }

    fun getStructureInRegion(pos: BlockPos): StructureType? {
        for (structure in structures.values) {
            if (structure.region.contains(pos)) {
                return structure.type
            }
        }
        return null
    }

    fun isStructureInRegion(pos: BlockPos): Boolean {
        for (structure in structures.values) {
            if (structure.region.contains(pos)) {
                return true
            }
        }
        return false
    }

    fun isStructureInRange(
        pos: BlockPos,
        range: Float,
    ): Boolean {
        for (structure in structures.values) {
            if (pos.getManhattanDistance(structure.region.center()) < range) {
                return true
            }
        }
        return false
    }

    fun getProfessionsBySettlementLevel(): List<ProfessionType> {
        val levelProfessionMap =
            mapOf(
                1 to listOf(ProfessionType.GUARD, ProfessionType.FARMER, ProfessionType.FISHERMAN),
            )
        val professions = levelProfessionMap[level] ?: listOf(ProfessionType.GATHERER, ProfessionType.HUNTER)
        return professions
    }

    fun toNbt(): NbtCompound =
        NbtCompound().apply {
            putInt("VillageKey", id)
            putString("VillageName", name)
            putInt("SettlementOriginPosX", pos.x)
            putInt("SettlementOriginPosY", pos.y)
            putInt("SettlementOriginPosZ", pos.z)
            putByte("DimensionType", dim)
            putIntArray("VillagersData", settlers)
            put("StructuresData", structuresSerialize())
            put("AlliesData", alliesSerialize())
        }

    fun alliesSerialize(): NbtList {
        val nbtList = NbtList()
        for (ally in allies) {
            val allyData = NbtCompound()
            allyData.putUuid("AllyUUID", ally.key)
            allyData.putInt("AllyReputation", ally.value)
            nbtList.add(allyData)
        }
        return nbtList
    }

    fun structuresSerialize(): NbtList {
        val nbtList = NbtList()
        for (structure in structures) {
            val data = structure.value.toNbt()
            data.putInt("StructureKey", structure.key)
            nbtList.add(data)
        }
        return nbtList
    }

    fun getDebugData(): List<SettlementDebugData> {
        val data = mutableListOf<SettlementDebugData>()
        this.structures.forEach { structure ->
            data.add(
                SettlementDebugData(
                    structure.key,
                    structure.value.capacity,
                    structure.value.currentCapacity,
                    structure.value.region.lower,
                    structure.value.region.upper,
                    structure.value.errands,
                ),
            )
        }
        return data
    }

    companion object {
        // TODO: find a better impl to get Map ids
        fun getAvailableKey(existingNumbers: List<Int>): Int {
            var idNumber: Int = 0
            do {
                idNumber = ++idNumber
            } while (idNumber in existingNumbers)
            return idNumber
        }

        fun alliesDeserialize(nbtList: NbtList): MutableMap<UUID, Int> {
            val alliesList = mutableMapOf<UUID, Int>()
            for (i in 0 until nbtList.size) {
                val data = nbtList.getCompound(i)
                alliesList[data.getUuid("AllyUUID")] = data.getInt("AllyReputation")
            }
            return alliesList
        }

        fun structuresDeserialize(nbtList: NbtList): MutableMap<Int, Structure> {
            val structureList = mutableMapOf<Int, Structure>()

            for (i in 0 until nbtList.size) {
                val nbt = nbtList.getCompound(i)
                Structure.fromNbt(nbt)?.let { structure ->
                    structureList.put(structure.first, structure.second)
                }
            }

            return structureList
        }

        fun fromNbt(nbt: NbtCompound): Settlement {
            val id = nbt.getInt("VillageKey")
            val name = nbt.getString("VillageName")
            val pos = BlockPos(nbt.getInt("SettlementOriginPosX"), nbt.getInt("SettlementOriginPosY"), nbt.getInt("SettlementOriginPosZ"))
            val dim = nbt.getByte("DimensionType")
            val settlers = nbt.getIntArray("VillagersData").toMutableList()
            val structures = structuresDeserialize(nbt.getList("StructuresData", NbtElement.COMPOUND_TYPE.toInt()))
            val allies = alliesDeserialize(nbt.getList("AlliesData", NbtElement.COMPOUND_TYPE.toInt()))
            return Settlement(id, name, pos, dim, structures, settlers, allies)
        }
    }
}
