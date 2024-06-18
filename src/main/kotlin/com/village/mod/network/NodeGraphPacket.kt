package com.village.mod.network

import com.village.mod.MODID
import com.village.mod.client.render.debug.DebugGraph
import net.minecraft.util.Identifier
import net.minecraft.server.network.ServerPlayerEntity
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf
import com.village.mod.client.render.debug.Edge
import com.village.mod.client.render.debug.Vertex

data class NodeGraphPacket(val graph: MutableMap<DebugGraph, MutableList<DebugGraph>>) {
    companion object {
        val ID = Identifier(MODID, "village_debug_graph")

        fun encode(packet: NodeGraphPacket, buf: PacketByteBuf) {
            packet.graph.forEach { settlement ->
                buf.writeInt(settlement.key.vertices.size)
                settlement.key.vertices.forEach {
                    buf.writeVec3d(it.pos)
                }
                buf.writeInt(settlement.key.edges.size)
                settlement.key.edges.forEach {
                    buf.writeVec3d(it.start)
                    buf.writeVec3d(it.end)
                }
                buf.writeInt(settlement.value.size)
                settlement.value.forEach {
                    buf.writeInt(it.vertices.size)
                    it.vertices.forEach { v ->
                        buf.writeVec3d(v.pos)
                    }
                    buf.writeInt(it.edges.size)
                    it.edges.forEach { e ->
                        buf.writeVec3d(e.start)
                        buf.writeVec3d(e.end)
                    }
                }
            }
        }

        fun decode(buf: PacketByteBuf): NodeGraphPacket {
            val dgraph = mutableMapOf<DebugGraph, MutableList<DebugGraph>>()

            while (buf.isReadable) {
                val verticesSize = buf.readInt()
                val vertices = mutableSetOf<Vertex>()
                repeat(verticesSize) {
                    vertices.add(Vertex(buf.readVec3d()))
                }

                val edgesSize = buf.readInt()
                val edges = mutableSetOf<Edge>()
                repeat(edgesSize) {
                    edges.add(Edge(buf.readVec3d(), buf.readVec3d()))
                }

                val key = DebugGraph(edges, vertices)

                val valueSize = buf.readInt()
                val value = mutableListOf<DebugGraph>()
                repeat(valueSize) {
                    val innerVerticesSize = buf.readInt()
                    val innerVertices = mutableSetOf<Vertex>()
                    repeat(innerVerticesSize) {
                        innerVertices.add(Vertex(buf.readVec3d()))
                    }

                    val innerEdgesSize = buf.readInt()
                    val innerEdges = mutableSetOf<Edge>()
                    repeat(innerEdgesSize) {
                        innerEdges.add(Edge(buf.readVec3d(), buf.readVec3d()))
                    }

                    value.add(DebugGraph(innerEdges, innerVertices))
                }

                dgraph[key] = value
            }
            return NodeGraphPacket(dgraph)
        }
        fun sendToClient(player: ServerPlayerEntity, debugGraph: MutableMap<DebugGraph, MutableList<DebugGraph>>) {
            val buf = PacketByteBufs.create()
            encode(NodeGraphPacket(debugGraph), buf)
            ServerPlayNetworking.send(player, ID, buf)
        }
    }
}
