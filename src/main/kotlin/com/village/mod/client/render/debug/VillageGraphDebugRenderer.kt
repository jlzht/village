package com.village.mod.client.render.debug

import com.village.mod.LOGGER
import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.RotationAxis
import org.lwjgl.opengl.GL11

class VillageGraphDebugRenderer(val pos: MutableList<Vec3d>) : WorldRenderEvents.End {
    // populateData
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
        for (v in pos) {
            LOGGER.info("-- {}", v)
            buffer.vertex(positionMatrix, v.x.toFloat() - offSet, v.y.toFloat(), v.z.toFloat() + offSet).color(r, g, b, a).next()
            buffer.vertex(positionMatrix, v.x.toFloat() - offSet, v.y.toFloat(), v.z.toFloat() - offSet).color(r, g, b, a).next()
            buffer.vertex(positionMatrix, v.x.toFloat() + offSet, v.y.toFloat(), v.z.toFloat() - offSet).color(r, g, b, a).next()
            buffer.vertex(positionMatrix, v.x.toFloat() + offSet, v.y.toFloat(), v.z.toFloat() + offSet).color(r, g, b, a).next()
        }
        tessellator.draw()
    }

    fun drawEdges(matrixStack: MatrixStack) {
        RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram)
        RenderSystem.lineWidth(4f)

        val positionMatrix = matrixStack.peek().getPositionMatrix()

        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.getBuffer()
        val r = 0.0f
        val g = 1.0f
        val b = 0.0f
        val a = 1.0f

        buffer.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR)

        tessellator.draw()
    }

    override fun onEnd(context: WorldRenderContext) {
        if (pos.isEmpty()) return
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
        matrixStack.pop()
        RenderSystem.depthFunc(GL11.GL_LEQUAL)
        RenderSystem.enableCull()
    }
}
