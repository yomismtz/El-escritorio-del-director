package com.profecuaderno.director

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.profecuaderno.director.network.*
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

enum class DirectorOfflineWriteResult { SENT, QUEUED }

data class DirectorCoreCache(
    val current: UserDto?,
    val institution: InstitutionDto?,
    val teachers: List<UserDto>,
    val classes: List<ClassDto>,
    val notices: List<DirectorNoticeDto>,
)

private data class PendingDirectorAction(
    val type: String,
    val institutionName: String? = null,
    val email: String? = null,
    val title: String? = null,
    val body: String? = null,
    val teacherId: Int? = null,
    val classId: Int? = null,
    val weekday: Int? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val room: String? = null,
    val scheduleId: Int? = null,
)

class DirectorOfflineStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("director_offline_sync", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun loadCore(): DirectorCoreCache? = prefs.getString("core", null)?.let { raw ->
        runCatching { gson.fromJson(raw, DirectorCoreCache::class.java) }.getOrNull()
    }

    fun saveCore(cache: DirectorCoreCache) {
        prefs.edit().putString("core", gson.toJson(cache)).apply()
    }

    fun loadAttendance(): List<InstitutionalAttendanceSummaryDto> = readList("attendance")
    fun saveAttendance(items: List<InstitutionalAttendanceSummaryDto>) = writeList("attendance", items)

    fun loadSchedule(): List<ScheduleDto> = readList("schedule")
    fun saveSchedule(items: List<ScheduleDto>) = writeList("schedule", items)

    fun pendingCount(): Int = pendingActions().size

    suspend fun createInstitutionOrQueue(backend: CentralBackend, name: String): Pair<DirectorOfflineWriteResult, InstitutionDto?> {
        return try {
            DirectorOfflineWriteResult.SENT to backend.api.createInstitution(InstitutionRequest(name))
        } catch (_: IOException) {
            enqueue(PendingDirectorAction(type = "create_institution", institutionName = name))
            DirectorSyncScheduler.enqueueNow(appContext)
            DirectorOfflineWriteResult.QUEUED to null
        }
    }

    suspend fun attachTeacherOrQueue(backend: CentralBackend, email: String): Pair<DirectorOfflineWriteResult, UserDto?> {
        return try {
            DirectorOfflineWriteResult.SENT to backend.api.attachTeacher(email)
        } catch (_: IOException) {
            enqueue(PendingDirectorAction(type = "attach_teacher", email = email))
            DirectorSyncScheduler.enqueueNow(appContext)
            DirectorOfflineWriteResult.QUEUED to null
        }
    }

    suspend fun createNoticeOrQueue(backend: CentralBackend, title: String, body: String): Pair<DirectorOfflineWriteResult, DirectorNoticeDto?> {
        return try {
            DirectorOfflineWriteResult.SENT to backend.api.createDirectorNotice(NoticeRequest(title, body))
        } catch (_: IOException) {
            enqueue(PendingDirectorAction(type = "create_notice", title = title, body = body))
            DirectorSyncScheduler.enqueueNow(appContext)
            DirectorOfflineWriteResult.QUEUED to null
        }
    }

    suspend fun createScheduleOrQueue(backend: CentralBackend, request: ScheduleRequest): Pair<DirectorOfflineWriteResult, ScheduleDto?> {
        return try {
            DirectorOfflineWriteResult.SENT to backend.api.createSchedule(request)
        } catch (_: IOException) {
            enqueue(
                PendingDirectorAction(
                    type = "create_schedule",
                    teacherId = request.teacherId,
                    classId = request.classId,
                    weekday = request.weekday,
                    startTime = request.startTime,
                    endTime = request.endTime,
                    room = request.room,
                )
            )
            DirectorSyncScheduler.enqueueNow(appContext)
            DirectorOfflineWriteResult.QUEUED to null
        }
    }

    suspend fun deleteScheduleOrQueue(backend: CentralBackend, scheduleId: Int): DirectorOfflineWriteResult {
        return try {
            backend.api.deleteSchedule(scheduleId)
            DirectorOfflineWriteResult.SENT
        } catch (_: IOException) {
            enqueue(PendingDirectorAction(type = "delete_schedule", scheduleId = scheduleId))
            DirectorSyncScheduler.enqueueNow(appContext)
            DirectorOfflineWriteResult.QUEUED
        }
    }

    suspend fun flush(backend: CentralBackend): Boolean {
        if (backend.tokenStore.accessToken.isNullOrBlank()) return true
        val actions = pendingActions().toMutableList()
        if (actions.isEmpty()) return true
        val remaining = mutableListOf<PendingDirectorAction>()
        var retryLater = false

        actions.forEach { action ->
            try {
                when (action.type) {
                    "create_institution" -> backend.api.createInstitution(InstitutionRequest(action.institutionName.orEmpty()))
                    "attach_teacher" -> backend.api.attachTeacher(action.email.orEmpty())
                    "create_notice" -> backend.api.createDirectorNotice(NoticeRequest(action.title.orEmpty(), action.body.orEmpty()))
                    "create_schedule" -> backend.api.createSchedule(
                        ScheduleRequest(
                            teacherId = action.teacherId ?: return@forEach,
                            classId = action.classId,
                            weekday = action.weekday ?: return@forEach,
                            startTime = action.startTime.orEmpty(),
                            endTime = action.endTime.orEmpty(),
                            room = action.room.orEmpty(),
                        )
                    )
                    "delete_schedule" -> backend.api.deleteSchedule(action.scheduleId ?: return@forEach)
                }
            } catch (_: IOException) {
                remaining += action
                retryLater = true
            } catch (error: HttpException) {
                if (error.code() == 401 || error.code() == 429 || error.code() >= 500) {
                    remaining += action
                    retryLater = true
                }
            } catch (_: Throwable) {
                remaining += action
                retryLater = true
            }
        }
        savePendingActions(remaining)
        return !retryLater
    }

    suspend fun refreshCaches(backend: CentralBackend): Boolean {
        return runCatching {
            val current = backend.api.me()
            val core = if (current.institutionId == null) {
                DirectorCoreCache(current, null, emptyList(), emptyList(), emptyList())
            } else {
                DirectorCoreCache(
                    current = current,
                    institution = backend.api.institution(),
                    teachers = backend.api.teachers(),
                    classes = backend.api.classes(),
                    notices = backend.api.directorNotices(),
                )
            }
            saveCore(core)
            if (current.institutionId != null) {
                saveAttendance(backend.api.attendanceSummary())
                saveSchedule(backend.api.schedule())
            }
            true
        }.getOrDefault(false)
    }

    private fun enqueue(action: PendingDirectorAction) {
        val next = pendingActions().toMutableList().apply { add(action) }
        savePendingActions(next)
    }

    private fun pendingActions(): List<PendingDirectorAction> {
        val raw = prefs.getString("pending", null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<PendingDirectorAction>>() {}.type
            gson.fromJson<List<PendingDirectorAction>>(raw, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun savePendingActions(items: List<PendingDirectorAction>) {
        prefs.edit().putString("pending", gson.toJson(items)).apply()
    }

    private inline fun <reified T> readList(key: String): List<T> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<T>>() {}.type
            gson.fromJson<List<T>>(raw, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun writeList(key: String, items: Any) {
        prefs.edit().putString(key, gson.toJson(items)).apply()
    }
}

class DirectorPendingSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val backend = CentralBackend(applicationContext)
        if (backend.tokenStore.accessToken.isNullOrBlank()) return Result.success()
        val store = DirectorOfflineStore(applicationContext)
        val flushed = store.flush(backend)
        if (flushed) store.refreshCaches(backend)
        return if (flushed) Result.success() else Result.retry()
    }
}

object DirectorSyncScheduler {
    private const val IMMEDIATE = "director-offline-pending-sync"
    private const val PERIODIC = "director-offline-periodic-sync"

    private fun constraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun ensurePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<DirectorPendingSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun enqueueNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<DirectorPendingSyncWorker>()
            .setConstraints(constraints())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
