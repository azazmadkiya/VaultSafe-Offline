package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DecryptedPasswordItem
import com.example.data.PasswordDatabase
import com.example.data.PasswordRepository
import com.example.security.CryptoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VaultViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PasswordDatabase.getDatabase(application)
    private val cryptoManager = CryptoManager()
    private val repository = PasswordRepository(database.passwordDao(), cryptoManager)

    private val prefs = application.getSharedPreferences("vault_settings_prefs", android.content.Context.MODE_PRIVATE)

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(prefs.getBoolean("pref_biometric", true))
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    // Default to false so data is never removed automatically from clipboard
    private val _autoClearClipboard = MutableStateFlow(prefs.getBoolean("pref_auto_clear_clipboard", false))
    val autoClearClipboard: StateFlow<Boolean> = _autoClearClipboard.asStateFlow()

    // Default auto-lock timeout: 5 minutes (or -1 for Never). Generous so user never loses data when switching apps
    private val _autoLockMinutes = MutableStateFlow(prefs.getInt("pref_auto_lock_minutes", 5))
    val autoLockMinutes: StateFlow<Int> = _autoLockMinutes.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val filteredItems: StateFlow<List<DecryptedPasswordItem>> = combine(
        repository.allItems,
        _searchQuery,
        _selectedCategory
    ) { items, query, category ->
        items.filter { item ->
            val matchesCategory = category == "All" || item.category.equals(category, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.username.contains(query, ignoreCase = true) ||
                    item.url.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun unlockApp() {
        _isUnlocked.value = true
    }

    fun lockApp() {
        _isUnlocked.value = false
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setBiometricEnabled(enabled: Boolean) {
        _biometricEnabled.value = enabled
        prefs.edit().putBoolean("pref_biometric", enabled).apply()
    }

    fun setAutoClearClipboard(enabled: Boolean) {
        _autoClearClipboard.value = enabled
        prefs.edit().putBoolean("pref_auto_clear_clipboard", enabled).apply()
        com.example.security.ClipboardSecurityManager.setAutoClearEnabled(getApplication(), enabled)
    }

    fun setAutoLockMinutes(minutes: Int) {
        _autoLockMinutes.value = minutes
        prefs.edit().putInt("pref_auto_lock_minutes", minutes).apply()
    }

    fun insertItem(title: String, category: String, username: String, password: String, notes: String, url: String, imageUri: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.insertItem(title, category, username, password, notes, url, imageUri)
            onComplete()
        }
    }

    fun updateItem(id: Long, title: String, category: String, username: String, password: String, notes: String, url: String, imageUri: String, createdAt: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.updateItem(id, title, category, username, password, notes, url, imageUri, createdAt)
            onComplete()
        }
    }

    fun deleteItem(item: DecryptedPasswordItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    suspend fun getItemById(id: Long): DecryptedPasswordItem? {
        return repository.getItemById(id)
    }

    fun generatePassword(
        length: Int = 16,
        useUppercase: Boolean = true,
        useLowercase: Boolean = true,
        useNumbers: Boolean = true,
        useSymbols: Boolean = true
    ): String {
        val upperChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lowerChars = "abcdefghijklmnopqrstuvwxyz"
        val numberChars = "0123456789"
        val symbolChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"

        var pool = ""
        if (useUppercase) pool += upperChars
        if (useLowercase) pool += lowerChars
        if (useNumbers) pool += numberChars
        if (useSymbols) pool += symbolChars

        if (pool.isEmpty()) pool = lowerChars + numberChars

        val rnd = java.security.SecureRandom()
        return (1..length).map {
            pool[rnd.nextInt(pool.length)]
        }.joinToString("")
    }

    fun calculatePasswordStrength(password: String): Int {
        if (password.isEmpty()) return 0
        var score = 0
        if (password.length >= 8) score++
        if (password.length >= 12) score++
        if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        return score.coerceIn(1, 4)
    }
}
