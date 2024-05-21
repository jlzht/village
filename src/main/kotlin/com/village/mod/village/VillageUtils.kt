package com.village.mod.village

import com.village.mod.LOGGER
import com.village.mod.entity.village.Errand
import com.village.mod.village.structure.Region
import com.village.mod.village.structure.StructureType
import com.village.mod.village.villager.Action
import net.minecraft.block.AbstractCauldronBlock
import net.minecraft.block.AnvilBlock
import net.minecraft.block.BedBlock
import net.minecraft.block.Block
import net.minecraft.block.BrewingStandBlock
import net.minecraft.block.GrindstoneBlock
import net.minecraft.block.SmokerBlock
import net.minecraft.block.TrappedChestBlock
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.world.poi.PointOfInterest
import net.minecraft.world.poi.PointOfInterestStorage
import java.util.stream.Collectors

class placeholderClass {
    val structure: StructureType
    val capacity: Int
    val errands: HashSet<Errand>

    constructor(structure: StructureType, capacity: Int, errands: HashSet<Errand>) {
        this.structure = structure
        this.capacity = capacity
        this.errands = errands
    }
}

object VillageUtils {
    fun getErrandByBlock(block: Block, pos: BlockPos): Errand {
        when (block) {
            is BedBlock -> return Errand(pos, Action.SLEEP)
            is SmokerBlock -> return Errand(pos, Action.BAKE)
            is TrappedChestBlock -> return Errand(pos, Action.STORE)
            is AbstractCauldronBlock -> return Errand(pos, Action.POUR)
            is BrewingStandBlock -> return Errand(pos, Action.BREW)
            is AnvilBlock -> return Errand(pos, Action.FORGE)
            is GrindstoneBlock -> return Errand(pos, Action.GRIND)
        }
        return Errand(pos, Action.MOVE)
    }
    fun getStructureData(pos: BlockPos, lower: BlockPos, upper: BlockPos, world: ServerWorld): placeholderClass? {
        val region = Region(lower, upper)
        val pointOfInterestStorage = world.getPointOfInterestStorage()
        val optional = pointOfInterestStorage.getInSquare(
            { _ -> true },
            pos,
            8,
            PointOfInterestStorage.OccupationStatus.ANY,
        ).filter { poi -> region.contains(poi.getPos()) }.map(PointOfInterest::getPos).collect(Collectors.toSet())

        val list: HashSet<Errand> = hashSetOf()
        optional.toList().forEach { block ->
            list.add(getErrandByBlock(world.getBlockState(block).getBlock(), block))
        }
        val groups = list.groupBy {
            when (it.component2()) {
                Action.FORGE, Action.GRIND -> "FORGERY"
                Action.POUR, Action.BREW -> "HUT"
                Action.BAKE -> "KITCHEN"
                Action.SLEEP -> "HOUSE"
                else -> "NONE"
            }
        }.mapValues { it.value.size }
        val structure = groups.maxByOrNull { it.value }?.key
        structure?.let {
            LOGGER.info("THIS IS A: {}", structure)
            when (structure) {
                "HUT" -> {
                    val s2 = list.count { it.component2() == Action.BREW }
                    val s3 = list.count { it.component2() == Action.POUR }
                    val capacity = s2 + s3 - Math.max(s2, s3)
                    if (capacity > 0) {
                        val eList = list.filter { it.component2() == Action.BREW || it.component2() == Action.POUR }.toHashSet()
                        return placeholderClass(StructureType.HUT, capacity, eList)
                    } else {
                        LOGGER.info("THIS IS INVALID")
                        return null
                    }
                }
                "FORGERY" -> {
                    val s2 = list.count { it.component2() == Action.BREW }
                    val s3 = list.count { it.component2() == Action.POUR }
                    val capacity = s2 + s3 - Math.max(s2, s3)
                    if (capacity > 0) {
                        val eList = list.filter { it.component2() == Action.FORGE || it.component2() == Action.GRIND }.toHashSet()
                        return placeholderClass(StructureType.FORGERY, (eList.size / 2), eList)
                    } else {
                        LOGGER.info("THIS IS INVALID")
                        return null
                    }
                }
                "HOUSE" -> {
                    val eList = list.filter { it.component2() == Action.SLEEP }.toHashSet()
                    return placeholderClass(StructureType.HOUSE, eList.size, eList)
                }
                "KITCHEN" -> {
                    val eList = list.filter { it.component2() == Action.BAKE }.toHashSet()
                    return placeholderClass(StructureType.KITCHEN, eList.size, eList)
                }
                else -> null
            }
        }
        return null
    }
}
