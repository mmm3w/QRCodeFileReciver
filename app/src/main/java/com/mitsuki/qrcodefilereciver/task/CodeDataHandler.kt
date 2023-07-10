package com.mitsuki.qrcodefilereciver.task

import android.content.Context
import com.mitsuki.qrcodefilereciver.DataCollectEvent
import com.mitsuki.qrcodefilereciver.file.FileTools
import java.io.File
import java.util.concurrent.Executors

class CodeDataHandler(context: Context, private val event: DataCollectEvent) {

    private val executorsService = Executors.newCachedThreadPool()

    private var readyCollector: DataCollector? = null
    private val fileTools by lazy { FileTools(context) }

    fun postData(raw: ByteArray) {
        try {
            executorsService.execute { readyCollector?.collect(raw) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createTask() {
        readyCollector = DataCollector(fileTools, event)
    }

    fun clearTask() {
        readyCollector = null
    }


    fun obtainFile(): File? {
        return readyCollector?.mergeData()
    }

    fun clearChip() {
        Thread { fileTools.clearChip() }.start()
    }

    fun clearMerge() {
        Thread { fileTools.clearMerge() }.start()
    }

}