import 'dart:async';

import 'package:sceneview_flutter/sceneview_flutter_platform_interface.dart';
import 'package:sceneview_flutter/sceneview_node.dart';

class SceneViewController {
  final int sceneId;
  final _sessionCreatedCompleter = Completer<SceneViewController>();
  final StreamController<String> _nodeTouchedController = StreamController.broadcast();

  Stream<String> get onNodeTouched => _nodeTouchedController.stream;

  SceneViewController._(this.sceneId);

  static Future<SceneViewController> init(int sceneId) async {
    final controller = SceneViewController._(sceneId);

    SceneviewFlutterPlatform.instance.registerEventHandler("onSessionCreated", (data) {
      if (data == true && !controller._sessionCreatedCompleter.isCompleted) {
        controller._sessionCreatedCompleter.complete(controller);
      }
    });

    SceneviewFlutterPlatform.instance.registerEventHandler("nodeTouched", (data) {
      if (data is String && data.isNotEmpty) {
        controller._nodeTouchedController.add(data);
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
