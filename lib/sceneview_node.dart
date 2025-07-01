import 'package:sceneview_flutter/extension/vector_extension.dart';
import 'package:vector_math/vector_math_64.dart';

class SceneViewNode {
  final String fileLocation;
  final String? name;
  final String? parentName;
  final Vector3? position;
  final Vector3? rotation;
  final double? scale;

  SceneViewNode({
    required this.fileLocation,
    this.name,
    this.parentName,
    this.position,
    this.rotation,
    this.scale,
  });

  Map<String, dynamic> toMap() {
    final map = {
      'name': name,
      'parentNodeName': parentName,
      'fileLocation': fileLocation,
      'position': position?.toMap(),
      'rotation': rotation?.toMap(),
      'scale': scale,
    };
    map.removeWhere((key, value) => value == null);
    return map;
  }
}
