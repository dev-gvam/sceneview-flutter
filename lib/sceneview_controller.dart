import 'dart:async';

import 'package:sceneview_flutter/sceneview_flutter_platform_interface.dart';
import 'package:sceneview_flutter/sceneview_node.dart';

class SceneViewController {
  final int sceneId;
  final _sessionCreatedCompleter = Completer<SceneViewController>();

  SceneViewController._(this.sceneId);

  static Future<SceneViewController> init(int sceneId) async {
    final controller = SceneViewController._(sceneId);

    SceneviewFlutterPlatform.instance.registerEventHandler("onSessionCreated", (data) {
      if (data == true && !controller._sessionCreatedCompleter.isCompleted) {
        controller._sessionCreatedCompleter.complete(controller);
      }
    });

    await SceneviewFlutterPlatform.instance.init(sceneId);

    return controller._sessionCreatedCompleter.future;
  }

  void addNode(SceneViewNode node) {
    SceneviewFlutterPlatform.instance.addNode(node);
  }

  Future<void> dispose() async {
    SceneviewFlutterPlatform.instance.dispose(sceneId);
  }
}
