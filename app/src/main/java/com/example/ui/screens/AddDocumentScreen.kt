package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.viewmodel.VaultViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDocumentScreen(
    viewModel: VaultViewModel,
    docId: Long = 0L,
    initialDocType: String = DocumentType.AADHAAR.displayName,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedType by rememberSaveable { mutableStateOf(DocumentType.fromString(initialDocType)) }
    var customTitle by rememberSaveable { mutableStateOf("") }
    var idNumber by rememberSaveable { mutableStateOf("") }
    var holderName by rememberSaveable { mutableStateOf("") }
    var dobOrExpiry by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var frontImageUri by rememberSaveable { mutableStateOf("") }
    var backImageUri by rememberSaveable { mutableStateOf("") }
    var isDataLoaded by rememberSaveable { mutableStateOf(false) }

    // Launcher for Front Photo
    val frontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val permanentUri = AttachmentStorageManager.saveToInternalStorage(context, uri)
            frontImageUri = permanentUri
        }
    }

    // Launcher for Back Photo
    val backPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val permanentUri = AttachmentStorageManager.saveToInternalStorage(context, uri)
            backImageUri = permanentUri
        }
    }

    var activeSlotForCamera by remember { mutableStateOf("front") }
    var tempCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            val permanentUri = AttachmentStorageManager.saveToInternalStorage(context, tempCameraUri!!)
            if (activeSlotForCamera == "front") {
                frontImageUri = permanentUri
            } else {
                backImageUri = permanentUri
            }
            android.widget.Toast.makeText(context, "Document page scanned successfully", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val photoFile = java.io.File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Error starting camera: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            android.widget.Toast.makeText(context, "Camera permission is required to scan documents", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher for PDF Document
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val permanentUri = AttachmentStorageManager.saveToInternalStorage(context, uri)
            if (frontImageUri.isBlank()) {
                frontImageUri = permanentUri
            } else {
                backImageUri = permanentUri
            }
            android.widget.Toast.makeText(context, "PDF Document attached successfully", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(docId) {
        if (docId > 0L && !isDataLoaded) {
            val item = viewModel.getItemById(docId)
            if (item != null) {
                selectedType = DocumentType.fromString(item.url.ifEmpty { item.title })
                customTitle = item.title
                idNumber = item.username
                holderName = item.password
                notes = item.notes
                frontImageUri = item.getFrontImageUri()
                backImageUri = item.getBackImageUri()
                isDataLoaded = true
            }
        }
    }

    val finalTitle = if (selectedType == DocumentType.OTHER) {
        customTitle.ifBlank { "Other Document" }
    } else {
        if (holderName.isNotBlank()) "${selectedType.displayName} ($holderName)" else selectedType.displayName
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (docId == 0L) "Upload ${selectedType.displayName}" else "Edit ${selectedType.displayName}",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Document Type Selector Chips
            Text(
                text = "Select Document Type",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(DocumentType.AADHAAR, DocumentType.PAN, DocumentType.DRIVING_LICENSE).forEach { docType ->
                    FilterChip(
                        selected = selectedType == docType,
                        onClick = { selectedType = docType },
                        label = { Text(docType.displayName, fontWeight = if (selectedType == docType) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(DocumentType.PASSPORT, DocumentType.VOTER_ID, DocumentType.OTHER).forEach { docType ->
                    FilterChip(
                        selected = selectedType == docType,
                        onClick = { selectedType = docType },
                        label = { Text(docType.displayName, fontWeight = if (selectedType == docType) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Document Info Card Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (selectedType) {
                        DocumentType.AADHAAR -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        DocumentType.PAN -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = selectedType.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedType.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Custom Title if "Other Document"
            if (selectedType == DocumentType.OTHER) {
                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    label = { Text("Document Name (e.g. Birth Certificate, RC Book, Visa)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("doc_custom_title_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // ID / Document Number Input (with auto formatting for Aadhaar & PAN)
            OutlinedTextField(
                value = idNumber,
                onValueChange = { input ->
                    idNumber = when (selectedType) {
                        DocumentType.AADHAAR -> formatAadhaarNumber(input)
                        DocumentType.PAN -> formatPanNumber(input)
                        else -> input
                    }
                },
                label = {
                    Text(
                        when (selectedType) {
                            DocumentType.AADHAAR -> "12-digit Aadhaar Number *"
                            DocumentType.PAN -> "10-character PAN Number *"
                            DocumentType.DRIVING_LICENSE -> "Driving License (DL) Number *"
                            DocumentType.PASSPORT -> "Passport Number *"
                            DocumentType.VOTER_ID -> "Voter ID / EPIC Number *"
                            DocumentType.OTHER -> "Document / Registration Number *"
                        }
                    )
                },
                placeholder = { Text(selectedType.idPlaceholder) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("doc_id_number_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = when (selectedType) {
                        DocumentType.AADHAAR -> KeyboardType.Number
                        else -> KeyboardType.Ascii
                    },
                    capitalization = KeyboardCapitalization.Characters
                ),
                leadingIcon = {
                    Icon(Icons.Default.Numbers, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            )

            // Full Name on Document
            OutlinedTextField(
                value = holderName,
                onValueChange = { holderName = it },
                label = { Text("Full Name on Document *") },
                placeholder = { Text("Enter name exactly as printed on card") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("doc_holder_name_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            )

            // DOB / Expiry Date (Optional)
            OutlinedTextField(
                value = dobOrExpiry,
                onValueChange = { dobOrExpiry = it },
                label = {
                    Text(
                        if (selectedType == DocumentType.DRIVING_LICENSE || selectedType == DocumentType.PASSPORT)
                            "Expiry Date / Valid Till (DD/MM/YYYY)"
                        else
                            "Date of Birth / Father's Name (Optional)"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            )

            // DOCUMENT PHOTO UPLOADS SECTION
            Text(
                text = "Document Photos / Scans",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Front Photo Upload Card
            DocumentPhotoSlot(
                title = "Front Side Photo",
                subtitle = "Front of ${selectedType.displayName} with photo and ID number",
                imageUri = frontImageUri,
                onScanCameraClick = {
                    activeSlotForCamera = "front"
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                },
                onGalleryClick = {
                    frontPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemoveClick = { frontImageUri = "" },
                testTag = "upload_front_photo_btn"
            )

            // Back Photo Upload Card (Crucial for Aadhaar address side, DL, etc.)
            DocumentPhotoSlot(
                title = "Back Side Photo ${if (selectedType.requiresBackPhoto) "(Recommended for Address)" else "(Optional)"}",
                subtitle = "Back of card containing address, QR code, or endorsements",
                imageUri = backImageUri,
                onScanCameraClick = {
                    activeSlotForCamera = "back"
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                },
                onGalleryClick = {
                    backPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemoveClick = { backImageUri = "" },
                testTag = "upload_back_photo_btn"
            )

            // PDF Upload Option Button
            OutlinedButton(
                onClick = { pdfPickerLauncher.launch("application/pdf") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_pdf_button"),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(14.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload PDF Document (e-Aadhaar PDF, Statement)", fontWeight = FontWeight.Bold)
            }

            // Additional Notes / Address
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Address / Additional Information / Remarks") },
                placeholder = { Text("e.g. Registered address, issuance authority, emergency notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .testTag("doc_notes_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            val combinedImageUri = when {
                frontImageUri.isNotBlank() && backImageUri.isNotBlank() -> "$frontImageUri|$backImageUri"
                frontImageUri.isNotBlank() -> frontImageUri
                backImageUri.isNotBlank() -> backImageUri
                else -> ""
            }

            val combinedPassword = if (dobOrExpiry.isNotBlank()) "$holderName (DOB/Exp: $dobOrExpiry)" else holderName

            val isFormValid = idNumber.isNotBlank() && holderName.isNotBlank()

            Button(
                onClick = {
                    if (isFormValid) {
                        coroutineScope.launch {
                            if (docId == 0L) {
                                viewModel.insertItem(
                                    title = finalTitle,
                                    category = "Documents & IDs",
                                    username = idNumber,
                                    password = combinedPassword,
                                    notes = notes,
                                    url = selectedType.displayName,
                                    imageUri = combinedImageUri
                                ) {
                                    onNavigateBack()
                                }
                            } else {
                                val existingItem = viewModel.getItemById(docId)
                                val createdAt = existingItem?.createdAt ?: System.currentTimeMillis()
                                viewModel.updateItem(
                                    id = docId,
                                    title = finalTitle,
                                    category = "Documents & IDs",
                                    username = idNumber,
                                    password = combinedPassword,
                                    notes = notes,
                                    url = selectedType.displayName,
                                    imageUri = combinedImageUri,
                                    createdAt = createdAt
                                ) {
                                    onNavigateBack()
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_document_button"),
                shape = RoundedCornerShape(16.dp),
                enabled = isFormValid
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (docId == 0L) "Save Document Securely" else "Update Document",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DocumentPhotoSlot(
    title: String,
    subtitle: String,
    imageUri: String,
    onScanCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onRemoveClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (imageUri.isNotBlank()) {
                    IconButton(onClick = onRemoveClick) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete photo", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (imageUri.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onScanCameraClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${testTag}_retake"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retake Scan")
                    }
                    OutlinedButton(
                        onClick = onGalleryClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${testTag}_gallery"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onScanCameraClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(testTag),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan Camera")
                    }
                    OutlinedButton(
                        onClick = onGalleryClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${testTag}_gallery"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery")
                    }
                }
            }
        }
    }
}
