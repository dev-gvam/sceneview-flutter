package io.github.sceneview.sceneview_flutter

import android.media.Image
import android.util.Log
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotYetAvailableException
import io.github.sceneview.arsceneview.ARSceneView
import java.nio.ByteOrder
import kotlin.math.sqrt

object DepthUtils {

    fun performFullDepthHitTest(
        arSceneView: ARSceneView?,
        xNorm: Float,
        yNorm: Float,
        screenWidth: Float,
        screenHeigth: Float,
    ): Map<String, Any>? {

        if(arSceneView==null){
            return null
        }
        val frame = arSceneView.frame ?: return null
        val session = arSceneView.session ?: return null
        val keypoints =NormalizedPoint(xNorm,yNorm)
        val resizedKeyPoint= KeypointUtils.convertNormalizedKeypointFrom3x4To16x9(keypoints)



        Log.w("ARCore", "❌ Screen width and height($screenWidth, $screenHeigth)")
        if (xNorm !in 0.0f..1.0f || yNorm !in 0.0f..1.0f) {
            Log.w("ARCore", "❌ Normalized coordinates out of bounds: ($xNorm, $yNorm)")

            return null
        }
        logCameraCpuImageSize(frame)
        if (frame.camera.trackingState != TrackingState.TRACKING) {
            Log.d("ARCore", "⚠️ Camera not tracking")
            return null
        }

        val px = resizedKeyPoint.x * screenWidth
        val py = resizedKeyPoint.y * screenHeigth

        // 1. Try hitTest against Plane or Point
        val hits = frame.hitTest(px, py)
        for (hit in hits) {

            val trackable = hit.trackable
            if ((trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) ||
                (trackable is Point && trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)
            ) {
                val pose = hit.hitPose
                //  val cameraPose = frame.camera.displayOrientedPose
                val distance = hit.distance

                Log.d("ARCoreHit", "✅ Plane/Point hit at (${pose.tx()}, ${pose.ty()}, ${pose.tz()})")

                return mapOf(
                    "x" to pose.tx(),
                    "y" to pose.ty(),
                    "z" to pose.tz(),
                    "depth" to distance,
                    "source" to "hitTest"
                )
            }
        }

        // 2. Fallback: Use raw depth
        val depth = getDepthAtNormalizedPoint(frame, keypoints.x, keypoints.y)
        if (depth != null) {
            val intrinsics = frame.camera.imageIntrinsics
            val worldPoint = convertDepthToWorld(
                keypoints.x,
                keypoints.y,
                depthMeters = depth,
                frame
            )

            val cameraPose = frame.camera.displayOrientedPose
            val distance = calculateDistanceToCamera(
                cameraPose,
                worldPoint["x"]!!,
                worldPoint["y"]!!,
                worldPoint["z"]!!,
            )

            Log.d("ARCoreHit", "✅ Raw depth fallback world point: (80,45) at $distance meters")
            return mapOf(
                "x" to      worldPoint["x"]!!,
                "y" to    worldPoint["y"]!!,
                "z" to    worldPoint["z"]!!,
                "depth" to depth,
                "source" to "rawDepth"
            )

        }

        Log.d("ARCore", "❌ No hit or depth at point ($xNorm, $yNorm)")
        return null
    }
    fun calculateDistanceToCamera(cameraPose: Pose, worldX: Float, worldY: Float, worldZ: Float): Float {
        val camX = cameraPose.tx()
        val camY = cameraPose.ty()
        val camZ = cameraPose.tz()

        val dx = worldX - camX
        val dy = worldY - camY
        val dz = worldZ - camZ


        return sqrt(dx * dx + dy * dy + dz * dz)

    }
    fun convertDepthToWorld(
        xNorm224: Float, // normalized in [0, 1]
        yNorm224: Float,
        depthMeters: Float,
        frame: Frame
    ): Map<String, Float> {
        // Get current frame


        // Get camera intrinsics
        val intrinsics = frame.camera.imageIntrinsics
        val imageWidth = intrinsics.imageDimensions[0]  // typically 640
        val imageHeight = intrinsics.imageDimensions[1] // typically 480

        // Scale normalized 224x224 coordinates to full image resolution
        val u = xNorm224 * imageHeight
        val v = yNorm224 * imageWidth

        val fx = intrinsics.focalLength[0]
        val fy = intrinsics.focalLength[1]
        val cx = intrinsics.principalPoint[0]
        val cy = intrinsics.principalPoint[1]

        // Unproject depth pixel to camera space
        val xCam = (u - cx) * depthMeters / fx
        val yCam = (v - cy) * depthMeters / fy
        val zCam = depthMeters

        // Transform from camera to world space
        val worldCoords = frame.camera.displayOrientedPose.transformPoint(floatArrayOf(xCam, yCam, zCam))

        // Return in a Map<String, Float> format for Flutter
        return mapOf(
            "x" to worldCoords[0],
            "y" to worldCoords[1],
            "z" to worldCoords[2]
        )
    }


