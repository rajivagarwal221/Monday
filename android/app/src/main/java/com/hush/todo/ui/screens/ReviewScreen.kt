package com.hush.todo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hush.todo.core.SecureSettings
import com.hush.todo.core.extractDeadlines
import com.hush.todo.data.Repo
import com.hush.todo.data.entities.ExtractionEntity
import com.hush.todo.data.entities.TaskEntity
import com.hush.todo.google.fetchGoogleDeadlines
import com.hush.todo.notify.scheduleForTask
import com.hush.todo.ui.components.GlassSurface
import com.hush.todo.ui.theme.FragmentMono
import com.hush.todo.ui.theme.Inter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.content.Context

@Composable
fun ReviewScreen(
    repo: Repo,
    settings: SecureSettings,
    scope: CoroutineScope,
    context: Context,
) {
    val pending by repo.pendingExtractions().collectAsState(initial = emptyList())
    var msg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val expired = settings.tokenNeedsRefresh()

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (expired) {
            Text(
                "Google access is about a week old. Re-sign in under Settings.",
                color = Color(0xFFFFE08A),
                fontSize = 12.sp,
                fontFamily = Inter,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    busy = true
                    msg = ""
                    scope.launch {
                        val added = withContext(Dispatchers.IO) {
                            val raw = fetchGoogleDeadlines(settings)
                            if (raw.isEmpty()) return@withContext -1
                            val extracted = extractDeadlines(raw, settings.senderDomain)
                            var n = 0
                            extracted.forEach { e ->
                                val ok = repo.addExtractionIfNew(
                                    ExtractionEntity(
                                        id = "x-" + UUID.randomUUID().toString().take(8),
                                        title = e.title,
                                        dueEpochMs = e.dueMs,
                                        source = e.source,
                                        sourceRef = e.sourceRef,
                                        snippet = e.snippet,
                                        createdMs = System.currentTimeMillis(),
                                    )
                                )
                                if (ok) n++
                            }
                            n
                        }
                        msg = when (added) {
                            -1 -> if (settings.googleClientId.isBlank()) "Add a Google client ID in Settings first." else "Nothing new, or sign-in expired."
                            0 -> "Caught up."
                            else -> "$added to review"
                        }
                        busy = false
                    }
                },
                enabled = !busy,
            ) { Text(if (busy) "Reading…" else "Pull Gmail and Calendar") }
        }
        if (msg.isNotBlank()) {
            Text(msg, color = Color.White.copy(0.6f), fontSize = 11.sp, fontFamily = FragmentMono, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(12.dp))
        if (pending.isEmpty()) {
            Text(
                "No extracted deadlines waiting. Pull mail and calendar, then accept or reject each one.",
                color = Color.White.copy(0.55f),
                fontSize = 13.sp,
                fontFamily = Inter,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(pending, key = { it.id }) { e ->
                    GlassSurface(shape = RoundedCornerShape(14.dp), strong = true, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(e.title, color = Color.White, fontFamily = Inter, fontSize = 14.sp)
                            Text(
                                "${e.source} · ${e.dueEpochMs?.let { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(it)) } ?: "no time"} · ${e.snippet}",
                                color = Color.White.copy(0.45f),
                                fontSize = 10.sp,
                                fontFamily = FragmentMono,
                            )
                            Row {
                                TextButton(onClick = {
                                    scope.launch {
                                        val now = System.currentTimeMillis()
                                        val t = TaskEntity(
                                            id = "t-" + UUID.randomUUID().toString().take(8),
                                            title = e.title,
                                            notes = e.snippet,
                                            dueEpochMs = e.dueEpochMs,
                                            createdMs = now,
                                            updatedMs = now,
                                            source = e.source,
                                            sourceRef = e.sourceRef,
                                            confidence = 0.8f,
                                            needsReview = false,
                                        )
                                        repo.addTask(t)
                                        scheduleForTask(context, t)
                                        repo.setExtractionStatus(e.id, "accepted")
                                    }
                                }) { Text("Accept", color = Color.White) }
                                TextButton(onClick = {
                                    scope.launch { repo.setExtractionStatus(e.id, "rejected") }
                                }) { Text("Reject", color = Color.White.copy(0.7f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}
