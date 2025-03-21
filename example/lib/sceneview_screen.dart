import 'dart:async';

import 'package:flutter/material.dart';
import 'package:sceneview_flutter/sceneview_flutter.dart';

class SceneViewScreen extends StatefulWidget {
  const SceneViewScreen({super.key});

  @override
  State<SceneViewScreen> createState() => _SceneViewScreenState();
}

class _SceneViewScreenState extends State<SceneViewScreen> {
  SceneViewController? _controller;
  StreamSubscription<bool>? _onSessionResumed;

  @override
  void dispose() {
    _onSessionResumed?.cancel();
    _controller?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) {
        _controller?.dispose().then(_closeView);
      },
      child: Scaffold(
        appBar: AppBar(
          title: const Text('AR Scene view'),
        ),
        body: Stack(
          children: [
            SceneView(
              onViewCreated: (controller) {
                _controller = controller;
                _onSessionResumed = _controller?.on<bool>(SceneViewEvent.sessionResumed).listen((data) {
                  debugPrint("Flutter: onSessionResumed $data");
                });
              },
            ),
            Positioned(
              bottom: 0,
              left: 0,
              right: 0,
              child: Center(
                child: ElevatedButton(
                  onPressed: _placeModel,
                  child: Text("Place model"),
                ),
              ),
            )
          ],
        ),
      ),
    );
  }

  void _closeView(bool dispose) {
    if (dispose) {
      if (mounted) {
        Navigator.pop(context);
      }
    }
  }

  void _placeModel() {
    _controller?.addNode(SceneViewNode(
      fileLocation: 'assets/models/MaterialSuite.glb',
      position: KotlinFloat3(z: -1.0),
      rotation: KotlinFloat3(x: 15),
    ));
  }
}
