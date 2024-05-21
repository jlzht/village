package com.village.mod.entity.ai

import com.village.mod.village.profession.Farmer
import com.village.mod.village.profession.Fisherman
import com.village.mod.village.profession.Guard
import com.village.mod.village.profession.Merchant
import com.village.mod.village.profession.Profession
import com.village.mod.village.profession.ProfessionType
import com.village.mod.village.profession.Unemployed

interface EntityWithProfession {
    fun getProfession(): Profession?

    fun setProfession(professionType: ProfessionType) {
        val profession = when (professionType) {
            ProfessionType.NONE -> Unemployed()
            ProfessionType.FARMER -> Farmer()
            ProfessionType.FISHERMAN -> Fisherman()
            ProfessionType.MERCHANT -> Merchant()
            ProfessionType.GUARD -> Guard()
        }
        setProfession(profession)
    }
    fun setProfession(profession: Profession)
}
