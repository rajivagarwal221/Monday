package com.hush.todo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hush.todo.data.entities.TaskEntity
import com.hush.todo.data.entities.TaskStates
import com.hush.todo.ui.components.AgentRing
import com.hush.todo.ui.theme.FragmentMono
import com.hush.todo.ui.theme.HushColors
import com.hush.todo.ui.theme.Inter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskRow(
    task: TaskEntity,
    projectName: String?,
    projectEmoji: String?,
    onToggle: () -> Unit,
    onSnooze: () -> Unit = {},
    onDelete: () -> Unit = {},
    onEdit: () -> Unit = {},
    onAgentAction: (String) -> Unit,
    onApprove: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val isDone = task.state == TaskStates.DONE
    val working = task.state == TaskStates.AGENT_WORKING
    val needs = task.state == TaskStates.NEEDS_YOU

    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, if (needs) HushColors.AgentGold.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f), shape)
            .padding(12.dp)
    ) {
        if (working) AgentRing(cornerRadius = 14.dp, modifier = Modifier.matchParentSize())

        Column {
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(
                    task = task,
                    isDone = isDone,
                    onToggle = onToggle,
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PriorityDot(task.priority)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            task.title,
                            style = TextStyle(
                                color = if (isDone) Color.White.copy(alpha = 0.4f) else Color.White,
                                fontFamily = Inter,
                                fontSize = 14.sp,
                                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                            ),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (projectName != null) {
                            Text(
                                "${projectEmoji ?: ""} $projectName".trim(),
                                color = Color.White.copy(alpha = 0.5f),
                                fontFamily = Inter,
                                fontSize = 10.sp,
                            )
                        }
                        task.dueEpochMs?.let {
                            Text(
                                SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()).format(Date(it)),
                                color = Color.White.copy(alpha = 0.5f),
                                fontFamily = FragmentMono,
                                fontSize = 10.sp,
                            )
                        }
                        Text(task.source, color = Color.White.copy(alpha = 0.4f), fontFamily = FragmentMono, fontSize = 10.sp)
                        if (task.needsReview && !isDone) Text("needs review", color = HushColors.AgentGold, fontSize = 10.sp, fontFamily = FragmentMono)
                        if (working) Text("Agent is on it", color = HushColors.AgentGold, fontSize = 10.sp, fontFamily = FragmentMono)
                        if (needs) Text("Needs you", color = HushColors.AgentGold, fontSize = 10.sp, fontFamily = FragmentMono)
                    }
                }
            }

            if (!isDone) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 6.dp, start = 32.dp)) {
                    TextButton(onClick = onEdit, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                        Text("Edit", fontSize = 11.sp, fontFamily = Inter)
                    }
                    TextButton(onClick = onSnooze, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                        Text("Snooze 1h", fontSize = 11.sp, fontFamily = Inter)
                    }
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(0.7f))) {
                        Text("Delete", fontSize = 11.sp, fontFamily = Inter)
                    }
                }
            }

            AnimatedVisibility(visible = needs && task.agentOutput != null, enter = fadeIn(), exit = fadeOut()) {
                Column(
                    Modifier
                        .padding(top = 10.dp, start = 32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(HushColors.AgentGold.copy(alpha = 0.1f))
                        .border(1.dp, HushColors.AgentGold.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                ) {
                    Text("Agent output", fontFamily = FragmentMono, fontSize = 9.sp, color = HushColors.AgentGold.copy(alpha = 0.8f))
                    Spacer(Modifier.height(4.dp))
                    Text(task.agentOutput ?: "", color = Color.White.copy(alpha = 0.9f), fontFamily = Inter, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(
                            onClick = onApprove,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                        ) { Text("Looks good", fontSize = 11.sp, fontFamily = Inter) }
                        TextButton(
                            onClick = { onAgentAction("research") },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.7f)),
                        ) { Text("Try again", fontSize = 11.sp, fontFamily = Inter) }
                        TextButton(onClick = onSnooze, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text("Snooze 1h", fontSize = 11.sp, fontFamily = Inter) }
                        TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(0.7f))) { Text("Delete", fontSize = 11.sp, fontFamily = Inter) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Checkbox(task: TaskEntity, isDone: Boolean, onToggle: () -> Unit) {
    val shape = CircleShape
    val outline = if (isDone) HushColors.BrandPrimary else Color.White.copy(alpha = 0.4f)
    IconButton(onClick = onToggle, modifier = Modifier.size(22.dp)) {
        Box(
            Modifier
                .size(20.dp)
                .clip(shape)
                .background(if (isDone) HushColors.BrandPrimary else Color.Transparent)
                .border(1.5.dp, outline, shape),
            contentAlignment = Alignment.Center,
        ) {
            if (isDone) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun PriorityDot(priority: Int) {
    val color = when (priority) {
        1 -> HushColors.P1
        2 -> HushColors.P2
        3 -> HushColors.P3
        else -> Color.Transparent
    }
    Box(Modifier.size(7.dp).clip(CircleShape).background(color))
}
