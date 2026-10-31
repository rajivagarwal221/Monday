package com.hush.todo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hush.todo.ai.AgentStub
import com.hush.todo.core.CloudLlm
import com.hush.todo.core.DateParser
import com.hush.todo.core.ParseResult
import com.hush.todo.core.SecureSettings
import com.hush.todo.core.SpeechCapture
import com.hush.todo.core.deviceTz
import com.hush.todo.data.Repo
import com.hush.todo.data.entities.TaskEntity
import com.hush.todo.data.entities.TranscriptEntity
import com.hush.todo.notify.scheduleForTask
import com.hush.todo.data.entities.TaskStates
import com.hush.todo.ui.components.AgentRing
import com.hush.todo.ui.components.GlassSurface
import com.hush.todo.ui.components.StillAgentBorder
import com.hush.todo.ui.components.Wallpaper
import com.hush.todo.ui.theme.FragmentMono
import com.hush.todo.ui.theme.HushColors
import com.hush.todo.ui.theme.HushType
import com.hush.todo.ui.theme.Inter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class Tab { Today, Inbox, Upcoming }

@Composable
fun HomeScreen(repo: Repo, scope: CoroutineScope) {
    val context = LocalContext.current
    val tasks by repo.tasks().collectAsState(initial = emptyList())
    val projects by repo.projects().collectAsState(initial = emptyList())
    val messages by repo.messages().collectAsState(initial = emptyList())

    var tab by remember { mutableStateOf(Tab.Today) }
    var drawerOpen by remember { mutableStateOf(false) }
    var agentOpen by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<ParseResult?>(null) }
    var pendingTranscript by remember { mutableStateOf("") }
    var fallbackOpen by remember { mutableStateOf(false) }
    var fallbackText by remember { mutableStateOf("") }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var reviewOpen by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val settings = remember { SecureSettings(context) }
    val speech = remember { SpeechCapture(context) }
    val llm = remember { CloudLlm(settings) }

    val agent = remember { AgentStub(repo) }

    val filtered = remember(tasks, tab) {
        val now = System.currentTimeMillis()
        val startOfTomorrow = run {
            val c = java.util.Calendar.getInstance(); c.timeInMillis = now
            c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0)
            c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0)
            c.add(java.util.Calendar.DAY_OF_YEAR, 1); c.timeInMillis
        }
        when (tab) {
            Tab.Today -> tasks.filter { it.state != TaskStates.DONE && it.dueEpochMs != null }
            Tab.Inbox -> tasks.filter { it.state != TaskStates.DONE && it.projectId == null }
            Tab.Upcoming -> tasks.filter { it.state != TaskStates.DONE && it.dueEpochMs != null && it.dueEpochMs >= startOfTomorrow }
        }.sortedWith(compareBy({ it.priority }, { it.dueEpochMs ?: Long.MAX_VALUE }))
    }

    Box(Modifier.fillMaxSize()) {
        Wallpaper()

        Column(Modifier.fillMaxSize()) {
            // Top bar
            Row(
                Modifier.fillMaxWidth().padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassSurface(shape = CircleShape, strong = true) {
                    IconButton(onClick = { drawerOpen = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date()),
                    color = Color.White.copy(alpha = 0.75f),
                    fontFamily = FragmentMono,
                    fontSize = 11.sp
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(48.dp))
            }

            // Title block
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    reviewOpen -> Text("Review", style = HushType.DisplayLarge, color = Color.White)
                    tab == Tab.Today -> Text("Today", style = HushType.DisplayLarge, color = Color.White)
                    tab == Tab.Inbox -> Text("Inbox", style = HushType.DisplayLargeItalic, color = Color.White)
                    else -> Row {
                        Text("Up", style = HushType.DisplayLarge, color = Color.White)
                        Text("coming", style = HushType.DisplayLargeItalic, color = Color.White)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        reviewOpen -> "Accept or reject extracted deadlines"
                        tab == Tab.Today -> SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
                        tab == Tab.Inbox -> "Unsorted captures"
                        else -> "The next seven days"
                    },
                    color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = FragmentMono,
                )
            }

            // Quick capture bar
            notice?.let {
                Text(it, color = Color.White.copy(0.8f), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp), fontFamily = Inter)
            }

            if (!reviewOpen) {
            QuickCaptureBar(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                onSubmit = { text ->
                    scope.launch {
                        var parsed = DateParser.parse(text)
                        if (parsed.needsReview) {
                            llm.parseAmbiguous(text, System.currentTimeMillis(), deviceTz())?.let { parsed = it }
                        }
                        val t = agent.parseCapture(text).copy(
                            title = parsed.title.ifBlank { text },
                            dueEpochMs = parsed.dueMs,
                            confidence = parsed.confidence,
                            needsReview = parsed.needsReview,
                            source = "manual",
                        )
                        repo.addTask(t)
                        scheduleForTask(context, t)
                    }
                },
            )
            }

            if (reviewOpen) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    ReviewScreen(repo = repo, settings = settings, scope = scope, context = context)
                }
            } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 72.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("All quiet.", style = HushType.DisplayMedium, color = Color.White.copy(alpha = 0.65f))
                            Text("Add one thing.", style = HushType.DisplayMediumItalic, color = Color.White)
                            Spacer(Modifier.height(8.dp))
                            Text("Tap the mic and talk, or type above.", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, fontFamily = Inter)
                        }
                    }
                } else if (tab == Tab.Today) {
                    val start = run {
                        val c = java.util.Calendar.getInstance()
                        c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0)
                        c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0)
                        c.timeInMillis
                    }
                    val weekEnd = start + 7L * 24 * 60 * 60 * 1000
                    val overdue = filtered.filter { it.dueEpochMs != null && it.dueEpochMs!! < start }
                    val week = filtered.filter { val d = it.dueEpochMs ?: return@filter false; d >= start && d < weekEnd }
                    val later = filtered.filter { (it.dueEpochMs ?: 0) >= weekEnd }
                    listOf("Overdue" to overdue, "Due this week" to week, "Upcoming" to later).forEach { (label, list) ->
                        item {
                            Text(label, color = Color.White.copy(0.45f), fontSize = 10.sp, fontFamily = FragmentMono, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                        }
                        items(list, key = { it.id }) { task ->
                            val project = projects.firstOrNull { it.id == task.projectId }
                            TaskRow(
                                task = task,
                                projectName = project?.name,
                                projectEmoji = project?.emoji,
                                onToggle = { scope.launch { repo.toggleDone(task) } },
                                onSnooze = { scope.launch { repo.snooze(task.id, System.currentTimeMillis() + 60L * 60 * 1000) } },
                                onDelete = { scope.launch { repo.deleteTask(task.id) } },
                                onEdit = { editingTask = task },
                                onAgentAction = { kind -> scope.launch { agent.runOnTask(task, kind) } },
                                onApprove = { scope.launch { repo.updateTask(task.copy(state = TaskStates.TODO, agentStatus = null, agentOutput = null, agentLastMessage = null, updatedMs = System.currentTimeMillis())) } },
                            )
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { task ->
                        val project = projects.firstOrNull { it.id == task.projectId }
                        TaskRow(
                            task = task,
                            projectName = project?.name,
                            projectEmoji = project?.emoji,
                            onToggle = { scope.launch { repo.toggleDone(task) } },
                            onSnooze = { scope.launch { repo.snooze(task.id, System.currentTimeMillis() + 60L * 60 * 1000) } },
                            onDelete = { scope.launch { repo.deleteTask(task.id) } },
                            onEdit = { editingTask = task },
                            onAgentAction = { kind -> scope.launch { agent.runOnTask(task, kind) } },
                            onApprove = { scope.launch { repo.updateTask(task.copy(state = TaskStates.TODO, agentStatus = null, agentOutput = null, agentLastMessage = null, updatedMs = System.currentTimeMillis())) } },
                        )
                    }
                }
                item { Spacer(Modifier.height(96.dp)) }
            }
            }
        }

        // Bottom nav with raised mic FAB
        BottomNav(
            modifier = Modifier.align(Alignment.BottomCenter),
            activeTab = tab,
            agentOpen = agentOpen,
            listening = listening,
            onTab = { tab = it; agentOpen = false; reviewOpen = false },
            onAgent = { agentOpen = !agentOpen },
            onMic = {
                if (listening) {
                    speech.stop { text, conf, audio ->
                        listening = false
                        scope.launch {
                            var transcript = text
                            if ((transcript.isBlank() || conf < 0.55f) && audio != null) {
                                llm.transcribe(audio, settings.language)?.let { transcript = it }
                            }
                            if (transcript.isBlank()) {
                                fallbackText = ""
                                fallbackOpen = true
                                return@launch
                            }
                            applyVoice(transcript, conf, tasks, repo, llm) { parsed, note, openConfirm, raw ->
                                notice = note
                                pendingTranscript = raw
                                if (openConfirm) pending = parsed
                            }
                        }
                    }
                } else {
                    listening = true
                    speech.start(settings.language)
                }
            },
        )

        AnimatedVisibility(visible = drawerOpen, enter = slideInVertically { -it }, exit = slideOutVertically { -it }) {
            NavDrawer(
                projects = projects,
                onProject = { },
                onSettings = { drawerOpen = false; settingsOpen = true },
                onReview = { drawerOpen = false; reviewOpen = true; agentOpen = false },
                onDismiss = { drawerOpen = false },
            )
        }

        AnimatedVisibility(
            visible = agentOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            AgentSheet(
                messages = messages,
                onSend = { text -> scope.launch { agent.chat(text) } },
                onClose = { agentOpen = false },
            )
        }

        pending?.let { p ->
            ConfirmSheet(
                pending = p,
                transcript = pendingTranscript,
                onSave = { edited ->
                    scope.launch {
                        val t = agent.parseCapture(edited.title)
                        val saved = t.copy(
                            title = edited.title.ifBlank { t.title },
                            dueEpochMs = edited.dueMs ?: t.dueEpochMs,
                            source = "voice",
                            confidence = edited.confidence,
                            needsReview = edited.needsReview,
                        )
                        repo.addTask(saved)
                        scheduleForTask(context, saved)
                        repo.addTranscript(
                            TranscriptEntity(
                                id = "tr-" + UUID.randomUUID().toString().take(8),
                                raw = pendingTranscript,
                                corrected = saved.title,
                                lang = settings.language,
                                createdMs = System.currentTimeMillis(),
                                taskId = saved.id,
                            )
                        )
                        pending = null
                    }
                },
                onDismiss = { pending = null },
            )
        }

        if (fallbackOpen) {
            TranscriptFallbackSheet(
                text = fallbackText,
                onChange = { fallbackText = it },
                onContinue = {
                    val t = fallbackText
                    fallbackOpen = false
                    if (t.isBlank()) return@TranscriptFallbackSheet
                    scope.launch {
                        applyVoice(t, 0.5f, tasks, repo, llm) { parsed, note, openConfirm, raw ->
                            notice = note
                            pendingTranscript = raw
                            if (openConfirm) pending = parsed
                        }
                    }
                },
                onDismiss = { fallbackOpen = false },
            )
        }

        editingTask?.let { task ->
            TaskEditSheet(
                task = task,
                onSave = { title, dueMs ->
                    scope.launch {
                        val next = task.copy(title = title, dueEpochMs = dueMs, updatedMs = System.currentTimeMillis())
                        repo.updateTask(next)
                        scheduleForTask(context, next)
                        editingTask = null
                    }
                },
                onDismiss = { editingTask = null },
            )
        }

        if (settings.tokenNeedsRefresh()) {
            TextButton(
                onClick = { settingsOpen = true },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
            ) {
                Text("Google access expires about weekly. Tap to re-sign in.", color = Color(0xFFFFE08A), fontSize = 12.sp)
            }
        }

        if (settingsOpen) {
            SettingsScreen(settings = settings, onClose = { settingsOpen = false })
        }
    }
}

