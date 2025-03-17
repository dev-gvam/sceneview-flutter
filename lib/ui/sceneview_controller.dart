import 'dart:async';

import 'package:sceneview_flutter/core/platform_interface.dart';
import 'package:sceneview_flutter/models/custom_models.dart';
import 'package:sceneview_flutter/models/events.dart';

/// When creating new functions call SceneviewPlatformInterface.instance.invokeMethod()
class SceneViewController {
  final int sceneId;

  SceneViewController._(this.sceneId);

  static Future<SceneViewController> init(int sceneId) async {
    await SceneviewPlatformInterface.instance.init(sceneId);
    return SceneViewController._(sceneId);
  }

  Future<bool> dispose() async {
    return await SceneviewPlatformInterface.instance.dispose();
  }

  void addNode(SceneViewNode node) {
    SceneviewPlatformInterface.instance.invokeMethod("addNode", node.toMap());
  }

  /// All events available in native code through the eventChannel must be declared in SceneViewEvent
  Stream<T> on<T>(SceneViewEvent event) {
    return SceneviewPlatformInterface.instance.on(event);
  }
}
