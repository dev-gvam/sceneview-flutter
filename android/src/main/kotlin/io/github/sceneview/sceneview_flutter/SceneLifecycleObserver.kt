package io.github.sceneview.sceneview_flutter

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import io.github.sceneview.ar.ARSceneView

class SceneLifecycleObserver(private val sceneView: ARSceneView?) : LifecycleEventObserver {

    private val TAG = "ARSceneLifecycleObserver"

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                Log.i(TAG, "Resuming AR session")
                sceneView?.session?.resume()
            }
            Lifecycle.Event.ON_PAUSE -> {
                Log.i(TAG, "Pausing AR session")
                sceneView?.session?.pause()
            }
            Lifecycle.Event.ON_DESTROY -> {
                Log.i(TAG, "Destroying AR session")
                sceneView?.session?.close()
            }
            else -> {
                // No hacer nada para otros eventos
            }
        }

    }
}
