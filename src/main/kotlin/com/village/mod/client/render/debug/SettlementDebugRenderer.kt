package com.village.mod.client.render.debug

import com.mojang.blaze3d.systems.RenderSystem
import com.village.mod.network.SettlementDebugData
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text
import net.minecraft.util.math.RotationAxis
import org.lwjgl.opengl.GL11

// TODO:
//  - show residents ID
//  - color structures by type
//  - add errands to debugData
class SettlementDebugRenderer(
    val debugData: List<SettlementDebugData>,
) : WorldRenderEvents.End {
    fun render(
        matrixStack: MatrixStack,
        debugData: List<SettlementDebugData>,
        yaw: Float,
        pitch: Float,
    ) {
        val positionMatrix = matrixStack.peek().getPositionMatrix()
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.getBuffer()

        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.enableDepthTest()
        RenderSystem.depthFunc(GL11.GL_ALWAYS)

        val r = 0.5f
        val g = 0.12f
        val b = 0.5f
        val a = 0.2f

        for (data in debugData) {
            val lower = data.lower
            val upper = data.upper
            val text = data.capacity.toString() + "/" + data.maxCapacity.toString()

            val xMin = lower.x.toFloat()
            val yMin = lower.y.toFloat()
            val zMin = lower.z.toFloat()
            val xMax = upper.x.toFloat()
            val yMax = (upper.y + 1).toFloat()
            val zMax = upper.z.toFloat()

            buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
            buffer.vertex(positionMatrix, xMin, yMin, zMin).color(r, g, b, a).next()
            buffer.vertex(positionMatrix, xMin, yMin, zMax).color(r, g, b, a).next()
            buffer.vertex(positionMatrix, xMax, yMin, zMax).color(r, g, b, a).next()
            buffer.vertex(positionMatrix, xMax, yMin, zMin).color(r, g, b, a).next()

            buffer.vertex(positionMatrix, xMin, yMax, zMin).color(r, g, b, a).next()
            buffer.vertex(positionMatrix, xMin, yMax, zMax).color(r, g, b, a).next()
            buffer.vertex(positionMatrix, xMax, yMax, zMax).color(r, g, b, a).next()
            buffer.vertex(positionMatrix, xMax, yMax, zMin).color(r, g, b, a).next()

            buffer.vertex(positionMatrix, xMin, yMin, zMin).color(r, g, b, a).next()
            buffer.vertex(positionMatrix, xMin, yMax, zMin).color(r, g, b, a).next()
            buffer.vertex(positionMatrix, xMin, yMax, zMax).color(r, g, b, a).next()
            buffer.vertex(positionMatrix, xMin, yMin, zMax).color(r, g, b, a).next()

            buffer.vertex(positionMatrix, xMax, yMin, zMin).color(r, g, b, a).next()
            buffer.vertex(positionMatrix, xMax, yMax, zMin).color(r, g, b, a).next()
            buffer.vertex(positionMatrix, xMax, yMax, zMax).color(r, g, b, a).next()
            buffer.vertex(positionMatrix, xMax, yMin, zMax).color(r, g, b, a).next()

            buffer.vertex(positionMatrix, xMin, yMin, zMin).color(r, g, b, a).next()
            buffer.vertex(positionMatrix, xMax, yMin, zMin).color(r, g, b, a).next()
            buffer.vertex(positionMatrix, xMax, yMax, zMin).color(r, g, b, a).next()
            buffer.vertex(positionMatrix, xMin, yMax, zMin).color(r, g, b, a).next()

            buffer.vertex(positionMatrix, xMin, yMin, zMax).color(r, g, b, a).next()
            buffer.vertex(positionMatrix, xMax, yMin, zMax).color(r, g, b, a).next()
            buffer.vertex(positionMatrix, xMax, yMax, zMax).color(r, g, b, a).next()
            buffer.vertex(positionMatrix, xMin, yMax, zMax).color(r, g, b, a).next()
            tessellator.draw()

            RenderSystem.disableBlend()
            RenderSystem.depthFunc(GL11.GL_LEQUAL)
            RenderSystem.disableDepthTest()

            val textRenderer = MinecraftClient.getInstance().textRenderer

            val centerX = (xMin + xMax) / 2.0
            val centerY = (yMin + yMax) / 2.0
            val centerZ = (zMin + zMax) / 2.0

            matrixStack.push()
            matrixStack.translate(centerX, centerY, centerZ)

            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw))
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch))

            val scale = 0.04f
            matrixStack.scale(-scale, -scale, -scale)

            val textMatrix = matrixStack.peek().positionMatrix
            val vertexConsumerProvider = VertexConsumerProvider.immediate(buffer)

            textRenderer.draw(
                Text.of(text),
                -textRenderer.getWidth(text) / 2.0f,
                0f,
                0xFFFFFF,
                false,
                textMatrix,
                vertexConsumerProvider,
                TextRenderer.TextLayerType.SEE_THROUGH,
                0,
                15728880,
            )
            vertexConsumerProvider.draw()
            matrixStack.pop()
        }
    }

    override fun onEnd(context: WorldRenderContext) {
        if (debugData.isEmpty()) return
        val camera = context.camera()
        val matrixStack = context.matrixStack()
        matrixStack.push()
        matrixStack.loadIdentity()
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.pitch))
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.yaw + 180.0F))
        matrixStack.translate(-camera.pos.x, -camera.pos.y, -camera.pos.z)
        RenderSystem.setShader(GameRenderer::getPositionColorProgram)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        RenderSystem.disableCull()
        this.render(matrixStack, debugData, camera.yaw, camera.pitch)
        matrixStack.pop()
        RenderSystem.enableCull()
    }
}
