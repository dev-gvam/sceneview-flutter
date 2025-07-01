package io.github.sceneview.sceneview_flutter

import android.util.Log
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.cross
import dev.romainguy.kotlin.math.dot
import dev.romainguy.kotlin.math.normalize
import io.github.sceneview.collision.Vector3
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Transform
import io.github.sceneview.math.toVector3Box
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import kotlin.math.abs
import kotlin.math.acos


object Transform {
    fun animateNodePositionFromStartToEnd(
        node: ModelNode,
        start: Position,
        end: Position
    ) {
        node.isSmoothTransformEnabled=true
        node.smoothTransformSpeed=5f

        Log.d("Transform", "=== Starting animateNodePositionFromStartToEnd ===")
        Log.d("Transform", "Node name: ${node.name}")
        Log.d("Transform", "Start position: $start")
        Log.d("Transform", "End position: $end")
        Log.d("Transform", "Node initial position: ${node.position}")
        Log.d("Transform", "Node initial scale: ${node.scale}")

        val midpoint = Position(
            x = (start.x + end.x) / 2f,
            y = (start.y + end.y) / 2f,
            z = (start.z + end.z) / 2f

        )
        val diection= (end-start)
        val quaternion=     _computeLookRotation(Position(1f,0f,0f),diection)
        val scalefactor=(_computeScaleFactor(node,start,end)*1.3).toFloat()


            node.smoothTransform= Transform(midpoint, quaternion, Scale(scalefactor,scalefactor,scalefactor))


    }

    fun _computeLookRotation(from: Float3, to: Float3): Quaternion {
        val forward = normalize(from)
        val target = normalize(to)

        val dot = dot(forward, target)

        return if (dot >= 1.0f) {
            // Vectors are the same
            Quaternion()
        } else if (dot <= -1.0f) {
            // Vectors are opposite; rotate 180 degrees around any perpendicular axis
            val orthogonal = if (abs(forward.x) < 0.1f) {
                cross(forward, Float3(1f, 0f, 0f))
            } else {
                cross(forward, Float3(0f, 1f, 0f))
            }
            Quaternion.fromAxisAngle(normalize(orthogonal), 180f)
        } else {
            val axis = normalize(cross(forward, target))
            val angle = acos(dot)
            Quaternion.fromAxisAngle(axis, radiansToDegrees(angle))
        }
    }


    fun _computeScaleFactor(node: ModelNode, start: Position, end: Position): Float {
        // Get the size of the model's bounding box
        val size = node.boundingBox.toVector3Box().size

        // Find the longest side of the model
        val longestSide = maxOf(size.x, size.y, size.z)

        // Calculate the distance between the two positions
        val startVec = Vector3(start.x,start.y,start.z)
        val endVec =Vector3(end.x,end.y,end.z)
        val distance = Vector3.subtract(endVec, startVec).length()

        // Scale factor = desired world distance / model size along its longest axis
        if (longestSide == 0f) return 1f // Avoid division by zero
        return distance / longestSide
    }


}
fun radiansToDegrees(radians: Float): Float {
    return Math.toDegrees(radians.toDouble()).toFloat()
}



