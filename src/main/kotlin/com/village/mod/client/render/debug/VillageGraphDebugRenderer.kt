package com.village.mod.client.render.debug

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.util.math.Vec3d
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.RotationAxis
import org.lwjgl.opengl.GL11

data class Vertex(val pos: Vec3d)
data class Edge(val start: Vec3d, val end: Vec3d)

data class DebugGraph(val edges: Set<Edge>, val vertices: Set<Vertex>)

class VillageGraphDebugRenderer(val graph: MutableMap<DebugGraph, MutableList<DebugGraph>>) : WorldRenderEvents.End {
    fun drawVertices(matrixStack: MatrixStack) {
        val positionMatrix = matrixStack.peek().getPositionMatrix()
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.getBuffer()
        val offSet = 0.25f
        val r = 1.0f
        val g = 0.0f
        val b = 0.0f
        val a = 1.0f

        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
        for (q in graph.values) {
            q.forEach { d ->
                d.vertices.forEach {
                    buffer.vertex(positionMatrix, it.pos.x.toFloat() - offSet, it.pos.y.toFloat(), it.pos.z.toFloat() + offSet).color(b, g, r, a).next()
                    buffer.vertex(positionMatrix, it.pos.x.toFloat() - offSet, it.pos.y.toFloat(), it.pos.z.toFloat() - offSet).color(b, g, r, a).next()
                    buffer.vertex(positionMatrix, it.pos.x.toFloat() + offSet, it.pos.y.toFloat(), it.pos.z.toFloat() - offSet).color(b, g, r, a).next()
                    buffer.vertex(positionMatrix, it.pos.x.toFloat() + offSet, it.pos.y.toFloat(), it.pos.z.toFloat() + offSet).color(b, g, r, a).next()
                }
            }
        }

        for (q in graph.keys) {
            for (d in q.vertices) {
                buffer.vertex(positionMatrix, d.pos.x.toFloat() - offSet, d.pos.y.toFloat() + 0.25f, d.pos.z.toFloat() + offSet).color(r, g, b, a).next()
                buffer.vertex(positionMatrix, d.pos.x.toFloat() - offSet, d.pos.y.toFloat() + 0.25f, d.pos.z.toFloat() - offSet).color(r, g, b, a).next()
                buffer.vertex(positionMatrix, d.pos.x.toFloat() + offSet, d.pos.y.toFloat() + 0.25f, d.pos.z.toFloat() - offSet).color(r, g, b, a).next()
                buffer.vertex(positionMatrix, d.pos.x.toFloat() + offSet, d.pos.y.toFloat() + 0.25f, d.pos.z.toFloat() + offSet).color(r, g, b, a).next()
            }
        }
        tessellator.draw()
    }

    fun drawEdges(matrixStack: MatrixStack) {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram)
        val line = RenderSystem.getShaderLineWidth()
        val positionMatrix = matrixStack.peek().getPositionMatrix()

        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.getBuffer()
        val r = 0.0f
        val g = 1.0f
        val b = 0.0f
        val a = 1.0f

        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.lineWidth(8.0f)

        buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR)
        for (q in graph.values) {
            q.forEach { d ->
                d.edges.forEach {
                    buffer.vertex(positionMatrix, it.start.x.toFloat(), it.start.y.toFloat(), it.start.z.toFloat()).color(r, g, b, a).next()
                    buffer.vertex(positionMatrix, it.end.x.toFloat(), it.end.y.toFloat(), it.end.z.toFloat()).color(r, g, b, a).next()
                }
            }
        }

        for (q in graph.keys) {
            for (d in q.edges) {
                buffer.vertex(positionMatrix, d.start.x.toFloat(), d.start.y.toFloat() + 0.25f, d.start.z.toFloat()).color(r, g, b, a).next()
                buffer.vertex(positionMatrix, d.end.x.toFloat(), d.end.y.toFloat() + 0.25f, d.end.z.toFloat()).color(r, g, b, a).next()
            }
        }

        tessellator.draw()
        RenderSystem.disableBlend()
        RenderSystem.lineWidth(line)
    }

    override fun onEnd(context: WorldRenderContext) {
        if (graph.isEmpty()) return
        val camera = context.camera()
        val matrixStack = context.matrixStack()
        matrixStack.push()
        matrixStack.loadIdentity()
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()))
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F))
        matrixStack.translate(-camera.getPos().x, -camera.getPos().y, -camera.getPos().z)
        RenderSystem.setShader(GameRenderer::getPositionColorProgram)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        RenderSystem.disableCull()
        RenderSystem.depthFunc(GL11.GL_ALWAYS)
        this.drawVertices(matrixStack)
        this.drawEdges(matrixStack)
        matrixStack.pop()
        RenderSystem.depthFunc(GL11.GL_LEQUAL)
        RenderSystem.enableCull()
    }
}
