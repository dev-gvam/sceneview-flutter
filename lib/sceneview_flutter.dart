import 'package:flutter/services.dart';

export 'models/custom_models.dart';
export 'models/events.dart';
export 'ui/sceneview_controller.dart';
export 'ui/sceneview_widget.dart';

class SceneviewFlutter {
  static final _globalChannel = const MethodChannel("sceneview_global");

  static Future<bool> isAvailable() async {
    return await _globalChannel.invokeMethod("isAvailable");
  }
}
