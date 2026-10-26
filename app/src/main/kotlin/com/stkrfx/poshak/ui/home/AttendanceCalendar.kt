@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.stkrfx.poshak.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stkrfx.poshak.data.AttendanceMonth
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@Composable
internal fun CalendarGrid(
    modifier: Modifier = Modifier,
    selectedMonth: AttendanceMonth,
    allMonths: List<AttendanceMonth>,
    onSelectMonth: (String) -> Unit,
    onShowNote: (String) -> Unit,
    weekStartsMonday: Boolean,
) {
    val start = selectedMonth.yearMonth.atDay(1)
    val startOffset = if (weekStartsMonday) start.dayOfWeek.value - 1 else start.dayOfWeek.value % 7
    val firstCellDate = start.minusDays(startOffset.toLong())
    val allowedYearMonths = allMonths.map { it.yearMonth }.toSet()
    val daysByDate = allMonths.flatMap { it.days }.associateBy { it.date }
    val today = LocalDate.now()

    Column(modifier = modifier) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        (0..6).forEach { index ->
            val weekday = if (weekStartsMonday) DayOfWeek.of(index + 1) else if (index == 0) DayOfWeek.SUNDAY else DayOfWeek.of(index)
            Text(
                text = weekday.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH).take(3),
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selectedMonth.yearMonth == YearMonth.from(today) && weekday == today.dayOfWeek) FontWeight.Bold else FontWeight.Normal,
                color = if (selectedMonth.yearMonth == YearMonth.from(today) && weekday == today.dayOfWeek) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    repeat(6) { weekIndex ->
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            repeat(7) { dayIndex ->
                val date = firstCellDate.plusDays((weekIndex * 7 + dayIndex).toLong())
                val record = daysByDate[date]
                val status = record?.status ?: if (date == today) "UNMARKED" else "NA"
                val isInVisibleMonth = YearMonth.from(date) == selectedMonth.yearMonth
                val canNavigateToMonth = YearMonth.from(date) in allowedYearMonths
                val backgroundColor = statusColor(status, date == today)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(2.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(backgroundColor)
                        .clickable {
                            if (!isInVisibleMonth && canNavigateToMonth) {
                                allMonths.firstOrNull { it.yearMonth == YearMonth.from(date) }?.let { onSelectMonth(it.sheetName) }
                            }
                            record?.note?.takeIf { it.isNotBlank() }?.let(onShowNote)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxSize().padding(2.dp)) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (date == today) {
                                Box(
                                    modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(date.dayOfMonth.toString(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    color = when {
                                        date == today -> MaterialTheme.colorScheme.onPrimary
                                        status == "P" || status == "PRESENT" || status == "A" || status == "ABSENT" || status == "HD" || status == "HALF_DAY" || status == "HALFDAY" ->
                                            if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF151515) else Color(0xFF151515)
                                        !isInVisibleMonth && !canNavigateToMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
                                        !isInVisibleMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                        if (!record?.note.isNullOrBlank()) {
                            Box(
                                modifier = Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
internal fun StatusLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun statusColor(status: String, isToday: Boolean): Color = when (status.uppercase(Locale.ROOT)) {
    "P", "PRESENT" -> PRESENT
    "A", "ABSENT" -> ABSENT
    "H", "HALF_DAY", "HALF-DAY" -> HALF_DAY
    "UNMARKED" -> if (isToday) UNMARKED else Color.Transparent
    else -> if (isToday) UNMARKED else Color.Transparent
}

internal fun fullMonthName(yearMonth: YearMonth): String =
    yearMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.ENGLISH)

internal val PRESENT = Color(0xFFD5F5E3)
internal val ABSENT = Color(0xFFFADBD8)
internal val HALF_DAY = Color(0xFFFFF0B3)
internal val UNMARKED = Color(0xFFE4B83A)
