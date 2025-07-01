package io.github.sceneview.sceneview_flutter

import io.github.sceneview.math.Position

data class NormalizedPoint(val x: Float, val y: Float)
object KeypointUtils {
    fun convertNormalizedKeypointFrom3x4To16x9(keyPoint: NormalizedPoint): NormalizedPoint {
        val modelAspectRatio =1f
        val canvasAspectRatio = 3f / 4f*1.15f

        val targetHeight = 1f
        val targetWidth = targetHeight * modelAspectRatio

        val fullCanvasWidth = targetHeight * canvasAspectRatio
        val horizontalPadding = (fullCanvasWidth - targetWidth) / 2f

        val xInCanvas = keyPoint.x * targetWidth + horizontalPadding
        val yInCanvas = keyPoint.y * targetHeight

        val normalizedX = xInCanvas / fullCanvasWidth
        val normalizedY = yInCanvas

        return NormalizedPoint(normalizedX, normalizedY)
    }

    fun mapToPosition(map: Map<String, Double>?): Position {
        return Position(
            x = (map?.get("x") ?: 0.0).toFloat(),
            y = (map?.get("y") ?: 0.0).toFloat(),
            z = (map?.get("z") ?: 0.0).toFloat()
        )
    }

}