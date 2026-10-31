package com.hush.todo.core

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureSettings(ctx: Context) {
    private val master = MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val enc: SharedPreferences = EncryptedSharedPreferences.create(
        ctx, "hush-secrets", master,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
    private val pub = ctx.getSharedPreferences("hush-public", Context.MODE_PRIVATE)

    var language: String
        get() = pub.getString("lang", "auto") ?: "auto"
        set(v) { pub.edit().putString("lang", v).apply() }

    var senderDomain: String
        get() = pub.getString("domain", "") ?: ""
        set(v) { pub.edit().putString("domain", v).apply() }

    var groqKey: String
        get() = enc.getString("groq", "") ?: ""
        set(v) { enc.edit().putString("groq", v).apply() }

    var geminiKey: String
        get() = enc.getString("gemini", "") ?: ""
        set(v) { enc.edit().putString("gemini", v).apply() }

    var googleClientId: String
        get() = pub.getString("googleClientId", "") ?: ""
        set(v) { pub.edit().putString("googleClientId", v).apply() }

    var googleToken: String
        get() = enc.getString("googleToken", "") ?: ""
        set(v) { enc.edit().putString("googleToken", v).apply() }

    var googleSignedInAt: Long
        get() = pub.getLong("googleSignedInAt", 0L)
        set(v) { pub.edit().putLong("googleSignedInAt", v).apply() }

    fun tokenNeedsRefresh(now: Long = System.currentTimeMillis()): Boolean {
        if (googleSignedInAt == 0L) return false
        return now - googleSignedInAt > 6L * 24 * 60 * 60 * 1000
    }
}
