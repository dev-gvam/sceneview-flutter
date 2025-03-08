package io.github.sceneview.sceneview_flutter

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.FatalException
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.platform.PlatformView
import io.github.sceneview.ar.ARSceneView
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
    private val _mainScope = CoroutineScope(Dispatchers.Main)
    private val _methodChannel = MethodChannel(messenger, "sceneview_method_$id")
    private val _eventChannel = EventChannel(messenger, "sceneview_event_$id")
    private var eventSink: EventChannel.EventSink? = null

    private val containerView: FrameLayout = FrameLayout(context)

    private var sceneView: ARSceneView? = null
    private var isSessionReady = false

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
                }, 50)
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
                isSessionReady = true
                eventSink?.success(mapOf("sessionCreated" to true))
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

        containerView.post {
            containerView.addView(sceneView)
            lifecycle.addObserver(SceneLifecycleObserver(sceneView))
        }
    }

    override fun getView(): View {
        Log.i(TAG, "getView() called")
        return containerView
    }

    override fun dispose() {
        _mainScope.launch(Dispatchers.Main) {
            sceneView?.let {
                Log.i(TAG, "Destroying ARSceneView")
                it.destroy()
            }
            containerView.removeAllViews()
            sceneView = null
            isSessionReady = false
            _methodChannel.setMethodCallHandler(null)
            Log.i(TAG, "SceneView disposed")
        }
    }

    private fun configureSession(session: Session, config: Config) {
        config.focusMode = Config.FocusMode.AUTO
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        config.planeFindingMode = Config.PlaneFindingMode.DISABLED
        config.lightEstimationMode = Config.LightEstimationMode.DISABLED
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

    private suspend fun addNode(flutterNode: FlutterSceneViewNode) {
        val node = buildNode(flutterNode) ?: return
        sceneView?.addChildNode(node)
        Log.d(TAG, "Node added to scene")
    }

    private suspend fun buildNode(flutterNode: FlutterSceneViewNode): ModelNode? {
        val model: ModelInstance? = when (flutterNode) {
            is FlutterReferenceNode -> {
                val fileLocation = Utils.getFlutterAssetKey(activity, flutterNode.fileLocation)
                Log.d(TAG, fileLocation)
                sceneView?.modelLoader?.loadModelInstance(fileLocation)
            }
            else -> null
        }
        return model?.let {
            ModelNode(modelInstance = model, scaleToUnits = 1.0f).apply {
                transform(
                    position = flutterNode.position,
                    rotation = flutterNode.rotation
                )
            }
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "init" -> result.success(null)
            "dispose" -> {
                Log.i(TAG, "dispose called from Flutter")
                dispose()
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
            else -> result.notImplemented()
        }
    }
}
