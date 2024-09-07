package com.village.mod.network

import com.village.mod.MODID
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

data class SettlementDebugData(
    val id: Int,
    val capacity: Int,
    val maxCapacity: Int,
    val lower: BlockPos,
    val upper: BlockPos,
)

data class SettlementDebugDataPacket(
    val data: List<SettlementDebugData>,
) {
    companion object {
        val ID = Identifier(MODID, "settlement_debug")

        fun encode(
            packet: SettlementDebugDataPacket,
            buf: PacketByteBuf,
        ) {
            packet.data.forEach { data ->
                buf.writeInt(data.id)
                buf.writeInt(data.capacity)
                buf.writeInt(data.maxCapacity)
                buf.writeBlockPos(data.lower)
                buf.writeBlockPos(data.upper)
            }
        }

        fun decode(buf: PacketByteBuf): SettlementDebugDataPacket {
            val data = mutableListOf<SettlementDebugData>()

            while (buf.isReadable) {
                val id = buf.readInt()
                val capacity = buf.readInt()
                val maxCapacity = buf.readInt()
                val lower = buf.readBlockPos()
                val upper = buf.readBlockPos()
                data.add(SettlementDebugData(id, capacity, maxCapacity, lower, upper))
            }
            return SettlementDebugDataPacket(data)
        }

        fun sendToClient(
            player: ServerPlayerEntity,
            data: List<SettlementDebugData>,
        ) {
            val buf = PacketByteBufs.create()
            encode(SettlementDebugDataPacket(data), buf)
            ServerPlayNetworking.send(player, ID, buf)
        }
    }
}
