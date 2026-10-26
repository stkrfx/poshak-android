package com.stkrfx.poshak.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.stkrfx.poshak.MainActivity
import com.stkrfx.poshak.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A small persistence/rendering boundary shared by foreground and background refreshes. */
object AttendanceWidgetUpdater {
    private const val PREFS = "poshak_widget_data"

    fun update(context: Context, status: String) {
        val normalized = normalize(status)
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("today_date", LocalDate.now().toString())
            .putString("today_status", normalized)
            .apply()
        renderAll(context.applicationContext)
    }

    fun renderAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, TodayAttendanceWidgetProvider::class.java)
        manager.getAppWidgetIds(component).forEach { id -> renderOne(context, manager, id) }
    }

    private fun renderOne(context: Context, manager: AppWidgetManager, id: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now()
        val storedDate = prefs.getString("today_date", null)
        val rawStatus = prefs.getString("today_status", "UNMARKED") ?: "UNMARKED"
        // Never carry yesterday's attendance status into a new date before today's sync.
        val status = if (storedDate == today.toString()) normalize(rawStatus) else "UNMARKED"
        val todayFormatter = DateTimeFormatter.ofPattern("EEEE  •  MMMM yyyy", Locale.getDefault())
        val (label, background, accent) = when (status) {
            "PRESENT" -> Triple("PRESENT", R.drawable.widget_bg_present, Color.rgb(20, 83, 45))
            "ABSENT" -> Triple("ABSENT", R.drawable.widget_bg_absent, Color.rgb(153, 27, 27))
            "HALF_DAY" -> Triple("HALF DAY", R.drawable.widget_bg_half_day, Color.rgb(133, 77, 14))
            "LOGIN_REQUIRED" -> Triple("OPEN POSHAK TO SYNC", R.drawable.widget_bg_unmarked, Color.rgb(69, 80, 69))
            else -> Triple("NOT MARKED YET", R.drawable.widget_bg_unmarked, Color.rgb(133, 77, 14))
        }

        val openApp = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val views = RemoteViews(context.packageName, R.layout.widget_today_attendance).apply {
            setInt(R.id.widget_card, "setBackgroundResource", background)
            setTextViewText(R.id.widget_date, today.dayOfMonth.toString())
            setTextViewText(R.id.widget_month, todayFormatter.format(today).uppercase(Locale.getDefault()))
            setTextViewText(R.id.widget_status, label)
            setTextColor(R.id.widget_status, accent)
            setOnClickPendingIntent(R.id.widget_card, openApp)
        }
        manager.updateAppWidget(id, views)
    }

    private fun normalize(value: String): String = when (value.trim().uppercase(Locale.ROOT)) {
        "P", "PRESENT" -> "PRESENT"
        "A", "ABSENT" -> "ABSENT"
        "H", "HD", "HALF_DAY", "HALF-DAY", "HALFDAY" -> "HALF_DAY"
        "LOGIN_REQUIRED", "SESSION_EXPIRED", "SESSION_REVOKED", "ACCOUNT_BLOCKED" -> "LOGIN_REQUIRED"
        else -> "UNMARKED"
    }
}
