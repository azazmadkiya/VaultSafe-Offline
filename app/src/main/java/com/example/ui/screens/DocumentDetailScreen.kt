package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.*
import com.example.security.ClipboardSecurityManager
import com.example.viewmodel.VaultViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    viewModel: VaultViewModel,
    docId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToPdfViewer: (String) -> Unit = {},
    onNavigateToImageViewer: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var item by remember { mutableStateOf<DecryptedPasswordItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isMasked by rememberSaveable { mutableStateOf(false) }
    var fullScreenPhotoUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(docId) {
        item = viewModel.getItemById(docId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.title ?: "Document Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigateToEdit(docId) },
                        modifier = Modifier.testTag("edit_document_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Document")
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.testTag("delete_document_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Document", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (item == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val docItem = item!!
            val docType = DocumentType.fromString(docItem.url.ifEmpty { docItem.title })
            val frontPhoto = docItem.getFrontImageUri()
            val backPhoto = docItem.getBackImageUri()

            val headerGradient = when (docType) {
                DocumentType.AADHAAR -> listOf(Color(0xFFE65100).copy(alpha = 0.15f), Color(0xFF2E7D32).copy(alpha = 0.15f))
                DocumentType.PAN -> listOf(Color(0xFF0277BD).copy(alpha = 0.18f), Color(0xFF01579B).copy(alpha = 0.10f))
                DocumentType.DRIVING_LICENSE -> listOf(Color(0xFFF57F17).copy(alpha = 0.18f), Color(0xFFE65100).copy(alpha = 0.10f))
                DocumentType.PASSPORT -> listOf(Color(0xFF1A237E).copy(alpha = 0.18f), Color(0xFF283593).copy(alpha = 0.10f))
                else -> listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Official Document Style Card Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(headerGradient))
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = docType.displayName.uppercase(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "100% OFFLINE SECURE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Document Number with Mask & Copy
                            Column {
                                Text(
                                    text = "DOCUMENT NUMBER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val displayNum = if (isMasked) maskDocumentNumber(docItem.username, docType) else docItem.username
                                    Text(
                                        text = displayNum,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                        letterSpacing = 1.2.sp
                                    )
                                    IconButton(onClick = { isMasked = !isMasked }) {
                                        Icon(
                                            imageVector = if (isMasked) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle Mask"
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            ClipboardSecurityManager.copyPassword(context, docItem.username, "${docType.displayName} Number")
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Number",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // Holder Name
                            if (docItem.password.isNotBlank()) {
                                Column {
                                    Text(
                                        text = "NAME ON DOCUMENT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = docItem.password,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // Document Photos Section
                if (frontPhoto.isNotBlank() || backPhoto.isNotBlank()) {
                    Text(
                        text = "Document Photos (Tap to Enlarge)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (frontPhoto.isNotBlank()) {
                        DocumentPhotoCard(
                            label = "Front Side Photo",
                            uri = frontPhoto,
                            onEnlargeClick = {
                                if (frontPhoto.lowercase().contains(".pdf")) {
                                    onNavigateToPdfViewer(java.net.URLEncoder.encode(frontPhoto, "UTF-8"))
                                } else {
                                    onNavigateToImageViewer(
                                        java.net.URLEncoder.encode(frontPhoto, "UTF-8"),
                                        java.net.URLEncoder.encode("Front Side Photo", "UTF-8")
                                    )
                                }
                            }
                        )
                    }

                    if (backPhoto.isNotBlank()) {
                        DocumentPhotoCard(
                            label = "Back Side Photo",
                            uri = backPhoto,
                            onEnlargeClick = {
                                if (backPhoto.lowercase().contains(".pdf")) {
                                    onNavigateToPdfViewer(java.net.URLEncoder.encode(backPhoto, "UTF-8"))
                                } else {
                                    onNavigateToImageViewer(
                                        java.net.URLEncoder.encode(backPhoto, "UTF-8"),
                                        java.net.URLEncoder.encode("Back Side Photo", "UTF-8")
                                    )
                                }
                            }
                        )
                    }
                }

                // 1-Page PDF Export Button (Front & Back Photos on One Page)
                if ((frontPhoto.isNotBlank() && !frontPhoto.lowercase().contains(".pdf")) ||
                    (backPhoto.isNotBlank() && !backPhoto.lowercase().contains(".pdf"))) {
                    Button(
                        onClick = {
                            val file = PdfExportManager.generateSinglePageCardPdf(
                                context = context,
                                documentTitle = docItem.title,
                                idNumber = docItem.username,
                                holderName = docItem.password,
                                frontImageUri = if (frontPhoto.lowercase().contains(".pdf")) "" else frontPhoto,
                                backImageUri = if (backPhoto.lowercase().contains(".pdf")) "" else backPhoto
                            )
                            if (file != null) {
                                Toast.makeText(context, "1-Page PDF generated successfully!", Toast.LENGTH_LONG).show()
                                PdfExportManager.openPdfFile(context, file)
                            } else {
                                Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("download_single_page_pdf_btn"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download Front & Back as 1-Page PDF", fontWeight = FontWeight.Bold)
                    }
                }

                // Attached PDF Document Open/Download Button
                val allUris = docItem.getAllImageUris()
                val pdfUris = allUris.filter { it.lowercase().contains(".pdf") }
                if (pdfUris.isNotEmpty()) {
                    pdfUris.forEach { pdfUri ->
                        OutlinedButton(
                            onClick = {
                                onNavigateToPdfViewer(java.net.URLEncoder.encode(pdfUri, "UTF-8"))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_attached_pdf_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open / Download Attached PDF", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Share PDF Button
                OutlinedButton(
                    onClick = {
                        if (pdfUris.isNotEmpty()) {
                            PdfExportManager.sharePdfUri(context, pdfUris[0])
                        } else {
                            val file = PdfExportManager.generateSinglePageCardPdf(
                                context = context,
                                documentTitle = docItem.title,
                                idNumber = docItem.username,
                                holderName = docItem.password,
                                frontImageUri = if (frontPhoto.lowercase().contains(".pdf")) "" else frontPhoto,
                                backImageUri = if (backPhoto.lowercase().contains(".pdf")) "" else backPhoto
                            )
                            if (file != null) {
                                PdfExportManager.sharePdfFile(context, file)
                            } else {
                                Toast.makeText(context, "Unable to share PDF", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("share_pdf_btn"),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share PDF", fontWeight = FontWeight.Bold)
                }

                // Address / Remarks Notes Card
                if (docItem.notes.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Address & Additional Notes",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = docItem.notes, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

        // Full Screen Photo Preview Dialog
        if (fullScreenPhotoUrl != null) {
            Dialog(
                onDismissRequest = { fullScreenPhotoUrl = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.92f))
                        .padding(16.dp)
                ) {
                    IconButton(
                        onClick = { fullScreenPhotoUrl = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 24.dp, end = 8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    AsyncImage(
                        model = fullScreenPhotoUrl,
                        contentDescription = "Full Screen Document Photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f)
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        // Delete Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Document") },
                text = { Text("Are you sure you want to permanently delete this document and all its attached photos from your secure vault?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            coroutineScope.launch {
                                item?.let { viewModel.deleteItem(it) }
                                onNavigateBack()
                            }
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun DocumentPhotoCard(
    label: String,
    uri: String,
    onEnlargeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEnlargeClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Enlarge",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
