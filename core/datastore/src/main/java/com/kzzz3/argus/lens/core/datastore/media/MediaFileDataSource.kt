package com.kzzz3.argus.lens.core.datastore.media

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

fun interface MediaFileDataSource {
    @Throws(IOException::class)
    fun saveDownloadedAttachment(
        attachmentId: String,
        fileName: String,
        content: InputStream,
    ): String
}

class AndroidMediaFileDataSource(
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

fun sanitizeMediaFileName(
    fileName: String,
    attachmentId: String,
): String {
    val sanitized = fileName
        .toSafeMediaFileSegment()
    val safeAttachmentId = attachmentId.toSafeMediaFileSegment()
    return sanitized.ifBlank {
        safeAttachmentId.ifBlank { "attachment" }.let { "attachment-$it" }
    }
}

private fun String.toSafeMediaFileSegment(): String {
    return trim()
        .replace('\\', '_')
        .replace('/', '_')
        .replace(Regex("[^a-zA-Z0-9._-]"), "_")
        .replace(Regex("^[._]+"), "")
        .replace(Regex("_+"), "_")
}
