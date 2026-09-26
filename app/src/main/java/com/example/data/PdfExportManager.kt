package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

object PdfExportManager {

    fun generateSinglePageCardPdf(
        context: Context,
        documentTitle: String,
        idNumber: String = "",
        holderName: String = "",
        frontImageUri: String,
        backImageUri: String
    ): File? {
        try {
            val validImageUris = listOf(frontImageUri, backImageUri).filter {
                it.isNotBlank() && !it.lowercase().contains(".pdf")
            }

            if (validImageUris.isEmpty()) {
                Toast.makeText(context, "No photos found to create PDF", Toast.LENGTH_SHORT).show()
                return null
            }

            val pdfDocument = PdfDocument()
            // Standard A4 dimensions in points: 595 x 842
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Pure clean white background — ONLY photos, NO text
            canvas.drawColor(Color.WHITE)

            val loadedBitmaps = validImageUris.mapNotNull { uriToBitmap(context, it) }
            if (loadedBitmaps.isEmpty()) {
                pdfDocument.close()
                Toast.makeText(context, "Could not load selected photos", Toast.LENGTH_SHORT).show()
                return null
            }

            drawOptimizedPrintableLayout(
                canvas = canvas,
                bitmaps = loadedBitmaps,
                pageWidth = pageWidth.toFloat(),
                pageHeight = pageHeight.toFloat()
            )

            pdfDocument.finishPage(page)

            val file = savePdfDocument(context, pdfDocument, documentTitle)
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            android.util.Log.e("PdfExportManager", "Failed to generate photos-only PDF", e)
            return null
        }
    }

    private fun getVaultSafeFolder(context: Context): File {
        // Try public phone storage first: /storage/emulated/0/Documents/VauldSafe
        try {
            val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "VauldSafe")
            if (!publicDir.exists()) {
                publicDir.mkdirs()
            }
            if (publicDir.exists() && publicDir.canWrite()) {
                return publicDir
            }
        } catch (e: Exception) {
            android.util.Log.w("PdfExportManager", "Cannot access public Documents/VauldSafe: ${e.message}")
        }

