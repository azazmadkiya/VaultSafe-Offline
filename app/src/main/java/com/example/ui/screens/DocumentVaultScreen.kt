package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.viewmodel.VaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentVaultScreen(
    viewModel: VaultViewModel,
    onNavigateToAddDoc: (String) -> Unit,
    onNavigateToDocDetail: (Long) -> Unit,
    onLockApp: () -> Unit
) {
    val context = LocalContext.current
    val allItems by viewModel.filteredItems.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("All") }

    // Filter items that belong to Documents & IDs
    val documentItems = remember(allItems, searchQuery, selectedFilter) {
        allItems.filter { item ->
            val isDoc = item.category.equals("Documents & IDs", ignoreCase = true) ||
                    DocumentType.entries.any { item.title.contains(it.displayName, ignoreCase = true) || item.url.contains(it.displayName, ignoreCase = true) }
            if (!isDoc) return@filter false

            val matchesFilter = when (selectedFilter) {
                "All" -> true
                "Aadhaar" -> item.title.contains("Aadhaar", ignoreCase = true) || item.url.contains("Aadhaar", ignoreCase = true)
                "PAN" -> item.title.contains("PAN", ignoreCase = true) || item.url.contains("PAN", ignoreCase = true)
                "Driving License" -> item.title.contains("Driving", ignoreCase = true) || item.url.contains("Driving", ignoreCase = true) || item.title.contains("License", ignoreCase = true)
                "Passport" -> item.title.contains("Passport", ignoreCase = true) || item.url.contains("Passport", ignoreCase = true)
                "Voter ID" -> item.title.contains("Voter", ignoreCase = true) || item.url.contains("Voter", ignoreCase = true)
                else -> true
            }

            val matchesQuery = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.username.contains(searchQuery, ignoreCase = true) ||
                    item.password.contains(searchQuery, ignoreCase = true) ||
                    item.notes.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesQuery
        }
    }

    val filterOptions = listOf("All", "Aadhaar", "PAN", "Driving License", "Passport", "Voter ID")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Documents & IDs Wallet", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(
                        onClick = onLockApp,
                        modifier = Modifier.testTag("lock_docs_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock Vault")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToAddDoc(DocumentType.AADHAAR.displayName) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Upload Document", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("upload_document_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar for Documents
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_docs_bar"),
                placeholder = { Text("Search Aadhaar, PAN, Name, or ID number...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            // Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        leadingIcon = if (selectedFilter == filter) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            if (documentItems.isEmpty()) {
                // Empty State with quick presets
                EmptyDocumentsState(
                    onSelectPreset = { presetType -> onNavigateToAddDoc(presetType) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(documentItems, key = { it.id }) { docItem ->
                        DocumentItemCard(
                            item = docItem,
                            onClick = { onNavigateToDocDetail(docItem.id) },
                            onCopyNumber = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Document Number", docItem.username))
                                Toast.makeText(context, "Document Number copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentItemCard(
    item: DecryptedPasswordItem,
    onClick: () -> Unit,
    onCopyNumber: () -> Unit
) {
    val docType = DocumentType.fromString(item.url.ifEmpty { item.title })
    var isMasked by rememberSaveable { mutableStateOf(true) }

    val cardGradient = when (docType) {
        DocumentType.AADHAAR -> listOf(Color(0xFFE65100).copy(alpha = 0.12f), Color(0xFF2E7D32).copy(alpha = 0.12f))
        DocumentType.PAN -> listOf(Color(0xFF0277BD).copy(alpha = 0.15f), Color(0xFF01579B).copy(alpha = 0.08f))
        DocumentType.DRIVING_LICENSE -> listOf(Color(0xFFF57F17).copy(alpha = 0.15f), Color(0xFFE65100).copy(alpha = 0.08f))
        DocumentType.PASSPORT -> listOf(Color(0xFF1A237E).copy(alpha = 0.15f), Color(0xFF283593).copy(alpha = 0.08f))
        else -> listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    }

    val typeBadgeColor = when (docType) {
        DocumentType.AADHAAR -> Color(0xFFE65100)
        DocumentType.PAN -> Color(0xFF0277BD)
        DocumentType.DRIVING_LICENSE -> Color(0xFFF57F17)
        DocumentType.PASSPORT -> Color(0xFF1A237E)
        else -> MaterialTheme.colorScheme.primary
    }

    val frontPhoto = item.getFrontImageUri()
    val backPhoto = item.getBackImageUri()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("doc_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(cardGradient))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Header: Badge + Copy Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = typeBadgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = typeBadgeColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = docType.displayName.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = typeBadgeColor
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isMasked = !isMasked },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isMasked) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Mask",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onCopyNumber,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Number",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Document Number
                val displayNumber = if (isMasked) {
                    maskDocumentNumber(item.username, docType)
                } else {
                    item.username
                }

                Text(
                    text = displayNumber.ifEmpty { "No Document Number" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.5.sp
                )

                // Holder Name & Title
                Column {
                    Text(
                        text = item.password.ifEmpty { item.title },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.title != docType.displayName && item.title != item.password) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                // Photos Thumbnails Row
                if (frontPhoto.isNotBlank() || backPhoto.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (frontPhoto.isNotBlank()) {
                            ThumbnailCard(
                                label = "Front Photo",
                                uri = frontPhoto,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (backPhoto.isNotBlank()) {
                            ThumbnailCard(
                                label = "Back Photo",
                                uri = backPhoto,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThumbnailCard(label: String, uri: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = uri,
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 8.dp, bottomEnd = 0.dp),
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyDocumentsState(
    onSelectPreset: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Badge,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Digital Document Safe",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Store Aadhaar card, PAN card, Driving License, and other ID documents with front & back photo scans safely offline.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Quick Document Upload Options:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetUploadButton(
                icon = Icons.Default.CreditCard,
                title = "Upload Aadhaar Card",
                subtitle = "12-digit number + front & back address scans",
                onClick = { onSelectPreset(DocumentType.AADHAAR.displayName) }
            )
            PresetUploadButton(
                icon = Icons.Default.Badge,
                title = "Upload PAN Card",
                subtitle = "10-character PAN number + photo scan",
                onClick = { onSelectPreset(DocumentType.PAN.displayName) }
            )
            PresetUploadButton(
                icon = Icons.Default.DirectionsCar,
                title = "Upload Driving License",
                subtitle = "DL permit number + front/back scans",
                onClick = { onSelectPreset(DocumentType.DRIVING_LICENSE.displayName) }
            )
            PresetUploadButton(
                icon = Icons.Default.Article,
                title = "Upload Other Document",
                subtitle = "Passport, Voter ID, Certificates, RC book",
                onClick = { onSelectPreset(DocumentType.OTHER.displayName) }
            )
        }
    }
}

@Composable
fun PresetUploadButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
