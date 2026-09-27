package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "VaultSafe Offline Privacy Policy",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("privacy_policy_title")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Last updated: September 2026",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                PolicySection(
                    title = "1. Introduction",
                    content = "VaultSafe Offline (\"we\", \"our\", or \"app\") is designed with a strict privacy-first and security-first architecture. We believe your sensitive credentials, secure notes, and personal documents belong entirely to you and should never leave your device."
                )
            }

            item {
                PolicySection(
                    title = "2. Local-Only Storage",
                    content = "All data stored within VaultSafe Offline—including passwords, notes, categories, and documents—is maintained exclusively on your local device storage using a secure, encrypted local database (Room/SQLite). Your data is never synced to any cloud servers, remote databases, or third-party storage providers."
                )
            }

            item {
                PolicySection(
                    title = "3. Absence of Internet Permissions",
                    content = "VaultSafe Offline is engineered as a 100% offline application. The app does not request or utilize internet connectivity permissions (android.permission.INTERNET). Because the app lacks internet access capabilities, it is physically impossible for any data, diagnostics, or telemetry to be transmitted over the internet to us or any third party."
                )
            }

            item {
                PolicySection(
                    title = "4. Encryption & Security",
                    content = "Your master password and encryption keys are protected using industry-standard cryptographic practices (such as Android Keystore and strong key derivation functions). Only you hold the key to unlock your vault."
                )
            }

            item {
                PolicySection(
                    title = "5. Third-Party Services & Analytics",
                    content = "Because VaultSafe Offline operates completely offline without internet connectivity, we do not utilize third-party analytics SDKs, advertising networks, tracking pixels, or remote crash reporting services."
                )
            }

            item {
                PolicySection(
                    title = "6. Changes to This Privacy Policy",
                    content = "We may update our Privacy Policy from time to time. Any changes will be reflected directly within this screen in future app updates. Since all data remains locally on your device, your privacy is continuously safeguarded under these terms."
                )
            }

            item {
                PolicySection(
                    title = "7. Contact Us",
                    content = "If you have any questions or concerns regarding this Privacy Policy or the security practices of VaultSafe Offline, please review our repository documentation or reach out via our support channels."
                )
            }
        }
    }
}

@Composable
fun PolicySection(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}
