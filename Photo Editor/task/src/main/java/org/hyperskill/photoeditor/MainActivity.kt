package org.hyperskill.photoeditor

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.viewbinding.ViewBinding
import org.hyperskill.photoeditor.databinding.ActivityMainBinding

private lateinit var binding: ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var originalBitmap: Bitmap
    lateinit var workingBitmap: Bitmap
    private lateinit var currentImage: ImageView

    fun adjustBrightness(brightness: Int) {
        val width = originalBitmap.width
        val height = originalBitmap.height

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = originalBitmap.getPixel(x, y)
                val r = Color.red(pixel) + brightness
                val g = Color.green(pixel) + brightness
                val b = Color.blue(pixel) + brightness

                // Clamp the values to be within [0, 255]
                val newR = r.coerceIn(0, 255)
                val newG = g.coerceIn(0, 255)
                val newB = b.coerceIn(0, 255)

                workingBitmap.setPixel(x, y, Color.rgb(newR, newG, newB))
            }
        }
    }


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivPhoto.also { currentImage = it }

        originalBitmap = createBitmap()
        workingBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        currentImage.setImageBitmap(originalBitmap)

        val activityResultLauncher =
            registerForActivityResult(StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val photoUri = result.data?.data ?: return@registerForActivityResult
                    // code to update ivPhoto with loaded image
                    @Suppress("DEPRECATION")
                    val loadedBitmap = MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
                    originalBitmap = loadedBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    workingBitmap = loadedBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    currentImage.setImageBitmap(originalBitmap)
                }
            }

        binding.btnGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intent)
        }

        binding.slBrightness.addOnChangeListener { _, value, _ ->
            adjustBrightness(value.toInt())
            currentImage.setImageBitmap(workingBitmap)
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