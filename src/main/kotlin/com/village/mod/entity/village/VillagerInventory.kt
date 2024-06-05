package com.village.mod.entity.village

import com.google.common.collect.ImmutableList
import com.village.mod.LOGGER
import com.village.mod.item.ItemPredicate
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ArrowItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.registry.tag.DamageTypeTags
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.collection.DefaultedList

class VillagerInventory(val villager: CustomVillagerEntity) : Inventory {
    private val held = DefaultedList.ofSize(2, ItemStack.EMPTY)
    private val main = DefaultedList.ofSize(9, ItemStack.EMPTY)
    private val armor = DefaultedList.ofSize(4, ItemStack.EMPTY)
    private val fixed = DefaultedList.ofSize(2, ItemStack.EMPTY)
    private val merged: List<DefaultedList<ItemStack>> = ImmutableList.of(this.held, this.main, this.fixed, this.armor)

    fun setArmorField(id: Int, itemStack: ItemStack): ItemStack {
        return this.armor.set(id, itemStack)
    }

    fun setHeldField(id: Int, itemStack: ItemStack): ItemStack {
        return this.held.set(id, itemStack)
    }

    fun getHeldField(): DefaultedList<ItemStack> {
        return held
    }

    fun getMainField(): DefaultedList<ItemStack> {
        return main
    }

    fun getArmorField(): DefaultedList<ItemStack> {
        return armor
    }

    fun getFixedField(): DefaultedList<ItemStack> {
        return fixed
    }

    fun pickUpItem(item: ItemEntity) {
        // bundle and pouche
        val itemStack = item.stack
        if (villager.canGather(itemStack)) {
            val canInsert = this.canInsert(itemStack)
            if (!canInsert) return
            villager.triggerItemPickedUpByEntityCriteria(item)
            val originalCount = itemStack.count
            val remainingStack = if (ItemPredicate.ARROW(item.stack.item)) { this.specialItemHandle(itemStack) } else { this.addStack(main, itemStack) }
            villager.sendPickup(item, originalCount - remainingStack.count)
            if (remainingStack.isEmpty) {
                item.discard()
            } else {
                itemStack.setCount(remainingStack.getCount())
            }
            if (ItemPredicate.ARMOR(item.stack.item)) {
                this.tryEquipArmor()
            }
        }
    }

    fun specialItemHandle(itemStack: ItemStack): ItemStack {
        LOGGER.info("GOT HERE!")
        val kk = itemStack.copy()
        if (itemStack.item is ArrowItem) {
            if (fixed.get(1).isEmpty) {
                fixed.set(1, kk.copyAndEmpty())
                this.addToNewSlot(fixed, itemStack)
                if (itemStack.isEmpty()) {
                    return ItemStack.EMPTY
                }
            } else {
                val fixedd = fixed.get(1)
                if (ItemStack.canCombine(fixedd, kk)) {
                    this.transfer(fixedd, kk)
                }
                if (kk.isEmpty()) {
                    return ItemStack.EMPTY
                }
            }
        }
        return kk
    }

    fun tryInsert(itemStack: ItemStack) {
        if (this.canInsert(itemStack)) {
            this.addStack(main, itemStack)
        } else {
            this.villager.dropStack(itemStack)
        }
    }
    // override tryEqup instead of declaring this here
    fun tryEquipArmor() {
        val itemTaken = this.takeItem(ItemPredicate.ARMOR)
        if (itemTaken != ItemStack.EMPTY) {
            val equipmentSlot = MobEntity.getPreferredEquipmentSlot(itemTaken)
            val itemStack = this.villager.getEquippedStack(equipmentSlot)
            val prefersNew = ItemPredicate.prefersNewEquipment(itemTaken, itemStack)
            if (prefersNew) {
                this.tryInsert(itemStack)
                this.villager.equipStack(equipmentSlot, itemTaken)
            }
        }
    }

    fun canInsert(stack: ItemStack): Boolean {
        for (item in main) {
            if (!item.isEmpty && (!ItemStack.canCombine(item, stack) || item.count >= item.maxCount)) continue
            return true
        }
        return false
    }

    fun takeItem(predicate: (Item) -> Boolean): ItemStack {
        for (i in 0 until main.size) {
            val stack = this.getStack(i)
            if (predicate(stack.item)) {
                return this.removeStack(i)
            }
        }
        return ItemStack.EMPTY
    }
    fun findItem(predicate: (Item) -> Boolean): Int {
        for (i in 0 until main.size) {
            val stack = this.getStack(i)
            if (predicate(stack.item)) {
                return i
            }
        }
        return -1
    }

