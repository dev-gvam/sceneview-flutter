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
import io.github.sceneview.collision.Sphere
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SceneViewWrapper(
    context: Context,
    private val activity: Activity,
    private val lifecycle: Lifecycle,
    messenger: BinaryMessenger,
    id: Int,
) : PlatformView, MethodCallHandler {

    private val TAG = "SceneViewWrapper"
    private val _mainScope = CoroutineScope(Dispatchers.Main)
    private val _methodChannel = MethodChannel(messenger, "sceneview_method_$id")
    private val _eventChannel = EventChannel(messenger, "sceneview_event_$id")
    private var eventSink: EventChannel.EventSink? = null

    private val containerView: FrameLayout = FrameLayout(context)

    private var sceneView: ARSceneView? = null

    init {
        Log.i(TAG, "Initializing SceneViewWrapper")
        _methodChannel.setMethodCallHandler(this)
        _eventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                eventSink = events
            }
            override fun onCancel(arguments: Any?) {
                eventSink = null
            }
        })
        Log.i(TAG, "Initializing SceneViewWrapper")

        containerView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                Log.i(TAG, "containerView attached, delaying initialization")
                containerView.postDelayed({
                    initializeSceneView(context)
                }, 200)
                containerView.removeOnAttachStateChangeListener(this)
            }
            override fun onViewDetachedFromWindow(v: View) {}
        })
    }

    private fun initializeSceneView(context: Context) {
        Log.i(TAG, "Initializing ARSceneView")
        sceneView = ARSceneView(
            context,
            sharedLifecycle = lifecycle,
            sessionConfiguration = ::configureSession,
            onSessionCreated = { session ->
                Log.i(TAG, "onSessionCreated ${session.hashCode()}")
                val event = mapOf("type" to "onSessionCreated", "data" to true)
                eventSink?.success(event)
            },
            onSessionResumed = { session ->
                Log.i(TAG, "onSessionResumed")
            },
            onSessionFailed = { exception ->
                Log.e(TAG, "onSessionFailed: $exception")
            },
            onTrackingFailureChanged = { reason ->
                Log.i(TAG, "onTrackingFailureChanged: $reason")
            }
        ).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            keepScreenOn = true

            holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    Log.i(TAG, "ARSceneView surfaceCreated")
                }
                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                    Log.i(TAG, "ARSceneView surfaceChanged: $width x $height")
                }
                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    Log.i(TAG, "ARSceneView surfaceDestroyed")
                }
            })
        }

        sceneView.let {
            sceneView!!.cameraNode.far = 8000f
            Log.i(TAG, "Set cameraNode to 8000")
        }

        containerView.post {
            containerView.addView(sceneView)
            lifecycle.addObserver(SceneLifecycleObserver(sceneView))
        }
    }

    override fun getView(): View {
        return containerView
    }

    override fun dispose() {
        sceneView?.let {
            Log.i(TAG, "Destroying ARSceneView")
            it.destroy()
        }
        containerView.removeAllViews()
        sceneView = null
        _methodChannel.setMethodCallHandler(null)
        Log.i(TAG, "SceneView disposed")
    }

    private fun configureSession(session: Session, config: Config) {
        config.focusMode = Config.FocusMode.AUTO
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        config.planeFindingMode = Config.PlaneFindingMode.DISABLED
        config.lightEstimationMode = Config.LightEstimationMode.DISABLED
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

    private suspend fun addNode(flutterNode: FlutterSceneViewNode) {
        /*val node = buildNode(flutterNode) ?: return
        sceneView?.addChildNode(node)*/
        Log.d(TAG, "Node added to scene")
    }

    private suspend fun loadPositions(loader: GeoPositionLoader) {
        var session: Session? = null
        var earth: Earth? = null

        while (sceneView == null || sceneView?.session.also { session = it } == null || session?.earth.also { earth = it } == null) {
            Log.i(TAG, "Esperando inicialización de sceneView, session y earth...")
            delay(100)
        }

        while (earth?.trackingState != TrackingState.TRACKING) {
            Log.i(TAG, "Esperando TRACKING...")
            delay(100)
        }
        Log.i(TAG, "NODES START -> ${sceneView?.childNodes?.size}")
        for (position in loader.positions) {
            val earthAnchorNode = createEarthAnchorNode(position)
            if(earthAnchorNode != null) {
                val cameraPose = sceneView?.session?.earth?.cameraGeospatialPose
                if(cameraPose != null) {
                    val results = FloatArray(1) { 1.0f }
                    Location.distanceBetween(
                        position.latitude,
                        position.longitude,
                        cameraPose.latitude,
                        cameraPose.longitude,
                        results
                    )
                    val scale = Utils.calculateScale(results[0].toDouble())
                    val modelNode = createModelNodeFromFlutterAsset(position.id, loader.modelFilePath, scale)
                    Log.i(TAG, "Scale $scale")
                    if(modelNode != null) {
                        Utils.rotateModelX(modelNode)
                        earthAnchorNode.addChildNode(modelNode).apply {
                            name = position.type
                        }
                        sceneView?.addChildNode(earthAnchorNode)
                        Log.i(TAG, "Anchor created ${position.id}")
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

    private suspend fun createModelNodeFromFlutterAsset(id: String, assetFilePath: String, scale: Float = 1.0f): ModelNode? {
        val flutterAsset = Utils.getFlutterAssetKey(activity, assetFilePath)
        val model: ModelInstance? = sceneView?.modelLoader?.loadModelInstance(flutterAsset)

        return model?.let {
            ModelNode(modelInstance = model, scaleToUnits = scale).apply {
                collisionShape = Sphere(scale / 8f)
                isTouchable = true
                onSingleTapConfirmed = {
                        _ ->
                    val event = mapOf("type" to "nodeTouched", "data" to id)
                    eventSink?.success(event)
                    true
                }
            }
        }
    }

    private fun filterPositions(filters: List<String>) {
        sceneView?.childNodes?.forEach {
            node ->
            node.isVisible = filters.isEmpty() || filters.contains(node.name)
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "init" -> result.success(null)
            "dispose" -> {
                Log.i(TAG, "dispose called from Flutter")
                _mainScope.launch {
                    dispose()
                }
                result.success(null)
            }
            "addNode" -> {
                Log.i(TAG, "addNode")
                val flutterNode = FlutterSceneViewNode.from(call.arguments as Map<String, *>)
                _mainScope.launch {
                    addNode(flutterNode)
                }
                result.success(null)
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
