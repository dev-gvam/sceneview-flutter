package io.github.sceneview.sceneview_flutter

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Plane
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
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

class SceneViewWrapper(
    context: Context,
    private val activity: Activity,
    private val lifecycle: Lifecycle,
    messenger: BinaryMessenger,
    id: Int,
) : PlatformView, MethodCallHandler {

    private val TAG = "SceneViewWrapper"

    private var sceneView: ARSceneView? = null
    private val _mainScope = CoroutineScope(Dispatchers.Main + Job())
    private val _methodChannel = MethodChannel(messenger, "sceneview_methods-$id")
    private val _eventChannel = EventChannel(messenger, "sceneview_events-$id")
    private var eventSink: EventChannel.EventSink? = null
    private val container: FrameLayout = FrameLayout(context)

    @Volatile
    private var isDisposed = false

    @Volatile
    private var isSessionReady = false

    init {
        Log.i(TAG, "Initializing SceneView")
        initializeSceneView(context)
        initializeChannels()
        container.addView(sceneView)
    }

    private fun initializeSceneView(context: Context) {
        sceneView = ARSceneView(
            context,
            sharedLifecycle = lifecycle,
            sessionConfiguration = ::configureSession,
            onSessionCreated = {
                Log.i(TAG, "Session created")
                isSessionReady = true
            },
            onSessionResumed = {
                Log.i(TAG, "Session resumed")
                isSessionReady = true
                sendEvent("onSessionResumed", true)
            },
            onSessionFailed = { exception ->
                Log.e(TAG, "Session failed: $exception")
                isSessionReady = false
            }
        ).apply {
            keepScreenOn = true
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
    }

    private fun configureSession(session: Session, config: Config) {
        config.apply {
            focusMode = Config.FocusMode.AUTO
            updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
            lightEstimationMode = Config.LightEstimationMode.DISABLED

            depthMode = Config.DepthMode.DISABLED
            semanticMode = Config.SemanticMode.DISABLED
            geospatialMode = Config.GeospatialMode.DISABLED
            cloudAnchorMode = Config.CloudAnchorMode.DISABLED
            augmentedFaceMode = Config.AugmentedFaceMode.DISABLED
            imageStabilizationMode = Config.ImageStabilizationMode.OFF
            instantPlacementMode = Config.InstantPlacementMode.DISABLED
            streetscapeGeometryMode = Config.StreetscapeGeometryMode.DISABLED
        }
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

    override fun dispose() {
        if (isDisposed) {
            Log.w(TAG, "Already disposed")
            return
        }

        Log.i(TAG, "Starting dispose")
        isDisposed = true

        try {

            // Limpiar event channel
            eventSink = null
            _methodChannel.setMethodCallHandler(null)

            // Solo limpiar sceneView si está inicializada y la sesión está lista
            sceneView?.let { sv ->
                try {
                    if (isSessionReady) {
                        sv.clearChildNodes()
                    }
                    sv.destroy()
                } catch (e: Exception) {
                    Log.e(TAG, "Error cleaning sceneView: ${e.message}")
                }
            }

            // Limpiar container
            container.removeAllViews()

        } catch (e: Exception) {
            Log.e(TAG, "Error during dispose: ${e.message}", e)
        } finally {
            isSessionReady = false
            Log.i(TAG, "Dispose completed")
        }
    }

    override fun getView(): View = container

    private suspend fun buildModelNode(flutterNode: FlutterSceneViewNode): ModelNode? {
        val modelLoader = sceneView?.modelLoader ?: run {
            Log.e(TAG, "ModelLoader not available")
            return null
        }

        val model = loadModel(flutterNode, modelLoader) ?: return null

        return ModelNode(
            modelInstance = model,
            scaleToUnits = flutterNode.scaleUnits
        ).apply {
            transform(
                position = flutterNode.position,
                rotation = flutterNode.rotation
            )

            // Configuración de interactividad
            isTouchable = true
            isEditable = true
            isSmoothTransformEnabled = true
            isPositionEditable = true
            isRotationEditable = true
            isScaleEditable = false

            // CRÍTICO: Deshabilitar sombras para reducir brillo
            isShadowCaster = false
            isShadowReceiver = false

            // Evento de toque
            onSingleTapConfirmed = { _ ->
                (flutterNode as? FlutterReferenceNode)?.id?.let { id ->
                    sendEvent("nodeTouched", id)
                }
                true
            }
        }
    }

    private suspend fun loadModel(
        flutterNode: FlutterSceneViewNode,
        modelLoader: ModelLoader
    ): ModelInstance? {
        if (flutterNode !is FlutterReferenceNode) return null

        return try {
            when (flutterNode.assetType) {
                AssetType.flutterAsset -> {
                    val assetKey = Utils.getFlutterAssetKey(activity, flutterNode.path)
                    Log.d(TAG, "Loading model from asset: $assetKey")
                    modelLoader.loadModelInstance(assetKey)
                }

                AssetType.documents -> {
                    val file = File(flutterNode.path)
                    if (!file.exists()) {
                        Log.e(TAG, "File not found: ${flutterNode.path}")
                        return null
                    }
                    Log.d(TAG, "Loading model from file: ${file.absolutePath}")
                    modelLoader.createModelInstance(file)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: $e", e)
            null
        }
    }

    private suspend fun renderModelAtGround(flutterNode: FlutterSceneViewNode): Boolean {
        if (isDisposed || !isSessionReady) {
            Log.w(TAG, "Cannot render: disposed=$isDisposed, sessionReady=$isSessionReady")
            return false
        }

        val sv = sceneView ?: return false
        val anchor = tryCreateGroundAnchor() ?: return false
        val modelNode = buildModelNode(flutterNode) ?: return false

        val anchorNode = AnchorNode(sv.engine, anchor)
        sv.addChildNode(anchorNode)
        anchorNode.addChildNode(modelNode)
        sv.planeRenderer.isVisible = false

        Log.d(TAG, "Model rendered successfully")
        return true
    }

    private fun tryCreateGroundAnchor(): Anchor? {
        val sv = sceneView ?: return null
        val frame = sv.session?.frame ?: return null
        val (cx, cy) = getScreenCenter(sv)

        return frame.hitTest(cx, cy).firstOrNull { hit ->
            (hit.trackable as? Plane)?.let { plane ->
                plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                        plane.isPoseInPolygon(hit.hitPose) &&
                        plane.trackingState == TrackingState.TRACKING &&
                        plane.subsumedBy == null
            } ?: false
        }?.createAnchor()
    }

    private fun getScreenCenter(view: View): Pair<Float, Float> =
        (view.width.coerceAtLeast(1) / 2f) to (view.height.coerceAtLeast(1) / 2f)

    private fun sendEvent(type: String, data: Any) {
        eventSink?.success(mapOf("type" to type, "data" to data))
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "init" -> result.success(true)

            "dispose" -> {
                _mainScope.launch {
                    dispose()
                    result.success(true)
                    _methodChannel.setMethodCallHandler(null)
                }
            }

            "addModel" -> {
                Log.i(TAG, "addModel")
                val flutterNode = FlutterSceneViewNode.from(call.arguments as Map<String, *>)
                _mainScope.launch {
                    val loaded = renderModelAtGround(flutterNode)
                    val event = mapOf("type" to "nodeLoaded", "data" to loaded)
                    eventSink?.success(event)
                    result.success(true)
                }
            }

            else -> result.notImplemented()
        }
    }
}
