package com.settlement.mod.screen

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text

enum class Response(
    val message: Text,
) {
    // TODO: add messages to en_us
    ANOTHER_STRUCTURE_CLOSE(Text.translatable("Another structure is too close")),
    ANOTHER_STRUCTURE_INSIDE(Text.translatable("This is part of another structure")),
    INVALID_BLOCK(Text.translatable("Not valid")),
    TOO_OBSTRUCTED(Text.translatable("Place is too obstructed")),
    NO_SETTLEMENT_NEARBY(Text.translatable("item.settlement.hand_bell.fail.nearby")),
    NOT_ENOUGHT_REPUTATION(Text.translatable("item.settlement.hand_bell.fail.reputation")),
    PLACE_IS_SETTLEMENT_ALREADY(Text.translatable("block.settlement.bell.interaction.full")),
    ANOTHER_SETTLEMENT_HAS_NAME(Text.translatable("block.settlement.bell.interaction.same")),
    ANOTHER_SETTLEMENT_NEARBY(Text.translatable("block.settlement.bell.interaction.near")),
    NOT_ENOUGHT_MOISTURE(Text.translatable("Not enough moisture")),
    SMALL_BODY_WATER(Text.translatable("There is not enough water nearby")),
    NOT_ENOUGHT_LIGHT(Text.translatable("settlement.creation.building.fail.light")),
    NOT_ENOUGHT_SPACE(Text.translatable("settlement.creation.building.fail.space")),
    NEW_STRUCTURE(Text.translatable("settlement.creation.building.success")),
    NEW_SETTLEMENT(Text.translatable("block.settlement.bell.interaction.new")),
    NOT_ENOUGHT_FURNITURE(Text.translatable("settlement.creation.building.fail.empty")),
    STRUCTURE_NOT_ENCLOSED(Text.translatable("settlement.creation.building.fail.bound")),
    ;

    fun send(
        player: PlayerEntity,
        string: String,
    ) {
        player.sendMessage(message.copy().append(Text.translatable(string)), true)
    }

    fun send(player: PlayerEntity) {
        player.sendMessage(message, true)
    }
}
