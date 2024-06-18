package com.village.mod.screen

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text

enum class Response(val message: Text) {
    // TODO: add messages to en_us
    ANOTHER_STRUCTURE_CLOSE(Text.translatable("Another structure is too close")),
    ANOTHER_STRUCTURE_INSIDE(Text.translatable("This is part of another structure")),
    INVALID_BLOCK(Text.translatable("Not valid")),
    TOO_OBSTRUCTED(Text.translatable("Place is too obstructed")),
    NO_SETTLEMENT_NEARBY(Text.translatable("item.village.hand_bell.fail.nearby")),
    NOT_ENOUGHT_REPUTATION(Text.translatable("item.village.hand_bell.fail.reputation")),
    PLACE_IS_SETTLEMENT_ALREADY(Text.translatable("block.village.bell.interaction.full")),
    ANOTHER_SETTLEMENT_HAS_NAME(Text.translatable("block.village.bell.interaction.same")),
    ANOTHER_SETTLEMENT_NEARBY(Text.translatable("block.village.bell.interaction.near")),
    NOT_ENOUGHT_MOISTURE(Text.translatable("Not enought moisture")),
    SMALL_BODY_WATER(Text.translatable("There is not enough water nearby")),
    NOT_ENOUGHT_LIGHT(Text.translatable("village.creation.building.fail.light")),
    NOT_ENOUGHT_SPACE(Text.translatable("village.creation.building.fail.space")),
    NEW_STRUCTURE(Text.translatable("village.creation.building.success")),
    NEW_SETTLEMENT(Text.translatable("block.village.bell.interaction.new")),
    NOT_ENOUGHT_FURNITURE(Text.translatable("village.creation.building.fail.empty")),
    STRUCTURE_NOT_ENCLOSED(Text.translatable("village.creation.building.fail.bound")),
    ;
    fun send(player: PlayerEntity, string: String) {
        player.sendMessage(message.copy().append(Text.translatable(string)), true)
    }
    fun send(player: PlayerEntity) {
        player.sendMessage(message, true)
    }
}
