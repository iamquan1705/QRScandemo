package com.iamquan.qrcode

import QRCodeFragment
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.iamquan.qrcode.databinding.ActivityMainBinding
import com.iamquan.qrcode.model.UrlQR
import com.iamquan.qrcode.model.WifiQR
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraExecutor: ExecutorService? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var cameraSelector: CameraSelector? = null
    private var imageCapture: ImageCapture? = null
    private var uriImage: Uri? = null
    private var optionsBarcode: BarcodeScannerOptions? = null
    private var barcodeScanner: BarcodeScanner? = null


    private var lenFacing: Int = CameraSelector.LENS_FACING_BACK

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initBarcodeScanner()
        setupCamera()
        binding.imgLibrary.setOnClickListener {
            openGalleryForImage()
        }

        binding.imgRotateCamera.setOnClickListener {
            lenFacing = if (lenFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            setupCamera()
        }

    }

    private fun setupCamera() {
        if (isCameraPermissionGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                PERMISSION_CAMERA_REQUEST
            )
        }
    }

    private fun startCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture?.addListener(Runnable {
            cameraProvider = cameraProviderFuture!!.get()
            bindPreview()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindPreview() {
        val preview: Preview = Preview.Builder()
            .build().also {
                it.setSurfaceProvider(binding.prView.surfaceProvider)
            }
        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lenFacing)
            .build()
        imageCapture = ImageCapture.Builder()
            .build()
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetRotation(binding.prView.display.rotation)
            .build()
        imageAnalysis?.setAnalyzer(
            cameraExecutor!!,
            ImageAnalysis.Analyzer { imageProxy ->
                processImageProxy(barcodeScanner!!, imageProxy)
            }
        )
        try {
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(this, cameraSelector!!, preview, imageAnalysis)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun initBarcodeScanner() {
        optionsBarcode = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
        barcodeScanner = BarcodeScanning.getClient(optionsBarcode!!)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(barcodeScanner: BarcodeScanner, imageProxy: ImageProxy) {
        val inputImage =
            InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        processImage(barcodeScanner, inputImage)
    }

    private fun processImageFromGallery(barcodeScanner: BarcodeScanner, uri: Uri) {
        val inputImage: InputImage
        try {
            inputImage = InputImage.fromFilePath(this, uriImage!!)
            processImage(barcodeScanner, inputImage)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    @SuppressLint("ResourceType")
    private fun processImage(barcodeScanner: BarcodeScanner, inputImage: InputImage) {
        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                barcodes.forEach { barcode ->
                    val bounds = barcode.boundingBox
                    val corners = barcode.cornerPoints
                    val rawValue = barcode.rawValue
                    // See API reference for complete list of supported types
                    when (barcode.valueType) {
                        Barcode.TYPE_WIFI -> {
                            val ssid = barcode.wifi!!.ssid
                            val password = barcode.wifi!!.password
                            val type = barcode.wifi!!.encryptionType
                            Log.d(
                                "iamquan1705",
                                rawValue.toString()
                            )
                            var qr = WifiQR("string", ssid.toString(), password.toString(), type)
                            supportFragmentManager
                                .beginTransaction()
                                .add(R.id.frFragment, QRCodeFragment(qr), "rageComicList")
                                .commit()
                        }
                        Barcode.TYPE_URL -> {
                            val title = barcode.url!!.title
                            val url = barcode.url!!.url
                            Log.d("iamquan1705", "Title: $title\nURL: $url")
                            val urlQR = UrlQR("string", title.toString(), url.toString())
                            supportFragmentManager
                                .beginTransaction()
                                .add(R.id.frFragment, QRCodeFragment(urlQR), "rageComicList").commit()
                        }
                    }
                }
            }
            .addOnFailureListener {
                Log.e(TAG, it.message ?: it.toString())
            }
            .addOnCompleteListener {
                // When the image is from CameraX analysis use case, must call image.close() on received
                // images when finished using them. Otherwise, new images may not be received or the camera
                // may stall.
                //imageProxy.close()

            }
    }

    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, GALLERY_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == GALLERY_REQUEST) {
            Glide.with(this).load(data?.data).into(binding.imgImageAfter)
            uriImage = data?.data
            processImageFromGallery(barcodeScanner!!, uriImage!!)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_CAMERA_REQUEST) {
            if (isCameraPermissionGranted()) {
                startCamera()
            } else {
                Log.e(TAG, "no camera permission")
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

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val PERMISSION_CAMERA_REQUEST = 1
        private const val GALLERY_REQUEST = 2
    }
}