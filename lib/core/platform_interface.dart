import 'dart:async';

import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:sceneview_flutter/models/events.dart';

import 'channels.dart';

abstract class SceneviewPlatformInterface extends PlatformInterface {
  SceneviewPlatformInterface() : super(token: _token);

  static final Object _token = Object();

  static SceneviewPlatformInterface _instance = SceneViewChannels();

  /// The default instance of [SceneviewPlatformInterface] to use.
  ///
  /// Defaults to [SceneViewChannels].
  static SceneviewPlatformInterface get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [SceneviewPlatformInterface] when
  /// they register themselves.
  static set instance(SceneviewPlatformInterface instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<void> init(int sceneId) {
    throw UnimplementedError("init() has not been implemented.");
  }

  Future<bool> dispose() async {
    throw UnimplementedError("dispose() has not been implemented.");
  }

  Future<T?> invokeMethod<T>(String method, [dynamic arguments]) {
    throw UnimplementedError("invokeMethod() has not been implemented.");
  }

  Stream<T> on<T>(SceneViewEvent event) {
    throw UnimplementedError("on() has not been implemented.");
  }
}
