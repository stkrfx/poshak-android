package com.stkrfx.poshak.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.stkrfx.poshak.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class TodayAttendanceWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { id -> updateOne(context, appWidgetManager, id) }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, TodayAttendanceWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) updateAllIds(context, manager, ids)
        }

        private fun updateAllIds(context: Context, manager: AppWidgetManager, ids: IntArray) {
            ids.forEach { updateOne(context, manager, it) }
        }

        private fun updateOne(context: Context, manager: AppWidgetManager, id: Int) {
            val preferences = context.getSharedPreferences("poshak_widget_data", Context.MODE_PRIVATE)
            val date = runCatching {
                LocalDate.parse(preferences.getString("today_date", null) ?: LocalDate.now().toString())
            }.getOrDefault(LocalDate.now())
            val status = preferences.getString("today_status", "UNMARKED") ?: "UNMARKED"
            val label = when (status.uppercase(Locale.ROOT)) {
                "P", "PRESENT" -> "Present"
                "A", "ABSENT" -> "Absent"
                "H", "HD", "HALF_DAY", "HALF-DAY", "HALFDAY" -> "Half-day"
                else -> "Not marked yet"
            }
            val views = RemoteViews(context.packageName, R.layout.widget_today_attendance).apply {
                setTextViewText(R.id.widget_date, date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())))
                setTextViewText(R.id.widget_status, label)
                setTextViewText(R.id.widget_title, context.getString(R.string.app_name))
            }
            manager.updateAppWidget(id, views)
        }
    }
}
