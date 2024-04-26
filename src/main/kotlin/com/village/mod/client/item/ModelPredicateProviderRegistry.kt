package com.village.mod.client.item

// import com.village.mod.entity.village.VillagerEntity
import net.minecraft.client.item.ModelPredicateProviderRegistry
import net.minecraft.item.Items
import net.minecraft.util.Identifier

object VillageModelPredicateProviders {
    fun registerModelPredicateProviders() {
        //ModelPredicateProviderRegistry.register(
        //    Items.FISHING_ROD,
        //    Identifier("cast"),
        //    { stack, _, entity, _ ->
        //        if (entity == null) {
        //            0.0f
        //        } else {
        //            if (entity.mainHandStack == stack && entity is VillagerEntity && entity.FishHook != null) {
        //                1.0f
        //            } else {
        //                0.0f
        //            }
        //        }
        //    },
        //)
    }
}