        // Fallback to app external files dir: Android/data/<pkg>/files/Documents/VauldSafe
        try {
            val extDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "VauldSafe")
            if (!extDir.exists()) {
                extDir.mkdirs()
            }
            if (extDir.exists()) {
                return extDir
            }
        } catch (e: Exception) {
            android.util.Log.w("PdfExportManager", "Cannot access app external Documents/VauldSafe: ${e.message}")
        }

        // Fallback to internal files
        val internalDir = File(context.filesDir, "VauldSafe")
        if (!internalDir.exists()) {
            internalDir.mkdirs()
        }
        return internalDir
    }

    private fun savePdfDocument(context: Context, pdfDocument: PdfDocument, title: String): File {
        val cleanTitle = title.replace(Regex("[^A-Za-z0-9]"), "_").ifBlank { "Secure_Document" }
        val fileName = "${cleanTitle}_${System.currentTimeMillis()}.pdf"
        val folder = getVaultSafeFolder(context)
        val file = File(folder, fileName)

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }

        // On Android 10+ (Q+), also register in MediaStore so it appears directly in the phone's Documents/VauldSafe storage
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/VauldSafe")
                }
                val contentResolver = context.contentResolver
                val contentUri = contentResolver.insert(android.provider.MediaStore.Files.getContentUri("external"), contentValues)
                if (contentUri != null) {
                    contentResolver.openOutputStream(contentUri)?.use { outStream ->
                        file.inputStream().use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("PdfExportManager", "MediaStore sync notice: ${e.message}")
            }
        }

        Toast.makeText(context, "PDF saved to phone storage: Documents/VauldSafe\n${file.name}", Toast.LENGTH_LONG).show()
        return file
    }

    private fun drawOptimizedPrintableLayout(
        canvas: Canvas,
        bitmaps: List<Bitmap>,
        pageWidth: Float,
        pageHeight: Float
    ) {
        val printPaint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        val borderPaint = Paint().apply {
            color = Color.parseColor("#CCCCCC")
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
            isAntiAlias = true
        }

        if (bitmaps.size == 1) {
            val bitmap = bitmaps[0]
            val ratio = bitmap.width.toFloat() / maxOf(1, bitmap.height).toFloat()

            // If it's a card (landscape), keep at professional print size (not bulky)
            val (maxWidth, maxHeight) = if (ratio >= 1.25f) {
                Pair(340f, 220f)
            } else {
                Pair(480f, 680f) // full document with printer safety margins
            }

            val scaled = scaleBitmapToFit(bitmap, maxWidth.toInt(), maxHeight.toInt())
            val x = (pageWidth - scaled.width) / 2f
            val y = (pageHeight - scaled.height) / 2f
            canvas.drawBitmap(scaled, x, y, printPaint)
            canvas.drawRect(x, y, x + scaled.width, y + scaled.height, borderPaint)
            return
        }

        // Exactly 2 images (e.g., Front & Back)
        val bm1 = bitmaps[0]
        val bm2 = bitmaps[1]
        val ratio1 = bm1.width.toFloat() / maxOf(1, bm1.height).toFloat()
        val ratio2 = bm2.width.toFloat() / maxOf(1, bm2.height).toFloat()

        // Case 1: Both images are landscape / ID cards (Aadhaar, Driving License, PAN, etc.)
        if (ratio1 >= 1.15f && ratio2 >= 1.15f) {
            // Standard professional photocopy/print size: 330pt width (~11.6 cm), matched for both
            val targetCardWidth = 330
            val maxCardHeight = 220
            val gap = 32f

            val scaled1 = scaleBitmapToFit(bm1, targetCardWidth, maxCardHeight)
            val scaled2 = scaleBitmapToFit(bm2, targetCardWidth, maxCardHeight)

            val totalHeight = scaled1.height + gap + scaled2.height
            val startY = (pageHeight - totalHeight) / 2f

            val x1 = (pageWidth - scaled1.width) / 2f
            canvas.drawBitmap(scaled1, x1, startY, printPaint)
            canvas.drawRect(x1, startY, x1 + scaled1.width, startY + scaled1.height, borderPaint)

            val startY2 = startY + scaled1.height + gap
            val x2 = (pageWidth - scaled2.width) / 2f
            canvas.drawBitmap(scaled2, x2, startY2, printPaint)
            canvas.drawRect(x2, startY2, x2 + scaled2.width, startY2 + scaled2.height, borderPaint)
        }
        // Case 2: Both are portrait/vertical documents (e.g. 2 vertical certificate pages)
        else if (ratio1 <= 0.88f && ratio2 <= 0.88f) {
            // Place side-by-side for optimal A4 2-up printable presentation
            val gap = 24f
            val targetWidth = 245
            val maxHeight = 360

            val scaled1 = scaleBitmapToFit(bm1, targetWidth, maxHeight)
            val scaled2 = scaleBitmapToFit(bm2, targetWidth, maxHeight)

            val totalWidth = scaled1.width + gap + scaled2.width
            val startX = (pageWidth - totalWidth) / 2f
            val y = (pageHeight - maxOf(scaled1.height, scaled2.height)) / 2f

            canvas.drawBitmap(scaled1, startX, y, printPaint)
            canvas.drawRect(startX, y, startX + scaled1.width, y + scaled1.height, borderPaint)

            val startX2 = startX + scaled1.width + gap
            canvas.drawBitmap(scaled2, startX2, y, printPaint)
            canvas.drawRect(startX2, y, startX2 + scaled2.width, y + scaled2.height, borderPaint)
        }
        // Case 3: Mixed orientations or square-ish photos
        else {
            val gap = 28f
            val maxWidth = 340
            val maxHeightPerItem = 280

            val scaled1 = scaleBitmapToFit(bm1, maxWidth, maxHeightPerItem)
            val scaled2 = scaleBitmapToFit(bm2, maxWidth, maxHeightPerItem)

            val totalHeight = scaled1.height + gap + scaled2.height
            val startY = (pageHeight - totalHeight) / 2f

            val x1 = (pageWidth - scaled1.width) / 2f
            canvas.drawBitmap(scaled1, x1, startY, printPaint)
            canvas.drawRect(x1, startY, x1 + scaled1.width, startY + scaled1.height, borderPaint)

            val startY2 = startY + scaled1.height + gap
            val x2 = (pageWidth - scaled2.width) / 2f
            canvas.drawBitmap(scaled2, x2, startY2, printPaint)
            canvas.drawRect(x2, startY2, x2 + scaled2.width, startY2 + scaled2.height, borderPaint)
        }
    }

    private fun uriToBitmap(context: Context, uriString: String): Bitmap? {
        return try {
            if (uriString.startsWith("/")) {
                BitmapFactory.decodeFile(uriString)
            } else {
                val uri = Uri.parse(uriString)
                if (uri.scheme == "file") {
                    val path = uri.path
                    if (path != null) BitmapFactory.decodeFile(path)
                    else context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                } else {
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PdfExportManager", "Error loading bitmap from $uriString", e)
            null
        }
    }

    private fun scaleBitmapToFit(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = maxOf(1, bitmap.width)
        val height = maxOf(1, bitmap.height)
        val bitmapRatio = width.toFloat() / height.toFloat()
        val maxRatio = maxWidth.toFloat() / maxHeight.toFloat()

        var finalWidth = maxWidth
        var finalHeight = maxHeight
        if (bitmapRatio > maxRatio) {
            finalHeight = (maxWidth / bitmapRatio).toInt()
        } else {
            finalWidth = (maxHeight * bitmapRatio).toInt()
        }
        finalWidth = maxOf(1, finalWidth)
        finalHeight = maxOf(1, finalHeight)
        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    fun openPdfFile(context: Context, file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                clipData = android.content.ClipData.newRawUri("PDF Document", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("PdfExportManager", "Error opening PDF file", e)
            Toast.makeText(context, "PDF saved to Documents/VauldSafe:\n${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }

    fun sharePdfFile(context: Context, file: File) {
        try {
            if (!file.exists()) {
                Toast.makeText(context, "PDF file not found", Toast.LENGTH_SHORT).show()
                return
            }
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = android.content.ClipData.newRawUri("PDF Document", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share PDF Document").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.util.Log.e("PdfExportManager", "Error sharing PDF file", e)
            Toast.makeText(context, "Unable to share PDF: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
        }
    }

    fun sharePdfUri(context: Context, uriString: String) {
        try {
            val parsedUri = Uri.parse(uriString)
            val fileToShare: File? = when {
                uriString.startsWith("file://") -> {
                    parsedUri.path?.let { File(it) }
                }
                uriString.startsWith("/") -> {
                    File(uriString)
                }
                else -> {
                    // If it's a content URI, create a temporary shareable file copy in cache to ensure FileProvider grants read permission
                    try {
                        val tempFile = File(context.cacheDir, "share_${System.currentTimeMillis()}.pdf")
                        context.contentResolver.openInputStream(parsedUri)?.use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (tempFile.exists() && tempFile.length() > 0) tempFile else null
                    } catch (e: Exception) {
                        android.util.Log.w("PdfExportManager", "Failed to cache content URI for sharing: ${e.message}")
                        null
                    }
                }
            }

            if (fileToShare != null && fileToShare.exists()) {
                sharePdfFile(context, fileToShare)
            } else {
                // Direct URI fallback
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, parsedUri)
                    clipData = android.content.ClipData.newRawUri("PDF Document", parsedUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Share PDF Document").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            android.util.Log.e("PdfExportManager", "Error sharing PDF URI", e)
            Toast.makeText(context, "Unable to share PDF: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
        }
    }
}
