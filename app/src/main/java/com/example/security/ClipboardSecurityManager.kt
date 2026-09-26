package com.example.security

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Background manager and observer that automatically clears the system clipboard
 * after 30 seconds when a user copies a password or sensitive text to prevent data exposure.
 */
object ClipboardSecurityManager {
    const val SENSITIVE_LABEL = "Vault Password"
    const val TIMEOUT_MS = 30_000L // 30 seconds

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var clearJob: Job? = null
    private var lastCopiedText: String? = null

    private const val PREFS_NAME = "vault_settings_prefs"
    private const val KEY_AUTO_CLEAR = "pref_auto_clear_clipboard"

    fun isAutoClearEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_CLEAR, false) // Default to false so data is never removed automatically
    }

    fun setAutoClearEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_CLEAR, enabled).apply()
        if (!enabled) {
            clearJob?.cancel()
            clearJob = null
        }
    }

    /**
     * Copies a password to the system clipboard, marks it as sensitive (Android 13+),
     * and only schedules auto-clearing if explicitly enabled by the user in Settings.
     */
    fun copyPassword(
        context: Context,
        password: String,
        customLabel: String = SENSITIVE_LABEL,
        forceAutoClear: Boolean? = null
    ) {
        val appContext = context.applicationContext
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return

        val clip = ClipData.newPlainText(customLabel, password)
        // Mark as sensitive on Android 13+ (API 33+) so system overlays do not show the password
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }

        clipboard.setPrimaryClip(clip)
        lastCopiedText = password

        val shouldAutoClear = forceAutoClear ?: isAutoClearEnabled(appContext)

        if (shouldAutoClear) {
            Toast.makeText(appContext, "Copied (clears in 30s)", Toast.LENGTH_SHORT).show()
            // Cancel previous timer and schedule new 30-second background clearing job
            clearJob?.cancel()
            clearJob = scope.launch {
                delay(TIMEOUT_MS)
                clearIfSensitive(appContext)
            }
        } else {
            Toast.makeText(appContext, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Clears the clipboard if it still contains the sensitive data copied by the app.
     */
    fun clearIfSensitive(context: Context) {
        try {
            val appContext = context.applicationContext
            val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return

            val description = clipboard.primaryClipDescription
            val isOurLabel = description?.label?.toString()?.let { label ->
                label == SENSITIVE_LABEL ||
                        label == "Vault Password" ||
                        label == "Generated Password" ||
                        label == "Password" ||
                        label == "Secure Notes"
            } ?: false

            val currentText = try {
                if (clipboard.hasPrimaryClip()) {
                    clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                } else null
            } catch (_: Exception) {
                null
            }

            val shouldClear = isOurLabel || (currentText != null && currentText == lastCopiedText)

            if (shouldClear) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    clipboard.clearPrimaryClip()
                } else {
                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                }
                lastCopiedText = null
                Toast.makeText(appContext, "Clipboard cleared for security", Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {
            // Safe fallback
        }
    }

    /**
     * Immediately clears sensitive clipboard data if the app is locked or backgrounded.
     */
    fun onAppLocked(context: Context) {
        if (isAutoClearEnabled(context)) {
            clearJob?.cancel()
            clearJob = null
            clearIfSensitive(context)
        }
    }
}