    fun addStack(field: DefaultedList<ItemStack>, stack: ItemStack): ItemStack {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY
        }
        val itemStack = stack.copy()
        this.addToExistingSlot(field, itemStack)
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY
        }
        this.addToNewSlot(field, itemStack)
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY
        }
        return itemStack
    }

    private fun addToNewSlot(field: DefaultedList<ItemStack>, stack: ItemStack) {
        for (i in 0 until field.size) {
            val itemStack = this.getStack(i)
            if (!itemStack.isEmpty) continue
            this.setStack(i, stack.copyAndEmpty())
        }
    }
    private fun transfer(target: ItemStack, source: ItemStack) {
        val i = Math.min(this.getMaxCountPerStack(), target.getMaxCount())
        val j = Math.min(source.getCount(), i - target.getCount())
        if (j > 0) {
            target.increment(j)
            source.decrement(j)
            this.markDirty()
        }
    }

    private fun addToExistingSlot(field: DefaultedList<ItemStack>, stack: ItemStack) {
        for (k in 0 until field.size) {
            val itemStack = this.getStack(k)
            if (!ItemStack.canCombine(itemStack, stack)) continue
            //this.transfer(itemStack, stack)
            val i = Math.min(this.getMaxCountPerStack(), itemStack.getMaxCount())
            val j = Math.min(stack.getCount(), i - itemStack.getCount())
            if (j > 0) {
                itemStack.increment(j)
                stack.decrement(j)
                this.markDirty()
            }
            this.markDirty()
            if (!stack.isEmpty) continue
            return
        }
    }

    override fun removeStack(slot: Int, amount: Int): ItemStack {
        val itemStack = Inventories.splitStack(this.main, slot, amount)
        if (!itemStack.isEmpty) {
            this.markDirty()
        }
        return itemStack
    }

    override fun removeStack(id: Int): ItemStack {
        return this.main.set(id, ItemStack.EMPTY)
    }

    override fun setStack(slot: Int, stack: ItemStack) {
        this.main.set(slot, stack)
        if (!stack.isEmpty() && stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack())
        }
        this.markDirty()
    }

    fun writeNbt(): NbtList {
        val nbtList: NbtList = NbtList()
        var nbtCompound: NbtCompound
        for (i in this.main.indices) {
            if (this.main[i].isEmpty) continue
            nbtCompound = NbtCompound()
            nbtCompound.putByte("Slot", i.toByte())
            this.main[i].writeNbt(nbtCompound)
            nbtList.add(nbtCompound)
        }

        for (i in this.armor.indices) {
            if (this.armor[i].isEmpty) continue
            nbtCompound = NbtCompound()
            nbtCompound.putByte("Slot", (i + 100).toByte())
            this.armor[i].writeNbt(nbtCompound)
            nbtList.add(nbtCompound)
        }

        for (i in this.held.indices) {
            if (this.held[i].isEmpty) continue
            nbtCompound = NbtCompound()
            nbtCompound.putByte("Slot", (i + 150).toByte())
            this.held[i].writeNbt(nbtCompound)
            nbtList.add(nbtCompound)
            LOGGER.info("I GOT HERE")
        }
        return nbtList
    }

    fun readNbt(nbtList: NbtList) {
        this.clear()
        for (i in 0 until nbtList.size) {
            val nbtCompound = nbtList.getCompound(i)
            val j = nbtCompound.getByte("Slot").toInt() and 0xFF
            val itemStack = ItemStack.fromNbt(nbtCompound)
            if (itemStack.isEmpty) continue
            when {
                j >= 0 && j < this.main.size -> {
                    this.main[j] = itemStack
                }
                j >= 100 && j < this.armor.size + 100 -> {
                    this.armor[j - 100] = itemStack
                }
                j >= 150 && j < this.held.size + 150 -> {
                    this.held[j - 150] = itemStack
                }
            }
        }
    }

    override fun size(): Int {
        return this.main.size + this.armor.size + this.held.size
    }

    override fun isEmpty(): Boolean {
        for (field in merged) {
            for (item in field) {
                if (item.isEmpty()) continue
                return false
            }
        }
        return true
    }

    override fun getStack(slot: Int): ItemStack {
        if (slot < 0 || slot >= this.main.size) {
            return ItemStack.EMPTY
        }
        return this.main.get(slot)
    }

    fun getArmorStack(slot: Int): ItemStack {
        return this.armor.get(slot)
    }

    fun damageArmor(damageSource: DamageSource, amount: Float, slots: IntArray) {
        if (amount <= 0.0f) {
            return
        }
        val damage: Int = if ((amount / 4.0f) < 1.0f) 1 else (amount / 4.0f).toInt()
        for (i in slots) {
            val itemStack = this.armor.get(i)
            if (damageSource.isIn(DamageTypeTags.IS_FIRE) && itemStack.getItem().isFireproof() || !(ItemPredicate.ARMOR(itemStack.getItem()))) continue
            itemStack.damage(damage, this.villager, { e -> e.sendEquipmentBreakStatus(EquipmentSlot.fromTypeIndex(EquipmentSlot.Type.ARMOR, i)) })
        }
    }

    fun dropAll() {
        for (field in merged) {
            for (item in field) {
                if (item.isEmpty()) continue
                this.villager.dropStack(item)
            }
        }
        this.clear()
    }

    override fun markDirty() {}

    override fun canPlayerUse(player: PlayerEntity): Boolean {
        return false
    }

    fun contains(stack: ItemStack): Boolean {
        for (field in merged) {
            for (item in field) {
                if (item.isEmpty() || !ItemStack.canCombine(item, stack)) continue
                return true
            }
        }
        return false
    }

    fun contains(tag: TagKey<Item>): Boolean {
        for (field in merged) {
            for (item in field) {
                if (item.isEmpty() || !item.isIn(tag)) continue
                return true
            }
        }
        return false
    }

    override fun clear() {
        for (list in merged) {
            list.clear()
        }
        this.markDirty()
    }
}