private suspend fun applyVoice(
    transcript: String,
    conf: Float,
    tasks: List<TaskEntity>,
    repo: Repo,
    llm: CloudLlm,
    emit: (ParseResult?, String?, Boolean, String) -> Unit,
) {
    var parsed = DateParser.parse(transcript)
    if (parsed.needsReview || parsed.dueMs == null && parsed.intent == "add") {
        withContext(Dispatchers.IO) { llm.parseAmbiguous(transcript, System.currentTimeMillis(), deviceTz()) }?.let { parsed = it }
    }
    if (conf < 0.55f && parsed.intent == "add") parsed = parsed.copy(confidence = minOf(parsed.confidence, 0.4f), needsReview = true)
    when (parsed.intent) {
        "query_week" -> {
            val start = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            val weekEnd = start + 7L * 24 * 60 * 60 * 1000
            val n = tasks.count { it.state != TaskStates.DONE && it.dueEpochMs != null && it.dueEpochMs!! in start until weekEnd }
            emit(null, if (n == 0) "Nothing due this week." else "$n due this week", false, transcript)
        }
        "complete" -> {
            val q = parsed.completeQuery ?: parsed.title
            val hit = tasks.firstOrNull { it.state != TaskStates.DONE && it.title.contains(q, true) }
            if (hit != null) repo.toggleDone(hit)
            emit(null, if (hit != null) "Marked done: ${hit.title}" else "Not found", false, transcript)
        }
        else -> emit(parsed, null, true, transcript)
    }
}

