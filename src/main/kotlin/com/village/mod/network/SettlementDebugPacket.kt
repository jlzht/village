package com.village.mod.network

import com.village.mod.MODID
import com.village.mod.action.Action
import com.village.mod.action.Errand
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
    val errands: List<Errand>
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
                buf.writeInt(data.errands.size)
                data.errands.forEach { errand ->
                    buf.writeInt(errand.cid.ordinal)
                    buf.writeBlockPos(errand.pos)
                }
            }
        }

        fun decode(buf: PacketByteBuf): SettlementDebugDataPacket {
            val data = mutableListOf<SettlementDebugData>()
            val types = Action.Type.values()

            while (buf.isReadable) {
                val id = buf.readInt()
                val capacity = buf.readInt()
                val maxCapacity = buf.readInt()
                val lower = buf.readBlockPos()
                val upper = buf.readBlockPos()
                val size = buf.readInt()
                val errands = ArrayList<Errand>()
                repeat(size) {
                    val cid = buf.readInt()
                    val pos = buf.readBlockPos()
                    errands.add(Errand(types[cid], pos))
                }
                data.add(SettlementDebugData(id, capacity, maxCapacity, lower, upper, errands))
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
