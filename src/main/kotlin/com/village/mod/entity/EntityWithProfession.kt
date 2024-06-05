package com.village.mod.entity.ai

import com.village.mod.village.profession.Farmer
import com.village.mod.village.profession.Fisherman
import com.village.mod.village.profession.Guard
import com.village.mod.village.profession.Merchant
import com.village.mod.village.profession.Profession
import com.village.mod.village.profession.ProfessionType
import com.village.mod.village.profession.Unemployed
import com.village.mod.entity.village.CustomVillagerEntity

interface EntityWithProfession {
    fun getProfession(): Profession?

    fun setProfession(villager: CustomVillagerEntity, professionType: ProfessionType) {
        val profession = when (professionType) {
            ProfessionType.NONE -> Unemployed(villager)
            ProfessionType.FARMER -> Farmer(villager)
            ProfessionType.FISHERMAN -> Fisherman(villager)
            ProfessionType.MERCHANT -> Merchant(villager)
            ProfessionType.GUARD -> Guard(villager)
        }
        setProfession(profession)
    }
    fun setProfession(profession: Profession)
}
