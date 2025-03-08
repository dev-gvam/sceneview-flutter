import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:sceneview_flutter/sceneview_node.dart';

import 'sceneview_flutter_method_channel.dart';

abstract class SceneviewFlutterPlatform extends PlatformInterface {
  SceneviewFlutterPlatform() : super(token: _token);

  static final Object _token = Object();

  static SceneviewFlutterPlatform _instance = MethodChannelSceneViewFlutter();

  static SceneviewFlutterPlatform get instance => _instance;

  static set instance(SceneviewFlutterPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<void> init(int sceneId) {
    throw UnimplementedError('init() has not been implemented.');
  }

  void addNode(SceneViewNode node) {
    throw UnimplementedError('addNode() has not been implemented.');
  }

  Future<void> dispose(int sceneId) async {
    throw UnimplementedError('dispose() has not been implemented.');
  }

  void onSessionCreated(Function() callback) {
    throw UnimplementedError('onSessionCreated() has not been implemented.');
  }
}
