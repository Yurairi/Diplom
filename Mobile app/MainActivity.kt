package com.example.smartflowusiassistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.camera.core.ImageCaptureException
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.smartflowusiassistant.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import java.io.File

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, FragmentInteractionListener {
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var context: Context
    private lateinit var drawerLayout: DrawerLayout
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var currentFolderPath: String? = null
    private var isFlashOn = false
    var isFragmentVisible = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        context = applicationContext
        currentFolderPath = getLastFolderPath()
        if (currentFolderPath.isNullOrEmpty() || !File(currentFolderPath!!).exists()) {
            currentFolderPath = getDefaultFolderPath()
        }

        NavigationTools()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        ImageButtonClick(viewBinding.btnCamera)
        ImageButtonClick(viewBinding.btnGallery)
        ImageButtonClick(viewBinding.btnFlash)

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onFragmentVisibilityChanged(isVisible: Boolean) {
        isFragmentVisible = isVisible
    }
    override fun onFileSavePathChanged(newPath: String) {
        currentFolderPath = newPath
        saveLastFolderPath(newPath)
    }

    private fun saveLastFolderPath(folderPath: String) {
        val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().putString("lastFolderPath", folderPath).apply()
    }

    private fun getLastFolderPath(): String? {
        val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPref.getString("lastFolderPath", null)
    }

    private fun getDefaultFolderPath(): String {
        val folderName = "DigitalAssistant"
        val defaultFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), folderName)
        return defaultFolder.absolutePath
    }

    fun NavigationTools() {
        drawerLayout = findViewById(R.id.drawer_layout)
        val toolbar = viewBinding.toolbar
        setSupportActionBar(toolbar)
        val navigationView = viewBinding.navView
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav )
        toggle.drawerArrowDrawable.color = ContextCompat.getColor(this, R.color.white)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.nav_settings -> {
                isFragmentVisible = true
                replaceFragment(SettingsFragment())
            }
            R.id.nav_inf -> {
                isFragmentVisible = true
                replaceFragment(InformationFragment())
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction : FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()

    }

    private fun ImageButtonClick(button: ImageButton) {
        button.setOnClickListener {
            Log.d("MainActivity", "Before setting isFragmentVisible to false: $isFragmentVisible")
            if (!isFragmentVisible) {
                applyClickAnimation(it)
                when (button) {
                    viewBinding.btnCamera -> takePhoto(context)
                    viewBinding.btnGallery -> openGallery()
                    viewBinding.btnFlash -> turnFlash()
                }
            }
        }
    }
    fun applyClickAnimation(view: View) {
        view.animate().apply {
            duration = 100
            scaleX(0.9f)
            scaleY(0.9f)
            alpha(0.7f)
            withEndAction {
                view.animate().apply {
                    duration = 0
                    scaleX(1.0f)
                    scaleY(1.0f)
                    alpha(1.0f)
                }
            }
        }
    }
    private fun turnFlash() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                val cameraControl = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector
                ).cameraControl

                val newTorchState = !isFlashOn
                cameraControl.enableTorch(newTorchState)

                isFlashOn = newTorchState

                val newIcon = if (isFlashOn) R.drawable.ic_flash_on  else R.drawable.ic_flash_off
                viewBinding.btnFlash.setImageResource(newIcon)

            } catch (e: Exception) {
                Log.e(TAG, "Error turning on/off flash: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { handleImageSelection(it) }
        }
    private fun openGallery() {
        pickImage.launch("image/*")
    }


    private fun handleImageSelection(uri: Uri) {
        val intent = Intent(this@MainActivity, ImagePreviewActivity::class.java)
        intent.putExtra("photoUri", uri)
        Log.d(TAG, "MainActivity photoUri: $uri")
        intent.putExtra("pathFile", currentFolderPath)
        Log.d(TAG, "MainActivity pathFile: $currentFolderPath")
        startActivity(intent)
    }

    private fun takePhoto(context: Context) {
        val imageCapture = imageCapture ?: return
        val outputFile = File.createTempFile("IMG_${System.currentTimeMillis()}", ".jpg", context.cacheDir)
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(outputFile)
                    val intent = Intent(this@MainActivity, ImagePreviewActivity::class.java)
                    intent.putExtra("photoUri", savedUri)
                    intent.putExtra("pathFile", currentFolderPath)
                    startActivity(intent)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraX", "Photo capture failed: ${exception.message}", exception)
                }
            }
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraUSI"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }
}