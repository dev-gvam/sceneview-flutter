package io.github.sceneview.sceneview_flutter

import com.google.ar.core.GeospatialPose

class GeoPositionLoader(
    val modelFilePath: String,
    val positions: List<GeoPosition>
) {

    companion object {
        fun fromJson(map: Map<String, *>): GeoPositionLoader {
            val modelFilePath = (map["modelFilePath"] as String)
            val positionList = (map["positions"] as List<Map<String, *>>)
            val positions = positionList.map { GeoPosition.fromJson(it) }
            return GeoPositionLoader(
                modelFilePath = modelFilePath,
                positions = positions
            )
        }
    }
}

data class GeoPosition(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
) {
    companion object {
        fun fromGeospatialPose(geospatialPose: GeospatialPose, id: String = ""): GeoPosition {
            return GeoPosition(
                id = id,
                latitude = geospatialPose.latitude,
                longitude = geospatialPose.longitude,
                altitude = geospatialPose.altitude
            )
        }
        fun fromJson(map: Map<String, *>): GeoPosition {
            return GeoPosition(
                id = (map["id"] as String),
                latitude = (map["latitude"] as Double),
                longitude = (map["longitude"] as Double),
                altitude = (map["altitude"] as Double),
            )
        }
    }
}