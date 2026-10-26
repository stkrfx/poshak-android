package com.stkrfx.poshak.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class TodayAttendanceWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        AttendanceWidgetScheduler.schedule(context)
        AttendanceWidgetUpdater.renderAll(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        AttendanceWidgetScheduler.schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        AttendanceWidgetScheduler.cancel(context)
    }
}
