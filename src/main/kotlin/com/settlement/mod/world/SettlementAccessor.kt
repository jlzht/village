package com.settlement.mod.world

import com.settlement.mod.LOGGER
import com.settlement.mod.action.Action
import com.settlement.mod.entity.mob.AbstractVillagerEntity
import com.settlement.mod.profession.ProfessionType
import com.settlement.mod.structure.StructureType

object SettlementAccessor {
    fun visitSettlement(entity: AbstractVillagerEntity) {
        SettlementManager.findNearestSettlement(entity)?.let { settlement ->
            entity.pushErrand(Action.Type.PICK, settlement.pos)
        }
    }

    fun findSettlementToAttach(entity: AbstractVillagerEntity) {
        SettlementManager.findNearestSettlement(entity)?.let { settlement ->
            settlement.addVillager(entity)
        }
    }

    fun getSettlementToAttach(entity: AbstractVillagerEntity) {
        SettlementManager.findSettlementById(entity.errandProvider.freeKey)?.let { settlement ->
            entity.errandProvider.attachProvider({ key -> settlement.getErrands(key) }, null)
        }
    }

    fun leaveSettlement(entity: AbstractVillagerEntity) {
        SettlementManager.findSettlementById(entity.errandProvider.freeKey)?.let { settlement ->
            val key = entity.errandProvider.selfKey
            settlement.removeVillager(key)
        }
    }

    fun getStructureToAttach(
        entity: AbstractVillagerEntity,
        id: Int,
        isHouse: Boolean,
    ) {
        SettlementManager.findSettlementById(entity.errandProvider.freeKey)?.let { settlement ->
            settlement.getStructure(id)?.let { structure ->
                if (structure.getResidents().contains(entity.errandProvider.selfKey)) {
                    entity.errandProvider.attachProvider(
                        { key -> structure.getErrands(key) },
                        structure.type == StructureType.HOUSE,
                    )
                }
            } ?: run {
                if (isHouse) {
                    LOGGER.info("got in house")
                    entity.errandProvider.homeKey = 0
                } else {
                    LOGGER.info("got in work")
                    entity.errandProvider.workKey = 0
                }
            }
        }
    }

    fun findStructureToAttach(
        entity: AbstractVillagerEntity,
        type: StructureType,
    ) {
        SettlementManager.findSettlementById(entity.errandProvider.freeKey)?.let { settlement ->
            // TODO: if not structure is available, force a delay of villager request
            settlement.getStructureByType(type)?.let { (id, structure) ->
                structure.addResident(entity.errandProvider.selfKey)
                entity.errandProvider.assignProvider(
                    id,
                    { i -> structure.getErrands(i) },
                    structure.type == StructureType.HOUSE,
                )
                LOGGER.info("structure key: {} - my id: {}", id, entity.errandProvider.selfKey)
            }
        }
    }

    // defines villager profession based on level of nearest settlement (called once on spawn)
    fun setProfession(entity: AbstractVillagerEntity) {
        SettlementManager.findNearestSettlement(entity)?.let { settlement ->
            val professions = settlement.getProfessionsBySettlementLevel()
            val profession = professions[entity.random.nextInt(professions.size)]
            entity.setProfession(profession)
        } ?: run {
            // BAD LOGIC
            val base = listOf(ProfessionType.GATHERER, ProfessionType.HUNTER).shuffled()
            entity.setProfession(base[0])
        }
    }
}
