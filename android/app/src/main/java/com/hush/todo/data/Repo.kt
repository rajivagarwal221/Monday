package com.hush.todo.data

import com.hush.todo.data.entities.ExtractionEntity
import com.hush.todo.data.entities.MessageEntity
import com.hush.todo.data.entities.ProjectEntity
import com.hush.todo.data.entities.TaskEntity
import com.hush.todo.data.entities.TaskStates
import com.hush.todo.data.entities.TranscriptEntity
import kotlinx.coroutines.flow.Flow

class Repo(
    private val tasks: TaskDao,
    private val projects: ProjectDao,
    private val messages: MessageDao,
    private val transcripts: TranscriptDao,
    private val extractions: ExtractionDao,
) {
    fun tasks(): Flow<List<TaskEntity>> = tasks.observeAll()
    fun projects(): Flow<List<ProjectEntity>> = projects.observe()
    fun messages(): Flow<List<MessageEntity>> = messages.observe()
    fun pendingExtractions(): Flow<List<ExtractionEntity>> = extractions.observePending()

    suspend fun addTask(t: TaskEntity) = tasks.upsert(t)
    suspend fun updateTask(t: TaskEntity) = tasks.update(t)
    suspend fun deleteTask(id: String) = tasks.delete(id)
    suspend fun getTask(id: String) = tasks.get(id)

    suspend fun addMessage(m: MessageEntity) = messages.upsert(m)
    suspend fun addProject(p: ProjectEntity) = projects.upsert(p)

    suspend fun toggleDone(t: TaskEntity): TaskEntity {
        val now = System.currentTimeMillis()
        val next = if (t.state == TaskStates.DONE) {
            t.copy(state = TaskStates.TODO, completedMs = null, updatedMs = now)
        } else {
            t.copy(state = TaskStates.DONE, completedMs = now, updatedMs = now)
        }
        tasks.update(next); return next
    }

    suspend fun snooze(id: String, untilMs: Long) {
        val t = tasks.get(id) ?: return
        tasks.update(t.copy(dueEpochMs = untilMs, updatedMs = System.currentTimeMillis(), state = TaskStates.TODO, completedMs = null))
    }

    suspend fun tasksSnapshot(): List<TaskEntity> = tasks.getAll()

    suspend fun addTranscript(t: TranscriptEntity) = transcripts.upsert(t)

    suspend fun addExtractionIfNew(e: ExtractionEntity): Boolean {
        if (extractions.allRefs().contains(e.sourceRef)) return false
        extractions.upsert(e)
        return true
    }

    suspend fun setExtractionStatus(id: String, status: String) = extractions.setStatus(id, status)

    suspend fun hasProjects(): Boolean = projects.count() > 0

    internal suspend fun bulkSeedProjects(ps: List<ProjectEntity>) = projects.upsertAll(ps)
    internal suspend fun bulkSeedTasks(ts: List<TaskEntity>) = tasks.upsertAll(ts)
    internal suspend fun bulkSeedMessages(ms: List<MessageEntity>) {
        ms.forEach { messages.upsert(it) }
    }
}
