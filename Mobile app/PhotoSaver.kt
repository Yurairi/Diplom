package com.example.smartflowusiassistant
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class PhotoSaver {

    fun savePhotoToExternalStorage(context: Context, data: Any, absolutePath: String, fileName: String) {
        when (data) {
            is ByteArray -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    savePhotoUsingMediaStoreQ(context, data, absolutePath, fileName)
                } else {
                    savePhotoLegacy(context, data, absolutePath, fileName)
                }
            }
            is Bitmap -> {
                val byteArray = convertBitmapToByteArray(data)
                savePhotoToExternalStorage(context, byteArray, absolutePath, fileName)
            }
            else -> {
                showToast(context, "Неизвестный тип данных для сохранения фото")
            }
        }
    }

    private fun savePhotoUsingMediaStoreQ(context: Context, image: ByteArray, absolutePath: String, fileName: String) {
        val resolver: ContentResolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/$absolutePath")
        }

        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        imageUri?.let {
            resolver.openOutputStream(it).use { outputStream ->
                outputStream?.write(image)
                outputStream?.close()
                showToast(context, "Фото успешно сохранено в $absolutePath/$fileName")
            }
        }
    }

    private fun savePhotoLegacy(context: Context, image: ByteArray, absolutePath: String, fileName: String) {
        val directory = File(absolutePath)

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, fileName)

        try {
            val stream: OutputStream = FileOutputStream(file)
            stream.write(image)
            stream.close()
            showToast(context, "Фото успешно сохранено в $absolutePath/$fileName")
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(context, "Ошибка при сохранении фото")
        }
    }

    private fun convertBitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return stream.toByteArray()
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

