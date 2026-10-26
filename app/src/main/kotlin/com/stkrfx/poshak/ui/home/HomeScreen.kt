package com.stkrfx.poshak.ui.home

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
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
    var pullDistance by remember { mutableStateOf(0f) }
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
                        TextButton(onClick = { monthPickerExpanded = !monthPickerExpanded }) {
                            Text(
                                text = selectedMonth?.let { fullMonthName(it.yearMonth) } ?: "Attendance",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Icon(
                                imageVector = if (monthPickerExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                contentDescription = if (monthPickerExpanded) "Hide months" else "Choose month",
                                modifier = Modifier.padding(start = 2.dp),
                            )
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
                }

                if (monthPickerExpanded && currentSnapshot != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        currentSnapshot.months.sortedBy { it.yearMonth }.takeLast(3).forEach { month ->
                            val isSelected = month.sheetName == selectedSheetName
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedSheetName = month.sheetName
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
                modifier = Modifier.fillMaxSize().padding(innerPadding).pointerInput(loading, currentSnapshot) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, amount -> pullDistance = (pullDistance + amount).coerceIn(0f, 180f) },
                        onDragEnd = { if (pullDistance > 65f && !loading) refreshKey++; pullDistance = 0f },
                        onDragCancel = { pullDistance = 0f },
                    )
                },
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            currentSnapshot == null -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp).pointerInput(loading) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, amount -> pullDistance = (pullDistance + amount).coerceIn(0f, 180f) },
                        onDragEnd = { if (pullDistance > 65f && !loading) refreshKey++; pullDistance = 0f },
                        onDragCancel = { pullDistance = 0f },
                    )
                },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(loadError ?: "Attendance is unavailable.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(14.dp))
                TextButton(onClick = { refreshKey++ }) { Text("Try again") }
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
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 12.dp).pointerInput(loading) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, amount -> pullDistance = (pullDistance + amount).coerceIn(0f, 180f) },
                        onDragEnd = { if (pullDistance > 65f && !loading) refreshKey++; pullDistance = 0f },
                        onDragCancel = { pullDistance = 0f },
                    )
                },
            ) {
                if (loadError != null) {
                    Text(loadError.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
                }
                AnimatedVisibility(
                    visible = showSummary,
                    enter = fadeIn(tween(220)) + expandVertically(tween(260)),
                    exit = fadeOut(tween(160)) + shrinkVertically(tween(220)),
                ) {
                    Column(Modifier.animateContentSize(animationSpec = tween(260))) {
                        SummaryGrid(selectedMonth)
                        Spacer(Modifier.height(10.dp))
                    }
                }
                val orderedMonths = currentSnapshot.months.sortedBy { it.yearMonth }
                val initialPage = (orderedMonths.indexOfFirst { it.sheetName == selectedMonth.sheetName }).coerceAtLeast(0)
                val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { orderedMonths.size })
                androidx.compose.runtime.LaunchedEffect(selectedSheetName, orderedMonths) {
                    val wanted = orderedMonths.indexOfFirst { it.sheetName == selectedSheetName }
                    if (wanted >= 0 && pagerState.currentPage != wanted) pagerState.animateScrollToPage(wanted)
                }
                androidx.compose.runtime.LaunchedEffect(pagerState.currentPage, orderedMonths) {
                    orderedMonths.getOrNull(pagerState.currentPage)?.let { pageMonth ->
                        if (pageMonth.sheetName != selectedSheetName) selectedSheetName = pageMonth.sheetName
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    key = { page -> orderedMonths[page].sheetName },
                    beyondViewportPageCount = 1,
                ) { page ->
                    CalendarGrid(
                        modifier = Modifier.fillMaxSize(),
                        selectedMonth = orderedMonths[page],
                        allMonths = orderedMonths,
                        onSelectMonth = { selectedSheetName = it },
                        onShowNote = { note -> coroutineScope.launch { snackbarHost.showSnackbar(note) } },
                        weekStartsMonday = weekStartsMonday,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatusLegend("Present", PRESENT)
                    StatusLegend("Absent", ABSENT)
                    StatusLegend("Half-day", HALF_DAY)
                    StatusLegend("Unmarked", UNMARKED)
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(
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

    Column(modifier = modifier.pointerInput(selectedMonth, allMonths) {
        var horizontalDistance = 0f
        detectHorizontalDragGestures(
            onHorizontalDrag = { _, amount -> horizontalDistance += amount },
            onDragEnd = {
                if (kotlin.math.abs(horizontalDistance) > 70f) {
                    val sorted = allMonths.sortedBy { it.yearMonth }
                    val currentIndex = sorted.indexOfFirst { it.sheetName == selectedMonth.sheetName }
                    val targetIndex = if (horizontalDistance < 0f) currentIndex + 1 else currentIndex - 1
                    sorted.getOrNull(targetIndex)?.let { onSelectMonth(it.sheetName) }
                }
                horizontalDistance = 0f
            },
            onDragCancel = { horizontalDistance = 0f },
        )
    }) {
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
private fun SummaryGrid(month: AttendanceMonth) {
    val summary = month.summary
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Present", summary?.present ?: "—", Modifier.weight(1f), Color(0xFFD5F5E3))
            SummaryCard("Absent", summary?.absent ?: "—", Modifier.weight(1f), Color(0xFFFADBD8))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Payable days", summary?.payable ?: "—", Modifier.weight(1f), Color(0xFFFFF0B3))
            SummaryCard("Paid days", summary?.paid ?: "—", Modifier.weight(1f), Color(0xFFD6EAF8))
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier, backgroundColor: Color) {
    Card(modifier = modifier, colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = backgroundColor)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = Color(0xFF333333))
            Spacer(Modifier.height(4.dp))
            val numericValue = value.toIntOrNull()
            val animatedValue by animateIntAsState(
                targetValue = numericValue ?: 0,
                animationSpec = tween(durationMillis = 650),
                label = "summary-number-$title",
            )
            Text(
                text = numericValue?.let { animatedValue.toString() } ?: value,
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF171717),
                fontWeight = FontWeight.SemiBold,
            )
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
    yearMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.ENGLISH)

private val PRESENT = Color(0xFFD5F5E3)
private val ABSENT = Color(0xFFFADBD8)
private val HALF_DAY = Color(0xFFFFF0B3)
private val UNMARKED = Color(0xFFE4B83A)
