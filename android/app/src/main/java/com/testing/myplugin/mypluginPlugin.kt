// package com.testing.myplugin

// import com.mrousavy.camera.frameprocessors.Frame
// import com.mrousavy.camera.frameprocessors.FrameProcessorPlugin
// import com.mrousavy.camera.frameprocessors.VisionCameraProxy

// class mypluginPlugin(proxy: VisionCameraProxy, options: Map<String, Any>?): FrameProcessorPlugin() {
//   override fun callback(frame: Frame, arguments: Map<String, Any>?): Any? {
//     // code goes here
//     return "dello"
//   }
// }


package com.testing.myplugin

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import com.mrousavy.camera.frameprocessors.Frame
import com.mrousavy.camera.frameprocessors.FrameProcessorPlugin
import com.mrousavy.camera.frameprocessors.VisionCameraProxy
import com.testing.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

class mypluginPlugin(proxy: VisionCameraProxy, options: Map<String, Any>?) : FrameProcessorPlugin() {

    private val labels: List<String> = FileUtil.loadLabels(proxy.context, "labels.txt")
    private val model: SsdMobilenetV11Metadata1 = SsdMobilenetV11Metadata1.newInstance(proxy.context)
    private val imageProcessor: ImageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR))
        .build()

    override fun callback(frame: Frame, arguments: Map<String, Any>?): Any? {
        // Convert Frame to Bitmap
        val bitmap = frame.toBitmap()

        // Convert Bitmap to TensorImage
        var tensorImage = TensorImage.fromBitmap(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // Process image using the model
        val outputs = model.process(tensorImage)
        val locations = outputs.locationsAsTensorBuffer.floatArray
        val classes = outputs.classesAsTensorBuffer.floatArray
        val scores = outputs.scoresAsTensorBuffer.floatArray

        // Prepare the result array
        val results = mutableListOf<Map<String, Any>>()

        val h = bitmap.height
        val w = bitmap.width

        scores.forEachIndexed { index: Int, score: Float ->
            if (score > 0.5) {
                val x = index * 4
                val boundingBox = mapOf(
                    "left" to (locations[x + 1] * w).toDouble(),
                    "top" to (locations[x] * h).toDouble(),
                    "right" to (locations[x + 3] * w).toDouble(),
                    "bottom" to (locations[x + 2] * h).toDouble()
                )
                val result = mapOf(
                    "boundingBox" to boundingBox,
                    "label" to labels[classes[index].toInt()],
                    "confidence" to score.toDouble()
                )
                results.add(result)
            }
        }

        return results
    }

    // Extension function to convert Frame to Bitmap
    private fun Frame.toBitmap(): Bitmap {
        val image = this.image ?: throw IllegalArgumentException("Frame does not contain an image")
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val vuBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val vuSize = vuBuffer.remaining()

        val nv21 = ByteArray(ySize + vuSize)
        yBuffer.get(nv21, 0, ySize)
        vuBuffer.get(nv21, ySize, vuSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)

        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}





// package com.testing.myplugin

// import android.graphics.*
// import android.util.Base64  // Import for Base64 encoding
// import java.io.ByteArrayOutputStream
// import com.mrousavy.camera.frameprocessors.Frame
// import com.mrousavy.camera.frameprocessors.FrameProcessorPlugin
// import com.mrousavy.camera.frameprocessors.VisionCameraProxy
// import com.testing.ml.SsdMobilenetV11Metadata1
// import org.tensorflow.lite.support.common.FileUtil
// import org.tensorflow.lite.support.image.ImageProcessor
// import org.tensorflow.lite.support.image.TensorImage
// import org.tensorflow.lite.support.image.ops.ResizeOp

// class mypluginPlugin(proxy: VisionCameraProxy, options: Map<String, Any>?) : FrameProcessorPlugin() {

//     private val labels: List<String> = FileUtil.loadLabels(proxy.context, "labels.txt")
//     private val model: SsdMobilenetV11Metadata1 = SsdMobilenetV11Metadata1.newInstance(proxy.context)
//     private val imageProcessor: ImageProcessor = ImageProcessor.Builder()
//         .add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR))
//         .build()

//     override fun callback(frame: Frame, arguments: Map<String, Any>?): Any? {
//         // Convert Frame to Bitmap
//         val bitmap = frame.toBitmap()

//         // Create a mutable copy of the bitmap
//         val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
//         val canvas = Canvas(mutableBitmap)
//         val paint = Paint().apply {
//             color = Color.GREEN
//             strokeWidth = 8f
//             style = Paint.Style.STROKE
//         }

//         // Convert Bitmap to TensorImage
//         var tensorImage = TensorImage.fromBitmap(bitmap)
//         tensorImage = imageProcessor.process(tensorImage)

//         // Process image using the model
//         val outputs = model.process(tensorImage)
//         val locations = outputs.locationsAsTensorBuffer.floatArray
//         val classes = outputs.classesAsTensorBuffer.floatArray
//         val scores = outputs.scoresAsTensorBuffer.floatArray

//         val h = mutableBitmap.height
//         val w = mutableBitmap.width

//         // Draw bounding boxes on the mutable Bitmap
//         scores.forEachIndexed { index: Int, score: Float ->
//             if (score > 0.5) {
//                 val x = index * 4
//                 val left = (locations[x + 1] * w)
//                 val top = (locations[x] * h)
//                 val right = (locations[x + 3] * w)
//                 val bottom = (locations[x + 2] * h)

//                 // Draw the bounding box
//                 canvas.drawRect(left, top, right, bottom, paint)
//             }
//         }

//         // Convert the mutable Bitmap back to a Base64 string
//         val outputStream = ByteArrayOutputStream()
//         mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
//         val byteArray = outputStream.toByteArray()
//         return Base64.encodeToString(byteArray, Base64.DEFAULT)
//     }

//     // Extension function to convert Frame to Bitmap
//     private fun Frame.toBitmap(): Bitmap {
//         val image = this.image ?: throw IllegalArgumentException("Frame does not contain an image")
//         val planes = image.planes
//         val yBuffer = planes[0].buffer
//         val vuBuffer = planes[2].buffer

//         val ySize = yBuffer.remaining()
//         val vuSize = vuBuffer.remaining()

//         val nv21 = ByteArray(ySize + vuSize)
//         yBuffer.get(nv21, 0, ySize)
//         vuBuffer.get(nv21, ySize, vuSize)

//         val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
//         val out = ByteArrayOutputStream()
//         yuvImage.compressToJpeg(android.graphics.Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)

//         val imageBytes = out.toByteArray()
//         return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
//     }
// }
