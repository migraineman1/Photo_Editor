package org.hyperskill.photoeditor

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.hyperskill.photoeditor.databinding.ActivityMainBinding
import kotlin.math.pow

private lateinit var binding: ActivityMainBinding

/**
 * Main activity for the photo editor application.
 *
 * Provides functionality for loading an image from the device gallery or using a synthetic sample bitmap,
 * applying real-time photo filters (brightness, contrast, saturation, and gamma),
 * and saving the edited photo to media storage with runtime permission management.
 */
class MainActivity : AppCompatActivity() {

    /** Unmodified source [Bitmap] used as the baseline for all filter operations. */
    lateinit var originalBitmap: Bitmap

    /** Mutable copy of [originalBitmap] updated with applied filter pixel values. */
    lateinit var workingBitmap: Bitmap

    /** ImageView component displaying the current photo state. */
    private lateinit var currentImage: ImageView

    private var lastJob: Job? = null

    /**
     * Applies brightness, contrast, saturation, and gamma adjustments sequentially
     * to [originalBitmap] based on current slider values asynchronously using coroutines,
     * updating [workingBitmap] and displaying the result in [currentImage].
     */
    fun applyFilters() {
        val brightness = binding.slBrightness.value.toInt()
        val contrast = binding.slContrast.value.toInt()
        val saturation = binding.slSaturation.value.toInt()
        val gamma = binding.slGamma.value.toDouble()

        lastJob?.cancel()
        lastJob = GlobalScope.launch(Dispatchers.Default) {
            val width = originalBitmap.width
            val height = originalBitmap.height

            val pixels = IntArray(width * height)
            originalBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val brightPixelsR = IntArray(width * height)
            val brightPixelsG = IntArray(width * height)
            val brightPixelsB = IntArray(width * height)

            var totalBrightnessSum = 0L

            // brightness filter
            for (i in 0 until width * height) {
                val pixel = pixels[i]
                val r = (Color.red(pixel) + brightness).coerceIn(0, 255)
                val g = (Color.green(pixel) + brightness).coerceIn(0, 255)
                val b = (Color.blue(pixel) + brightness).coerceIn(0, 255)

                brightPixelsR[i] = r
                brightPixelsG[i] = g
                brightPixelsB[i] = b

                totalBrightnessSum += (r + g + b)
            }

            ensureActive()

            // contrast filter
            val avgBright = (totalBrightnessSum / (width * height * 3)).toInt()
            val cAlpha: Double = (255.0 + contrast) / (255.0 - contrast)

            val contrastPixelsR = IntArray(width * height)
            val contrastPixelsG = IntArray(width * height)
            val contrastPixelsB = IntArray(width * height)

            for (i in 0 until width * height) {
                contrastPixelsR[i] = (cAlpha * (brightPixelsR[i] - avgBright) + avgBright).toInt().coerceIn(0, 255)
                contrastPixelsG[i] = (cAlpha * (brightPixelsG[i] - avgBright) + avgBright).toInt().coerceIn(0, 255)
                contrastPixelsB[i] = (cAlpha * (brightPixelsB[i] - avgBright) + avgBright).toInt().coerceIn(0, 255)
            }

            ensureActive()

            // saturation filter
            val sAlpha: Double = (255.0 + saturation) / (255.0 - saturation)

            val saturationPixelsR = IntArray(width * height)
            val saturationPixelsG = IntArray(width * height)
            val saturationPixelsB = IntArray(width * height)

            for (i in 0 until width * height) {
                val r = contrastPixelsR[i]
                val g = contrastPixelsG[i]
                val b = contrastPixelsB[i]
                val rgbAvg = (r + g + b) / 3

                saturationPixelsR[i] = (sAlpha * (r - rgbAvg) + rgbAvg).toInt().coerceIn(0, 255)
                saturationPixelsG[i] = (sAlpha * (g - rgbAvg) + rgbAvg).toInt().coerceIn(0, 255)
                saturationPixelsB[i] = (sAlpha * (b - rgbAvg) + rgbAvg).toInt().coerceIn(0, 255)
            }

            ensureActive()

            // gamma filter
            val resultPixels = IntArray(width * height)
            for (i in 0 until width * height) {
                val r = (255.0 * (saturationPixelsR[i] / 255.0).pow(gamma)).toInt().coerceIn(0, 255)
                val g = (255.0 * (saturationPixelsG[i] / 255.0).pow(gamma)).toInt().coerceIn(0, 255)
                val b = (255.0 * (saturationPixelsB[i] / 255.0).pow(gamma)).toInt().coerceIn(0, 255)

                resultPixels[i] = Color.rgb(r, g, b)
            }

            ensureActive()

            runOnUiThread {
                workingBitmap.setPixels(resultPixels, 0, width, 0, 0, width, height)
                currentImage.setImageBitmap(workingBitmap)
            }
        }
    }

    /**
     * Generates a sample bitmap with calculated color values.
     *
     * Note: Do not change this function implementation.
     *
     * @return A 200x100 [Bitmap] with generated pixel colors.
     */
    // do not change this function
    fun createBitmap(): Bitmap {
        val width = 200
        val height = 100
        val pixels = IntArray(width * height)

        // get pixel array from source
        var R: Int
        var G: Int
        var B: Int
        var index: Int

        for (y in 0 until height) {
            for (x in 0 until width) {
                // get current index in 2D-matrix
                index = y * width + x
                // get color
                R = x % 100 + 40
                G = y % 100 + 80
                B = (x + y) % 100 + 120

                pixels[index] = Color.rgb(R, G, B)
            }
        }
        // output bitmap
        val bitmapOut = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        bitmapOut.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmapOut
    }

    /**
     * Saves the photo currently in the photo view into the system's external media store as a JPEG image.
     */
    private fun saveImage() {
        val bitmap = (binding.ivPhoto.drawable as BitmapDrawable).bitmap
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.ImageColumns.WIDTH, bitmap.width)
            put(MediaStore.Images.ImageColumns.HEIGHT, bitmap.height)
        }

        val uri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: return

        contentResolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
    }

    /**
     * Handles the result of runtime permission requests.
     * If the storage permission is granted, saves the current photo.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            saveImage()
        }
    }

    /**
     * Called when the activity is starting.
     * Sets up view binding, default bitmaps, gallery launcher, filter change listeners, and save button logic.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivPhoto.also { currentImage = it }

        originalBitmap = createBitmap()
        workingBitmap = originalBitmap.copy(originalBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        currentImage.setImageBitmap(originalBitmap)

        val activityResultLauncher =
            registerForActivityResult(StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val photoUri = result.data?.data ?: return@registerForActivityResult
                    // code to update ivPhoto with loaded image
                    @Suppress("DEPRECATION")
                    val loadedBitmap = MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
                    originalBitmap = loadedBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    workingBitmap = originalBitmap.copy(originalBitmap.config ?: Bitmap.Config.ARGB_8888, true)
                    currentImage.setImageBitmap(originalBitmap)
                }
            }

        binding.btnGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intent)
        }

        binding.slBrightness.addOnChangeListener { _, _, _ ->
            applyFilters()
        }

        binding.slContrast.addOnChangeListener { _, _, _ ->
            applyFilters()
        }

        binding.slSaturation.addOnChangeListener { _, _, _ ->
            applyFilters()
        }

        binding.slGamma.addOnChangeListener { _, _, _ ->
            applyFilters()
        }

        binding.btnSave.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                saveImage()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    0
                )
            }
        }
    }
}
