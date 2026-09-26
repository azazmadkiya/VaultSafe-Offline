package com.example.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Manages secure internal storage for vault attachments (Cards, IDs, Documents).
 * Copies selected photos directly into the app's private sandbox directory
 * so that Android system permission revocations and external file deletions
 * NEVER remove or revoke user document images.
 */
object AttachmentStorageManager {

    fun saveToInternalStorage(context: Context, sourceUri: Uri): String {
        return try {
            val attachmentsDir = File(context.filesDir, "vault_attachments").apply {
                if (!exists()) mkdirs()
            }
            val mimeType = context.contentResolver.getType(sourceUri)
            val isPdf = mimeType?.contains("pdf", ignoreCase = true) == true ||
                    sourceUri.toString().lowercase().contains(".pdf")
            val extension = if (isPdf) "pdf" else "jpg"

            val destinationFile = File(attachmentsDir, "doc_${System.currentTimeMillis()}.$extension")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destinationFile).toString()
        } catch (e: Exception) {
            sourceUri.toString()
        }
    }

    fun deleteAttachmentFile(uriString: String) {
        try {
            val uris = uriString.split("|").filter { it.isNotBlank() }
            for (itemUri in uris) {
                if (itemUri.startsWith("file://")) {
                    val path = Uri.parse(itemUri).path
                    if (path != null) {
                        val file = File(path)
                        if (file.exists() && file.absolutePath.contains("vault_attachments")) {
                            file.delete()
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Ignored
        }
    }
}
