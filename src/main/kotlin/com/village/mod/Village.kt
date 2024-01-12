package com.village.mod

import com.village.mod.entity.village.VigerEntity
import com.village.mod.item.VillageItems
import com.village.mod.screen.TradingScreenHandler
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

const val MODID = "village"
val LOGGER = LoggerFactory.getLogger(MODID)

object Village : ModInitializer {
    val VIGER: EntityType<VigerEntity> =
        Registry.register(
            Registries.ENTITY_TYPE,
            Identifier(MODID, "viger"),
            FabricEntityTypeBuilder.create<VigerEntity>(SpawnGroup.CREATURE, { type, world -> VigerEntity(type, world) })
                .dimensions(EntityDimensions.fixed(0.6f, 1.95f)).build(),
        )
    val TRADING_SCREEN_HANDLER: ScreenHandlerType<TradingScreenHandler> =
        Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier(MODID, "viger_trading"),
            ExtendedScreenHandlerType({ syncId, playerInventory, buf -> TradingScreenHandler(syncId, playerInventory, buf) }),
        )

    override fun onInitialize() {
        VillageItems.register()
        FabricDefaultAttributeRegistry.register(VIGER, VigerEntity.createMobAttributes())
    }
}
