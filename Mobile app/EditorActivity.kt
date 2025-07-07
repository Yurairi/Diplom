package com.example.smartflowusiassistant

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.smartflowusiassistant.databinding.ActivityEditorBinding
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import ja.burhanrashid52.photoeditor.OnSaveBitmap
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.SaveSettings
import ja.burhanrashid52.photoeditor.shape.ShapeBuilder
import ja.burhanrashid52.photoeditor.shape.ShapeType
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



class EditorActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityEditorBinding
    private lateinit var photoEditor: PhotoEditor
    private lateinit var context: Context
    private var forLassoUri: Uri? = null
    private var resultCropUri: Uri? = null
    private var resultLassoUri: Uri? = null
    private val FirstActivity = MainActivity()
    private var pixelLassoCount : Int = 0
    private var pixelCropCount : Int = 0
    private var isCropOperationPerformed: Boolean = false
    private var currentFolderPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        context = applicationContext
        photoEditor = PhotoEditor.Builder(this, viewBinding.photoEditorView)
            .setClipSourceImage(true)
            .build()

        currentFolderPath = intent.getStringExtra("pathFile")
        forLassoUri = intent.getParcelableExtra("forLassoUri")
        viewBinding.photoEditorView.source.setImageBitmap(uriToBitmap(context, forLassoUri!!))

        ImageButtonClick(viewBinding.btnCrop)
        ImageButtonClick(viewBinding.btnBrush)
        ImageButtonClick(viewBinding.btnBack)
        ImageButtonClick(viewBinding.btnNext)
        ImageButtonClick(viewBinding.btnCancelLasso)
        ImageButtonClick(viewBinding.btnResLasso)


        viewBinding.btnClear.setOnClickListener {
            photoEditor.clearAllViews()
        }

        viewBinding.btnUndo.setOnClickListener {
            photoEditor.undo()
        }

        viewBinding.btnNext.visibility =View.INVISIBLE
        viewBinding.progressBar.visibility = View.INVISIBLE
    }

    private fun makeLasso() {
        photoEditor.setBrushDrawingMode(false)

        viewBinding.progressBar.visibility = View.VISIBLE
        saveEditedImage()

        viewBinding.btnBack.visibility = View.VISIBLE
        viewBinding.btnClear.visibility = View.INVISIBLE
        viewBinding.btnUndo.visibility = View.INVISIBLE
        viewBinding.btnBrush.visibility = View.VISIBLE
        viewBinding.btnCrop.visibility = View.VISIBLE
        viewBinding.llBrush.visibility = View.INVISIBLE
    }
    private fun ImageButtonClick(button: ImageButton) {
        button.setOnClickListener {
            FirstActivity.applyClickAnimation(it)
            when (button) {
                viewBinding.btnBack -> finish()
                viewBinding.btnCrop -> cropImage(if (resultLassoUri != null) resultLassoUri!! else forLassoUri!!)
                viewBinding.btnBrush -> makeBrush()
                viewBinding.btnNext -> sendResultImage()
                viewBinding.btnCancelLasso -> cancelLasso()
                viewBinding.btnResLasso -> makeLasso()
            }
        }
    }

    private fun cancelLasso() {
        photoEditor.clearAllViews()
        photoEditor.setBrushDrawingMode(false)
        viewBinding.btnClear.visibility = View.INVISIBLE
        viewBinding.btnUndo.visibility = View.INVISIBLE
        viewBinding.btnBack.visibility = View.VISIBLE
        viewBinding.btnNext.visibility = View.INVISIBLE
        viewBinding.btnBrush.visibility = View.VISIBLE
        viewBinding.btnCrop.visibility = View.VISIBLE
        viewBinding.llBrush.visibility = View.INVISIBLE
    }

    private fun sendResultImage() {
        val intent = Intent(this, ImagePreviewActivity::class.java)
        val resultEditUri = if (isCropOperationPerformed) resultCropUri else resultLassoUri
        intent.putExtra("resultEditUri", resultEditUri)
        Log.d(ContentValues.TAG, "Send image from EditorActivity: $resultEditUri")
        val resultPixelCount = if (isCropOperationPerformed) pixelCropCount else pixelLassoCount
        intent.putExtra("pixelCount", resultPixelCount)
        Log.d(ContentValues.TAG, "Finally pixelCount: $resultPixelCount")
        intent.putExtra("pathFile", currentFolderPath)
        startActivity(intent)
    }

    private fun cropImage(uri: Uri) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val destinationFileName = "cropped_image_$timeStamp"
        val destinationUri = Uri.fromFile(File(cacheDir, "$destinationFileName.jpg"))
        val options = UCrop.Options()

        options.setCompressionFormat(Bitmap.CompressFormat.JPEG)
        options.setCompressionQuality(100)
        options.setShowCropFrame(true)
        options.setShowCropGrid(false)
        options.setCircleDimmedLayer(false)
        options.setStatusBarColor(0)
        options.setActiveControlsWidgetColor(Color.parseColor("#2ecd96"))
        options.setFreeStyleCropEnabled(true)
        options.setMaxScaleMultiplier(20.0f)
        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL)
        options.setStatusBarColor(Color.parseColor("#2ecd96"))

        UCrop.of(uri, destinationUri)
            .withOptions(options)
            .start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            resultCropUri = UCrop.getOutput(data!!)
            // подсчет пикселей обрезанного изображения
            val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, resultCropUri)
            val width = bitmap.width
            val height = bitmap.height
            pixelCropCount = width * height
            Log.d(ContentValues.TAG, "pixelCropCount: $pixelCropCount")
            isCropOperationPerformed = true

            viewBinding.photoEditorView.source.setImageURI(resultCropUri)
            viewBinding.btnNext.visibility = View.VISIBLE

        } else if (resultCode == UCrop.RESULT_ERROR) {
            val error = UCrop.getError(data!!)
            error?.let { Log.e(ContentValues.TAG, "Error during cropping: ${it.localizedMessage}", it) }
        }
    }

    private fun makeBrush(){
        viewBinding.llBrush.visibility = View.VISIBLE
        viewBinding.btnClear.visibility = View.VISIBLE
        viewBinding.btnUndo.visibility = View.VISIBLE
        viewBinding.btnNext.visibility = View.INVISIBLE
        viewBinding.btnBack.visibility = View.INVISIBLE
        viewBinding.btnBrush.visibility = View.INVISIBLE
        viewBinding.btnCrop.visibility = View.INVISIBLE

        photoEditor.setBrushDrawingMode(true)
        val shapeBuilder = ShapeBuilder()
        val color = Color.parseColor("#BF0DE7")
        shapeBuilder.withShapeColor(color)
        shapeBuilder.withShapeSize(9f)
        shapeBuilder.withShapeType(ShapeType.Brush)
        photoEditor.setShape(shapeBuilder)
    }

    private fun saveEditedImage() {
        val saveSettings = SaveSettings.Builder()
            .setClearViewsEnabled(true)
            .setTransparencyEnabled(true)
            .setCompressQuality(100)
            .setCompressFormat(Bitmap.CompressFormat.JPEG)
            .build()

        photoEditor.saveAsBitmap(saveSettings, object : OnSaveBitmap {
            override fun onBitmapReady(saveBitmap: Bitmap) {
                val resultBitmap = applyLasso(saveBitmap)
                viewBinding.photoEditorView.source.setImageBitmap(resultBitmap)
                resultLassoUri = bitmapToUri(context, resultBitmap)
                viewBinding.progressBar.visibility = View.INVISIBLE
                viewBinding.btnNext.visibility = View.VISIBLE
                }
        })
    }

    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap: Bitmap? = inputStream?.use { stream -> BitmapFactory.decodeStream(stream) }
        val compressedBitmap = bitmap?.let { compressBitmap(uri, 800,600) }

        return compressedBitmap
    }

    fun compressBitmap(uri: Uri, maxWidth: Int, maxHeight: Int): Bitmap {
        var path: String? = null

        if ("content".equals(uri.scheme, ignoreCase = true)) {
            path = getRealPathFromURI(context, uri)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            path = uri.path
        }

        val options = BitmapFactory.Options().apply {
            // Установка параметра inJustDecodeBounds в true для определения размера изображения
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, this)

            // Вычисление оптимального значения inSampleSize
            inSampleSize = calculateInSampleSize(this, maxWidth, maxHeight)

            // Повторное декодирование изображения с установленным inSampleSize для уменьшения его размера
            inJustDecodeBounds = false
        }

        return BitmapFactory.decodeFile(path, options)
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


    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun bitmapToUri(context: Context, bitmap: Bitmap): Uri? {
        var uri: Uri? = null
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "image_lasso_$timeStamp.jpg")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            uri = Uri.fromFile(file)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return uri
    }

    fun applyLasso(originalBitmap: Bitmap): Bitmap {
        var maxBound = 0
        var minBound = 0
        pixelLassoCount = 0
        var boundColor = Color.parseColor("#BF0DE7")
        for (col in 0 until originalBitmap.height) {
            var listOfBounds = mutableListOf<Int>()
            for (row in 0 until originalBitmap.width) {
                val pixelColor = originalBitmap.getPixel(row, col)
                if(pixelColor.equals(boundColor)) {
                    listOfBounds.add(row)
                }
            }
            if (listOfBounds.isNotEmpty()){
                if(listOfBounds.last() - listOfBounds.first() < 18){
                    if (minBound - 18 < listOfBounds.first() && minBound + 18 > listOfBounds.first())
                        minBound = listOfBounds.first()
                    else{
                        maxBound = listOfBounds.last()
                    }
                    listOfBounds.clear()
                    listOfBounds.add(minBound)
                    listOfBounds.add(maxBound)
                }
                for (row in 0 until listOfBounds.first()){
                    originalBitmap.setPixel(row, col,Color.BLACK)
                }
                for (row in listOfBounds.last() until originalBitmap.width){
                    originalBitmap.setPixel(row, col,Color.BLACK)
                }
                maxBound = listOfBounds.last()
                minBound = listOfBounds.first()
            }
            else {
                for (row in 0 until originalBitmap.width){
                    originalBitmap.setPixel(row, col,Color.BLACK)
                }
            }
        }
        maxBound = 0
        minBound = 0
        for (row in 0 until originalBitmap.width) {
            var listOfBounds = mutableListOf<Int>()
            for (col in 0 until originalBitmap.height) {
                val pixelColor = originalBitmap.getPixel(row, col)
                if(pixelColor.equals(boundColor)) {
                    listOfBounds.add(col)
                }
            }
            if (listOfBounds.isNotEmpty()){
                if(listOfBounds.last() - listOfBounds.first() < 18){
                    listOfBounds.clear()
                    listOfBounds.add(minBound)
                    listOfBounds.add(maxBound)
                }
                for (col in 0 until listOfBounds.first()){
                    originalBitmap.setPixel(row, col,Color.BLACK)
                    pixelLassoCount += 1
                }
                for (col in listOfBounds.last() until originalBitmap.height){
                    originalBitmap.setPixel(row, col,Color.BLACK)
                    pixelLassoCount += 1
                }
                maxBound = listOfBounds.last()
                minBound = listOfBounds.first()
            }
            else {
                for (col in 0 until originalBitmap.height){
                    originalBitmap.setPixel(row, col,Color.BLACK)
                    pixelLassoCount += 1
                }
            }
        }
        pixelLassoCount = originalBitmap.width * originalBitmap.height - pixelLassoCount
        Log.d(ContentValues.TAG, "pixelLassoCount: $pixelLassoCount")

        isCropOperationPerformed = false
        return originalBitmap
    }
}

