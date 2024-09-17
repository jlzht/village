package com.settlement.mod.entity.mob

import com.settlement.mod.LOGGER
import com.settlement.mod.action.Errand
import net.minecraft.nbt.NbtCompound

class ErrandProvider(
    private var homeProvider: ((Int) -> List<Errand>?)? = null,
    private var workProvider: ((Int) -> List<Errand>?)? = null,
    private var freeProvider: ((Int) -> List<Errand>?)? = null,
) {
    var selfKey: Int = 0
    var homeKey: Int = 0
    var workKey: Int = 0
    var freeKey: Int = 0
    var sdim: Byte = 0

    // empty lists dettaches structure errands references
    fun pull(): List<Errand> {
        val combinedErrands = mutableListOf<Errand>()
        homeProvider?.let { errands ->
            val list = errands.invoke(selfKey)
            list?.let {
                if (list.isEmpty()) {
                    homeKey = 0
                    homeProvider = null
                } else {
                    combinedErrands.addAll(list)
                }
            }
        }
        workProvider?.let { errands ->
            val list = errands.invoke(selfKey)
            list?.let {
                if (list.isEmpty()) {
                    workKey = 0
                    workProvider = null
                } else {
                    combinedErrands.addAll(list)
                }
            }
        }
        freeProvider?.let { errands ->
            val list = errands.invoke(selfKey)
            list?.let {
                combinedErrands.addAll(list)
            }
        }
        return combinedErrands
    }

    fun assignProvider(
        key: Int,
        provider: ((Int) -> List<Errand>?)?,
        selector: Boolean,
        ownKey: Int? = null,
    ) {
        ownKey?.let {
            freeProvider = provider
            freeKey = key
            selfKey = ownKey
        } ?: run {
            if (selector) {
                homeProvider = provider
                homeKey = key
            } else {
                workProvider = provider
                workKey = key
            }
        }
    }

    fun attachProvider(
        provider: ((Int) -> List<Errand>?)?,
        selector: Boolean?,
    ) {
        selector?.let {
            if (selector) {
                homeProvider = provider
            } else {
                workProvider = provider
            }
        } ?: run {
            freeProvider = provider
        }
    }

    fun hasHomeProvider(): Boolean = homeProvider != null

    fun hasWorkProvider(): Boolean = workProvider != null

    fun hasFreeProvider(): Boolean = freeProvider != null

    fun toNbt(): NbtCompound =
        NbtCompound().apply {
            LOGGER.info("free: {}", freeKey)
            LOGGER.info("self: {}", selfKey)
            LOGGER.info("home: {}", homeKey)
            LOGGER.info("work: {}", workKey)
            putInt("FreeKey", freeKey)
            putInt("SelfKey", selfKey)
            putInt("HomeKey", homeKey)
            putInt("WorkKey", workKey)
            putByte("SDIM", sdim)
        }

    companion object {
        fun fromNbt(nbt: NbtCompound): ErrandProvider =
            ErrandProvider().apply {
                freeKey = nbt.getInt("FreeKey")
                selfKey = nbt.getInt("SelfKey")
                homeKey = nbt.getInt("HomeKey")
                workKey = nbt.getInt("WorkKey")
                sdim = nbt.getByte("SDIM")
            }
    }
}
