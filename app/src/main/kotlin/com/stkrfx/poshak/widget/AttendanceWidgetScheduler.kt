package com.stkrfx.poshak.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import java.util.concurrent.TimeUnit

object AttendanceWidgetScheduler {
    private const val PERIODIC_NAME = "poshak-attendance-widget-periodic"
    private const val IMMEDIATE_NAME = "poshak-attendance-widget-immediate"

    fun schedule(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val widget = ComponentName(appContext, TodayAttendanceWidgetProvider::class.java)
        if (manager.getAppWidgetIds(widget).isEmpty()) return
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodic = PeriodicWorkRequestBuilder<AttendanceWidgetRefreshWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        val work = WorkManager.getInstance(appContext)
        work.enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, periodic)
        requestNow(appContext)
    }

    fun requestNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<AttendanceWidgetRefreshWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(IMMEDIATE_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(PERIODIC_NAME)
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(IMMEDIATE_NAME)
    }
}
