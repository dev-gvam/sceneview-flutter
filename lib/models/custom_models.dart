/// Describes the location of the asset.
enum AssetType {
  /// Flutter asset folder (e.g. ./assets/gltf)
  /// See https://docs.flutter.dev/ui/assets/assets-and-images#specifying-assets
  flutterAsset,

  /// Documents folder for the current app
  /// This might be useful for downloading files from the internet
  documents,
}

class ModelNode {
  final AssetType assetType;
  final String path;
  final String? id;
  final KotlinFloat3? position;
  final KotlinFloat3? rotation;
  final double? scale;

  ModelNode({
    required this.assetType,
    required this.path,
    this.id,
    this.position,
    this.rotation,
    this.scale,
  });

  Map<String, dynamic> toMap() {
    final map = {
      'assetType': assetType.name,
      'path': path,
      'id': id,
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