    fun getDepthAtNormalizedPoint(frame: Frame, xNorm: Float, yNorm: Float): Float? {
        val depthImage: Image = try {
            frame.acquireDepthImage16Bits()
        } catch (e: NotYetAvailableException) {
            return null
        }
        val newX = xNorm * (160f / 224f)
        val newY = yNorm * (90f / 224f)

        logCameraCpuImageSize(frame)

        val depthWidth = depthImage.width
        val depthHeight = depthImage.height
//


        val depthMm =getMillimetersDepth(depthImage,(newX*depthWidth).toInt(),(newY*depthHeight).toInt())
        depthImage.close()
        return if (depthMm.toInt() == 0) null else (depthMm.toFloat()) / 1000.0f // Convert mm to meters
    }

    fun getMillimetersDepth(depthImage: Image, x: Int, y: Int): UInt {
        // The depth image has a single plane, which stores depth for each
        // pixel as 16-bit unsigned integers.
        val plane = depthImage.planes[0]
        val byteIndex = x* plane.pixelStride + y * plane.rowStride
        val buffer = plane.buffer.order(ByteOrder.nativeOrder())
        val depthSample = buffer.getShort(byteIndex)
        return depthSample.toUInt()
    }


    fun logCameraCpuImageSize(frame: Frame) {
        try {
            val cameraImage = frame.acquireCameraImage()
            val width = cameraImage.width
            val height = cameraImage.height
            Log.d("ARCore", "📷 Camera CPU image size: $width x $height")
            cameraImage.close()

            val depthImage = try {
                frame.acquireDepthImage16Bits()
            } catch (e: NotYetAvailableException) {
                Log.w("ARCore", "❌ Depth image not available yet.")
                return
            }

            // Log depth image resolution
            Log.d("ARCore", "📐 Depth image size: ${depthImage.width} x ${depthImage.height}")

            // Get camera intrinsics
            val intrinsics = frame.camera.imageIntrinsics

            val focalLength = FloatArray(2)
            val principalPoint = FloatArray(2)
            val imageDimensions = IntArray(2)

            intrinsics.getFocalLength(focalLength, 0)
            intrinsics.getPrincipalPoint(principalPoint, 0)
            intrinsics.getImageDimensions(imageDimensions, 0)

            Log.d("ARCore", "🔍 Camera Intrinsics:")
            Log.d("ARCore", "   • Focal length: fx = ${focalLength[0]}, fy = ${focalLength[1]}")
            Log.d("ARCore", "   • Principal point: cx = ${principalPoint[0]}, cy = ${principalPoint[1]}")
            Log.d("ARCore", "   • Image dimensions: width = ${imageDimensions[0]}, height = ${imageDimensions[1]}")

            depthImage.close()
        } catch (e: NotYetAvailableException) {
            Log.w("ARCore", "⚠️ Camera image not yet available.")
        } catch (e: Exception) {
            Log.e("ARCore", "❌ Error acquiring camera image: ${e.message}")
        }
    }

}