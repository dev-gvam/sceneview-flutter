/// Define here all the event types
enum SceneViewEvent {
  sessionResumed,
  nodeTouched,
  nodeLoaded,
}

class SceneViewEventData {
  final SceneViewEvent event;
  final dynamic data;

  SceneViewEventData({
    required this.event,
    required this.data,
  });
}

bool isValidSceneViewEvent(String value) {
  for (var type in SceneViewEvent.values) {
    if (type.name.toLowerCase() == value.toLowerCase()) {
      return true;
    }
  }
  return false;
}

SceneViewEvent? stringToSceneViewEvent(String value) {
  for (var type in SceneViewEvent.values) {
    if (type.name.toLowerCase() == value.toLowerCase()) {
      return type;
    }
  }
  return null;
}
