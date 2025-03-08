import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:sceneview_flutter/sceneview_controller.dart';

class SceneView extends StatefulWidget {
  const SceneView({
    super.key,
    required this.onSessionCreated,
  });

  final Function(SceneViewController) onSessionCreated;

  @override
  State<SceneView> createState() => _SceneViewState();
}

class _SceneViewState extends State<SceneView> {
  final Completer<SceneViewController> _controller = Completer<SceneViewController>();

  @override
  void initState() {
    super.initState();
  }

  @override
  void dispose() {
    _disposeController();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // This is used in the platform side to register the view.
    const String viewType = 'SceneView';
    // Pass parameters to the platform side.
    const Map<String, dynamic> creationParams = <String, dynamic>{};

    return PlatformViewLink(
      viewType: viewType,
      surfaceFactory: (context, controller) {
        return AndroidViewSurface(
          controller: controller as AndroidViewController,
          gestureRecognizers: const <Factory<OneSequenceGestureRecognizer>>{},
          hitTestBehavior: PlatformViewHitTestBehavior.opaque,
        );
      },
      onCreatePlatformView: (params) {
        return PlatformViewsService.initExpensiveAndroidView(
          id: params.id,
          viewType: viewType,
          layoutDirection: TextDirection.ltr,
          creationParams: creationParams,
          creationParamsCodec: const StandardMessageCodec(),
          onFocus: () {
            params.onFocusChanged(true);
          },
        )
          ..addOnPlatformViewCreatedListener(params.onPlatformViewCreated)
          ..addOnPlatformViewCreatedListener((id) => _onPlatformViewCreated(id));
      },
    );
  }

  Future<void> _onPlatformViewCreated(int id) async {
    await SceneViewController.init(id).then(widget.onSessionCreated);
  }

  Future<void> _disposeController() async {
    if (_controller.isCompleted) {
      final SceneViewController controller = await _controller.future;
      await controller.dispose();
    }
  }
}
