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
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.world.PersistentState

class SettlementManager(val world: ServerWorld) : PersistentState() {
    private val settlements = mutableListOf<Settlement>()

    init {
        this.markDirty()
    }

    fun addSettlement(name: String, pos: BlockPos, player: PlayerEntity): Settlement? {
        val k = Settlement.getAvailableKey(settlements.map { it.id })

        Settlement(true, k, name, pos).let {
            LOGGER.info("Add settlemented: {}", it)
            settlements.add(it)
            Response.NEW_SETTLEMENT.send(player, name)
            it.allies.add(PlayerDataField(50, player.getUuid()))
            return it
        }
    }

    fun removeSettlement(settlement: Settlement) {
        settlements.remove(settlement)
    }

    fun getSettlements(): MutableList<Settlement> {
        return settlements
    }

    fun findSettlement(id: Int): Settlement? {
        return settlements.find { it.id == id }
    }

    fun clearSettlements() {
        settlements.clear()
    }

    override fun writeNbt(nbt: NbtCompound): NbtCompound {
        nbt.put("Villages", settlementSerialize())
        return nbt
    }

    fun settlementSerialize(): NbtList {
        val nbtList = NbtList()
        for (village in settlements) {
            nbtList.add(village.toNbt())
        }
        return nbtList
    }

    fun tick() {
        settlements.forEach { settlement ->
            if (world.isChunkLoaded(settlement.pos)) {
                settlement.isLoaded = true
                settlement.structures.forEach { structure ->
                    if (!structure.value.hasErrands()) {
                        structure.value.updateErrands(world.getServer().getOverworld()) // lame fix for settlement singletron problem
                        LOGGER.info("Structure: {}", structure.value.type)
                        structure.value.showErrands()
                    }
                }
            } else {
                settlement.isLoaded = false
            }
        }
    }

    companion object {
        fun createFromNbt(world: ServerWorld, tag: NbtCompound): SettlementManager {
            val state = SettlementManager(world)
            val settlement = tag.getList("Villages", NbtElement.COMPOUND_TYPE.toInt())
            for (i in 0 until settlement.size) {
                val data = settlement.getCompound(i)
                val set = Settlement.fromNbt(data)
                state.settlements.add(set)
            }
            return state
        }

        @JvmStatic
        fun getPersistentStateType(world: ServerWorld): Type<SettlementManager> {
            return Type({ SettlementManager(world) }, { nbt -> SettlementManager.createFromNbt(world, nbt) }, null)
        }

        private lateinit var instance: SettlementManager
        fun getInstance() = instance
        fun setInstance(instance: SettlementManager) {
            this.instance = instance
        }

        fun visitSettlement(entity: CustomVillagerEntity) {
            LOGGER.info("LOOKIN FOR HOUSIN")
            instance.getSettlements().minByOrNull { it.pos.getSquaredDistance(entity.pos) }?.let { settlement ->
                LOGGER.info("Inside manager")
                settlement.getStructureByType(StructureType.CAMPFIRE)?.let { (id, structure) ->
                    LOGGER.info("I HAVE A CAMPFIRE")
                    entity.getErrandsManager().add(Action.Type.REACH, structure.area.center())
                }
            }
        }

        fun joinSettlement(entity: CustomVillagerEntity) {
            instance.getSettlements().minByOrNull { it.pos.getSquaredDistance(entity.pos) }?.let { settlement ->
                settlement.addVillager(entity)
            }
        }

        // called when villager dies or decides to leave settlement
        fun leaveSettlement(entity: CustomVillagerEntity) {
            instance.findSettlement(entity.villageKey)?.let { settlement ->
                settlement.removeVillager(entity.key)
            }
        }

        fun assignStructure(entity: CustomVillagerEntity, sid: Int) {
            // add structure check to see if ID matches
            instance.findSettlement(entity.villageKey)?.let { settlement ->
                settlement.getStructure(sid)?.let { structure ->
                    if (structure.getResidents().contains(entity.key)) {
                        entity.getErrandsManager().assignStructure(sid, { key -> structure.getErrands(key) }, structure.type == StructureType.HOUSE)
                    }
                }
            }
        }

        fun assignStructure(entity: CustomVillagerEntity, type: StructureType) {
            instance.findSettlement(entity.villageKey)?.let { settlement ->
                settlement.getStructureByType(type)?.let { (id, structure) ->
                    entity.getErrandsManager().assignStructure(id, { key -> structure.getErrands(key) }, structure.type == StructureType.HOUSE)
                    structure.addResident(entity.key)
                }
            }
        }

        // defines villager profession based on level of nearest settlement (called once on spawn)
        fun setProfession(entity: CustomVillagerEntity) {
            instance.getSettlements().minByOrNull { it.pos.getSquaredDistance(entity.pos) }?.let { settlement ->
                val professions = settlement.getProfessionsBySettlementLevel()
                val profession = professions[entity.random.nextInt(professions.size - 1)]
                LOGGER.info("SETTING PROFESSION TO: {}", profession)
                entity.setProfession(profession)
            }
        }
    }
}
