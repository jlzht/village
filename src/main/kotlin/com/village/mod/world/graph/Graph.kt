package com.village.mod.world.graph

import com.village.mod.LOGGER
import com.village.mod.client.render.debug.Edge
import com.village.mod.world.Settlement
import com.village.mod.world.StructureNodeRef
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.PriorityQueue

// TODO:find better name
data class Node<T>(val cid: T, val pos: BlockPos, val range: Float = 0.5f, val others: List<Int> = emptyList()) {
    companion object {
        fun <T> getData(cid: T, pos: BlockPos): Node<T> {
            val a = when (cid) {
                is StructureNodeRef -> StructureNodeRef() as T
                is Int -> -1 as T
                else -> throw IllegalArgumentException("Unsupported type")
            }
            return Node(a, pos)
        }

        fun <T> get(cid: T): Boolean {
            val a = when (cid) {
                is StructureNodeRef -> {
                    LOGGER.info("{}", cid)
                    cid.structure == -1 && cid.node == -1 }
                is Int -> cid == -1
                else -> throw IllegalArgumentException("Unsupported type")
            }
            return a
        }
    }
}

class Graph<T> {
    private val nodes: MutableMap<Int, Node<T>> = mutableMapOf()

    fun loadNode(key: Int, node: Node<T>): Node<T> {
        nodes[key] = node
        return node
    }

    // TODO: perpedincular merge after tryMerge recusion end
    // TODO: use KD-trees
    fun tryMerge(id: Int, world: World, target: Int, isk: Int) {
        if (nodes.isEmpty()) return
        getNode(id)?.let { node ->
            if (target != -1) { // -1 are mergible nodes
                getNode(target)?.let { tnode ->
                    val a = NodeUtils.findPath(world, node.pos, tnode.pos, 4)
                    if (a == null) {
                        this.addEdge(id, target)
                    } else {
                        if (a != BlockPos(0, 0, 0) && a != node.pos) {
                            nodes.entries.filter { a.getManhattanDistance(it.value.pos) <= 3 && !it.value.others.contains(id) && Node.get(it.value.cid) }
                                .sortedBy { node.pos.getManhattanDistance(it.value.pos) }
                                .let { s ->
                                    if (!s.isEmpty()) {
                                        // merges very close nodes
                                        s.firstOrNull()?.let {
                                            this.addEdge(it.key, id)
                                        }
                                    } else {
                                        // tries to walk to open nodes
                                        val data = Node.getData(node.cid, a)
                                        val k = this.addNode(data)
                                        this.addEdge(k, id)
                                        tryMerge(k, world, target, isk)
                                    }
                                }
                        }
                    }
                }
            } else {
                nodes.entries.filter { node.pos.getManhattanDistance(it.value.pos) >= 3 && !it.value.others.contains(id) && Node.get(it.value.cid) }
                    .sortedBy { node.pos.getManhattanDistance(it.value.pos) }
                    .take(isk)
                    .forEach { tryMerge(id, world, it.key, isk) }
            }
        }
    }

    fun addNode(node: Node<T>): Int {
        val key = Settlement.getAvailableKey(nodes.keys.toList())
        nodes[key] = node
        return key
    }

    fun getNode(id: Int): Node<T>? {
        return nodes[id]
    }

    fun addEdge(sourceId: Int, targetId: Int) {
        val sourceNode = getNode(sourceId)
        val targetNode = getNode(targetId)
        if (sourceNode != null && targetNode != null) {
            val updatedSourceNode = sourceNode.copy(others = sourceNode.others + targetId)
            val updatedTargetNode = targetNode.copy(others = targetNode.others + sourceId)
            nodes[sourceId] = updatedSourceNode
            nodes[targetId] = updatedTargetNode
        } else {
            LOGGER.info("Can't add edge!")
        }
    }

    fun getNeighbors(id: Int): List<Node<T>> {
        val node = getNode(id)
        return node?.others?.mapNotNull { nodes[it] } ?: emptyList()
    }

    fun getNodes(): Map<Int, Node<T>> {
        return nodes
    }

    // Debugging only
    fun getConnections(): Set<Edge> {
        val connections = mutableSetOf<Edge>()
        for (node in nodes) {
            for (nid in node.value.others) {
                val neighborNode = nodes[nid]
                if (neighborNode != null) {
                    connections.add(Edge(node.value.pos.toCenterPos(), neighborNode.pos.toCenterPos()))
                }
            }
        }
        return connections
    }

    fun navigate(startID: Int, endID: Int): List<Int>? {
        val dist = mutableMapOf<Int, Int>()
        val prev = mutableMapOf<Int, Int?>()
        val pq = PriorityQueue<Pair<Int, Int>>(compareBy { it.second })

        nodes.keys.forEach {
            dist[it] = Int.MAX_VALUE
            prev[it] = null
        }
        dist[startID] = 0
        pq.add(Pair(startID, 0))

        while (pq.isNotEmpty()) {
            val (currentID, currentDist) = pq.poll()
            if (currentID == endID) break

            val currentNode = getNode(currentID) ?: continue

            for (neighborID in currentNode.others) {
                val altDist = currentDist + 1
                if (altDist < dist[neighborID]!!) {
                    dist[neighborID] = altDist
                    prev[neighborID] = currentID
                    pq.add(Pair(neighborID, altDist))
                    LOGGER.info("pq: {}", pq)
                }
            }
        }

        if (dist[endID] == Int.MAX_VALUE) return null // No path found

        val path = mutableListOf<Int>()
        var step: Int? = endID
        while (step != null) {
            path.add(step)
            step = prev[step]
        }
        return path.reversed()
    }

    // cid is passed here just to bypass inference error
    fun <T> nodeSerialize(cid: T): NbtList {
        val nbtList = NbtList()
        for (node in this.nodes) {
            val nodeData = NbtCompound()
            nodeData.putInt("NodeKey", node.key)
            nodeData.putInt("NodePosX", node.value.pos.x)
            nodeData.putInt("NodePosY", node.value.pos.y)
            nodeData.putInt("NodePosZ", node.value.pos.z)
            Graph.serializeAction(nodeData, node.value.cid)
            nodeData.putIntArray("NodeConnectionsKeys", node.value.others.toList())
            nbtList.add(nodeData)
        }
        return nbtList
    }
    companion object {
        fun <T> serializeAction(nbt: NbtCompound, cid: T) {
            when (cid) {
                is Int -> nbt.putInt("NodeDataID", cid)
                is StructureNodeRef -> {
                    nbt.putInt("NodeDataStructure", cid.structure)
                    nbt.putInt("NodeDataID", cid.node)
                }
                else -> throw IllegalArgumentException("Unsupported cid type")
            }
        }
    }
}
