package com.mitsuki.qrcodefilereciver.file

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.result.contract.ActivityResultContract
import java.io.File

class ExportFileActivityResultContract : ActivityResultContract<File, Pair<File, Uri?>>() {

    private lateinit var tFile: File

    override fun createIntent(context: Context, input: File): Intent {
        tFile = input

        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(input.extension) ?: "*/*"
            putExtra(Intent.EXTRA_TITLE, input.name)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Pair<File, Uri?> {
        return tFile to (if (resultCode == Activity.RESULT_OK) intent?.data else null)
    }
}