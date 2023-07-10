package com.mitsuki.qrcodefilereciver.task

import com.mitsuki.qrcodefilereciver.DataCollectEvent
import com.mitsuki.qrcodefilereciver.clear
import com.mitsuki.qrcodefilereciver.file.FileTools
import com.mitsuki.qrcodefilereciver.toInt32
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock

class DataCollector(
    private val fileTools: FileTools, private val event: DataCollectEvent
) {
    private val readyMutex = Mutex()
    private val completeMutex = Mutex()
    private var isReady: Boolean = false
    private var isCompleted: Boolean = false

    private lateinit var fileName: String //文件名
    private var fileLength: Long = -1 //文件长度
    private var blockSize: Long = -1 //单块大小
    private var lastBlockIndex: Int = -1 //块数量

    private lateinit var dataCacheIndex: Array<Pair<ReentrantReadWriteLock, File?>>

    fun collect(raw: ByteArray) {
        val blockIndex = raw.toInt32(0)
        if (blockIndex == 0) {
            runBlocking {
                readyMutex.withLock {
                    if (isReady) return@withLock
                    try {
                        val data = raw.let { JSONObject(String(it.sliceArray(8 until it.size))) }
                        fileName = data.getString("fileName")
                        fileLength = data.getLong("fileLength")
                        blockSize = data.getLong("blockSize")
                        lastBlockIndex = data.getInt("lastBlockIndex")
                        dataCacheIndex =
                            Array(lastBlockIndex) { ReentrantReadWriteLock() to null }
                        isReady = true
                        event.onReady(fileName, fileLength, lastBlockIndex)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else {
            if (isReady) {
                if (isComplete()) {
                    //数据块全部ok
                    if (!isCompleted) {
                        runBlocking {
                            completeMutex.withLock {
                                if (!isCompleted) {
                                    event.onComplete()
                                    isCompleted = true
                                }
                            }
                        }
                    }

                } else {
//                    val blockOffset = raw.toInt32(4)
                    if (blockIndex > 0) {
                        dataCacheIndex[blockIndex - 1].also { dataNode ->
                            dataNode.first.writeLock().lock()
                            try {
                                val newFile = fileTools.saveChip(
                                    raw.let { it.sliceArray(8 until it.size) },
                                    fileName,
                                    blockIndex - 1
                                )
                                dataCacheIndex[blockIndex - 1] = dataNode.first to newFile
                            } finally {
                                dataNode.first.writeLock().unlock()
                            }
                        }

                        event.onBlock(dataCacheIndex.mapIndexed { index, pair -> if (pair.second == null) "${index + 1}" else "✓" }
                            .joinToString())
                    }
                }
            }
        }
    }

    private fun isComplete(): Boolean {
        return dataCacheIndex.filter { it.second != null }.size == lastBlockIndex
    }

    fun mergeData(): File {
        return fileTools.tempFile(fileName).also {
            it.outputStream().use { fileOutputStream ->
                fileOutputStream.channel.use { outputChannel ->
                    dataCacheIndex.forEachIndexed { index, fileNode ->
                        fileNode.second?.inputStream()?.use { fileInputStream ->
                            fileInputStream.channel.use { inputChannel ->
                                inputChannel.transferTo(
                                    0, inputChannel.size(), outputChannel
                                )
                            }
                        }
                        fileNode.second?.clear()
                    }
                }
            }
        }
    }
}