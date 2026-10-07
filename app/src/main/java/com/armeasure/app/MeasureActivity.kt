package com.armeasure.app

import android.graphics.Bitmap
import android.graphics.Color
import android.media.Image
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.armeasure.app.databinding.ActivityMeasureBinding
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.gesture.GestureDetector
import io.github.sceneview.math.Position
import io.github.sceneview.node.Node
import kotlinx.coroutines.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.sqrt

/**
 * MeasureActivity — এটাই মূল AR Measurement screen।
 *
 * কিভাবে কাজ করে:
 * 1. ARCore camera থেকে real-world surface detect করে (plane detection)
 * 2. User screen-এ tap করলে সেই point-এ একটা anchor বসে
 * 3. দুইটা point select করলে তাদের মধ্যে real-world distance calculate হয়
 * 4. Edge detection mode ON করলে camera frame-এ OpenCV দিয়ে edge detect হয়
 * 5. Detected edges AR overlay হিসেবে screen-এ দেখায়
 */
class MeasureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMeasureBinding

    // AR Scene View (SceneView library handles ARCore session)
    private lateinit var arSceneView: ARSceneView

    // Measurement এর জন্য anchor points store করি
    private val anchorPoints = mutableListOf<AnchorNode>()

    // Edge detection ON/OFF
    private var isEdgeDetectionEnabled = false

    // Measurement unit (cm or inch)
    private var useMetric = true

    // Measurement history (এই session এর)
    private val measurements = mutableListOf<MeasurementData>()

    // OpenCV initialized?
    private var openCVReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeasureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // OpenCV initialize করো
        initOpenCV()

        // AR SceneView setup
        setupARScene()

        // Button listeners setup
        setupButtons()

        // Instructions দেখাও
        showInstruction("Surface detect করতে phone টা আস্তে আস্তে ঘোরাও")
    }

    /**
     * OpenCV library load করে।
     * Edge detection এর জন্য OpenCV দরকার।
     */
    private fun initOpenCV() {
        openCVReady = OpenCVLoader.initLocal()
        if (!openCVReady) {
            Toast.makeText(this, "OpenCV load হয়নি। Edge detection কাজ নাও করতে পারে।", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * AR SceneView configure করে।
     * Plane detection enable করে যাতে real-world surface চিনতে পারে।
     */
    private fun setupARScene() {
        arSceneView = binding.arSceneView

        // Configure AR session when ready
        arSceneView.configureSession { session, config ->
            // Plane detection enable — horizontal & vertical দুটোই
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL

            // Depth API enable করো (যদি device support করে)
            if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                config.depthMode = Config.DepthMode.AUTOMATIC
            }

            // Light estimation — realistic AR এর জন্য
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR

            // Focus mode auto
            config.focusMode = Config.FocusMode.AUTO
        }

        // যখন AR plane detect হবে, instruction update করো
        arSceneView.onSessionUpdated = { session, frame ->
            val planes = session.getAllTrackables(Plane::class.java)
            if (planes.any { it.trackingState == TrackingState.TRACKING }) {
                runOnUiThread {
                    binding.tvPlaneStatus.text = "✅ Surface detected!"
                    binding.tvPlaneStatus.setTextColor(Color.GREEN)
                    showInstruction("Measure করতে screen-এ দুটো জায়গায় tap করো")
                }
            }

            // Edge detection enabled থাকলে frame process করো
            if (isEdgeDetectionEnabled && openCVReady) {
                processFrameForEdges(frame)
            }
        }

        // Screen-এ tap করলে AR hit-test kore anchor বসao
        arSceneView.onGestureListener = object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent, node: Node?) {
                val hitResult = arSceneView.hitTestAR(
                    e.x, e.y,
                    planeTypes = setOf(
                        Plane.Type.HORIZONTAL_UPWARD_FACING,
                        Plane.Type.HORIZONTAL_DOWNWARD_FACING,
                        Plane.Type.VERTICAL
                    )
                )
                hitResult?.let { handleTap(it) }
            }
        }
    }

    /**
     * User যখন screen-এ tap করে, সেই AR hit point-এ একটা anchor তৈরি হয়।
     * দুইটা anchor হলে তাদের মধ্যে distance measure করে।
     */
    private fun handleTap(hitResult: HitResult) {
        val anchor = hitResult.createAnchor()
        val anchorNode = AnchorNode(anchor, arSceneView)

        // Visual marker বানাও (ছোট sphere)
        anchorNode.createVisualMarker(this)

        anchorPoints.add(anchorNode)
        arSceneView.addChildNode(anchorNode)

        when (anchorPoints.size) {
            1 -> {
                showInstruction("প্রথম point বসেছে। এবার দ্বিতীয় point-এ tap করো।")
                binding.tvMeasurement.text = "Point 1 ✓"
            }
            2 -> {
                // দুইটা point আছে — distance calculate করো
                calculateAndShowDistance()
            }
        }
    }

    /**
     * দুইটা anchor point এর মধ্যে real-world distance calculate করে।
     * ARCore real-world coordinate system use করে, তাই measurement accurate হয়।
     */
    private fun calculateAndShowDistance() {
        if (anchorPoints.size < 2) return

        val point1 = anchorPoints[0].anchor?.pose
        val point2 = anchorPoints[1].anchor?.pose

        if (point1 == null || point2 == null) {
            showInstruction("Anchor lost হয়ে গেছে। আবার try করো।")
            return
        }

        // 3D distance calculate (Euclidean distance)
        val dx = point1.tx() - point2.tx()
        val dy = point1.ty() - point2.ty()
        val dz = point1.tz() - point2.tz()
        val distanceMeters = sqrt((dx * dx + dy * dy + dz * dz).toDouble())

        // Unit conversion
        val displayDistance: Double
        val unit: String
        if (useMetric) {
            displayDistance = distanceMeters * 100 // meter to cm
            unit = "cm"
        } else {
            displayDistance = distanceMeters * 39.3701 // meter to inch
            unit = "inch"
        }

        // Display measurement
        val formattedDistance = String.format("%.1f", displayDistance)
        binding.tvMeasurement.text = "$formattedDistance $unit"
        binding.tvMeasurement.setTextColor(Color.WHITE)
        binding.tvMeasurement.textSize = 32f

        // Draw line between points
        drawLineBetweenPoints(anchorPoints[0], anchorPoints[1])

        // Save to history
        val measurement = MeasurementData(
            distance = displayDistance,
            unit = unit,
            timestamp = System.currentTimeMillis()
        )
        measurements.add(measurement)
        MeasurementStore.addMeasurement(this, measurement)

        showInstruction("$formattedDistance $unit — নতুন measurement করতে Reset চাপো")
    }

    /**
     * দুইটা point-এর মধ্যে একটা visible line draw করে AR scene-এ।
     */
    private fun drawLineBetweenPoints(node1: AnchorNode, node2: AnchorNode) {
        val lineNode = LineNode(arSceneView, node1, node2)
        arSceneView.addChildNode(lineNode)
    }

    /**
     * Camera frame থেকে edge detect করে OpenCV ব্যবহার করে।
     * Canny edge detection algorithm use করে।
     */
    private fun processFrameForEdges(frame: Frame) {
        try {
            val image: Image = frame.acquireCameraImage()

            // Image কে Bitmap-এ convert করো
            val bitmap = imageToBitmap(image)
            image.close()

            if (bitmap != null) {
                // OpenCV edge detection
                val edges = detectEdges(bitmap)

                // Edge overlay update করো UI thread-এ
                runOnUiThread {
                    binding.edgeOverlay.setImageBitmap(edges)
                    binding.edgeOverlay.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            // Frame acquire fail হতে পারে, ignore করো
        }
    }

    /**
     * OpenCV Canny Edge Detection
     * Input bitmap-এ edge detect করে edge image return করে।
     */
    private fun detectEdges(bitmap: Bitmap): Bitmap {
        // Bitmap → OpenCV Mat
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        // Grayscale এ convert করো
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)

        // Gaussian blur — noise কমাতে
        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 1.5)

        // Canny edge detection
        val edges = Mat()
        Imgproc.Canny(blurred, edges, 50.0, 150.0)

        // Edge গুলো green color-এ highlight করো
        val colorEdges = Mat()
        Imgproc.cvtColor(edges, colorEdges, Imgproc.COLOR_GRAY2RGBA)

        // Green color apply for edges
        val result = Mat.zeros(src.size(), CvType.CV_8UC4)
        for (r in 0 until colorEdges.rows()) {
            for (c in 0 until colorEdges.cols()) {
                val pixel = colorEdges.get(r, c)
                if (pixel != null && pixel[0] > 0) {
                    result.put(r, c, doubleArrayOf(0.0, 255.0, 0.0, 200.0)) // Green with alpha
                }
            }
        }

        // Mat → Bitmap
        val resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(result, resultBitmap)

        // Memory cleanup
        src.release()
        gray.release()
        blurred.release()
        edges.release()
        colorEdges.release()
        result.release()

        return resultBitmap
    }

    /**
     * ARCore Image (YUV) কে Bitmap-এ convert করে।
     */
    private fun imageToBitmap(image: Image): Bitmap? {
        return try {
            val yPlane = image.planes[0]
            val uPlane = image.planes[1]
            val vPlane = image.planes[2]

            val yBuffer = yPlane.buffer
            val uBuffer = uPlane.buffer
            val vBuffer = vPlane.buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvMat = Mat(image.height + image.height / 2, image.width, CvType.CV_8UC1)
            yuvMat.put(0, 0, nv21)

            val rgbMat = Mat()
            Imgproc.cvtColor(yuvMat, rgbMat, Imgproc.COLOR_YUV2RGBA_NV21)

            val bitmap = Bitmap.createBitmap(rgbMat.cols(), rgbMat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(rgbMat, bitmap)

            yuvMat.release()
            rgbMat.release()

            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Buttons setup করে (Reset, Edge Toggle, Unit Toggle)
     */
    private fun setupButtons() {
        // Reset button — সব anchor মুছে নতুন করে শুরু করো
        binding.btnReset.setOnClickListener {
            resetMeasurement()
        }

        // Edge Detection toggle
        binding.btnEdgeToggle.setOnClickListener {
            isEdgeDetectionEnabled = !isEdgeDetectionEnabled
            if (isEdgeDetectionEnabled) {
                binding.btnEdgeToggle.text = "Edge: ON"
                binding.btnEdgeToggle.setBackgroundColor(Color.parseColor("#4CAF50"))
                showInstruction("Edge detection ON — object এর edges green-এ দেখাচ্ছে")
            } else {
                binding.btnEdgeToggle.text = "Edge: OFF"
                binding.btnEdgeToggle.setBackgroundColor(Color.parseColor("#666666"))
                binding.edgeOverlay.visibility = View.GONE
                showInstruction("Edge detection OFF")
            }
        }

        // Unit toggle (cm ↔ inch)
        binding.btnUnitToggle.setOnClickListener {
            useMetric = !useMetric
            binding.btnUnitToggle.text = if (useMetric) "cm" else "inch"

            // যদি measurement আগে থেকে থাকে, recalculate করো
            if (anchorPoints.size == 2) {
                calculateAndShowDistance()
            }
        }

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    /**
     * সব anchor ও line মুছে measurement reset করে।
     */
    private fun resetMeasurement() {
        anchorPoints.forEach { it.destroy() }
        anchorPoints.clear()

        // Scene এর সব child node সরাও
        arSceneView.childNodes.toList().forEach {
            arSceneView.removeChildNode(it)
        }

        binding.tvMeasurement.text = "Ready"
        binding.tvMeasurement.textSize = 20f
        binding.edgeOverlay.visibility = View.GONE

        showInstruction("Measure করতে screen-এ দুটো জায়গায় tap করো")
    }

    /**
     * User instructions দেখায় screen-এ।
     */
    private fun showInstruction(text: String) {
        binding.tvInstruction.text = text
    }

    override fun onResume() {
        super.onResume()
        try {
            arSceneView.onResume(this)
        } catch (e: CameraNotAvailableException) {
            Toast.makeText(this, "Camera available না।", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        arSceneView.onPause(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        arSceneView.onDestroy(this)
    }
}
