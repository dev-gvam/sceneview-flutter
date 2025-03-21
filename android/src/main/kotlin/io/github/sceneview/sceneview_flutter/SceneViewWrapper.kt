package io.github.sceneview.sceneview_flutter

import android.app.Activity
import android.content.Context
import android.location.Location
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import com.google.ar.core.Config
import com.google.ar.core.Earth
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.platform.PlatformView
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.scene.destroy
import io.github.sceneview.collision.Sphere
import io.github.sceneview.math.Size
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.ImageNode
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class SceneViewWrapper(
    context: Context,
    private val activity: Activity,
    private val lifecycle: Lifecycle,
    messenger: BinaryMessenger,
    id: Int,
) : PlatformView, MethodCallHandler {

    private val TAG = "SceneViewWrapper"
    private val methodChannelIdentifier = "sceneview_methods";
    private val eventChannelIdentifier = "sceneview_events";

    private var sceneView: ARSceneView? = null
    private val _mainScope = CoroutineScope(Dispatchers.Main)
    private val _methodChannel = MethodChannel(messenger, "$methodChannelIdentifier-$id")
    private val _eventChannel = EventChannel(messenger, "$eventChannelIdentifier-$id")
    private var eventSink: EventChannel.EventSink? = null

    private val container: FrameLayout = FrameLayout(context)
    private var disposed: Boolean = false

    init {
        Log.i(TAG, "init")
        sceneView = ARSceneView(
            context,
            sharedLifecycle = lifecycle,
            sessionConfiguration = ::configureSession,
            onSessionCreated = { session ->
                Log.i(TAG, "onSessionCreated")
            },
            onSessionResumed = { session ->
                Log.i(TAG, "onSessionResumed")
                val event = mapOf("type" to "onSessionResumed", "data" to true)
                eventSink?.success(event)
            },
            onSessionFailed = { exception ->
                Log.e(TAG, "onSessionFailed : $exception")
            },
            onTrackingFailureChanged = { reason ->
                Log.i(TAG, "onTrackingFailureChanged: $reason");
            }
        ).apply {
            keepScreenOn = true
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            cameraNode.far = 8000f
        }
        initializeChannels()
        disposed = false
        container.addView(sceneView)
    }

    override fun dispose() {
        if (disposed) return

        try {
            val earthAnchors = sceneView?.session?.earth?.anchors?.toList() ?: emptyList()
            earthAnchors.forEach { anchor ->
                try {
                    anchor.detach()
                    anchor.destroy()
                } catch (e: Exception) {
                    Log.e(TAG, "Error detaching anchor: ${e.message}")
                }
            }
            sceneView?.clearChildNodes()
            sceneView?.session?.close()
            sceneView?.destroy()
            container.removeAllViews()
            val emptyView = View(container.context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            container.addView(emptyView)
        } catch (e: Exception) {
            Log.e(TAG, "Error during dispose ${e.message}")
        } finally {
            sceneView = null
            disposed = true
            Log.i(TAG, "dispose")
        }
    }

    override fun getView(): View {
        return container
    }

    private fun configureSession(session: Session, config: Config) {
        config.focusMode = Config.FocusMode.AUTO
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        config.planeFindingMode = Config.PlaneFindingMode.DISABLED
        config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        config.textureUpdateMode = Config.TextureUpdateMode.BIND_TO_TEXTURE_EXTERNAL_OES

        config.depthMode = Config.DepthMode.DISABLED
        config.semanticMode = Config.SemanticMode.DISABLED
        config.geospatialMode = Config.GeospatialMode.ENABLED
        config.cloudAnchorMode = Config.CloudAnchorMode.DISABLED
        config.augmentedFaceMode = Config.AugmentedFaceMode.DISABLED
        config.imageStabilizationMode = Config.ImageStabilizationMode.OFF
        config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
        config.streetscapeGeometryMode = Config.StreetscapeGeometryMode.DISABLED

        Log.i(TAG, "Session Configured")
    }

    private fun initializeChannels() {
        _methodChannel.setMethodCallHandler(this)
        _eventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                eventSink = events
                Log.i(TAG, "Events initialized")
            }

            override fun onCancel(arguments: Any?) {
                eventSink = null
            }
        })
        Log.i(TAG, "Channels initialized")
    }

    private suspend fun addNode(flutterNode: FlutterSceneViewNode) {
        val node = buildNode(flutterNode) ?: return
        sceneView?.addChildNode(node)
        Log.d(TAG, "Model placed")
    }

    private suspend fun buildNode(flutterNode: FlutterSceneViewNode): ModelNode? {
        var model: ModelInstance? = null
        when (flutterNode) {
            is FlutterReferenceNode -> {
                val fileLocation = Utils.getFlutterAssetKey(activity, flutterNode.fileLocation)
                Log.d(TAG, fileLocation)
                model =
                    sceneView?.modelLoader?.loadModelInstance(fileLocation)
            }
        }
        if (model != null) {
            val modelNode = ModelNode(modelInstance = model, scaleToUnits = 1.0f).apply {
                transform(
                    position = flutterNode.position,
                    rotation = flutterNode.rotation,
                )
                isTouchable = true
                isEditable = true
                isSmoothTransformEnabled = true
                isShadowCaster = false
                isShadowReceiver = false
                isPositionEditable = true
                isRotationEditable = true
            }
            return modelNode
        }
        return null
    }


    private suspend fun loadPositions(loader: GeoPositionLoader) {
        var session: Session? = null
        var earth: Earth? = null

        while (sceneView == null || sceneView?.session.also {
                session = it
            } == null || session?.earth.also { earth = it } == null) {
            Log.i(TAG, "Esperando inicialización de sceneView, session y earth...")
            delay(100)
        }

        while (earth?.trackingState != TrackingState.TRACKING) {
            Log.i(TAG, "Esperando TRACKING...")
            delay(100)
        }
        val pose = earth!!.cameraGeospatialPose

        Log.i(
            TAG,
            "POSE ${pose.orientationYawAccuracy} -- ${pose.horizontalAccuracy} -- ${pose.verticalAccuracy}"
        )
        /*while (pose.horizontalAccuracy < 3.0f || pose.verticalAccuracy < 3.0f) {
            Log.i(TAG, "Esperando POSE...")
            Log.i(TAG, "POSE ${pose.horizontalAccuracy} -- ${pose.verticalAccuracy}")
            pose = earth!!.cameraGeospatialPose
            delay(100)
        }*/

        for (position in loader.positions) {
            val earthAnchorNode = createEarthAnchorNode(position)
            if (earthAnchorNode != null) {
                val cameraPose = sceneView?.session?.earth?.cameraGeospatialPose
                if (cameraPose != null) {
                    val results = FloatArray(1) { 1.0f }
                    Location.distanceBetween(
                        position.latitude,
                        position.longitude,
                        cameraPose.latitude,
                        cameraPose.longitude,
                        results
                    )
                    val scale = Utils.calculateScale(results[0].toDouble())
                    /* val scale = Utils.calculateScale(
                        results[0].toDouble(),
                        minScale = 0.1f,
                        maxScale = 3.0f
                    )*/
                    val modelNode =
                        createModelNodeFromFlutterAsset(
                            position.id,
                            loader.getModelPathByType(position.type),
                            scale
                        )
                    // val modelNode = createImageNodeFromFlutterAsset(position.id, loader.modelFilePath, scale)
                    if (modelNode != null) {
                        Utils.rotateModelX(modelNode)
                        earthAnchorNode.addChildNode(modelNode).apply {
                            name = position.type
                        }
                        sceneView?.addChildNode(earthAnchorNode)
                    }
                }
            }
        }
        Log.i(TAG, "NODES END -> ${sceneView?.childNodes?.size}")
    }

    private fun createEarthAnchorNode(geoPose: GeoPosition): AnchorNode? {
        val anchor = sceneView?.session?.earth?.createAnchor(
            geoPose.latitude,
            geoPose.longitude,
            geoPose.altitude,
            0f, 0f, 0f, 1f
        )
        if (anchor != null) {
            return AnchorNode(sceneView!!.engine, anchor = anchor)
        }
        return null
    }

    private suspend fun createModelNodeFromFlutterAsset(
        id: String,
        assetFilePath: String,
        scale: Float = 1.0f
    ): ModelNode? {
        if (assetFilePath.isEmpty()) return null
        val flutterAsset = Utils.getFlutterAssetKey(activity, assetFilePath)
        val model: ModelInstance? = sceneView?.modelLoader?.loadModelInstance(flutterAsset)

        return model?.let {
            ModelNode(modelInstance = model, scaleToUnits = scale).apply {
                onSingleTapConfirmed = { _ ->
                    val event = mapOf("type" to "nodeTouched", "data" to id)
                    eventSink?.success(event)
                    true
                }
            }
        }
    }

    private suspend fun createImageNodeFromFlutterAsset(
        id: String,
        assetFilePath: String,
        scale: Float = 1.0f
    ): ImageNode? {
        val bitmap = Utils.getBitmapFromFlutterAsset(activity, assetFilePath)

        return if (bitmap != null) {
            val materialLoader = sceneView?.materialLoader ?: return null
            val imageWidth = scale * bitmap.width.toFloat()
            val imageHeight = scale * bitmap.height.toFloat()

            val diagonal = sqrt((imageWidth * imageWidth) + (imageHeight * imageHeight))
            val sphereRadius = (diagonal / 2) * 1.1f
            val imageNode = ImageNode(
                materialLoader = materialLoader,
                bitmap = bitmap,
                size = Size(imageWidth, imageHeight)
            ).apply {
                collisionShape = Sphere(sphereRadius)
                isTouchable = true
                onSingleTapConfirmed = { _ ->
                    val event = mapOf("type" to "nodeTouched", "data" to id)
                    eventSink?.success(event)
                    true
                }
            }
            imageNode
        } else {
            Log.e(TAG, "Can't load asset: $assetFilePath")
            null
        }
    }

    private fun filterPositions(filters: List<String>) {
        sceneView?.childNodes?.forEach { node ->
            if (filters.isEmpty() || filters.contains(node.name)) {
                node.isVisible = true
                node.isTouchable = true
            } else {
                node.isVisible = false
                node.isTouchable = false
            }
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "init" -> {
                result.success(true)
            }

            "dispose" -> {
                _mainScope.launch {
                    dispose()
                    result.success(true)
                    _methodChannel.setMethodCallHandler(null)
                }
            }

            "addNode" -> {
                Log.i(TAG, "addNode")
                val flutterNode = FlutterSceneViewNode.from(call.arguments as Map<String, *>)
                _mainScope.launch {
                    addNode(flutterNode)
                    result.success(true)
                }
            }

            "loadPositions" -> {
                val positionLoader = GeoPositionLoader.fromJson(call.arguments as Map<String, *>)
                _mainScope.launch {
                    loadPositions(positionLoader)
                }
                result.success(null)
            }

            "filterPositions" -> {
                val filterType = (call.arguments as List<String>)
                _mainScope.launch {
                    filterPositions(filterType)
                }
                result.success(null)
            }

            else -> result.notImplemented()
        }
    }
}
