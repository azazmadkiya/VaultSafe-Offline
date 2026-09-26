package com.example.data

import com.example.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class DecryptedPasswordItem(
    val id: Long = 0L,
    val title: String,
    val category: String,
    val username: String,
    val password: String,
    val notes: String,
    val url: String,
    val imageUri: String = "",
    val createdAt: Long,
    val updatedAt: Long
)

class PasswordRepository(
    private val passwordDao: PasswordDao,
    private val cryptoManager: CryptoManager
) {
    val allItems: Flow<List<DecryptedPasswordItem>> = passwordDao.getAllItems().map { list ->
        list.map { entity ->
            DecryptedPasswordItem(
                id = entity.id,
                title = entity.title,
                category = entity.category,
                username = cryptoManager.decrypt(entity.username, entity.usernameIv),
                password = cryptoManager.decrypt(entity.password, entity.passwordIv),
                notes = cryptoManager.decrypt(entity.notes, entity.notesIv),
                url = entity.url,
                imageUri = entity.imageUri,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }

    suspend fun getItemById(id: Long): DecryptedPasswordItem? {
        val entity = passwordDao.getItemById(id) ?: return null
        return DecryptedPasswordItem(
            id = entity.id,
            title = entity.title,
            category = entity.category,
            username = cryptoManager.decrypt(entity.username, entity.usernameIv),
            password = cryptoManager.decrypt(entity.password, entity.passwordIv),
            notes = cryptoManager.decrypt(entity.notes, entity.notesIv),
            url = entity.url,
            imageUri = entity.imageUri,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    suspend fun insertItem(
        title: String,
        category: String,
        username: String,
        password: String,
        notes: String,
        url: String,
        imageUri: String
    ) {
        val encUsername = cryptoManager.encrypt(username)
        val encPassword = cryptoManager.encrypt(password)
        val encNotes = cryptoManager.encrypt(notes)

        val entity = PasswordEntity(
            title = title,
            category = category,
            username = encUsername.ciphertext,
            usernameIv = encUsername.iv,
            password = encPassword.ciphertext,
            passwordIv = encPassword.iv,
            notes = encNotes.ciphertext,
            notesIv = encNotes.iv,
            url = url,
            imageUri = imageUri,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        passwordDao.insertItem(entity)
    }

    suspend fun updateItem(
        id: Long,
        title: String,
        category: String,
        username: String,
        password: String,
        notes: String,
        url: String,
        imageUri: String,
        createdAt: Long
    ) {
        val encUsername = cryptoManager.encrypt(username)
        val encPassword = cryptoManager.encrypt(password)
        val encNotes = cryptoManager.encrypt(notes)

        val entity = PasswordEntity(
            id = id,
            title = title,
            category = category,
            username = encUsername.ciphertext,
            usernameIv = encUsername.iv,
            password = encPassword.ciphertext,
            passwordIv = encPassword.iv,
            notes = encNotes.ciphertext,
            notesIv = encNotes.iv,
            url = url,
            imageUri = imageUri,
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis()
        )
        passwordDao.updateItem(entity)
    }

    suspend fun deleteItem(item: DecryptedPasswordItem) {
        if (item.imageUri.isNotBlank()) {
            AttachmentStorageManager.deleteAttachmentFile(item.imageUri)
        }
        val entity = PasswordEntity(
            id = item.id,
            title = item.title,
            category = item.category,
            username = "",
            usernameIv = "",
            password = "",
            passwordIv = "",
            notes = "",
            notesIv = "",
            url = item.url,
            imageUri = item.imageUri,
            createdAt = item.createdAt,
            updatedAt = item.updatedAt
        )
        passwordDao.deleteItem(entity)
    }
}
