package com.mitsuki.qrcodefilereciver

interface DataCollectEvent {
    fun onReady(fileName: String, fileLength: Long, blockCount: Int)
    fun onBlock( blockMap: String)

    fun onComplete()
}