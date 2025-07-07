// ImagePreviewActivity.kt
package com.example.smartflowusiassistant

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.smartflowusiassistant.databinding.ActivityImagePreviewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ImagePreviewActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityImagePreviewBinding
    private lateinit var context: Context
    private var photoUri: Uri? = null
    private var resultEditUri: Uri? = null
    private val FirstActivity = MainActivity()
    private val PhotoSaver = PhotoSaver()
    private var currentFolderPath: String? = null

    private val REQUEST_PERMISSIONS = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityImagePreviewBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        context = applicationContext
        currentFolderPath = intent.getStringExtra("pathFile")
        Log.d(TAG, "Take pathFile to ImagePreviewActivity from mainActivity gallery: $currentFolderPath")
        photoUri = intent.getParcelableExtra("photoUri")
        Log.d(TAG, "ImagePreviewAcitivity : $photoUri")
        resultEditUri = intent.getParcelableExtra("resultEditUri")

        if (resultEditUri != null) {
            displayImage(resultEditUri!!)
        } else {
            displayImage(photoUri!!)
        }

        viewBinding.index.setOnClickListener {
            val clipboardManager = ContextCompat.getSystemService(this, ClipboardManager::class.java)
            val clipData = ClipData.newPlainText("label", viewBinding.index.text)
            clipboardManager?.setPrimaryClip(clipData)
            Toast.makeText(this, "Значение индекса скопировано", Toast.LENGTH_SHORT).show()
        }

        ImageButtonClick(viewBinding.btnBack)
        ImageButtonClick(viewBinding.btnEdit)
        ImageButtonClick(viewBinding.btnNext)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        viewBinding.progressBarPreview.visibility = View.INVISIBLE

        // Запрос разрешений
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                REQUEST_PERMISSIONS
            )
        }
    }

    private fun hasPermissions(): Boolean {
        val write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSIONS -> {
                if ((grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED })) {
                    // Разрешения предоставлены
                } else {
                    // Разрешения отклонены
                    Toast.makeText(this, "Разрешения необходимы для сохранения изображений", Toast.LENGTH_SHORT).show()
                }
                return
            }
            else -> {
                // Другие случаи
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CacheManager.clearCache(applicationContext)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EDIT_IMAGE && resultCode == RESULT_OK) {
            val resultLassoUri = data?.getParcelableExtra<Uri>("resultEditUri")
            resultEditUri = resultLassoUri // Update the resultEditUri here
            displayImage(resultEditUri!!)
        }
    }

    private fun ImageButtonClick(button: ImageButton) {
        button.setOnClickListener {
            FirstActivity.applyClickAnimation(it)
            when (button) {
                viewBinding.btnBack -> goToMainActivity()
                viewBinding.btnEdit -> {
                    openEditor()
                }
                viewBinding.btnNext -> {
                    displayAnalysisImage(if (resultEditUri != null) resultEditUri!! else photoUri!!)
                }
            }
        }
    }

    private fun goToMainActivity() {
        finish()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    companion object {
        const val REQUEST_EDIT_IMAGE = 1001
    }

    private fun openEditor() {
        val intent = Intent(this, EditorActivity::class.java)
        intent.putExtra("forLassoUri", if (resultEditUri != null) resultEditUri!! else photoUri!!)
        intent.putExtra("pathFile", currentFolderPath)
        startActivityForResult(intent, REQUEST_EDIT_IMAGE)
    }

    private fun displayImage(uri: Uri) {
        Log.d(TAG, "Displaying image: $uri")
        Glide.with(this)
            .load(uri)
            .into(viewBinding.ivPhoto)
    }

    fun getRealPathFromURI(context: Context, uri: Uri): String? {
        val inputStream = context.contentResolver.openInputStream(uri)
        inputStream?.use { input ->
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "temp_image_$timeStamp.jpg"
            val tempFile = File(context.cacheDir, fileName)
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
            return tempFile.absolutePath
        }
        return null
    }

    private fun calculatePixelCount(filePath: String): Int {
        val file = File(filePath)
        if (!file.exists()) {
            // Обработка ошибки: файл не существует
            return 0
        }

        // Декодируем изображение из файла
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, options)

        // Возвращаем количество пикселей
        return options.outWidth * options.outHeight
    }

    private fun displayAnalysisImage(uri: Uri) {
        var filePath: String? = null
        val seg = Segmentation()

        if ("content".equals(uri.scheme, ignoreCase = true)) {
            filePath = getRealPathFromURI(context, uri)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            filePath = uri.path
        }

        if (filePath != null) {
            val pixelCount = intent.getIntExtra("pixelCount", -1)

            val finalPixelCount = if (pixelCount == -1) {
                calculatePixelCount(filePath)
            } else {
                pixelCount
            }

            Log.d(TAG, "pixelCount from ImagePreviewActivity: $finalPixelCount")

            CoroutineScope(Dispatchers.Main).launch {
                viewBinding.progressBarPreview.visibility = View.VISIBLE
                val resultPicture = seg.processImage(context, filePath)

                if (resultPicture != null) {
                    runOnUiThread {
                        viewBinding.ivPhoto.setImageBitmap(resultPicture)
                    }

                    viewBinding.btnNext.visibility = View.INVISIBLE
                    viewBinding.btnEdit.visibility = View.INVISIBLE
                    viewBinding.tvEdit.visibility = View.INVISIBLE
                    viewBinding.btnDownload.visibility = View.VISIBLE

                    viewBinding.btnDownload.setOnClickListener {
                        FirstActivity.applyClickAnimation(it)
                        saveImage(resultPicture, context, currentFolderPath!!)
                    }
                    viewBinding.index.visibility = View.GONE // Нет индекса для отображения
                } else {
                    Toast.makeText(this@ImagePreviewActivity, "Ошибка обработки изображения", Toast.LENGTH_SHORT).show()
                }
                viewBinding.progressBarPreview.visibility = View.INVISIBLE
            }
        } else {
            Log.e(TAG, "Failed to get file path from URI: $uri")
            Toast.makeText(this, "Не удалось получить путь к изображению", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveImage(resultPicture: Bitmap, context: Context, currentFolderPath: String) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "IMG_$timeStamp.jpg"

            // Определение размеров нового битмапа
            val newWidth = resultPicture.width + 200 // Добавляем 200 пикселей по ширине
            val newHeight = resultPicture.height + 100 // Добавляем 100 пикселей по высоте

            // Создание нового битмапа с увеличенными размерами
            val mutableBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(mutableBitmap)

            // Наложение изначального изображения на новый битмап
            canvas.drawBitmap(resultPicture, 20f, 20f, null)

            // Сохранение объединенного изображения
            val imagePath = PhotoSaver.savePhotoToExternalStorage(context, mutableBitmap, currentFolderPath, fileName)

            // Вывод информации о сохраненном файле
            Log.d(TAG, "Saved image: $imagePath")
            Toast.makeText(context, "Изображение сохранено: $imagePath", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(TAG, "Error saving image: ${e.message}")
            Toast.makeText(context, "Ошибка сохранения изображения", Toast.LENGTH_SHORT).show()
        }
    }

    object CacheManager {

        fun clearCache(context: Context) {
            try {
                val cacheDir = context.cacheDir
                if (cacheDir.exists()) {
                    deleteDir(cacheDir)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun deleteDir(dir: File): Boolean {
            if (dir.isDirectory) {
                val children = dir.list()
                children?.forEach { child ->
                    val success = deleteDir(File(dir, child))
                    if (!success) {
                        return false
                    }
                }
            }
            return dir.delete()
        }
    }
}


