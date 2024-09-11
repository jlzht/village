package com.village.mod.client.render.debug

import com.mojang.blaze3d.systems.RenderSystem
import com.village.mod.network.SettlementDebugData
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.render.Camera
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.RotationAxis
import org.lwjgl.opengl.GL11

// TODO:
//  - show residents ID
//  - color structures by type
class SettlementDebugRenderer(
    val debugData: List<SettlementDebugData>,
) : WorldRenderEvents.End {
    fun render(
        matrixStack: MatrixStack,
        debugData: List<SettlementDebugData>,
        camera: Camera,
    ) {
        matrixStack.push()
        matrixStack.loadIdentity()
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.pitch))
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.yaw + 180.0F))
        matrixStack.translate(-camera.pos.x, -camera.pos.y, -camera.pos.z)
        val positionMatrix = matrixStack.peek().getPositionMatrix()
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.getBuffer()

        val r = 0.5f
        val g = 0.12f
        val b = 0.5f
        val a = 0.2f

        for (data in debugData) {
            val errands = data.errands
            val lower = data.lower
            val upper = data.upper
            val text = data.capacity.toString() + "/" + data.maxCapacity.toString()

            val xMin = lower.x.toFloat() + 1
            val yMin = lower.y.toFloat()
            val zMin = lower.z.toFloat() + 1
            val xMax = upper.x.toFloat() + 1
            val yMax = upper.y.toFloat() + 1
            val zMax = upper.z.toFloat() + 1

            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.enableDepthTest()
            RenderSystem.depthFunc(GL11.GL_ALWAYS)

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
            val centerY = yMax + 1.0
            val centerZ = (zMin + zMax) / 2.0

            val scale = 0.03f
            val vertexConsumerProvider = VertexConsumerProvider.immediate(buffer)
            // give a color for action
            for (errand in errands) {
                matrixStack.push()
                matrixStack.translate(errand.pos!!.x.toDouble() + 0.5, errand.pos.y.toDouble() + 1, errand.pos.z.toDouble() + 0.5)
                matrixStack.scale(-scale, -scale, -scale)
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.yaw))
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.pitch))

                val eText = "${errand.cid.name}"
                drawText(textRenderer, eText, matrixStack, vertexConsumerProvider)
                matrixStack.pop()
            }

            matrixStack.push()
            matrixStack.translate(centerX, centerY, centerZ)
            matrixStack.scale(-scale, -scale, -scale)
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.yaw))
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.pitch))
            drawText(textRenderer, text, matrixStack, vertexConsumerProvider)
            vertexConsumerProvider.draw()
            matrixStack.pop()
        }
        matrixStack.pop()
    }

    private fun drawText(
        textRenderer: TextRenderer,
        text: String,
        matrixStack: MatrixStack,
        vertexConsumerProvider: VertexConsumerProvider,
    ) {
        val textMatrix = matrixStack.peek().positionMatrix
        textRenderer.draw(
            text,
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
    }

    override fun onEnd(context: WorldRenderContext) {
        if (debugData.isEmpty()) return
        val camera = context.camera()
        val matrixStack = context.matrixStack()
        RenderSystem.setShader(GameRenderer::getPositionColorProgram)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        RenderSystem.disableCull()
        render(matrixStack, debugData, camera)
        RenderSystem.enableCull()
    }
}
