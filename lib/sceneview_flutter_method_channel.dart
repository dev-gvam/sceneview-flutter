import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:sceneview_flutter/sceneview_node.dart';

import 'sceneview_flutter_platform_interface.dart';

class MethodChannelSceneViewFlutter extends SceneviewFlutterPlatform {
  static void registerWith() {
    SceneviewFlutterPlatform.instance = MethodChannelSceneViewFlutter();
  }

  @visibleForTesting
  final methodChannel = const MethodChannel('sceneview_method');
  final eventChannel = const EventChannel('sceneview_event');

  MethodChannel? _methodChannel;
  EventChannel? _eventChannel;
  StreamSubscription<dynamic>? _eventChannelSubscription;
  Function()? _onSessionCreated;

  @override
  Future<void> init(int sceneId) async {
    final channel = _ensureMethodChannelInitialized(sceneId);
    _ensureEventChannelInitialized(sceneId);
    return channel.invokeMethod<void>('init');
  }

  @override
  Future<void> dispose(int sceneId) async {
    if (_methodChannel != null) {
      await _methodChannel!.invokeMethod('dispose');
      _methodChannel = null;
    }
    if (_eventChannel != null) {
      await _eventChannelSubscription?.cancel();
      _eventChannelSubscription = null;
      _eventChannel = null;
    }
  }

  @override
  void addNode(SceneViewNode node) {
    _methodChannel?.invokeMethod('addNode', node.toMap());
  }

  @override
  void onSessionCreated(Function() callback) {
    _handleSessionCreated(callback);
  }

  MethodChannel _ensureMethodChannelInitialized(int sceneId) {
    MethodChannel? channel = _methodChannel;
    if (channel == null) {
      channel = MethodChannel('sceneview_method_$sceneId');
      channel.setMethodCallHandler((MethodCall call) => _handleMethodCall(call, sceneId));
      _methodChannel = channel;
    }
    return channel;
  }

  Future<dynamic> _handleMethodCall(MethodCall call, int mapId) async {
    switch (call.method) {
      default:
        throw MissingPluginException();
    }
  }

  EventChannel _ensureEventChannelInitialized(int sceneId) {
    EventChannel? channel = _eventChannel;
    if (channel == null) {
      channel = EventChannel('sceneview_event_$sceneId');
      _eventChannelSubscription = channel.receiveBroadcastStream().listen(
        (event) {
          if (event is Map<dynamic, dynamic>) {
            bool sessionCreated = event['sessionCreated'] ?? false;
            if (sessionCreated) _onSessionCreated?.call();
          }
        },
        onError: (e) {
          print("--> Error in EventChannel: $e");
        },
      );
      _eventChannel = channel;
    }
    return channel;
  }

  void _handleSessionCreated(Function() callback) {
    _onSessionCreated = callback;
  }
}
