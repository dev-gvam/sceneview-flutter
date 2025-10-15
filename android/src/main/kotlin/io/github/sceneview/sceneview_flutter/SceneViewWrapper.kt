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
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        }
        initializeChannels()
        disposed = false
        container.addView(sceneView)
    }

    override fun dispose() {
        if (disposed) return

        try {
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
        config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
        config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        config.textureUpdateMode = Config.TextureUpdateMode.BIND_TO_TEXTURE_EXTERNAL_OES

        config.depthMode = Config.DepthMode.DISABLED
        config.semanticMode = Config.SemanticMode.DISABLED
        config.geospatialMode = Config.GeospatialMode.DISABLED
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
        var id = ""
        when (flutterNode) {
            is FlutterReferenceNode -> {
                val filePath = Utils.getFlutterAssetKey(activity, flutterNode.path)
                id = flutterNode.id
                Log.d(TAG, filePath)
                model = sceneView?.modelLoader?.loadModelInstance(filePath)
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
                isScaleEditable = false
                onSingleTapConfirmed = { _ ->
                    val event = mapOf("type" to "nodeTouched", "data" to id)
                    eventSink?.success(event)
                    true
                }
            }
            return modelNode
        }
        return null
    }

    private fun getScreenCenterPx(view: View): Pair<Float, Float> {
        val w = view.width.coerceAtLeast(1)
        val h = view.height.coerceAtLeast(1)
        return w / 2f to h / 2f
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

    private suspend fun renderModelAtGround(flutterNode: FlutterSceneViewNode): Boolean {
        val anchor = tryCreateGroundAnchorAtScreenCenter() ?: return false
        val sv = sceneView ?: return false

        val anchorNode = AnchorNode(sceneView!!.engine, anchor)
        sv.addChildNode(anchorNode)

        val modelNode = buildNode(flutterNode) ?: return false

        anchorNode.addChildNode(modelNode)
        sv.planeRenderer.isVisible = false
        return true
    }

    private fun tryCreateGroundAnchorAtScreenCenter(): Anchor? {
        val sv = sceneView ?: return null
        val frame = sceneView?.session?.frame ?: return null
        val (cx, cy) = getScreenCenterPx(sv)

        val hits = frame.hitTest(cx, cy)
        for (hit in hits) {
            when (val trackable = hit.trackable) {
                is Plane -> {
                    val isHorizontalUp = trackable.type == Plane.Type.HORIZONTAL_UPWARD_FACING
                    if (isHorizontalUp &&
                        trackable.isPoseInPolygon(hit.hitPose) &&
                        trackable.trackingState == TrackingState.TRACKING &&
                        trackable.subsumedBy == null
                    ) {
                        return hit.createAnchor()
                    }
                }

                else -> Unit
            }
        }
        return null
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
