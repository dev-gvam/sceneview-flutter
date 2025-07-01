package io.github.sceneview.sceneview_flutter

import io.github.sceneview.arsceneview.ARSceneView
import io.github.sceneview.node.Node

object NodeUtils {
     fun findNodeByName(name: String,sceneView: ARSceneView): Node? {
        return sceneView.childNodes.firstOrNull { it.name == name }
    }
}