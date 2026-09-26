package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val category: String, // "Logins", "Notes", "Credit Cards", "Wi-Fi"
    val username: String, // encrypted ciphertext (Base64)
    val usernameIv: String, // Base64 IV
    val password: String, // encrypted ciphertext (Base64)
    val passwordIv: String, // Base64 IV
    val notes: String, // encrypted ciphertext (Base64)
    val notesIv: String, // Base64 IV
    val url: String = "",
    val imageUri: String = "", // Photo / Document / ID image URI
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
