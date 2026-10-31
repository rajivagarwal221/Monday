package com.hush.todo.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hush.todo.core.SecureSettings
import com.hush.todo.google.GoogleAuthActivity
import com.hush.todo.ui.theme.Inter

@Composable
fun SettingsScreen(settings: SecureSettings, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var lang by remember { mutableStateOf(settings.language) }
    var domain by remember { mutableStateOf(settings.senderDomain) }
    var groq by remember { mutableStateOf(if (settings.groqKey.isBlank()) "" else "••••••••") }
    var gemini by remember { mutableStateOf(if (settings.geminiKey.isBlank()) "" else "••••••••") }
    var clientId by remember { mutableStateOf(settings.googleClientId) }
    var status by remember { mutableStateOf(if (settings.googleToken.isBlank()) "" else "Google connected") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val token = result.data?.getStringExtra(GoogleAuthActivity.EXTRA_TOKEN)
        if (result.resultCode == Activity.RESULT_OK && !token.isNullOrBlank()) {
            settings.googleToken = token
            settings.googleSignedInAt = System.currentTimeMillis()
            status = "Google connected"
        } else {
            status = "Google sign-in cancelled"
        }
    }

    Column(
        Modifier.fillMaxSize().background(Color(0xF20D0D0D)).padding(20.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Settings", color = Color.White, fontSize = 28.sp, fontFamily = Inter)
        Text("Keys stay in encrypted storage. Never in the APK.", color = Color.White.copy(0.55f), fontSize = 12.sp, fontFamily = Inter)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(lang, { lang = it }, label = { Text("Language: auto / en / hi") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(domain, { domain = it }, label = { Text("University sender domain") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(groq, { groq = it }, label = { Text("Groq API key") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(gemini, { gemini = it }, label = { Text("Gemini API key") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(clientId, { clientId = it }, label = { Text("Google OAuth client ID") }, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = {
            settings.googleClientId = clientId.trim()
            if (clientId.isBlank()) {
                status = "Add a Google client ID first"
                return@TextButton
            }
            launcher.launch(Intent(ctx, GoogleAuthActivity::class.java).putExtra(GoogleAuthActivity.EXTRA_CLIENT_ID, clientId.trim()))
        }) { Text("Sign in to Gmail and Calendar", color = Color.White) }
        if (status.isNotBlank()) Text(status, color = Color.White.copy(0.6f), fontSize = 12.sp, fontFamily = Inter)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                settings.language = lang.trim().ifBlank { "auto" }
                settings.senderDomain = domain.trim()
                settings.googleClientId = clientId.trim()
                if (!groq.startsWith("•")) settings.groqKey = groq.trim()
                if (!gemini.startsWith("•")) settings.geminiKey = gemini.trim()
                onClose()
            },
            shape = RoundedCornerShape(24.dp),
        ) { Text("Save") }
        TextButton(onClick = onClose) { Text("Close", color = Color.White) }
    }
}
