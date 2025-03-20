package io.github.sceneview.sceneview_flutter

import dev.romainguy.kotlin.math.Float3


class GeoPositionLoader(
    val models: List<GeoPositionModel>,
    val positions: List<GeoPosition>
) {
    fun getModelPathByType(type: String): String {
        return models.find { it.type == type }?.modelPath ?: ""
    }

    companion object {
        fun fromJson(map: Map<String, *>): GeoPositionLoader {
            val modelList = (map["models"] as List<Map<String, *>>)
            val models = modelList.map { GeoPositionModel.fromJson(it) }
            val positionList = (map["positions"] as List<Map<String, *>>)
            val positions = positionList.map { GeoPosition.fromJson(it) }
            return GeoPositionLoader(
                models = models,
                positions = positions
            )
        }
    }
}

data class GeoPosition(
    val id: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
) {
    companion object {
        fun fromJson(map: Map<String, *>): GeoPosition {
            return GeoPosition(
                id = (map["id"] as String),
                type = (map["type"] as String),
                latitude = (map["latitude"] as Double),
                longitude = (map["longitude"] as Double),
                altitude = (map["altitude"] as Double),
            )
        }
    }
}

data class GeoPositionModel(
    val type: String,
    val modelPath: String,
) {
    companion object {
        fun fromJson(map: Map<String, *>): GeoPositionModel {
            return GeoPositionModel(
                type = (map["type"] as String),
                modelPath = (map["modelPath"] as String)
            )
        }
    }
}

abstract class FlutterSceneViewNode(
    val position: Float3 = Float3(0f, 0f, 0f),
    val rotation: Float3 = Float3(0f, 0f, 0f),
    val scale: Float3 = Float3(0f, 0f, 0f),
    val scaleUnits: Float = 1.0f,
) {

    companion object {
        fun from(map: Map<String, *>): FlutterSceneViewNode {
            val fileLocation = map["fileLocation"] as String?
            if (fileLocation != null) {
                val p = FlutterPosition.from(map["position"] as Map<String, Float>?)
                val r = FlutterRotation.from(map["rotation"] as Map<String, Float>?)
                val s = FlutterScale.from(map["scale"] as Map<String, Float>?)
                val scaleUnits = map["scaleUnits"] as Float?
                return FlutterReferenceNode(
                    fileLocation,
                    p.position,
                    r.rotation,
                    s.scale,
                    scaleUnits ?: 1.0f,
                )
            }
            throw Exception()
        }
    }
}


class FlutterReferenceNode(
    val fileLocation: String,
    position: Float3,
    rotation: Float3,
    scale: Float3,
    scaleUnits: Float
) :
    FlutterSceneViewNode(position, rotation, scale, scaleUnits)

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