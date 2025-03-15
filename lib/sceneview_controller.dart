import 'package:sceneview_flutter/sceneview_flutter_platform_interface.dart';
import 'package:sceneview_flutter/sceneview_node.dart';

class SceneViewController {
  final int sceneId;

  SceneViewController._(this.sceneId);

  static Future<SceneViewController> init(int sceneId) async {
    await SceneviewFlutterPlatform.instance.init(sceneId);
    return SceneViewController._(sceneId);
  }

  Future<bool> dispose() async {
    return await SceneviewFlutterPlatform.instance.dispose();
  }

  void addNode(SceneViewNode node) {
    SceneviewFlutterPlatform.instance.addNode(node);
  }
}
