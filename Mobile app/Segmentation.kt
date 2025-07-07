package com.example.smartflowusiassistant

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.smartflowusiassistant.network.ApiResponse
import com.example.smartflowusiassistant.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class Segmentation {

    private var processedBitmap: Bitmap? = null

    // Функция для отправки изображения на API и получения результата
    suspend fun processImage(context: Context, imagePath: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(imagePath)
                if (!file.exists()) {
                    Log.e("Segmentation", "Файл не существует: $imagePath")
                    return@withContext null
                }
                Log.d("Segmentation", "Отправка файла: ${file.name} размером ${file.length()} байт")

                // Создание RequestBody
                val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
                // Создание MultipartBody.Part с именем "file"
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                // Отправка запроса
                val response = RetrofitClient.apiService.processImage(body)

                Log.d("Segmentation", "Код ответа: ${response.code()}")

                if (response.isSuccessful) {
                    val apiResponse: ApiResponse? = response.body()
                    apiResponse?.let {
                        val processedImageBase64 = it.image
                        Log.d("Segmentation", "Processed Image Base64: $processedImageBase64")

                        // Декодирование Base64 обратно в Bitmap
                        val decodedBytes = Base64.decode(processedImageBase64, Base64.DEFAULT)
                        val decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        processedBitmap = decodedBitmap
                        Log.d("Segmentation", "Размер обработанного Bitmap: ${decodedBitmap.width}x${decodedBitmap.height}")
                        return@withContext decodedBitmap
                    }
                } else {
                    Log.e("Segmentation", "Ошибка API: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("Segmentation", "Исключение: ${e.message}")
            }
            return@withContext null
        }
    }

    fun getProcessedBitmap(): Bitmap? {
        return processedBitmap
    }
}
