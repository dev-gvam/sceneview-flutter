import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:sceneview_flutter/sceneview_node.dart';
import 'package:vector_math/vector_math_64.dart';

import 'sceneview_flutter_platform_interface.dart';

/// An implementation of [SceneviewFlutterPlatform] that uses method channels.
class MethodChannelSceneViewFlutter extends SceneviewFlutterPlatform {
  /// Registers the Android implementation of SceneviewFlutterPlatform.
  static void registerWith() {
    SceneviewFlutterPlatform.instance = MethodChannelSceneViewFlutter();
  }

  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('sceneview_flutter');

  MethodChannel? _channel;

  MethodChannel ensureChannelInitialized(int sceneId) {
    MethodChannel? channel;

    channel = MethodChannel('scene_view_$sceneId');
    channel.setMethodCallHandler(
        (MethodCall call) => _handleMethodCall(call, sceneId));
    _channel = channel;

    return channel;
  }

  @override
  Future<void> init(int sceneId) async {
    final channel = ensureChannelInitialized(sceneId);

    print("SceenID called:$sceneId");
    return channel.invokeMethod<void>('init');
  }

  @override
  void addNode(SceneViewNode node) {
    print("SceenID called:${_channel?.name}");
    _channel?.invokeMethod('addNode', node.toMap());
  }

  @override
  Future<Map> hitTest(
      double x, double y, double screenWidth, double screenHeight) async {
    final result = await _channel?.invokeMethod('hitTest', {
      "x": x,
      "y": y,
      "screenWidth": screenWidth,
      "screenHeight": screenHeight
    });
    return result;
  }

  @override
  Future<void> update(
    List<String> nodeNames, {
    SceneViewNode? node,
    Vector3? translation,
    Vector3? startPoint,
    Vector3? endPoint,
  }) async {
    print("xxxx:::$translation");
    final params = <String, dynamic>{'nodeName': nodeNames};
    if (node != null) {
      params.addAll(node.toMap());
    }

    if (translation != null) {
      params["translation"] = {
        "x": translation.x,
        "y": translation.y,
        "z": translation.z
      };
    }
    if (startPoint != null) {
      params["startPoint"] = {
        "x": startPoint.x,
        "y": startPoint.y,
        "z": startPoint.z
      };
    }
    if (endPoint != null) {
      params["endPoint"] = {"x": endPoint.x, "y": endPoint.y, "z": endPoint.z};
    }
    print("this is params: $params");

    await _channel?.invokeMethod('onUpdateNode', params);
  }

  @override
  Stream<bool> planeDetected() {
    const EventChannel eventChannel = EventChannel('plane_detected');
    return eventChannel.receiveBroadcastStream().where((data) {
      print("Plane Detected: $data");
      return data is bool;
    }).cast<bool>();
  }

  Future<dynamic> _handleMethodCall(MethodCall call, int mapId) async {
    switch (call.method) {
      default:
        throw MissingPluginException();
    }
  }

  @override
  void dispose(int sceneId) {}
}
