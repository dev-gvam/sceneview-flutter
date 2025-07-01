package io.github.sceneview.sceneview_flutter

import android.app.Activity
import android.content.Context
import android.media.Image
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.Config
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.DeadlineExceededException
import com.google.ar.core.exceptions.NotYetAvailableException
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.platform.PlatformView
import io.github.sceneview.arsceneview.ARSceneView
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.launch
import io.flutter.plugin.common.EventChannel
import io.github.sceneview.arsceneview.arcore.getUpdatedPlanes
import io.github.sceneview.arsceneview.arcore.isTracking
import io.github.sceneview.geometries.Plane
import io.github.sceneview.math.toVector3Box
import io.github.sceneview.node.Node

//
//class SceneViewWrapper(
//    context: Context,
//    private val activity: Activity,
//    applifecycle: Lifecycle,
//    messenger: BinaryMessenger,
//    id: Int,
//) : PlatformView, MethodCallHandler {
//    private val TAG = "SceneViewWrapper"
//    private var sceneView: ARSceneView
//    private val _mainScope = CoroutineScope(Dispatchers.Main)
//    private val _channel = MethodChannel(messenger, "scene_view_$id")
//
//    override fun getView(): View {
//        Log.i(TAG, "getView:")
//        return sceneView
//    }
//
//    override fun dispose() {
//        sceneView.destroy()
//        Log.i(TAG, "dispose")
//    }
//
//    init {
//        Log.i(TAG, "init")
//        sceneView = ARSceneView(context)
//        sceneView.apply {
//            lifecycle=applifecycle
//            configureSession { session, config ->
//                Log.e(TAG, "configureSession : $session")
//                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
//                config.depthMode = when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
//                    true -> Config.DepthMode.AUTOMATIC
//                    else -> Config.DepthMode.DISABLED
//                }
//                config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
//            }
//            onSessionResumed = { session ->
//                Log.i(TAG, "onSessionCreated")
//            }
//            onSessionFailed = { exception ->
//                Log.e(TAG, "onSessionFailed : $exception")
//            }
//            onSessionCreated = { session ->
//                Log.i(TAG, "onSessionCreated")
//            }
//            onTrackingFailureChanged = { reason ->
//                Log.i(TAG, "onTrackingFailureChanged: $reason");
//            }
//        }
//        sceneView.layoutParams = FrameLayout.LayoutParams(
//            FrameLayout.LayoutParams.MATCH_PARENT,
//            FrameLayout.LayoutParams.MATCH_PARENT
//        )
//        sceneView.keepScreenOn = true
//        _channel.setMethodCallHandler(this)
//    }
//
//    private suspend fun addNode(flutterNode: FlutterSceneViewNode) {
//        val session = sceneView.session ?: return
//
//        val config = Config(session).apply {
//            lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
//            // Add more settings here if needed
//        }
//
//        session.configure(config)
//
//        val node = buildNode(flutterNode) ?: return
//        sceneView.addChildNode(node)
//        //AnchorNode(sceneView.engine, anchor).apply {}
//        Log.d(TAG, "Done")
//    }
//
//    private suspend fun buildNode(flutterNode: FlutterSceneViewNode): ModelNode? {
//        var model: ModelInstance? = null
//
//        /*
//                AnchorNode(sceneView.engine, anchor)
//                    .apply {
//                        isEditable = true
//                        //isLoading = true
//                        sceneView.modelLoader.loadModelInstance(
//                            "https://sceneview.github.io/assets/models/DamagedHelmet.glb"
//                        )?.let { modelInstance ->
//                            addChildNode(
//                                ModelNode(
//                                    modelInstance = modelInstance,
//                                    // Scale to fit in a 0.5 meters cube
//                                    scaleToUnits = 0.5f,
//                                    // Bottom origin instead of center so the model base is on floor
//                                    centerOrigin = Position(y = -0.5f)
//                                ).apply {
//                                    isEditable = true
//                                }
//                            )
//                        }
//                        //isLoading = false
//                        anchorNode = this
//                    }
//        */
//        when (flutterNode) {
//            is FlutterReferenceNode -> {
//                val fileLocation = Utils.getFlutterAssetKey(activity, flutterNode.fileLocation)
//                Log.d("SceneViewWrapper", fileLocation)
//                model =
//                    sceneView.modelLoader.loadModelInstance(fileLocation)
//            }
//        }
//        if (model != null) {
//            val modelNode = ModelNode(modelInstance = model, scaleToUnits = 1.0f).apply {
//                transform(
//                    position = flutterNode.position,
//                    rotation = flutterNode.rotation,
//                    //scale = flutterNode.scale,
//                )
//                //scaleToUnitsCube(flutterNode.scaleUnits)
//                // TODO: Fix centerOrigin
//                //     centerOrigin(Position(x=-1.0f, y=-1.0f))
//                //playAnimation()
//            }
//            return modelNode
//        }
//        return null
//    }
//
//    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
//        when (call.method) {
//            "init" -> {
//                result.success(null)
//            }
//
//            "addNode" -> {
//                Log.i(TAG, "addNode")
//                val flutterNode = FlutterSceneViewNode.from(call.arguments as Map<String, *>)
//                _mainScope.launch {
//                    addNode(flutterNode)
//                }
//                result.success(null)
//                return
//            }
//
//            else -> result.notImplemented()
//        }
//    }
//}

