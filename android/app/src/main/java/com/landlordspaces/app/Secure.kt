package com.landlordspaces.app

import android.content.Context
import android.content.ContextWrapper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts the saved sign-in tokens with a key that lives inside the phone's Android Keystore and never leaves it. */
object SecureStore {
    private const val ALIAS = "ls_session_key"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
        val existing = ks.getKey(ALIAS, null)
        if (existing is SecretKey) return existing
        val g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        g.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return g.generateKey()
    }

    /** Returns "e1:iv:ciphertext", or null if the Keystore is not usable (the token then lives in memory only). */
    fun encrypt(plain: String): String? = try {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key())
        val ct = c.doFinal(plain.toByteArray(Charsets.UTF_8))
        "e1:" + Base64.encodeToString(c.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(ct, Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }

    fun decrypt(stored: String): String? = try {
        val p = stored.split(":")
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(p[1], Base64.NO_WRAP)))
        String(c.doFinal(Base64.decode(p[2], Base64.NO_WRAP)), Charsets.UTF_8)
    } catch (e: Exception) {
        null
    }
}

fun Context.findFragmentActivity(): FragmentActivity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is FragmentActivity) return c
        c = c.baseContext
    }
    return null
}

/** The phone's own fingerprint / biometric check (AndroidX BiometricPrompt). No fingerprint data ever reaches the app. */
object Bio {
    private const val STRONG = BiometricManager.Authenticators.BIOMETRIC_STRONG

    private fun status(ctx: Context): Int = try { BiometricManager.from(ctx).canAuthenticate(STRONG) } catch (e: Exception) { BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE }

    fun available(ctx: Context): Boolean = status(ctx) == BiometricManager.BIOMETRIC_SUCCESS

    fun problem(ctx: Context): String = when (status(ctx)) {
        BiometricManager.BIOMETRIC_SUCCESS -> ""
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No fingerprint is set up on this phone yet. Add one in Settings, then Security."
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "This phone has no fingerprint sensor."
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "The fingerprint sensor is not available right now."
        else -> "Fingerprint sign-in is not available on this phone."
    }

    /** kind: 0 = failed or unavailable, 1 = cancelled, 2 = "Use password" tapped. */
    fun prompt(ctx: Context, title: String, subtitle: String, negative: String, onSuccess: () -> Unit, onError: (Int, String) -> Unit) {
        val act = ctx.findFragmentActivity()
        if (act == null) { onError(0, "Fingerprint check could not start. Please try again."); return }
        val cb = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { onSuccess() }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> onError(2, "")
                    BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_CANCELED -> onError(1, "")
                    BiometricPrompt.ERROR_LOCKOUT -> onError(0, "Too many attempts. Wait 30 seconds, or use your password.")
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> onError(0, "Fingerprint is locked on this phone. Use your password.")
                    else -> onError(0, errString.toString())
                }
            }
        }
        try {
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title).setSubtitle(subtitle).setNegativeButtonText(negative)
                .setAllowedAuthenticators(STRONG).build()
            BiometricPrompt(act, ContextCompat.getMainExecutor(act), cb).authenticate(info)
        } catch (e: Exception) {
            onError(0, "Fingerprint check could not start. Please try again.")
        }
    }
}
