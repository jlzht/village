package com.village.mod.world

import com.village.mod.action.Action
import com.village.mod.entity.village.CustomVillagerEntity
import com.village.mod.village.profession.ProfessionType
import com.village.mod.village.structure.StructureType

object SettlementAccessor {
    fun visitSettlement(entity: CustomVillagerEntity) {
        SettlementManager.findNearestSettlement(entity)?.let { settlement ->
            entity.getErrandsManager().add(Action.Type.MOVE, settlement.pos)
        }
    }

    fun findSettlementToAttach(entity: CustomVillagerEntity) {
        SettlementManager.findNearestSettlement(entity)?.let { settlement ->
            settlement.addVillager(entity)
        }
    }

    fun getSettlementToAttach(entity: CustomVillagerEntity) {
        SettlementManager.findSettlementById(entity.getErrandsManager().free)?.let { settlement ->
            entity.getErrandsManager().attachSettlement({ settlement.getErrands() })
        }
    }

    fun leaveSettlement(entity: CustomVillagerEntity) {
        SettlementManager.findSettlementById(entity.getErrandsManager().free)?.let { settlement ->
            val key = entity.getErrandsManager().self
            settlement.removeVillager(key)
        }
    }

    fun getStructureToAttach(
        entity: CustomVillagerEntity,
        id: Int,
    ) {
        SettlementManager.findSettlementById(entity.getErrandsManager().free)?.let { settlement ->
            settlement.getStructure(id)?.let { structure ->
                if (structure.getResidents().contains(entity.getErrandsManager().self)) {
                    entity.getErrandsManager().attachStructure(
                        { key -> structure.getErrands(key) },
                        structure.type == StructureType.HOUSE,
                    )
                }
            }
        }
    }

    fun findStructureToAttach(
        entity: CustomVillagerEntity,
        type: StructureType,
    ) {
        SettlementManager.findSettlementById(entity.getErrandsManager().free)?.let { settlement ->
            // TODO: if not structure is available, force a delay of villager request
            settlement.getStructureByType(type)?.let { (id, structure) ->
                structure.addResident(entity.getErrandsManager().self)
                entity.getErrandsManager().assignStructure(
                    id,
                    { key -> structure.getErrands(key) },
                    structure.type == StructureType.HOUSE,
                )
            }
        }
    }

    // defines villager profession based on level of nearest settlement (called once on spawn)
    fun setProfession(entity: CustomVillagerEntity) {
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
