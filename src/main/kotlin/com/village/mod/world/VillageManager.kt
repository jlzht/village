package com.village.mod.world

import com.village.mod.LOGGER
import com.village.mod.MODID
import com.village.mod.entity.village.CustomVillagerEntity
import net.minecraft.block.Blocks
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.BlockPos
import net.minecraft.world.PersistentState
import net.minecraft.world.World

// check radius 24 // 3x3x3 -> 4 27 . x = 4
// Structures, capacity, owner and filiated
enum class StructureType {
    FARM,
    HOUSE,
}

data class StructureData(
    val type: StructureType,
    var owner: Int,
    var lowerBlock: BlockPos,
    var upperBlock: BlockPos,
    var capacity: Int,
)

// list of players UUIDs?
data class VillageData(
    val id: Int,
    val name: String,
    var structures: MutableList<StructureData>,
    var isLoaded: Boolean,
    var pos: BlockPos,
    var villagers: MutableList<Pair<Int, CustomVillagerEntity?>>,
    var blocks: MutableList<Pair<Blocks, BlockPos>>,
)

class StateSaverAndLoader : PersistentState() {
    var cooldown: Int = 0
    var villages: MutableList<VillageData> = mutableListOf()
    override fun writeNbt(nbt: NbtCompound): NbtCompound {
        nbt.put("Villages", villagesSerialize())
        return nbt
    }

    protected fun villagesSerialize(): NbtList {
        val nbtList = NbtList()
        for (village in villages) {
            val tag: NbtCompound = NbtCompound()
            tag.putInt("villageID", village.id)
            tag.putString("villageName", village.name)
            var villagerIndex: MutableList<Int> = mutableListOf()
            // if (village.villagers != null) {
            for (villager in village.villagers) {
                villagerIndex.add(villager.component1())
            }
            // }
            tag.putIntArray("villagersID", villagerIndex)
            tag.putInt("posX", village.pos.x)
            tag.putInt("posY", village.pos.y)
            tag.putInt("posZ", village.pos.z)
            nbtList.add(tag)
        }
        return nbtList
    }

    companion object {
        fun createFromNbt(tag: NbtCompound): StateSaverAndLoader {
            val state = StateSaverAndLoader()
            val villagesNbt = tag.getList("Villages", NbtElement.COMPOUND_TYPE.toInt())
            for (i in 0..villagesNbt.count() - 1) {
                val data = villagesNbt.getCompound(i)
                val id = data.getInt("villageID")
                val vill = data.getIntArray("villagersID").toList()
                val tlr = BlockPos(data.getInt("posX"), data.getInt("posY"), data.getInt("posZ"))
                LOGGER.info("{}", tlr)
                state.villages.add(
                    VillageData(
                        id,
                        data.getString("villageName"),
                        mutableListOf(),
                        false,
                        tlr,
                        vill.map { it to null }.toMutableList(),
                        mutableListOf(),
                    ),
                )
            }
            return state
        }

        private val type = Type(
            { StateSaverAndLoader() },
            ::createFromNbt,
            null,
        )

        fun getServerState(server: MinecraftServer): StateSaverAndLoader {
            val persistentStateManager = server.getWorld(World.OVERWORLD)?.persistentStateManager
                ?: throw IllegalStateException("Failed to get PersistentStateManager for OVERWORLD")

            val state = persistentStateManager.getOrCreate(type, MODID)
            state.markDirty()
            return state
        }
    }
}
