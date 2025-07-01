import 'package:sceneview_flutter/sceneview_flutter_platform_interface.dart';
import 'package:sceneview_flutter/sceneview_node.dart';
import 'package:vector_math/vector_math_64.dart';

class SceneViewController {
  SceneViewController._({
    required this.sceneId,
  });

  final int sceneId;

  static Future<SceneViewController> init(
    int sceneId,
  ) async {
    await SceneviewFlutterPlatform.instance.init(sceneId);
    return SceneViewController._(sceneId: sceneId);
  }

  void addNode(SceneViewNode node) {
    SceneviewFlutterPlatform.instance.addNode(node);
  }

  Future<Map> hitTest(
      double x, double y, double screenWidth, double screenHeight) async {
    return SceneviewFlutterPlatform.instance
        .hitTest(x, y, screenWidth, screenHeight);
  }

  Stream<bool> planeDetected() {
    return SceneviewFlutterPlatform.instance.planeDetected();
  }

  Future<void> update(
    List<String> nodeNames, {
    SceneViewNode? node,
    Vector3? translation,
    Vector3? startPoint,
    Vector3? endPoint,
  }) {
    return SceneviewFlutterPlatform.instance.update(
      nodeNames,
      node: node,
      translation: translation,
      startPoint: startPoint,
      endPoint: endPoint,
    );
  }

  void dispose() {
    SceneviewFlutterPlatform.instance.dispose(sceneId);
  }
}
