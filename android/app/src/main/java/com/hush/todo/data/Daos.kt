package com.hush.todo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hush.todo.data.entities.ExtractionEntity
import com.hush.todo.data.entities.MessageEntity
import com.hush.todo.data.entities.ProjectEntity
import com.hush.todo.data.entities.TaskEntity
import com.hush.todo.data.entities.TranscriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE archived = 0 ORDER BY sortOrder")
    fun observe(): Flow<List<ProjectEntity>>

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ProjectEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ProjectEntity)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY priority, dueEpochMs")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: TaskEntity)

    @Update
    suspend fun update(item: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: String): TaskEntity?

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<TaskEntity>
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY createdMs")
    fun observe(): Flow<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MessageEntity)
}

@Dao
interface TranscriptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: TranscriptEntity)
}

@Dao
interface ExtractionDao {
    @Query("SELECT * FROM extractions WHERE status = 'pending' ORDER BY createdMs DESC")
    fun observePending(): Flow<List<ExtractionEntity>>

    @Query("SELECT sourceRef FROM extractions")
    suspend fun allRefs(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ExtractionEntity)

    @Query("UPDATE extractions SET status = :status WHERE id = :id")
    suspend fun setStatus(id: String, status: String)
}
