package com.village.mod.item

import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import com.village.mod.MODID

import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemGroups
import net.minecraft.util.Identifier

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents

import net.minecraft.registry.tag.TagKey;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;


object VillageItems {
  val EMERALD_TOKEN: Item = registerItem("emerald_token", Item(FabricItemSettings()))
  val EMERALD_BUNDLE: Item = registerItem("emerald_bundle", Item(FabricItemSettings()))

  private fun addItemsToIngredientItemGroup(entries: FabricItemGroupEntries) {
        entries.add(EMERALD_TOKEN)
        entries.add(EMERALD_BUNDLE)
  }

  public fun registerItem(name: String, item: Item): Item {
    return Registry.register(Registries.ITEM, Identifier(MODID, name), item)
  }

  public fun register() {
    ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(this::addItemsToIngredientItemGroup)
    }
}
