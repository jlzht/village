package com.village.mod.world

import com.village.mod.LOGGER
import com.village.mod.action.Action
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.screen.Response
import com.village.mod.village.structure.StructureType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.world.PersistentState
import net.minecraft.world.dimension.DimensionType
import net.minecraft.world.dimension.DimensionTypes

// I want to structure Settlements in any dimension into a graph
class SettlementManager : PersistentState() {
    private val settlements = mutableListOf<Settlement>()

    init {
        this.markDirty()
    }

    fun addSettlement(
        name: String,
        pos: BlockPos,
        player: PlayerEntity,
    ): Settlement? {
        val id = Settlement.getAvailableKey(settlements.map { it.id })
        val entry = player.world.getDimensionEntry()

        getDimensionString(entry)?.let { dim ->
            for (settlement in settlements) {
                if (settlement.name == name) {
                    Response.ANOTHER_SETTLEMENT_HAS_NAME.send(player).also { return null }
                }
                if (settlement.dim == dim) {
                    if (settlement.pos == pos) {
                        Response.PLACE_IS_SETTLEMENT_ALREADY.send(player).also { return null }
                    }
                    if (settlement.pos.getSquaredDistance(pos.toCenterPos()) < 16384.0f) {
                        Response.ANOTHER_SETTLEMENT_NEARBY.send(player).also { return null }
                    }
                }
            }

            Settlement(id, name, pos, dim).let {
                LOGGER.info("Add settlement: {}", it)
                settlements.add(it)
                Response.NEW_SETTLEMENT.send(player, name)
                it.allies.add(PlayerDataField(50, player.getUuid()))
                return it
            }
        } ?: run { return null } // notifies player about invalid dimension
    }

    fun removeSettlement(settlement: Settlement) {
        settlements.remove(settlement)
    }

    fun getSettlements(): MutableList<Settlement> = settlements

    fun findSettlement(id: Int): Settlement? = settlements.find { it.id == id }

    fun clearSettlements() {
        settlements.clear()
    }

    override fun writeNbt(nbt: NbtCompound): NbtCompound {
        nbt.put("Villages", settlementSerialize())
        return nbt
    }

    fun settlementSerialize(): NbtList {
        val nbtList = NbtList()
        for (settlement in settlements) {
            nbtList.add(settlement.toNbt())
        }
        return nbtList
    }

    // TODO: find of checks to skip settlement ticking
    fun tick() {
        settlements.forEach { settlement ->
            LOGGER.info("Settlement: {}", settlement.name)
            SettlementManager.getWorld(settlement.dim)?.let { world ->
                if (world.isChunkLoaded(settlement.pos)) {
                    LOGGER.info("> is loaded")
                    settlement.structures.forEach { structure ->
                        if (!structure.value.hasErrands()) {
                            structure.value.updateErrands(world)
                            LOGGER.info("Structure: {}", structure.value.type)
                            structure.value.showErrands()
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun createFromNbt(tag: NbtCompound): SettlementManager {
            val state = SettlementManager()
            val settlement = tag.getList("Villages", NbtElement.COMPOUND_TYPE.toInt())
            for (i in 0 until settlement.size) {
                val data = settlement.getCompound(i)
                val set = Settlement.fromNbt(data)
                state.settlements.add(set)
            }
            return state
        }

        @JvmStatic
        fun getPersistentStateType(): Type<SettlementManager> =
            Type({ SettlementManager() }, { nbt -> SettlementManager.createFromNbt(nbt) }, null)

        private lateinit var instance: SettlementManager

        fun setInstance(instance: SettlementManager) {
            this.instance = instance
        }

        fun getInstance() = instance

        // easy access to dimensions
        private val worlds = mutableMapOf<String, ServerWorld>()

        fun getWorld(string: String) = worlds[string]

        fun getWorlds(): MutableMap<String, ServerWorld> = worlds

        fun setWorld(
            entry: RegistryEntry<DimensionType>,
            world: ServerWorld,
        ) {
            SettlementManager.getDimensionString(entry)?.let { string ->
                worlds[string] = world
            }
        }

        fun tick() {
            instance.tick()
        }

        fun getDimensionString(entry: RegistryEntry<DimensionType>): String? {
            return when {
                entry.matchesKey(DimensionTypes.OVERWORLD) -> "o"
                entry.matchesKey(DimensionTypes.THE_END) -> "e"
                entry.matchesKey(DimensionTypes.THE_NETHER) -> "n"
                else -> return null
            }
        }

        // put these methods somewhere else
        fun visitSettlement(entity: CustomVillagerEntity) {
            val dim = getDimensionString(entity.world.getDimensionEntry())
            getInstance()
                .getSettlements()
                .filter { it.dim == dim }
                .minByOrNull { it.pos.getSquaredDistance(entity.pos) }
                ?.let { settlement ->
                    LOGGER.info("Visiting village: {}", settlement.name)
                    entity.getErrandsManager().add(Action.Type.MOVE, settlement.pos)
                }
        }

        fun joinSettlement(entity: CustomVillagerEntity) {
            val dim = getDimensionString(entity.world.getDimensionEntry())
            getInstance()
                .getSettlements()
                .filter { it.dim == dim }
                .minByOrNull { it.pos.getSquaredDistance(entity.pos) }
                ?.let { settlement ->
                    settlement.addVillager(entity)
                }
        }

        // // called when villager dies or decides to leave settlement
        fun leaveSettlement(entity: CustomVillagerEntity) {
            getInstance().findSettlement(entity.data.sid)?.let { settlement ->
                settlement.removeVillager(entity.data.key)
            }
        }

        fun assignStructure(
            entity: CustomVillagerEntity,
            sid: Int,
        ) {
            // add structure check to see if ID matches
            getInstance().findSettlement(entity.data.sid)?.let { settlement ->
                settlement.getStructure(sid)?.let { structure ->
                    if (structure.getResidents().contains(entity.data.key)) {
                        entity.getErrandsManager().assignStructure(
                            sid,
                            { key -> structure.getErrands(key) },
                            structure.type == StructureType.HOUSE,
                        )
                    }
                }
            }
        }

        fun assignStructure(
            entity: CustomVillagerEntity,
            type: StructureType,
        ) {
            getInstance().findSettlement(entity.data.sid)?.let { settlement ->
                settlement.getStructureByType(type)?.let { (id, structure) ->
                    entity.getErrandsManager().assignStructure(
                        id,
                        { key -> structure.getErrands(key) },
                        structure.type == StructureType.HOUSE,
                    )
                    structure.addResident(entity.data.key)
                }
            }
        }

        // defines villager profession based on level of nearest settlement (called once on spawn)
        fun setProfession(entity: CustomVillagerEntity) {
            getInstance().getSettlements().minByOrNull { it.pos.getSquaredDistance(entity.pos) }?.let { settlement ->
                val professions = settlement.getProfessionsBySettlementLevel()
                val profession = professions[entity.random.nextInt(professions.size - 1)]
                LOGGER.info("SETTING PROFESSION TO: {}", profession)
                entity.setProfession(profession)
            }
        }
    }
}
