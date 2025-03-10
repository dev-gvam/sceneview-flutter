import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:sceneview_flutter/sceneview_flutter.dart';

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
                sceneViewCtrl.onNodeTouched.listen((data) {
                  print("--> Node $data");
                });
              },
            ),
            Positioned(
              bottom: 0,
              child: Center(
                child: ElevatedButton(
                  onPressed: () {
                    sceneViewCtrl.loadPositions(
                      modelFilePath: "assets/models/map_pointer.glb",
                      positions: [
                        GeoPosition(
                            id: "Ed 1",
                            latitude: 40.44381554709421,
                            longitude: -3.702070465140602,
                            altitude: 776.8478780826553),
                        GeoPosition(
                            id: "Ed 2",
                            latitude: 40.44484051970683,
                            longitude: -3.7027837330439666,
                            altitude: 793.747869747690),
                        GeoPosition(
                            id: "Mechero", latitude: 40.44669338074851, longitude: -3.694475398363029, altitude: 853.0),
                        GeoPosition(
                            id: "Fuente delfines",
                            latitude: 40.44516877547935,
                            longitude: -3.685491388146533,
                            altitude: 800.0),
                      ],
                    );
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
    var cameraStatus = await Permission.camera.status;
    if (!cameraStatus.isGranted) {
      await Permission.camera.request();
    }
    var geolocationStatus = await Permission.location.status;
    if (!geolocationStatus.isGranted) {
      await Permission.location.request();
    }
  }
}
