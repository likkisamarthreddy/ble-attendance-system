package com.example.practice.features

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import com.example.practice.utils.await

/**
 * Device Security Manager — Android Keystore RSA + Play Integrity API support.
 *
 * RSA 2048-bit key pair: hardware-backed, private key never leaves secure enclave.
 * Used for challenge-response signing during attendance verification.
 *
 * Play Integrity API: called separately via IntegrityManager (requires Google Play Services).
 */
object DeviceSecurityManager {

    private const val KEY_ALIAS = "attendance_device_key"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"

    /**
     * Get or create the RSA key pair in Android Keystore.
     */
    fun getOrCreateKeyPair() {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)

        if (keyStore.containsAlias(KEY_ALIAS)) return

        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_PROVIDER
        )

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setKeySize(2048)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            // Require user authentication if biometric is set up
            // .setUserAuthenticationRequired(true)
            .build()

        keyPairGenerator.initialize(spec)
        keyPairGenerator.generateKeyPair()
    }

    /**
     * Get the public key as Base64 string for backend registration.
     */
    fun getPublicKeyBase64(): String? {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            val publicKey = keyStore.getCertificate(KEY_ALIAS)?.publicKey ?: return null
            Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Sign a challenge nonce with the device's private key.
     *
     * @param challenge The challenge bytes from the backend
     * @return Base64-encoded signature
     */
    fun signChallenge(challenge: ByteArray): String? {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            val privateKey = (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry)
                ?.privateKey ?: return null

            val signature = Signature.getInstance("SHA256withRSA")
            signature.initSign(privateKey)
            signature.update(challenge)
            val signedBytes = signature.sign()

            Base64.encodeToString(signedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if the device key has been generated.
     */
    fun hasDeviceKey(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            keyStore.containsAlias(KEY_ALIAS)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete the device key (for re-registration).
     */
    fun deleteDeviceKey() {
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            keyStore.deleteEntry(KEY_ALIAS)
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Request Play Integrity Token.
     */
    suspend fun requestIntegrityVerdict(context: android.content.Context, nonce: String): String? {
        return try {
            val integrityManager = com.google.android.play.core.integrity.IntegrityManagerFactory.create(context)
            val tokenResponse = integrityManager.requestIntegrityToken(
                com.google.android.play.core.integrity.IntegrityTokenRequest.builder()
                    .setNonce(nonce)
                    .build()
            ).addOnFailureListener { e ->
                android.util.Log.e("DeviceSecurity", "Integrity request failed", e)
            }.addOnSuccessListener { _ ->
                android.util.Log.d("DeviceSecurity", "Integrity token received")
            }
            
            // Convert Task<IntegrityTokenResponse> to coroutine
            tokenResponse.await().token()
        } catch (e: Exception) {
            android.util.Log.e("DeviceSecurity", "Error requesting integrity token", e)
            null
        }
    }
}
