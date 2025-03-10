import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:sceneview_flutter/scene_view_models.dart';
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
  final Map<String, Function(dynamic)> _eventHandlers = {};

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
  void loadPositions(String modelFilePath, List<GeoPosition> positions) {
    _methodChannel?.invokeListMethod('loadPositions', {
      "modelFilePath": modelFilePath,
      "positions": positions.map((m) => m.toJson()).toList(),
    });
  }

  @override
  void registerEventHandler(String eventType, Function(dynamic) callback) {
    _eventHandlers[eventType] = callback;
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
            _handleEventCall,
            onError: _handleEventError,
          );
      _eventChannel = channel;
    }
    return channel;
  }

  void _handleEventCall(dynamic event) {
    if (event is Map<dynamic, dynamic>) {
      String eventType = event['type'] ?? "";

      switch (eventType) {
        case 'onSessionCreated':
          bool data = event['data'] ?? false;
          _eventHandlers[eventType]?.call(data);
          break;
        case 'nodeTouched':
          String data = event['data'] ?? "";
          _eventHandlers[eventType]?.call(data);
          break;

        default:
          _handleEventError("Unhandled event type: $eventType");
          break;
      }
    } else {
      _handleEventError("Event is not a Map<dynamic, dynamic>");
    }
  }

  void _handleEventError(dynamic e) => print("--> Error in EventChannel: $e");
}
