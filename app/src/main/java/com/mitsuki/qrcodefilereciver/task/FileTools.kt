package com.mitsuki.qrcodefilereciver.task

import android.content.Context
import android.util.Log
import com.mitsuki.qrcodefilereciver.clear
import com.mitsuki.qrcodefilereciver.ensureDir
import java.io.File

class FileTools(context: Context) {
    private val cacheDir: File = context.cacheDir

    fun saveChip(raw: ByteArray, name: String, index: Int): File {
        return chipFile(name, index).apply {
            writeBytes(raw)
        }
    }

    fun chipFile(name: String, index: Int): File {
        val saveFile = File(cacheDir, "file_chip${File.separator}$name${File.separator}$index")
        saveFile.parentFile?.ensureDir()
        saveFile.clear()
        return saveFile
    }

    fun tempFile(name: String): File {
        val saveFile = File(cacheDir, "file_merge${File.separator}$name")
        saveFile.parentFile?.ensureDir()
        saveFile.clear()
        return saveFile
    }
}