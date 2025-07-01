package io.github.sceneview.sceneview_flutter

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import io.flutter.plugin.common.EventChannel

class SceneViewFactory(
    private val activity: Activity,
    private val messenger: BinaryMessenger,
    private val lifecycle: Lifecycle,
    private val lifecycleCoroutineScope: LifecycleCoroutineScope,
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        Log.d("Factory", "Creating new view instance")
        val view= SceneViewWrapper(context, activity, lifecycle, lifecycleCoroutineScope,messenger, viewId)

        val eventChannel = EventChannel(messenger, "ar_image")
        val camersState = EventChannel(messenger, "plane_detected")

        eventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                Log.d("ArCoreViewFactory", "[$viewId] onListen")
                view.startBackgroundThread()
                view.InitalizeEventSink(eventSink = events)

            }

            override fun onCancel(arguments: Any?) {
                Log.d("ArCoreViewFactory", "[$viewId] onCancel")
                view.stopBackgroundThread()
            }
        })



        camersState.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                Log.d("ArCoreViewFactory", "[$viewId] onListen")
                view.startBackgroundThread()
                view.InitalizeEventSink(planeDetectedSink = events)
            }

            override fun onCancel(arguments: Any?) {
                Log.d("ArCoreViewFactory", "[$viewId] onCancel")
                view.stopBackgroundThread()
            }
        })



        return  view

    }
}