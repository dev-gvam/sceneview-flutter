import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:sceneview_flutter/sceneview_flutter.dart';
import 'package:sceneview_flutter/sceneview_node.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  late final SceneViewController sceneViewCtrl;

  @override
  void initState() {
    checkCameraPermission();
    super.initState();
  }

  @override
  void dispose() {
    sceneViewCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Scene view example app'),
        ),
        body: Stack(
          children: [
            SceneView(
              onSessionCreated: (controller) {
                sceneViewCtrl = controller;
              },
            ),
            Positioned(
              bottom: 0,
              child: Center(
                child: ElevatedButton(
                  onPressed: () {
                    sceneViewCtrl.addNode(SceneViewNode(
                      fileLocation: 'assets/models/MaterialSuite.glb',
                      position: KotlinFloat3(z: -1.0),
                      rotation: KotlinFloat3(x: 15),
                    ));
                  },
                  child: Text("Test Anchor"),
                ),
              ),
            )
          ],
        ),
      ),
    );
  }

  void checkCameraPermission() async {
    var status = await Permission.camera.status;
    if (!status.isGranted) {
      await Permission.camera.request();
    }
  }
}
