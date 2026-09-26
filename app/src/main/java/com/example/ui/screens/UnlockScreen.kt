package com.example.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

@Composable
fun UnlockScreen(
    onUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showFallbackDialog by remember { mutableStateOf(false) }
    var fallbackReason by remember { mutableStateOf("") }
    var showPinInput by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }

    fun getBestAuthenticators(biometricManager: BiometricManager): Int {
        val strongWithDevice = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (biometricManager.canAuthenticate(strongWithDevice) == BiometricManager.BIOMETRIC_SUCCESS) {
            return strongWithDevice
        }
        val weakWithDevice = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (biometricManager.canAuthenticate(weakWithDevice) == BiometricManager.BIOMETRIC_SUCCESS) {
            return weakWithDevice
        }
        val strongOnly = BiometricManager.Authenticators.BIOMETRIC_STRONG
        if (biometricManager.canAuthenticate(strongOnly) == BiometricManager.BIOMETRIC_SUCCESS) {
            return strongOnly
        }
        val weakOnly = BiometricManager.Authenticators.BIOMETRIC_WEAK
        if (biometricManager.canAuthenticate(weakOnly) == BiometricManager.BIOMETRIC_SUCCESS) {
            return weakOnly
        }
        val deviceOnly = BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (biometricManager.canAuthenticate(deviceOnly) == BiometricManager.BIOMETRIC_SUCCESS) {
            return deviceOnly
        }
        return 0
    }

    fun triggerBiometricPrompt(showFallbackIfUnavailable: Boolean = true) {
        errorMessage = null
        val activity = context.findFragmentActivity()
        if (activity == null) {
            errorMessage = "Unable to initialize biometric prompt (Activity context unavailable)."
            showPinInput = true
            return
        }

        val biometricManager = BiometricManager.from(context)
        val authenticators = getBestAuthenticators(biometricManager)

        if (authenticators == 0) {
            val checkResult = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            val reason = when (checkResult) {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                    "No fingerprint or screen lock enrolled on this device. Please use PIN option."
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                    "No biometric hardware detected on this device. Use PIN option."
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                    "Biometric hardware unavailable. Please use PIN option."
                else ->
                    "Biometrics not configured. Please use PIN option."
            }
            // Automatically switch to PIN entry if biometric sensor is not available/enrolled
            showPinInput = true
            if (showFallbackIfUnavailable) {
                fallbackReason = reason
                showFallbackDialog = true
            } else {
                errorMessage = reason
            }
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    errorMessage = null
                    onUnlockSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) {
                        if (errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                            errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT
                        ) {
                            showPinInput = true
                            fallbackReason = errString.toString()
                            showFallbackDialog = true
                        } else {
                            errorMessage = errString.toString()
                        }
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    errorMessage = "Biometric not recognized. Try PIN option."
                }
            }
        )

        val promptBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Secure Vault")
            .setSubtitle("Authenticate to access your encrypted passwords")

        val hasDeviceCredential = (authenticators and BiometricManager.Authenticators.DEVICE_CREDENTIAL) != 0
        if (hasDeviceCredential) {
            promptBuilder.setAllowedAuthenticators(authenticators)
        } else {
            promptBuilder.setAllowedAuthenticators(authenticators)
            promptBuilder.setNegativeButtonText("Cancel")
        }

        try {
            biometricPrompt.authenticate(promptBuilder.build())
        } catch (e: Exception) {
            showPinInput = true
            if (showFallbackIfUnavailable) {
                fallbackReason = e.localizedMessage ?: "Biometric prompt error"
                showFallbackDialog = true
            } else {
                errorMessage = "Biometric error: ${e.localizedMessage}"
            }
        }
    }

    LaunchedEffect(Unit) {
        val biometricManager = BiometricManager.from(context)
        val authenticators = getBestAuthenticators(biometricManager)
        if (authenticators == 0) {
            // No biometric sensor / enrollment -> show PIN option directly
            showPinInput = true
        } else {
            triggerBiometricPrompt(showFallbackIfUnavailable = false)
        }
    }

    if (showFallbackDialog) {
        AlertDialog(
            onDismissRequest = { showFallbackDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Biometrics Unavailable",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        text = fallbackReason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Please enter your 4-digit Master PIN to unlock your vault.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFallbackDialog = false
                        showPinInput = true
                    },
                    modifier = Modifier.testTag("dialog_use_pin_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFallbackDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (showPinInput) Icons.Default.Key else Icons.Default.Lock,
                    contentDescription = "Vault Locked",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (showPinInput) "Enter Master PIN" else "Secure Vault",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (showPinInput) "Default PIN is 1234" else "End-to-End Encrypted • 100% Offline • Zero Internet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (showPinInput) {
                // PIN Dot Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Numeric Keypad
                val buttons = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "DEL")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    buttons.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth(0.85.dp.value.coerceAtLeast(0.8f))
                        ) {
                            row.forEach { label ->
                                if (label.isEmpty()) {
                                    Spacer(modifier = Modifier.size(72.dp))
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            errorMessage = null
                                            if (label == "DEL") {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                }
                                            } else {
                                                if (enteredPin.length < 4) {
                                                    val newPin = enteredPin + label
                                                    enteredPin = newPin
                                                    if (newPin.length == 4) {
                                                        if (newPin == "1234") {
                                                            onUnlockSuccess()
                                                        } else {
                                                            errorMessage = "Incorrect PIN (Default: 1234)"
                                                            enteredPin = ""
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(72.dp)
                                            .testTag("pin_key_$label"),
                                        shape = CircleShape,
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        if (label == "DEL") {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        } else {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = {
                        showPinInput = false
                        enteredPin = ""
                        errorMessage = null
                        triggerBiometricPrompt(showFallbackIfUnavailable = false)
                    },
                    modifier = Modifier.testTag("switch_to_biometric_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use Biometrics instead")
                }

            } else {
                Button(
                    onClick = { triggerBiometricPrompt(showFallbackIfUnavailable = true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("unlock_biometric_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Unlock with Biometrics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        showPinInput = true
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("unlock_pin_option_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Use 4-Digit PIN",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}
