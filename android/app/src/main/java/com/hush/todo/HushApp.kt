package com.hush.todo

import android.app.Application
import com.hush.todo.data.HushDatabase
import com.hush.todo.data.Repo
import com.hush.todo.data.purgeDemoTasks
import com.hush.todo.data.seedIfEmpty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HushApp : Application() {
    lateinit var repo: Repo
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        val db = HushDatabase.get(this)
        repo = Repo(db.tasks(), db.projects(), db.messages(), db.transcripts(), db.extractions())
        appScope.launch {
            seedIfEmpty(repo)
            purgeDemoTasks(repo)
            repo.tasksSnapshot().forEach { com.hush.todo.notify.scheduleForTask(this@HushApp, it) }
            com.hush.todo.notify.scheduleMorningSummary(this@HushApp)
        }
    }

    companion object {
        lateinit var instance: HushApp
            private set
    }
}
