package com.iamquan.qrcode

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.iamquan.qrcode.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraSelector: CameraSelector? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null
    private lateinit var cameraManager: CameraManager
    private var isOnFlash: Boolean = false
    private val cameraViewModel: CameraViewModel by viewModels {
        CameraViewModelFactory(application)
    }


    companion object {
        private const val PERMISSION_CAMERA_REQUEST = 1
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupCamera()
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        //  onClickImgLibrary()
        onClickImgFlash()
        //   onClickImgRotateCamera()
    }

    private fun onClickImgRotateCamera() {
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun onClickImgFlash() {
        binding.imgFlash.setOnClickListener {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                isOnFlash = !isOnFlash
                val cameraId = cameraManager.cameraIdList[0]
                cameraManager.setTorchMode(cameraId, isOnFlash)
            } else {
                Toast.makeText(this@MainActivity, "No flash", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onClickImgLibrary() {
    }

    private fun setupCamera() {
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        cameraViewModel.processCameraProvider.observe(this) {
            cameraProvider = it
            if (isCameraPermissionGranted()) {
                bindCameraUseCases()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    PERMISSION_CAMERA_REQUEST
                )
            }
        }
    }


    private fun bindCameraUseCases() {
//        bindAnalyseUseCase()
        bindPreviewUseCase()
    }

    private fun bindPreviewUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }

        previewUseCase = Preview.Builder()
            .setTargetRotation(binding.prView.display.rotation)
            .build()
        previewUseCase!!.setSurfaceProvider(binding.prView.surfaceProvider)

        try {

            cameraProvider!!.bindToLifecycle(
                this,
                cameraSelector!!,
                previewUseCase
            )
        } catch (illegalStateException: IllegalStateException) {
            Log.e("iamquan1705", illegalStateException.message ?: "IllegalStateException")
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e("iamquan1705", illegalArgumentException.message ?: "IllegalArgumentException")
        }
    }

    private fun bindAnalyseUseCase() {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()

        val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient(options)

        if (cameraProvider == null) {
            return
        }
        if (analysisUseCase != null) {
            cameraProvider!!.unbind(analysisUseCase)
        }

        analysisUseCase = ImageAnalysis.Builder()
            .setTargetRotation(binding.prView.display.rotation)
            .build()

        // Initialize our background executor
        val cameraExecutor = Executors.newSingleThreadExecutor()

        analysisUseCase?.setAnalyzer(
            cameraExecutor,
            ImageAnalysis.Analyzer { imageProxy ->
                processImageProxy(barcodeScanner, imageProxy)
            }
        )
        try {
            cameraProvider!!.bindToLifecycle(
                /* lifecycleOwner= */this,
                cameraSelector!!,
                analysisUseCase
            )
        } catch (illegalStateException: IllegalStateException) {
            Log.e("iamquan1705", illegalStateException.message ?: "IllegalStateException")
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e("iamquan1705", illegalArgumentException.message ?: "IllegalArgumentException")
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ) {
        val inputImage =
            InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                barcodes.forEach { barcode ->
                    val bounds = barcode.boundingBox
                    val corners = barcode.cornerPoints
                    val rawValue = barcode.rawValue
                    //  Log.d("iamquan1705",rawValue.toString())
                    // See API reference for complete list of supported types
                    when (barcode.valueType) {
                        Barcode.TYPE_PHONE -> {
                            val numberPhone = barcode.phone!!.number
                            Log.d("iamquan1705", "Numberphone :$numberPhone")
                        }
                        Barcode.TYPE_EMAIL -> {
                            val email = barcode.email!!.address
                            Log.d("iamquan1705", "Numberphone :$email")
                        }
                        Barcode.TYPE_URL -> {
                            val title = barcode.url!!.title
                            val url = barcode.url!!.url
                            Log.d("iamquan1705", "Title: $title\nURL: $url")
                        }
                        Barcode.TYPE_SMS -> {
                            var phone = barcode.sms!!.phoneNumber
                            var message = barcode.sms!!.message
                            Log.d("iamquan1705", "body: $message\n phone: $phone")
                        }

                        Barcode.TYPE_CONTACT_INFO -> {
                            var name = barcode.contactInfo?.name
                            var phone = barcode.contactInfo?.phones

                        }

                        Barcode.TYPE_ISBN -> {

                        }
                        Barcode.TYPE_TEXT -> {
                            var text = barcode.rawValue
                            Log.d("iamquan1705", rawValue.toString())
                        }
                        Barcode.TYPE_WIFI -> {
                            val ssid = barcode.wifi!!.ssid
                            val password = barcode.wifi!!.password
                            val type = barcode.wifi!!.encryptionType
                            Log.d("iamquan1705", "ssid: $ssid\npassword: $password\ntype: $type")
                        }
                        Barcode.TYPE_CALENDAR_EVENT -> {

                        }
                    }
                }
            }
            .addOnFailureListener {
                Log.e("iamquan1705", it.message ?: it.toString())
            }
            .addOnCompleteListener {
                // When the image is from CameraX analysis use case, must call image.close() on received
                // images when finished using them. Otherwise, new images may not be received or the camera
                // may stall.
                imageProxy.close()

            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_CAMERA_REQUEST) {
            if (isCameraPermissionGranted()) {
                bindCameraUseCases()
            } else {
                Log.e("IamQuan1705", "no camera permission")
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            baseContext,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}