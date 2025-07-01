import 'dart:typed_data';
import 'package:flutter/services.dart';

class SceneviewImageStreamHandler {
  static const EventChannel _eventChannel = EventChannel('ar_image');

  static Stream<Uint8List> getStream() {
    return _eventChannel.receiveBroadcastStream().where((data) {
      print("imagedataxxx : $data");
      return data is Uint8List;
    }).cast<Uint8List>();
  }

  static void startListening(void Function(Uint8List imageBytes) onData) {
    getStream().listen(onData);
  }
}
