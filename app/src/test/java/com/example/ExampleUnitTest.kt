package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testAadhaarFormatting() {
    val raw = "123456789012"
    val formatted = formatAadhaarNumber(raw)
    assertEquals("1234 5678 9012", formatted)
    assertTrue(isValidAadhaar(formatted))
  }

  @Test
  fun testPanFormatting() {
    val raw = "abcde1234f"
    val formatted = formatPanNumber(raw)
    assertEquals("ABCDE1234F", formatted)
    assertTrue(isValidPan(formatted))
  }

  @Test
  fun testMaskingDocumentNumbers() {
    val aadhaar = "1234 5678 9012"
    val maskedAadhaar = maskDocumentNumber(aadhaar, DocumentType.AADHAAR)
    assertEquals("•••• •••• 9012", maskedAadhaar)

    val pan = "ABCDE1234F"
    val maskedPan = maskDocumentNumber(pan, DocumentType.PAN)
    assertEquals("AB••••••4F", maskedPan)
  }

  @Test
  fun testMultiPhotoUriParsing() {
    val item = DecryptedPasswordItem(
      id = 1L,
      title = "Aadhaar Card",
      username = "1234 5678 9012",
      password = "Rohan Sharma",
      url = "Aadhaar Card",
      notes = "New Delhi",
      category = "Documents & IDs",
      imageUri = "/data/vault/front.jpg|/data/vault/back.jpg",
      createdAt = 1000L,
      updatedAt = 1000L
    )

    assertEquals("/data/vault/front.jpg", item.getFrontImageUri())
    assertEquals("/data/vault/back.jpg", item.getBackImageUri())
    val allPhotos = item.getAllImageUris()
    assertEquals(2, allPhotos.size)
    assertEquals("/data/vault/front.jpg", allPhotos[0])
    assertEquals("/data/vault/back.jpg", allPhotos[1])
  }

  @Test
  fun testPdfUriDetection() {
    val item = DecryptedPasswordItem(
      id = 2L,
      title = "e-Aadhaar",
      username = "1234 5678 9012",
      password = "Rohan Sharma",
      url = "Aadhaar Card",
      notes = "Official PDF",
      category = "Documents & IDs",
      imageUri = "/data/vault/doc_123.pdf",
      createdAt = 1000L,
      updatedAt = 1000L
    )
    val uris = item.getAllImageUris()
    assertEquals(1, uris.size)
    assertTrue(uris[0].lowercase().contains(".pdf"))
  }
}
