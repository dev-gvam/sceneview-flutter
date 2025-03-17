import 'dart:async';

import 'package:sceneview_flutter/sceneview_flutter_events.dart';
import 'package:sceneview_flutter/sceneview_flutter_platform_interface.dart';
import 'package:sceneview_flutter/sceneview_node.dart';

/// When creating new functions call SceneviewFlutterPlatform.instance.invokeMethod()
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
    SceneviewFlutterPlatform.instance.invokeMethod("addNode", node.toMap());
  }

  /// All events available in native code through the eventChannel must be declared in SceneViewEvent
  Stream<T> on<T>(SceneViewEvent event) {
    return SceneviewFlutterPlatform.instance.on(event);
  }
}
