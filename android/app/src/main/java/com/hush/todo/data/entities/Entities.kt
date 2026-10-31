package com.hush.todo.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String,
    val color: Long,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)

/** Task state as a stable string so SQLite stays human-readable. */
object TaskStates {
    const val TODO = "todo"
    const val DOING = "doing"
    const val DONE = "done"
    const val AGENT_WORKING = "agent_working"
    const val NEEDS_YOU = "needs_you"
    const val BLOCKED = "blocked"
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String = "",
    val projectId: String? = null,
    val tagsCsv: String = "",
    val priority: Int = 4,
    val dueEpochMs: Long? = null,
    val state: String = TaskStates.TODO,
    val agentStatus: String? = null,
    val agentLastMessage: String? = null,
    val agentOutput: String? = null,
    val agentStartedMs: Long? = null,
    val sortOrder: Long = 0,
    val createdMs: Long,
    val updatedMs: Long,
    val completedMs: Long? = null,
    val source: String = "manual",
    val sourceRef: String? = null,
    val confidence: Float = 1f,
    val needsReview: Boolean = false,
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val role: String, // "user" | "agent" | "system"
    val text: String,
    val createdMs: Long,
    val taskId: String? = null,
)

@Entity(tableName = "transcripts")
data class TranscriptEntity(
    @PrimaryKey val id: String,
    val raw: String,
    val corrected: String,
    val lang: String,
    val createdMs: Long,
    val taskId: String? = null,
)

@Entity(tableName = "extractions")
data class ExtractionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val dueEpochMs: Long? = null,
    val source: String,
    val sourceRef: String,
    val snippet: String,
    val status: String = "pending",
    val createdMs: Long,
)
