package io.github.sceneview.sceneview_flutter

import android.content.Context
import android.util.Log
import io.flutter.embedding.engine.loader.FlutterLoader


class Utils {
    companion object{
        fun getFlutterAssetKey(context:Context, flutterAsset: String): String {
            Log.d("Utils", flutterAsset)
            val loader = FlutterLoader()
            loader.startInitialization(context)
            return loader.getLookupKeyForAsset(flutterAsset)
        }
    }
}