package com.stkrfx.poshak.widget

import android.content.Context
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stkrfx.poshak.data.ApiException
import com.stkrfx.poshak.data.PoshakApi
import java.time.LocalDate
import java.time.YearMonth

class AttendanceWidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val manager = AppWidgetManager.getInstance(applicationContext)
        val component = ComponentName(applicationContext, TodayAttendanceWidgetProvider::class.java)
        if (manager.getAppWidgetIds(component).isEmpty()) return Result.success()
        val api = PoshakApi(applicationContext)
        if (!api.hasLocalSession()) {
            AttendanceWidgetUpdater.update(applicationContext, "LOGIN_REQUIRED")
            return Result.success()
        }
        return try {
            val snapshot = api.fetchAttendance()
            val today = LocalDate.now()
            val status = snapshot.months
                .firstOrNull { it.yearMonth == YearMonth.from(today) }
                ?.days?.firstOrNull { it.date == today }?.status ?: "UNMARKED"
            AttendanceWidgetUpdater.update(applicationContext, status)
            Result.success()
        } catch (error: ApiException) {
            if (error.statusCode == 401 || error.apiCode in setOf("ACCOUNT_BLOCKED", "SESSION_REVOKED", "SESSION_EXPIRED")) {
                AttendanceWidgetUpdater.update(applicationContext, "LOGIN_REQUIRED")
                Result.success()
            } else {
                Result.retry()
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
