package io.github.sceneview.sceneview_flutter



import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

object ImageUtils {

    /**
     * Converts an Android [Image] in YUV_420_888 format to NV21 byte array.
     *
     * @param image Image in YUV_420_888 format.
     * @return Byte array in NV21 format.
     */
    fun yuv420ToNV21(image: Image): ByteArray {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 4

        val nv21 = ByteArray(ySize + uvSize * 2)

        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        yBuffer.get(nv21, 0, ySize)  // Copy Y plane directly

        val chromaRowStrideU = image.planes[1].rowStride
        val chromaPixelStrideU = image.planes[1].pixelStride
        val chromaRowStrideV = image.planes[2].rowStride
        val chromaPixelStrideV = image.planes[2].pixelStride

        var position = ySize

        for (row in 0 until height / 2) {
            val uRowStart = row * chromaRowStrideU
            val vRowStart = row * chromaRowStrideV
            for (col in 0 until width / 2) {
                val uIndex = uRowStart + col * chromaPixelStrideU
                val vIndex = vRowStart + col * chromaPixelStrideV

                nv21[position++] = vBuffer[vIndex]
                nv21[position++] = uBuffer[uIndex]
            }
        }

        return nv21
    }


     fun nv21ToJpeg(
        nv21: ByteArray,
        imageWidth: Int,
        imageHeight: Int,
        targetAspectWidth: Int,
        targetAspectHeight: Int,
        quality: Int
    ): ByteArray? {
        return try {
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageWidth, imageHeight, null)

            val imageAspect = imageWidth.toFloat() / imageHeight
            val targetAspect = targetAspectWidth.toFloat() / targetAspectHeight

            // Determine cropped width & height to match target aspect ratio
            val cropWidth: Int
            val cropHeight: Int

            if (imageAspect > targetAspect) {
                // Image is too wide → crop width
                cropHeight = imageHeight
                cropWidth = (cropHeight * targetAspect).toInt()
            } else {
                // Image is too tall → crop height
                cropWidth = imageWidth
                cropHeight = (cropWidth / targetAspect).toInt()
            }

            // Center crop
            val cropLeft = (imageWidth - cropWidth)
            val cropTop = (imageHeight - cropHeight)

            val cropRect = Rect(
                cropLeft,
                cropTop,
                cropLeft + cropWidth,
                cropTop + cropHeight
            )

            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(cropRect, quality, out)
            out.toByteArray()

        } catch (e: Exception) {
            Log.e("ArCoreView", "JPEG compression error: ${e.message}")
            null
        }
    }

    fun rotateNV21(yuv: ByteArray, width: Int, height: Int, rotation: Int): ByteArray {
        require(rotation in listOf(0, 90, 180, 270)) { "Rotation must be 0, 90, 180, or 270" }
        if (rotation == 0) return yuv

        val output = ByteArray(yuv.size)
        val frameSize = width * height
        val swap = rotation % 180 != 0
        val xflip = rotation % 270 != 0
        val yflip = rotation >= 180

        val wOut = if (swap) height else width
        val hOut = if (swap) width else height

        for (j in 0 until height) {
            for (i in 0 until width) {
                val yIn = j * width + i
                val uIn = frameSize + (j shr 1) * width + (i and -2)
                val vIn = uIn + 1

                val iSwapped = if (swap) j else i
                val jSwapped = if (swap) i else j
                val iOut = if (xflip) wOut - iSwapped - 1 else iSwapped
                val jOut = if (yflip) hOut - jSwapped - 1 else jSwapped

                val yOut = jOut * wOut + iOut
                val uOut = frameSize + (jOut shr 1) * wOut + (iOut and -2)
                val vOut = uOut + 1

                output[yOut] = yuv[yIn]
                output[uOut] = yuv[uIn]
                output[vOut] = yuv[vIn]
            }
        }
        return output
    }


    fun cropCpuImageToMatchArViewAspect(
        nv21: ByteArray,
        imageWidth: Int,
        imageHeight: Int,
        viewWidth: Int,
        viewHeight: Int,
        quality: Int = 95
    ): ByteArray? {
        return try {
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageWidth, imageHeight, null)

            // Compute target aspect ratio from AR view
            val viewAspect = viewWidth.toFloat() / viewHeight
            val imageAspect = imageWidth.toFloat() / imageHeight

            val cropWidth: Int
            val cropHeight: Int

            if (imageAspect > viewAspect) {
                // Too wide → crop width (zoom horizontally)
                cropHeight = imageHeight
                cropWidth = (cropHeight * viewAspect).toInt()
            } else {
                // Too tall → crop height (zoom vertically)
                cropWidth = imageWidth
                cropHeight = (cropWidth / viewAspect).toInt()
            }

            // Center the crop
            val cropLeft = (imageWidth - cropWidth) / 2
            val cropTop = (imageHeight - cropHeight) / 2

            val cropRect = Rect(cropLeft, cropTop, cropLeft + cropWidth, cropTop + cropHeight)

            // Compress to JPEG the cropped region
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(cropRect, quality, out)
            out.toByteArray()

        } catch (e: Exception) {
            Log.e("ArCoreView", "JPEG compression error: ${e.message}")
            null
        }
    }

}
