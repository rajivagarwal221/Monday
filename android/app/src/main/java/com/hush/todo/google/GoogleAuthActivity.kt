package com.hush.todo.google

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import java.net.URLEncoder

class GoogleAuthActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val clientId = intent.getStringExtra(EXTRA_CLIENT_ID).orEmpty()
        if (clientId.isBlank()) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        val redirect = "http://127.0.0.1"
        val scope = URLEncoder.encode("$GMAIL $CAL", "UTF-8")
        val url =
            "https://accounts.google.com/o/oauth2/v2/auth?client_id=${URLEncoder.encode(clientId, "UTF-8")}" +
                "&redirect_uri=${URLEncoder.encode(redirect, "UTF-8")}" +
                "&response_type=token&include_granted_scopes=true&prompt=consent&scope=$scope"
        val web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.userAgentString = web.settings.userAgentString.replace("; wv", "")
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return handle(request.url)
            }
            @Deprecated("deprecated")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return handle(Uri.parse(url))
            }
        }
        setContentView(web)
        web.loadUrl(url)
    }

    private fun handle(uri: Uri): Boolean {
        val combined = uri.toString()
        if (!combined.startsWith("http://127.0.0.1") && !combined.startsWith("http://localhost") && !combined.startsWith("com.hush.todo")) {
            return false
        }
        val hash = combined.substringAfter('#', "")
        val token = hash.split("&").map { it.split("=") }.firstOrNull { it.getOrNull(0) == "access_token" }?.getOrNull(1)
        if (!token.isNullOrBlank()) {
            setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_TOKEN, token))
        } else {
            setResult(Activity.RESULT_CANCELED)
        }
        finish()
        return true
    }

    companion object {
        const val EXTRA_CLIENT_ID = "clientId"
        const val EXTRA_TOKEN = "token"
        const val GMAIL = "https://www.googleapis.com/auth/gmail.readonly"
        const val CAL = "https://www.googleapis.com/auth/calendar.readonly"
    }
}
