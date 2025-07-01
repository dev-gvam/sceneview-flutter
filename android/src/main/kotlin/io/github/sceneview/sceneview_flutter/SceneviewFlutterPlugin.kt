package io.github.sceneview.sceneview_flutter

import android.app.Activity
import android.util.Log
import androidx.annotation.NonNull
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel

/** SceneviewFlutterPlugin */
class SceneviewFlutterPlugin : FlutterPlugin, ActivityAware {
    private var isAttached = false
    private val TAG = "SceneviewFlutterPlugin"
    private lateinit var eventChannel: EventChannel
    private lateinit var cameraStateEventChannel: EventChannel
    private var eventSink: EventChannel.EventSink? = null
    private  val IMAGE_CHANNEL = "ar_image"
    private  val CAMERA_STATE_CHANNEL = "plane_detected"
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private var flutterPluginBinding: FlutterPlugin.FlutterPluginBinding? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        Log.i(TAG, "onAttachedToEngine")

        Log.d(TAG, "✅ onAttachedToEngine")
        if (isAttached) return
        isAttached = true

        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger,
            IMAGE_CHANNEL
        )

        cameraStateEventChannel=EventChannel(flutterPluginBinding.binaryMessenger,
            CAMERA_STATE_CHANNEL
        )

        this.flutterPluginBinding = flutterPluginBinding
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        Log.i(TAG, "onDetachedFromEngine")
        this.flutterPluginBinding = null
        eventSink = null
        flutterPluginBinding = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        Log.i(TAG, "onAttachedToActivity")
        val activity: Activity = binding.activity
        if (activity is LifecycleOwner) {
            Log.i(TAG, "activity is LifecycleOwner")
            flutterPluginBinding?.platformViewRegistry?.registerViewFactory(
                "SceneView",
                SceneViewFactory(
                    binding.activity,

                    flutterPluginBinding!!.binaryMessenger,


                    activity.lifecycle,
                    activity.lifecycleScope,
                )
            )
        }
    }

    override fun onDetachedFromActivityForConfigChanges() {
        Log.i(TAG, "onDetachedFromActivityForConfigChanges")
        onDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        Log.i(TAG, "onReattachedToActivityForConfigChanges")
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        Log.i(TAG, "onDetachedFromActivity")
    }
}
