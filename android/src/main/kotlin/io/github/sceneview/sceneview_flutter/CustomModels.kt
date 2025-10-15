package io.github.sceneview.sceneview_flutter

import dev.romainguy.kotlin.math.Float3

abstract class FlutterSceneViewNode(
    val id: String? = null,
    val position: Float3 = Float3(0f, 0f, 0f),
    val rotation: Float3 = Float3(0f, 0f, 0f),
    val scale: Float3 = Float3(0f, 0f, 0f),
    val scaleUnits: Float = 1.0f,
) {

    companion object {
        fun from(map: Map<String, *>): FlutterSceneViewNode {
            val id = map["id"]?.toString()

            val assetTypeStr = map["assetType"]?.toString()
                ?: throw IllegalArgumentException("assetType is required")
            val assetType = try {
                AssetType.valueOf(assetTypeStr)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Unknown assetType: '$assetTypeStr' -- $e")
            }

            val path = map["path"]?.toString()
                ?: throw IllegalArgumentException("path is required")

            fun vec3(key: String, def: Float3): Float3 {
                val m = map[key] as? Map<*, *> ?: return def
                val x = (m["x"] as? Number)?.toFloat() ?: def.x
                val y = (m["y"] as? Number)?.toFloat() ?: def.y
                val z = (m["z"] as? Number)?.toFloat() ?: def.z
                return Float3(x, y, z)
            }

            val position = vec3("position", Float3(0f, 0f, 0f))
            val rotation = vec3("rotation", Float3(0f, 0f, 0f))
            val scale = vec3("scale", Float3(1f, 1f, 1f))
            val scaleUnits = (map["scaleUnits"] as? Number)?.toFloat() ?: 1.0f

            return FlutterReferenceNode(
                assetType = assetType,
                path = path,
                id = id,
                position = position,
                rotation = rotation,
                scale = scale,
                scaleUnits = scaleUnits
            )
        }
    }
}

enum class AssetType {
    flutterAsset,
    documents
}

class FlutterReferenceNode(
    val assetType: AssetType,
    val path: String,
    id: String?,
    position: Float3,
    rotation: Float3,
    scale: Float3,
    scaleUnits: Float
) :
    FlutterSceneViewNode(id, position, rotation, scale, scaleUnits)

class FlutterPosition(val position: Float3) {
    companion object {
        fun from(map: Map<String, Float>?): FlutterPosition {
            if (map == null) {
                return FlutterPosition(Float3(0f, 0f, 0f))
            }
            val x = (map["x"] as Double).toFloat()
            val y = (map["y"] as Double).toFloat()
            val z = (map["z"] as Double).toFloat()
            return FlutterPosition(Float3(x, y, z))
        }
    }
}

class FlutterRotation(val rotation: Float3) {
    companion object {
        fun from(map: Map<String, Float>?): FlutterRotation {
            if (map == null) {
                return FlutterRotation(Float3(0f, 0f, 0f))
            }
            val x = (map["x"] as Double).toFloat()
            val y = (map["y"] as Double).toFloat()
            val z = (map["z"] as Double).toFloat()
            return FlutterRotation(Float3(x, y, z))
        }
    }
}

class FlutterScale(val scale: Float3) {
    companion object {
        fun from(map: Map<String, Float>?): FlutterScale {
            if (map == null) {
                return FlutterScale(Float3(0f, 0f, 0f))
            }
            val x = (map["x"] as Double).toFloat()
            val y = (map["y"] as Double).toFloat()
            val z = (map["z"] as Double).toFloat()
            return FlutterScale(Float3(x, y, z))
        }
    }
}