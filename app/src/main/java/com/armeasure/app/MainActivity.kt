package com.armeasure.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.armeasure.app.databinding.ActivityMainBinding
import com.google.ar.core.ArCoreApk

/**
 * MainActivity — App এর home screen।
 * এখান থেকে AR Measurement screen এ যাওয়া যায়।
 * Camera permission check ও ARCore availability check এখানে হয়।
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // "Start Measuring" button click
        binding.btnStartMeasure.setOnClickListener {
            checkPermissionsAndStart()
        }

        // "History" button click
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    /**
     * Camera permission আছে কিনা চেক করে।
     * থাকলে ARCore চেক করে, না থাকলে permission request করে।
     */
    private fun checkPermissionsAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            checkARCoreAndStart()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        }
    }

    /**
     * ARCore এই device-এ supported কিনা check করে।
     * Supported হলে MeasureActivity start করে।
     */
    private fun checkARCoreAndStart() {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability.isTransient) {
            // ARCore এখনও check করছে, একটু পরে আবার try করো
            binding.root.postDelayed({ checkARCoreAndStart() }, 200)
            return
        }

        if (availability.isSupported) {
            startActivity(Intent(this, MeasureActivity::class.java))
        } else {
            Toast.makeText(
                this,
                "আপনার device-এ ARCore supported না। AR Measurement চালাতে ARCore supported device লাগবে।",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkARCoreAndStart()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission দরকার measurement এর জন্য।",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
