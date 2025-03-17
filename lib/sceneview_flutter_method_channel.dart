import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:sceneview_flutter/sceneview_flutter_events.dart';

import 'sceneview_flutter_platform_interface.dart';

/// An implementation of [SceneviewFlutterPlatform] that uses method channels.
class MethodChannelSceneViewFlutter extends SceneviewFlutterPlatform {
  static const String methodChannelIdentifier = "sceneview_methods";
  static const String eventChannelIdentifier = "sceneview_events";

  /// Registers the Android implementation of SceneviewFlutterPlatform.
  static void registerWith() {
    SceneviewFlutterPlatform.instance = MethodChannelSceneViewFlutter();
  }

  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel(methodChannelIdentifier);
  final eventChannel = const EventChannel(eventChannelIdentifier);

  MethodChannel? _methodChannel;
  EventChannel? _eventChannel;
  StreamSubscription<dynamic>? _eventChannelSubscription;
  final StreamController<SceneViewEventData> _eventController = StreamController<SceneViewEventData>.broadcast();
  Stream<SceneViewEventData> get eventStream => _eventController.stream;

  @override
  Future<void> init(int sceneId) async {
    _ensureMethodChannelInitialized(sceneId);
    _ensureEventChannelInitialized(sceneId);
    return invokeMethod<void>("init");
  }

  @override
  Future<bool> dispose() async {
    final result = await invokeMethod<bool>("dispose") ?? false;
    if (result) {
      _methodChannel?.setMethodCallHandler(null);
      _methodChannel = null;
      _eventChannel = null;
      await _eventChannelSubscription?.cancel();
      _eventChannelSubscription = null;
      _eventController.close();
    }
    return result;
  }

  @override
  Future<T?> invokeMethod<T>(String method, [dynamic arguments]) async {
    return await _methodChannel?.invokeMethod<T>(method, arguments);
  }

  @override
  Stream<T> on<T>(SceneViewEvent event) => eventStream.where((e) => e.event == event).map((e) => e.data as T);

  MethodChannel _ensureMethodChannelInitialized(int sceneId) {
    MethodChannel? channel = _methodChannel;
    if (channel == null) {
      channel = MethodChannel("$methodChannelIdentifier-$sceneId");
      channel.setMethodCallHandler((MethodCall call) => _handleMethodCall(call, sceneId));
      _methodChannel = channel;
    }
    return channel;
  }

  /// Add here flutter functions you want to call from native
  Future<dynamic> _handleMethodCall(MethodCall call, int mapId) async {
    switch (call.method) {
      default:
        throw MissingPluginException();
    }
  }

  EventChannel _ensureEventChannelInitialized(int sceneId) {
    EventChannel? channel = _eventChannel;
    if (channel == null) {
      channel = EventChannel("$eventChannelIdentifier-$sceneId");
      _eventChannelSubscription = channel.receiveBroadcastStream().listen(
            _handleEventCall,
            onError: _handleEventError,
          );
      _eventChannel = channel;
    }
    return channel;
  }

  void _handleEventCall(dynamic map) {
    if (map is! Map<dynamic, dynamic>) {
      _handleEventError("Event is not a Map<dynamic, dynamic>");
      return;
    }

    final String event = map["type"] ?? "";
    final dynamic data = map["data"];

    if (!isValidSceneViewEvent(event) || event.isEmpty) {
      _handleEventError("Event type is empty or not valid");
      return;
    }

    _eventController.add(
      SceneViewEventData(
        event: stringToSceneViewEvent(event)!,
        data: data,
      ),
    );
  }

  void _handleEventError(dynamic e) {
    debugPrint("Flutter: Error in EventChannel: $e");
  }
}
