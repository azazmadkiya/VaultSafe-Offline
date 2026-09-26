# Privacy Policy for Secure Vault

**Effective Date:** September 26, 2026

Secure Vault ("we", "our", or "app") is committed to protecting your personal privacy and data security. This Privacy Policy explains how Secure Vault handles your information when you use our Android application.

## 1. Information Collection & Local Storage
Secure Vault is designed with a **100% Offline-First Architecture**:
- **No Cloud Servers:** We do not operate any backend servers, cloud databases, or external analytics servers.
- **Local Database:** All your credentials, passwords, secure notes, and document/ID attachments (such as Aadhaar, PAN, and ID card scans) are stored **exclusively on your local device** inside an encrypted SQLite database using Android Room.
- **Biometric & Master Password Security:** Your master password and biometric keys are secured locally using the Android Keystore system with AES-256-GCM encryption.

## 2. Device Permissions
Secure Vault requests specific device permissions solely to provide core app features:
- **Biometric Hardware (`USE_BIOMETRIC`):** Used locally on your device for fingerprint or face authentication to unlock your vault. We do not collect or transmit biometric templates.
- **Camera (`CAMERA`):** Used only when you choose to scan physical document pages or ID cards into your secure vault. Scans are saved directly to your private local app sandbox storage.
- **Photos / Media Picker (`READ_MEDIA_IMAGES` / Photo Picker):** Used only when you select existing images from your device gallery to attach to your document records.

## 3. Data Sharing & Third-Party Services
- **Zero Third-Party Sharing:** Because your data never leaves your device, we do not share, sell, rent, or trade your personal information with any third parties, advertisers, or analytics providers.
- **No Internet Access Required:** Secure Vault functions fully offline without requiring an internet connection for normal operations.

## 4. Data Security
We employ industry-standard security practices, including:
- AES-256-GCM authenticated encryption for stored records.
- Automatic clipboard clearing (optional) to protect copied passwords.
- Auto-lock timeout inactivity protection.

## 5. Changes to This Privacy Policy
We may update our Privacy Policy from time to time. Any changes will be posted in this document within our GitHub repository and updated within the app settings.

## 6. Contact Us
If you have any questions or suggestions about our Privacy Policy, please contact us via GitHub or at the developer email associated with this repository.
