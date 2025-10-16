import 'package:flutter/services.dart';

export 'models/custom_models.dart';
export 'models/events.dart';
export 'ui/sceneview_controller.dart';
export 'ui/sceneview_widget.dart';

class SceneviewFlutter {
  static final _globalChannel = const MethodChannel("sceneview_global");

  static Future<bool> isAvailable() async {
    try {
      var json = await _globalChannel.invokeMethod("isAvailable");
      var data = AvailableResult.fromJson(json);
      return data.status;
    } catch (e) {
      return false;
    }
  }
}

class AvailableResult {
  final String availavility;
  final bool status;

  AvailableResult({required this.availavility, required this.status});

  factory AvailableResult.fromJson(Map<String, dynamic> json) {
    if (!json.containsKey("availability") || json["availability"] == null) {
      throw ArgumentError("AvailableResult - availability IS NULL");
    }
    if (!json.containsKey("status") || json["status"] == null) {
      throw ArgumentError("AvailableResult - status IS NULL");
    }
    return AvailableResult(availavility: json["availability"], status: json["status"]);
  }
}