class SceneViewWrapper(
    context: Context,
    private val activity: Activity,
    private val applifecycle: Lifecycle,
    private val _mainScope: LifecycleCoroutineScope,
    messenger: BinaryMessenger,
    private val id: Int,
    private  var isSessionConfigured: Boolean=false
) : PlatformView, MethodCallHandler ,DefaultLifecycleObserver{

    private val TAG = "SceneViewWrapper"
    private var sceneView: ARSceneView? = null
    private var eventSink: EventChannel.EventSink? = null
    private var planeDetectedSink: EventChannel.EventSink? = null
    private val _channel = MethodChannel(messenger, "scene_view_$id")

    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler
    private var lastFrameTime = 0L
    private val frameIntervalMillis = 200L

    var lastPlaneTracking = false
    var noPlaneSince: Long? = null
    override fun getView(): View {





        return sceneView ?: FrameLayout(activity)
    }

    override fun onResume(owner: LifecycleOwner) {
        Log.i(TAG, "Lifecycle onResume called")



    }

    fun startBackgroundThread() {
        backgroundThread = HandlerThread("ImageProcessingThread")
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    fun stopBackgroundThread() {
        if(::backgroundThread.isInitialized){
            backgroundThread.quitSafely()
            backgroundThread.join()
        }




    }
    fun InitalizeEventSink(
        eventSink: EventChannel.EventSink? = null,
        planeDetectedSink: EventChannel.EventSink? = null
    ) {
        eventSink?.let { this.eventSink = it


        }
        planeDetectedSink?.let { this.planeDetectedSink = it
        it.success(false)
        }
    }



    init {
        Log.i(TAG, "init called")
        applifecycle.addObserver(this)
        initializeSceneView()

        _channel.setMethodCallHandler(this)

    }


    private fun initializeSceneView() {
        sceneView?.configureSession { config, session -> }


        sceneView = ARSceneView(activity).apply {
            Log.i(TAG, "inside apply")

            lifecycle = applifecycle


            configureSession { session, config ->
                Log.i(TAG, "configureSession: session=$session")
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                config.depthMode =
                    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                        Config.DepthMode.AUTOMATIC
                    } else {
                        Config.DepthMode.DISABLED
                    }
                config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
            }

            onSessionResumed = {
                Log.i(TAG, "onSessionResumed")
            }
            onSessionCreated = {
                Log.i(TAG, "onSessionCreated")
            }
            onSessionFailed = { exception ->
                Log.e(TAG, "onSessionFailed: $exception")
            }
            onTrackingFailureChanged = { reason ->
                Log.i(TAG, "onTrackingFailureChanged: $reason")
            }
            onSessionConfigChanged = { session, config ->

                Log.i(TAG, "Configuration Changed: ")
                config.planeFindingMode= Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL


            }
            onSessionUpdated = { session, frame ->
                val updatedPlanes = session.getAllTrackables<com.google.ar.core.Plane>(com.google.ar.core.Plane::class.java)
                val anyPlaneTracking = updatedPlanes.any { it.isTracking }

                val currentTime = System.currentTimeMillis()

                if (anyPlaneTracking) {
                    noPlaneSince = null

                    if (!lastPlaneTracking) {
                        planeDetectedSink?.success(true)
                        lastPlaneTracking = true
                        Log.d("PlaneDetected", "✅ Plane detected")
                    }
                } else {
                    if (lastPlaneTracking) {
                        // Only declare "no plane" after 1 second without any
                        if (noPlaneSince == null) {
                            noPlaneSince = currentTime
                        } else if (currentTime - noPlaneSince!! > 500) {
                            planeDetectedSink?.success(false)
                            lastPlaneTracking = false
                            Log.d("PlaneDetected", "❌ Plane lost")
                        }
                    }
                }


                printAllNodes()



                if (frame.camera.trackingState == TrackingState.TRACKING) {
                    val now = System.currentTimeMillis()
                    if (now - lastFrameTime > frameIntervalMillis) {
                        // Skip frame to throttle FPS


                        lastFrameTime = now

                        backgroundHandler.post {
                            var image: Image? = null
                            try {
                                image = frame.acquireCameraImage()
                                val width = image.width
                                val height = image.height
//
                                val startTime = System.nanoTime()
                                val nv21 = ImageUtils.yuv420ToNV21(image)

                                val rotatednv21 = ImageUtils.rotateNV21(nv21, width, height, 90)
                                val jpeg= ImageUtils.cropCpuImageToMatchArViewAspect(rotatednv21,height,width,1080,1920,50)

//                                val jpeg =
//                                    ImageUtils.nv21ToJpeg(rotatednv21, height, width, 16, 9, 50)
                                val endTime = System.nanoTime()
                                Log.d(
                                    "ArCoreView",
                                    "⏱️ YUV to RGB render took ${(endTime - startTime) / 1_000_000} ms"
                                )


                                Handler(Looper.getMainLooper()).post {


                                    eventSink?.success(jpeg)
                                }

                            } catch (e: DeadlineExceededException) {
                                Log.w(
                                    "ArCoreView",
                                    "⚠️ DeadlineExceededException, skipping frame"
                                )
                            } catch (e: NotYetAvailableException) {
                                Log.d("ArCoreView", "⏳ Camera image not yet available")
                            } catch (e: Exception) {
                                Log.e("ArCoreView", "❌ Exception: ${e.message}")
                            } finally {
                                image?.close()  // safe to call even if image == null
                            }
                        }
                    }
                }
            }


            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            keepScreenOn = true


        }
    }

    private fun printAllNodes() {
        val scene = sceneView ?: return
        Log.i(TAG, "Printing all nodes in sceneView:")
        scene.childNodes.forEach { node ->
            printNodeRecursive(node, "")
        }
    }

    private fun printNodeRecursive(node: Node, indent: String) {
        Log.i(TAG, "$indent- Node: ${node.name ?: "Unnamed"} (${node::class.simpleName})")
        node.childNodes.forEach { child ->
            printNodeRecursive(child, "$indent  ")
        }
    }



            override fun dispose() {
                Log.i(TAG, "dispose called, $sceneView")
                sceneView?.destroy()
                sceneView = null
                Log.i(TAG, "sceneView destroyed and set to null")
            }


            private suspend fun addNode(flutterNode: FlutterSceneViewNode) {
                val scene = sceneView

                if (scene == null) {
                    Log.e(TAG, "Cannot add node, sceneView is null $sceneView  $id")
                    return
                }

                val session = scene.session
                if (session == null) {
                    Log.e(TAG, "AR session is null, cannot configure")
                    return
                }

                if (!isSessionConfigured) {
                    val config = Config(session).apply {
                        lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                        focusMode = Config.FocusMode.AUTO
                        planeFindingMode= Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL





                        depthMode =
                            if (session!!.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                                Config.DepthMode.AUTOMATIC
                            } else {
                                Config.DepthMode.DISABLED
                            }
                        instantPlacementMode = Config.InstantPlacementMode.DISABLED


                    }

                    try {
                        session.configure(config)
                        isSessionConfigured = true
                        Log.d(TAG, "Session configured")
                    } catch (e: Exception) {
                        Log.e(TAG, "Session configuration failed: ${e.message}")
                    }
                }

                val node = buildNode(flutterNode)
                if (node != null) {




                    scene.addChildNode(node)

                    Log.d(TAG, "Node added ${node.name}")
                } else {
                    Log.e(TAG, "Failed to build node")
                }
            }



            private suspend fun buildNode(flutterNode: FlutterSceneViewNode): ModelNode? {
                val scene = sceneView ?: return null
                var model: ModelInstance? = null

                when (flutterNode) {
                    is FlutterReferenceNode -> {
                        val fileLocation =
                            Utils.getFlutterAssetKey(activity, flutterNode.fileLocation)
                        Log.d(TAG, "Model path: $fileLocation")
                        model = scene.modelLoader.loadModelInstance(fileLocation)
                    }
                }

                return model?.let {
                    ModelNode(modelInstance = it, scaleToUnits = 1.0f).apply {
                        transform(
                            position = flutterNode.position,
                            rotation = flutterNode.rotation
                        )
                        name=flutterNode.name
                    }
                }
            }


            override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
                when (call.method) {
                    "init" -> {
                        result.success(null)
                    }

                    "addNode" -> {
                        Log.i(TAG, "addNode called from Flutter")
                        val flutterNode =
                            FlutterSceneViewNode.from(call.arguments as Map<String, *>)
                        _mainScope.launch {
                            addNode(flutterNode)
                        }
                        result.success(null)
                    }
                    "hitTest" -> {
                        val x = call.argument<Double>("x") ?: 0.5
                        val y = call.argument<Double>("y") ?: 0.5
                        val scnreenWidth = call.argument<Double>("screenWidth") ?: 0.0
                        val screenHeight = call.argument<Double>("screenHeight") ?: 0.0

                        val coords = DepthUtils.performFullDepthHitTest(
                            arSceneView = sceneView,
                            xNorm = x.toFloat(),
                            yNorm = y.toFloat(),
                            scnreenWidth.toFloat(),
                            screenHeight.toFloat()
                        )
                        if (coords != null) {
                            result.success(coords)
                        } else {
                            result.error("NO_HIT", "No surface found", null)
                        }
                    }

                    "onUpdateNode" -> {
                        if (sceneView==null){
                            Log.d(TAG, "Sceneview is null")
                            return
                        }
                        val nodeNames = call.argument<List<String>>("nodeName") ?: emptyList<String>()
                        val startPoint = call.argument<Map<String, Double>>("startPoint")
                        val endPoint = call.argument<Map<String, Double>>("endPoint")
                        val translation = call.argument<Map<String, Double>>("translation")

                        Log.d(TAG, "[onUpdateNode] Updating node(s): $nodeNames")

                        for (name in nodeNames) {
                            val node = NodeUtils.findNodeByName(name,sceneView!!) as ModelNode?
                            if (node == null) {
                                Log.e(TAG, "Node with name '$name' not found")
                                continue
                            }
                            Log.d("BoundingBoxSize", node.boundingBox.toVector3Box().size.toString())

                            Transform.animateNodePositionFromStartToEnd(node,KeypointUtils.mapToPosition(startPoint), KeypointUtils.mapToPosition(endPoint))
                        }


                        result.success(null)
                    }

                    else -> result.notImplemented()
                }
            }
        }

