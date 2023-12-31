package com.mitsuki.qrcodefilereciver

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.mitsuki.qrcodefilereciver.databinding.ActivityMainBinding
import com.mitsuki.qrcodefilereciver.file.ExportFileActivityResultContract
import com.mitsuki.qrcodefilereciver.qrcode.BarcodeAnalysis
import com.mitsuki.qrcodefilereciver.qrcode.CodeMark
import com.mitsuki.qrcodefilereciver.task.CodeDataHandler
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), DataCollectEvent {

    companion object {
        private val REQUIRED_PERMISSIONS = mutableListOf(
            android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    private val previewRect = Rect()

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private val codeDataHandler: CodeDataHandler = CodeDataHandler(this, this)

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value) permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(
                    baseContext, "Permission request denied", Toast.LENGTH_SHORT
                ).show()
            } else {
                startCamera()
            }
        }

    private val fileExportLauncher = registerForActivityResult(ExportFileActivityResultContract()) {
        Thread {
            it.second?.let { uri -> contentResolver.openOutputStream(uri) }?.use { outputStream ->
                it.first.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }?.also {
                runOnUiThread { Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show() }
            } ?: kotlin.run {
                runOnUiThread { Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show() }
            }

        }.start()
    }


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        binding.dataReset.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.dataInfo.text = "Ready to transport"
                binding.dataProgress.text = ""
                binding.dataSave.isVisible = false
                codeDataHandler.createTask()
            } else {
                //这里不一定能清空，因为线程池中可能还存在任务
                binding.dataInfo.text = ""
                binding.dataProgress.text = ""
                binding.dataSave.isVisible = false
                codeDataHandler.clearChip()
            }
        }

        binding.dataSave.setOnClickListener {
            Toast.makeText(this, "Merge file", Toast.LENGTH_SHORT).show()
            Thread {
                codeDataHandler.obtainFile()?.also {
                    fileExportLauncher.launch(it)
                } ?: runOnUiThread {
                    Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
                }
            }.start()
        }
        binding.dataClearChip.setOnClickListener {
            Toast.makeText(this, "Clear chip file cache.", Toast.LENGTH_SHORT).show()
            codeDataHandler.clearChip()
        }

        binding.dataClearMerge.setOnClickListener {
            Toast.makeText(this, "Clear merge file cache.", Toast.LENGTH_SHORT).show()
            codeDataHandler.clearMerge()
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            activityResultLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({

            val imageAnalyzer = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(
                    cameraExecutor, BarcodeAnalysis(this::onMarkEvent, codeDataHandler)
                )
            }

            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
                .also { it.setSurfaceProvider(binding.barcodePreview.surfaceProvider) }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer
                )

            } catch (e: Exception) {
                e.printStackTrace()
            }

            preview.resolutionInfo?.apply { previewRect.set(cropRect) }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun onMarkEvent(data: CodeMark) {
        data.previewRect(previewRect.width(), previewRect.height())
        data.previewMapping()
        binding.barcodeIndicator.setMark(data)
    }

    @SuppressLint("SetTextI18n")
    override fun onReady(fileName: String, fileLength: Long, blockCount: Int) {
        runOnUiThread {
            binding.dataInfo.text =
                "File name: $fileName\nFile size: ${fileLength}KB\nTotal count: $blockCount"
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBlock(blockMap: String) {
        runOnUiThread { binding.dataProgress.text = blockMap }
    }

    override fun onComplete() {
        runOnUiThread { binding.dataSave.isVisible = true }
    }
}