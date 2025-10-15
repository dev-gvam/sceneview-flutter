import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:sceneview_flutter/sceneview_flutter.dart';
import 'package:sceneview_flutter_example/sceneview_screen.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: HomeScreen(),
    );
  }
}

class HomeScreen extends StatelessWidget {
  HomeScreen({super.key}) {
    checkCameraPermission();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Scene view example app'),
      ),
      body: SizedBox(
        width: MediaQuery.of(context).size.width,
        height: MediaQuery.of(context).size.height,
        child: Column(
          spacing: 20,
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            ElevatedButton(
              onPressed: () => _openSceneViewScreen(context),
              child: Text("Open SceneView Screen"),
            ),
            ElevatedButton(
              onPressed: () async {
                var available = await SceneviewFlutter.isAvailable();
                debugPrint("--> isAvailable $available");
              },
              child: Text("Is available"),
            ),
          ],
        ),
      ),
    );
  }

  void _openSceneViewScreen(BuildContext context) {
    Navigator.push(
      context,
      PageRouteBuilder(
        settings: RouteSettings(name: "sceneview_screen"),
        pageBuilder: (context, animation, secondaryAnimation) {
          return SceneViewScreen();
        },
        transitionsBuilder: (context, animation, secondaryAnimation, child) {
          return FadeTransition(
            opacity: animation,
            child: child,
          );
        },
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
