package io.github.sceneview.sceneview_flutter

import android.animation.ValueAnimator
import android.content.Context
import android.util.Log
import io.flutter.embedding.engine.loader.FlutterLoader
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode


class Utils {
    companion object{
        fun getFlutterAssetKey(context:Context, flutterAsset: String): String {
            Log.d("Utils", flutterAsset)
            val loader = FlutterLoader()
            loader.startInitialization(context)
            return loader.getLookupKeyForAsset(flutterAsset)
        }
        fun calculateScale(
            distance: Double,
            minDist: Double = 1.0,
            maxDist: Double = 8000.0,
            minScale: Float = 10f,
            maxScale: Float = 300f
        ): Float {
            val factor = ((distance - minDist) / (maxDist - minDist)).coerceIn(0.0, 1.0)
            return (minScale + factor * (maxScale - minScale)).toFloat()
        }
        fun rotateModelX(modelNode: ModelNode) {
            ValueAnimator.ofFloat(0f, 360f).apply {
                duration = 4000
                repeatCount = ValueAnimator.INFINITE
                interpolator = android.view.animation.LinearInterpolator()
                addUpdateListener { animation ->
                    val rotationY = animation.animatedValue as Float
                    modelNode.rotation = Rotation(0f, rotationY, 0f)
                }
                start()
            }
        }
    }
}