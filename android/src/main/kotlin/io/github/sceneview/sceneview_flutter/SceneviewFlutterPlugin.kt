package io.github.sceneview.sceneview_flutter

import android.app.Activity
import android.util.Log
import androidx.annotation.NonNull
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.ArCoreApk
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler


/** SceneviewFlutterPlugin */
class SceneviewFlutterPlugin : FlutterPlugin, ActivityAware, MethodCallHandler {

    private val TAG = "SceneviewFlutterPlugin"

    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity

    private lateinit var channel: MethodChannel
    private lateinit var activity: Activity
    private var flutterPluginBinding: FlutterPlugin.FlutterPluginBinding? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        Log.i(TAG, "onAttachedToEngine")
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "sceneview_global")
        channel.setMethodCallHandler(this)
        this.flutterPluginBinding = flutterPluginBinding
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        Log.i(TAG, "onDetachedFromEngine")
        channel.setMethodCallHandler(null)
        this.flutterPluginBinding = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        Log.i(TAG, "onAttachedToActivity")
        activity = binding.activity
        if (activity is LifecycleOwner) {
            Log.i(TAG, "activity is LifecycleOwner")
            flutterPluginBinding?.platformViewRegistry?.registerViewFactory(
                "SceneView",
                SceneViewFactory(
                    binding.activity,
                    flutterPluginBinding!!.binaryMessenger,
                    (activity as LifecycleOwner).lifecycle,
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

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "isAvailable" -> {
                val ctx = activity.applicationContext ?: return result.error(
                    "NO_CONTEXT",
                    "No context",
                    null
                )
                val availability = ArCoreApk.getInstance().checkAvailability(ctx)

                if (availability == ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED || availability == ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD) {
                    ArCoreApk.getInstance().requestInstall(activity, true)
                } else if (availability != ArCoreApk.Availability.SUPPORTED_INSTALLED) {
                    return result.error(
                        "NO_COMPATIBLE",
                        "Device not compatible",
                        null
                    )
                }
                return result.success(true)
            }

            else -> result.notImplemented()
        }
    }
}
