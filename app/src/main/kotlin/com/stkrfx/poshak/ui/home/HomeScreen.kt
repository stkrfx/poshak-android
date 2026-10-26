package com.stkrfx.poshak.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stkrfx.poshak.data.ApiException
import com.stkrfx.poshak.data.AppRelease
import com.stkrfx.poshak.data.AttendanceDay
import com.stkrfx.poshak.data.AttendanceMonth
import com.stkrfx.poshak.data.AttendanceSnapshot
import com.stkrfx.poshak.data.EmployeeProfile
import com.stkrfx.poshak.data.PoshakApi
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    api: PoshakApi,
    profile: EmployeeProfile,
    onSessionExpired: () -> Unit,
    onLogout: () -> Unit,
    onForceUpdate: (AppRelease) -> Unit,
    modifier: Modifier = Modifier,
    salaryLabel: String? = null,
    onSalaryClick: () -> Unit = {},
    onSalaryLongPress: () -> Unit = {},
    weekStartsMonday: Boolean = true,
) {
    var snapshot by remember { mutableStateOf<AttendanceSnapshot?>(null) }
    var loading by rememberSaveable { mutableStateOf(true) }
    var loadError by rememberSaveable { mutableStateOf<String?>(null) }
    var refreshKey by rememberSaveable { mutableIntStateOf(0) }
    var selectedSheetName by rememberSaveable { mutableStateOf<String?>(null) }
    var monthPickerExpanded by rememberSaveable { mutableStateOf(false) }
    var showSummary by rememberSaveable { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        loading = true
        loadError = null
        try {
            val loaded = api.fetchAttendance()
            snapshot = loaded
            val loadedSheetNames = loaded.months.map { it.sheetName }
            if (selectedSheetName == null || selectedSheetName !in loadedSheetNames) {
                selectedSheetName = loaded.months.firstOrNull { it.yearMonth == YearMonth.now() }?.sheetName
                    ?: loaded.months.firstOrNull()?.sheetName
            }
        } catch (exception: ApiException) {
            loadError = exception.message
            when {
                exception.apiCode == "APP_UPDATE_REQUIRED" && exception.appRelease != null -> onForceUpdate(exception.appRelease)
                exception.statusCode == 401 || exception.apiCode == "ACCOUNT_BLOCKED" -> onSessionExpired()
            }
        } catch (_: Exception) {
            loadError = "Could not load attendance. Check your connection and try again."
        } finally {
            loading = false
        }
    }

    val currentSnapshot = snapshot
    val selectedMonth = currentSnapshot?.months?.firstOrNull { it.sheetName == selectedSheetName }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hello, ${profile.name.substringBefore(' ')}", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { monthPickerExpanded = !monthPickerExpanded }) {
                            Text(
                                text = selectedMonth?.let { fullMonthName(it.yearMonth) } ?: "Attendance",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(if (monthPickerExpanded) "⌃" else "⌄", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                    IconButton(onClick = { showSummary = !showSummary }) {
                        Icon(Icons.Filled.Calculate, contentDescription = "Toggle attendance summary")
                    }
                    if (salaryLabel == null) {
                        TextButton(onClick = onSalaryClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("₹", style = MaterialTheme.typography.titleLarge) }
                    } else {
                        Text(
                            text = salaryLabel,
                            modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.secondaryContainer).combinedClickable(onClick = onSalaryClick, onLongClick = onSalaryLongPress).padding(horizontal = 10.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    IconButton(onClick = { refreshKey++ }, enabled = !loading) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh attendance")
                    }
                }

                if (monthPickerExpanded && currentSnapshot != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        currentSnapshot.months.sortedByDescending { it.yearMonth }.take(3).forEach { month ->
                            val isSelected = month.sheetName == selectedSheetName
                            AssistChip(
                                onClick = {
                                    selectedSheetName = month.sheetName
                                    monthPickerExpanded = false
                                },
                                label = {
                                    Text(
                                        month.yearMonth.month.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH).take(3).uppercase(Locale.ENGLISH),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        when {
            loading && currentSnapshot == null -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            currentSnapshot == null -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(loadError ?: "Attendance is unavailable.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(14.dp))
                TextButton(onClick = { refreshKey++ }) { Text("Try again") }
                TextButton(onClick = onLogout) { Text("Sign out") }
            }

            selectedMonth == null -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No attendance months are available yet.", textAlign = TextAlign.Center)
                TextButton(onClick = { refreshKey++ }) { Text("Refresh") }
            }

            else -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            ) {
                if (loadError != null) {
                    Text(loadError.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
                }
                if (showSummary) {
                    SummaryGrid(selectedMonth)
                    Spacer(Modifier.height(10.dp))
                }
                CalendarGrid(
                    selectedMonth = selectedMonth,
                    allMonths = currentSnapshot.months,
                    onSelectMonth = { selectedSheetName = it },
                    onShowNote = { note ->
                        // Notes are only surfaced after the employee taps a date that contains one.
                        coroutineScope.launch { snackbarHost.showSnackbar(note) }
                    },
                    weekStartsMonday = weekStartsMonday,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatusLegend("Present", PRESENT)
                    StatusLegend("Absent", ABSENT)
                    StatusLegend("Half-day", HALF_DAY)
                    StatusLegend("Unmarked", UNMARKED)
                }
                Spacer(Modifier.height(18.dp))
                TextButton(
                    onClick = {
                        // Logout revokes the server session when reachable and always clears local credentials.
                        onLogout()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("Sign out") }
            }
        }
    }
}

@Composable
private fun CalendarGrid(
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

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        (0..6).forEach { index ->
            val weekday = if (weekStartsMonday) DayOfWeek.of(index + 1) else if (index == 0) DayOfWeek.SUNDAY else DayOfWeek.of(index)
            Text(
                text = weekday.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH).take(3),
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (weekday == today.dayOfWeek) FontWeight.Bold else FontWeight.Medium,
                color = if (weekday == today.dayOfWeek) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    repeat(6) { weekIndex ->
        Row(modifier = Modifier.fillMaxWidth()) {
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
                        .height(44.dp)
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
                                        !isInVisibleMonth && !canNavigateToMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f)
                                        !isInVisibleMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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

@Composable
private fun SummaryGrid(month: AttendanceMonth) {
    val summary = month.summary
    Column {
        Text("Monthly summary", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Present", summary?.present ?: "—", Modifier.weight(1f))
            SummaryCard("Absent", summary?.absent ?: "—", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Payable days", summary?.payable ?: "—", Modifier.weight(1f))
            SummaryCard("Paid days", summary?.paid ?: "—", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StatusLegend(label: String, color: Color) {
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

private fun fullMonthName(yearMonth: YearMonth): String =
    "${yearMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.ENGLISH)} ${yearMonth.year}"

private val PRESENT = Color(0xFFD5F5E3)
private val ABSENT = Color(0xFFFADBD8)
private val HALF_DAY = Color(0xFFFFF0B3)
private val UNMARKED = Color(0xFFE4B83A)
