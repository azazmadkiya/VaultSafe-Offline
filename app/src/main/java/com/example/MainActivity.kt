package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.security.ClipboardSecurityManager
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.VaultViewModel

class MainActivity : FragmentActivity() {
    private val viewModel: VaultViewModel by viewModels()
    private var backgroundTimestamp = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(
            androidx.lifecycle.LifecycleEventObserver { _, event ->
                when (event) {
                    androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                        backgroundTimestamp = System.currentTimeMillis()
                    }
                    androidx.lifecycle.Lifecycle.Event.ON_START -> {
                        val timeoutMinutes = viewModel.autoLockMinutes.value
                        if (timeoutMinutes > 0 && backgroundTimestamp > 0) {
                            val elapsed = System.currentTimeMillis() - backgroundTimestamp
                            if (elapsed > timeoutMinutes * 60_000L) {
                                viewModel.lockApp()
                                ClipboardSecurityManager.onAppLocked(this@MainActivity)
                            }
                        }
                        backgroundTimestamp = 0L
                    }
                    else -> {}
                }
            }
        )

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    var showSplash by remember { mutableStateOf(true) }
                    val isUnlocked by viewModel.isUnlocked.collectAsState()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route ?: "vault_list"

                    val mainRoutes = listOf("vault_list", "documents_vault", "password_generator", "settings")
                    val showBottomBar = mainRoutes.any { currentRoute.startsWith(it) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            bottomBar = {
                                if (showBottomBar && isUnlocked && !showSplash) {
                                    NavigationBar {
                                        NavigationBarItem(
                                            selected = currentRoute == "vault_list",
                                            onClick = {
                                                if (currentRoute != "vault_list") {
                                                    navController.navigate("vault_list") {
                                                        popUpTo("vault_list") { inclusive = true }
                                                    }
                                                }
                                            },
                                            icon = { Icon(Icons.Default.Lock, contentDescription = "Vault") },
                                            label = { Text("Vault") }
                                        )
                                        NavigationBarItem(
                                            selected = currentRoute == "documents_vault",
                                            onClick = {
                                                if (currentRoute != "documents_vault") {
                                                    navController.navigate("documents_vault") {
                                                        popUpTo("vault_list")
                                                    }
                                                }
                                            },
                                            icon = { Icon(Icons.Default.Badge, contentDescription = "Docs & IDs") },
                                            label = { Text("Docs & IDs") }
                                        )
                                        NavigationBarItem(
                                            selected = currentRoute == "password_generator",
                                            onClick = {
                                                if (currentRoute != "password_generator") {
                                                    navController.navigate("password_generator") {
                                                        popUpTo("vault_list")
                                                    }
                                                }
                                            },
                                            icon = { Icon(Icons.Default.VpnKey, contentDescription = "Generator") },
                                            label = { Text("Generator") }
                                        )
                                        NavigationBarItem(
                                            selected = currentRoute == "settings",
                                            onClick = {
                                                if (currentRoute != "settings") {
                                                    navController.navigate("settings") {
                                                        popUpTo("vault_list")
                                                    }
                                                }
                                            },
                                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                            label = { Text("Settings") }
                                        )
                                    }
                                }
                            }
                        ) { scaffoldPadding ->
                            NavHost(
                                navController = navController,
                                startDestination = "vault_list",
                                modifier = Modifier.padding(scaffoldPadding)
                            ) {
                                composable("vault_list") {
                                    VaultListScreen(
                                        viewModel = viewModel,
                                        onNavigateToAddItem = { navController.navigate("add_edit_item?itemId=0") },
                                        onNavigateToDetail = { id -> navController.navigate("item_detail/$id") },
                                        onNavigateToGenerator = { navController.navigate("password_generator") },
                                        onNavigateToSettings = { navController.navigate("settings") },
                                        onNavigateToDocuments = { navController.navigate("documents_vault") },
                                        onLockApp = {
                                            viewModel.lockApp()
                                            ClipboardSecurityManager.onAppLocked(this@MainActivity)
                                        }
                                    )
                                }
                                composable("documents_vault") {
                                    DocumentVaultScreen(
                                        viewModel = viewModel,
                                        onNavigateToAddDoc = { preset -> navController.navigate("add_document?presetType=$preset") },
                                        onNavigateToDocDetail = { id -> navController.navigate("document_detail/$id") },
                                        onLockApp = {
                                            viewModel.lockApp()
                                            ClipboardSecurityManager.onAppLocked(this@MainActivity)
                                        }
                                    )
                                }
                                composable(
                                    route = "add_document?docId={docId}&presetType={presetType}",
                                    arguments = listOf(
                                        navArgument("docId") { type = NavType.LongType; defaultValue = 0L },
                                        navArgument("presetType") { type = NavType.StringType; defaultValue = "Aadhaar Card" }
                                    )
                                ) { backStackEntry ->
                                    val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
                                    val presetType = backStackEntry.arguments?.getString("presetType") ?: "Aadhaar Card"
                                    AddDocumentScreen(
                                        viewModel = viewModel,
                                        docId = docId,
                                        initialDocType = presetType,
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                                composable(
                                    route = "document_detail/{docId}",
                                    arguments = listOf(navArgument("docId") { type = NavType.LongType })
                                ) { backStackEntry ->
                                    val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
                                    DocumentDetailScreen(
                                        viewModel = viewModel,
                                        docId = docId,
                                        onNavigateBack = { navController.popBackStack() },
                                        onNavigateToEdit = { id -> navController.navigate("add_document?docId=$id") },
                                        onNavigateToPdfViewer = { encodedUrl -> navController.navigate("pdf_viewer?pdfUrl=$encodedUrl") },
                                        onNavigateToImageViewer = { encodedUrl, title -> navController.navigate("image_viewer?imageUrl=$encodedUrl&imageTitle=$title") }
                                    )
                                }
                                composable(
                                    route = "add_edit_item?itemId={itemId}",
                                    arguments = listOf(navArgument("itemId") { type = NavType.LongType; defaultValue = 0L })
                                ) { backStackEntry ->
                                    val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
                                    AddEditItemScreen(
                                        viewModel = viewModel,
                                        itemId = itemId,
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                                composable(
                                    route = "item_detail/{itemId}",
                                    arguments = listOf(navArgument("itemId") { type = NavType.LongType })
                                ) { backStackEntry ->
                                    val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
                                    ItemDetailScreen(
                                        viewModel = viewModel,
                                        itemId = itemId,
                                        onNavigateBack = { navController.popBackStack() },
                                        onNavigateToEdit = { id -> navController.navigate("add_edit_item?itemId=$id") },
                                        onNavigateToPdfViewer = { encodedUrl -> navController.navigate("pdf_viewer?pdfUrl=$encodedUrl") },
                                        onNavigateToImageViewer = { encodedUrl, title -> navController.navigate("image_viewer?imageUrl=$encodedUrl&imageTitle=$title") }
                                    )
                                }
                                composable(
                                    route = "pdf_viewer?pdfUrl={pdfUrl}",
                                    arguments = listOf(navArgument("pdfUrl") { type = NavType.StringType; defaultValue = "" })
                                ) { backStackEntry ->
                                    val pdfUrl = backStackEntry.arguments?.getString("pdfUrl") ?: ""
                                    PdfViewerScreen(
                                        pdfUriString = java.net.URLDecoder.decode(pdfUrl, "UTF-8"),
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                                composable(
                                    route = "image_viewer?imageUrl={imageUrl}&imageTitle={imageTitle}",
                                    arguments = listOf(
                                        navArgument("imageUrl") { type = NavType.StringType; defaultValue = "" },
                                        navArgument("imageTitle") { type = NavType.StringType; defaultValue = "Document Image" }
                                    )
                                ) { backStackEntry ->
                                    val imageUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""
                                    val imageTitle = backStackEntry.arguments?.getString("imageTitle") ?: "Document Image"
                                    ImageViewerScreen(
                                        imageUriString = java.net.URLDecoder.decode(imageUrl, "UTF-8"),
                                        imageTitle = java.net.URLDecoder.decode(imageTitle, "UTF-8"),
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                                composable("password_generator") {
                                    PasswordGeneratorScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                                composable("settings") {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }

                        if (!isUnlocked) {
                            UnlockScreen(
                                onUnlockSuccess = { viewModel.unlockApp() }
                            )
                        }

                        if (showSplash) {
                            SplashScreen(onTimeout = { showSplash = false })
                        }
                    }
                }
            }
        }
    }
}

