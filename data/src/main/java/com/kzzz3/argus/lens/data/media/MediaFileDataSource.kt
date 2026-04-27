package com.kzzz3.argus.lens.data.media

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

internal fun interface MediaFileDataSource {
    @Throws(IOException::class)
    fun saveDownloadedAttachment(
        attachmentId: String,
        fileName: String,
        content: InputStream,
    ): String
}

internal class AndroidMediaFileDataSource(
    private val context: Context,
) : MediaFileDataSource {
    override fun saveDownloadedAttachment(
        attachmentId: String,
        fileName: String,
        content: InputStream,
    ): String {
        val downloadsDir = (context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir).also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
        val safeFileName = sanitizeMediaFileName(fileName, attachmentId)
        val targetFile = File(downloadsDir, safeFileName)

        FileOutputStream(targetFile).use { output ->
            content.copyTo(output)
        }

        return targetFile.absolutePath
    }
}
