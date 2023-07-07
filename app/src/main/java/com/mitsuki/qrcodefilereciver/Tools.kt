package com.mitsuki.qrcodefilereciver

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder


fun ByteArray.toInt32(index: Int): Int {
    return ByteBuffer.wrap(this, index, 4).order(ByteOrder.LITTLE_ENDIAN).int
}

fun File.ensureDir() {
    if (exists() && isFile) {
        delete()
        mkdirs()
    } else {
        mkdirs()
    }
}

fun File.clear() {
    if (exists()) {
        if (isDirectory) {
            listFiles()?.forEach { it.clear() }
        }
        delete()
    }
}
