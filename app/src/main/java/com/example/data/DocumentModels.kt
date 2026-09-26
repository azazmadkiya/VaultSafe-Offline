package com.example.data

enum class DocumentType(
    val displayName: String,
    val description: String,
    val idPlaceholder: String,
    val requiresBackPhoto: Boolean = false
) {
    AADHAAR(
        displayName = "Aadhaar Card",
        description = "12-digit UIDAI Identity with address on back",
        idPlaceholder = "1234 5678 9012",
        requiresBackPhoto = true
    ),
    PAN(
        displayName = "PAN Card",
        description = "10-character Permanent Account Number",
        idPlaceholder = "ABCDE1234F",
        requiresBackPhoto = false
    ),
    DRIVING_LICENSE(
        displayName = "Driving License",
        description = "State Transport Permit & License",
        idPlaceholder = "MH14 20210012345",
        requiresBackPhoto = true
    ),
    PASSPORT(
        displayName = "Passport",
        description = "International Travel Identity Document",
        idPlaceholder = "A1234567",
        requiresBackPhoto = false
    ),
    VOTER_ID(
        displayName = "Voter ID (EPIC)",
        description = "Election Commission Identity Card",
        idPlaceholder = "ABC1234567",
        requiresBackPhoto = true
    ),
    OTHER(
        displayName = "Other Document",
        description = "Certificate, Registration, ID, or Visa",
        idPlaceholder = "Document / ID Number",
        requiresBackPhoto = false
    );

    companion object {
        fun fromString(value: String): DocumentType {
            return entries.find {
                it.displayName.equals(value, ignoreCase = true) ||
                        it.name.equals(value, ignoreCase = true)
            } ?: if (value.contains("Aadhaar", ignoreCase = true)) AADHAAR
            else if (value.contains("PAN", ignoreCase = true)) PAN
            else if (value.contains("Driving", ignoreCase = true) || value.contains("License", ignoreCase = true)) DRIVING_LICENSE
            else if (value.contains("Passport", ignoreCase = true)) PASSPORT
            else if (value.contains("Voter", ignoreCase = true)) VOTER_ID
            else OTHER
        }
    }
}

fun DecryptedPasswordItem.getFrontImageUri(): String {
    if (imageUri.isBlank()) return ""
    return imageUri.split("|").firstOrNull { it.isNotBlank() } ?: ""
}

fun DecryptedPasswordItem.getBackImageUri(): String {
    if (imageUri.isBlank()) return ""
    val parts = imageUri.split("|").filter { it.isNotBlank() }
    return if (parts.size > 1) parts[1] else ""
}

fun DecryptedPasswordItem.getAllImageUris(): List<String> {
    if (imageUri.isBlank()) return emptyList()
    return imageUri.split("|").filter { it.isNotBlank() }
}

fun formatAadhaarNumber(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(12)
    return digits.chunked(4).joinToString(" ")
}

fun formatPanNumber(raw: String): String {
    return raw.filter { it.isLetterOrDigit() }.take(10).uppercase()
}

fun isValidAadhaar(number: String): Boolean {
    val digits = number.filter { it.isDigit() }
    return digits.length == 12
}

fun isValidPan(number: String): Boolean {
    val clean = number.trim().uppercase()
    return clean.matches(Regex("[A-Z]{5}[0-9]{4}[A-Z]{1}"))
}

fun maskDocumentNumber(number: String, type: DocumentType): String {
    val clean = number.trim()
    if (clean.length < 4) return clean
    return when (type) {
        DocumentType.AADHAAR -> {
            val digits = clean.filter { it.isDigit() }
            if (digits.length >= 8) {
                "•••• •••• " + digits.takeLast(4)
            } else {
                "•••• " + clean.takeLast(4)
            }
        }
        DocumentType.PAN -> {
            if (clean.length >= 5) {
                clean.take(2) + "••••••" + clean.takeLast(2)
            } else {
                "••••" + clean.takeLast(2)
            }
        }
        else -> {
            val visibleCount = if (clean.length > 6) 4 else 2
            "•••• " + clean.takeLast(visibleCount)
        }
    }
}