@Composable
private fun ConfirmSheet(
    pending: ParseResult,
    transcript: String,
    onSave: (ParseResult) -> Unit,
    onDismiss: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(pending.title) }
    var dueLocal by remember { mutableStateOf(pending.dueMs?.let { fmtLocal(it) } ?: "") }
    Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
        GlassSurface(shape = RoundedCornerShape(20.dp), strong = true, modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("Confirm before saving", color = Color.White.copy(0.5f), fontSize = 10.sp, fontFamily = FragmentMono)
                if (pending.needsReview) Text("needs review", color = HushColors.AgentGold, fontSize = 11.sp, fontFamily = Inter)
                Spacer(Modifier.height(8.dp))
                if (editing) {
                    OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(dueLocal, { dueLocal = it }, label = { Text("Due yyyy-MM-dd HH:mm") }, modifier = Modifier.fillMaxWidth())
                } else {
                    Text(title.ifBlank { "Untitled" }, color = Color.White, style = HushType.DisplayMedium)
                    Text(
                        dueLocal.ifBlank { "No time set" }.let { if (pending.dueMs != null && dueLocal.isBlank()) SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault()).format(Date(pending.dueMs)) else it },
                        color = Color.White.copy(0.7f),
                        fontSize = 12.sp,
                        fontFamily = FragmentMono,
                    )
                }
                if (transcript.isNotBlank()) {
                    Text("Heard: $transcript", color = Color.White.copy(0.45f), fontSize = 12.sp, fontFamily = Inter, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val due = parseLocal(dueLocal) ?: pending.dueMs
                        onSave(pending.copy(title = title, dueMs = due))
                    }) { Text("Save") }
                    TextButton(onClick = { editing = !editing }) { Text(if (editing) "Preview" else "Edit", color = Color.White) }
                    TextButton(onClick = onDismiss) { Text("Dismiss", color = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun TranscriptFallbackSheet(
    text: String,
    onChange: (String) -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
        GlassSurface(shape = RoundedCornerShape(20.dp), strong = true, modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("Recognition did not catch that. Edit the transcript, then continue.", color = Color.White.copy(0.7f), fontSize = 13.sp, fontFamily = Inter)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(text, onChange, modifier = Modifier.fillMaxWidth(), label = { Text("Transcript") })
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onContinue) { Text("Continue") }
                    TextButton(onClick = onDismiss) { Text("Dismiss", color = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun TaskEditSheet(
    task: TaskEntity,
    onSave: (String, Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(task.title) }
    var dueLocal by remember { mutableStateOf(task.dueEpochMs?.let { fmtLocal(it) } ?: "") }
    Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
        GlassSurface(shape = RoundedCornerShape(20.dp), strong = true, modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("Edit task", color = Color.White.copy(0.5f), fontSize = 10.sp, fontFamily = FragmentMono)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(dueLocal, { dueLocal = it }, label = { Text("Due yyyy-MM-dd HH:mm") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onSave(title.trim().ifBlank { task.title }, parseLocal(dueLocal) ?: task.dueEpochMs) }) { Text("Save") }
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
                }
            }
        }
    }
}

private fun fmtLocal(ms: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ms))

private fun parseLocal(raw: String): Long? {
    if (raw.isBlank()) return null
    return runCatching { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(raw)?.time }.getOrNull()
        ?: runCatching { SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(raw)?.time }.getOrNull()
}

@Composable
private fun BottomNav(
    modifier: Modifier,
    activeTab: Tab,
    agentOpen: Boolean,
    listening: Boolean,
    onTab: (Tab) -> Unit,
    onAgent: () -> Unit,
    onMic: () -> Unit,
) {
    Box(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp), contentAlignment = Alignment.BottomCenter) {
        GlassSurface(shape = CircleShape, strong = true) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                NavItem(Icons.Default.Star, "Today", active = !agentOpen && activeTab == Tab.Today) { onTab(Tab.Today) }
                NavItem(Icons.Default.Inbox, "Inbox", active = !agentOpen && activeTab == Tab.Inbox) { onTab(Tab.Inbox) }
                // Raised mic
                Box(Modifier.width(72.dp).height(60.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .offset(y = (-20).dp)
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(if (listening) HushColors.AgentGold else HushColors.Ink),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (listening) AgentRing(cornerRadius = 30.dp, modifier = Modifier.matchParentSize())
                        IconButton(onClick = onMic, modifier = Modifier.matchParentSize()) {
                            Icon(Icons.Default.Mic, contentDescription = "Talk", tint = if (listening) HushColors.Ink else Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                }
                NavItem(Icons.Default.CalendarMonth, "Upcoming", active = !agentOpen && activeTab == Tab.Upcoming) { onTab(Tab.Upcoming) }
                NavItem(Icons.Default.Message, "Agent", active = agentOpen) { onAgent() }
            }
        }
    }
}

@Composable
private fun NavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.widthIn(min = 56.dp).padding(horizontal = 4.dp).clip(RoundedCornerShape(12.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, tint = if (active) Color.White else Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
        }
        Text(label, fontSize = 9.sp, color = if (active) Color.White else Color.White.copy(alpha = 0.4f), fontFamily = Inter, fontWeight = FontWeight.Medium)
    }
}
