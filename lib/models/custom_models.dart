class GeoPosition {
  final String id;
  final double latitude;
  final double longitude;
  final double altitude;
  String type;

  GeoPosition({
    required this.id,
    required this.latitude,
    required this.longitude,
    required this.altitude,
    this.type = "",
  });

  Map<String, dynamic> toJson() => {
        "id": id,
        "type": type,
        "latitude": latitude,
        "longitude": longitude,
        "altitude": altitude,
      };
}

class GeoPositionModel {
  final String type;
  final String modelPath;

  GeoPositionModel(this.type, this.modelPath);

  Map<String, String> toJson() => {"type": type, "modelPath": modelPath};
}

class SceneViewNode {
  final String fileLocation;
  final KotlinFloat3? position;
  final KotlinFloat3? rotation;
  final double? scale;

  SceneViewNode({
    required this.fileLocation,
    this.position,
    this.rotation,
    this.scale,
  });

  Map<String, dynamic> toMap() {
    final map = {
      'fileLocation': fileLocation,
      'position': position?.toMap(),
      'rotation': rotation?.toMap(),
      'scale': scale,
    };
    map.removeWhere((key, value) => value == null);
    return map;
  }
}

class KotlinFloat3 {
  final double x;
  final double y;
  final double z;

  KotlinFloat3({this.x = 0.0, this.y = 0.0, this.z = 0.0});

  toMap() {
    return <String, double>{
      'x': x,
      'y': y,
      'z': z,
    };
  }
}
