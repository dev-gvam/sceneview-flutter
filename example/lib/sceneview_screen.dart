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
  StreamSubscription<String>? _streamNodeTouched;
  StreamSubscription<bool>? _streamLoadedNodes;

  @override
  void dispose() {
    _streamNodeTouched?.cancel();
    _streamLoadedNodes?.cancel();
    _controller?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) async {
        await _controller?.dispose().then(_closeView);
      },
      child: Scaffold(
        appBar: AppBar(
          title: const Text('AR Scene view'),
        ),
        body: Stack(
          children: [
            SceneView(
              onSessionCreated: (controller) {
                _controller = controller;
                _streamNodeTouched = _controller!.on<String>(SceneViewEvent.nodeTouched).listen((data) {
                  debugPrint("--> touched $data");
                });
                _streamLoadedNodes = _controller!.on<bool>(SceneViewEvent.nodeLoaded).listen((data) {
                  debugPrint("--> loaded $data");
                });
              },
            ),
            Positioned(
              bottom: MediaQuery.of(context).padding.bottom + 32,
              left: 32,
              right: 32,
              child: ElevatedButton(onPressed: _showModel, child: Text("Show Model")),
            ),
          ],
        ),
      ),
    );
  }

  void _showModel() {
    _controller?.addModel(
      ModelNode(
        assetType: AssetType.flutterAsset,
        id: "pin_natural",
        path: "assets/models/pin_natural.glb",
        rotation: KotlinFloat3(y: -90),
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
}
