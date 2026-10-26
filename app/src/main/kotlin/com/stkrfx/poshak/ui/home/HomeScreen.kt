package com.stkrfx.poshak.ui.home

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.offset
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
    val refreshThresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 72.dp.toPx() }
    var selectedSheetName by rememberSaveable { mutableStateOf<String?>(null) }
    var monthPickerExpanded by rememberSaveable { mutableStateOf(false) }
    var showSummary by rememberSaveable { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val widgetContext = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(refreshKey) {
        loading = true
        loadError = null
        try {
            val loaded = api.fetchAttendance()
            snapshot = loaded
            val today = LocalDate.now()
            val todayStatus = loaded.months.firstOrNull { it.yearMonth == YearMonth.from(today) }
                ?.days?.firstOrNull { it.date == today }?.status ?: "UNMARKED"
            com.stkrfx.poshak.widget.AttendanceWidgetUpdater.update(widgetContext, todayStatus)
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

                val monthChipOffset by animateDpAsState(
                    targetValue = if (monthPickerExpanded) 0.dp else (-8).dp,
                    animationSpec = tween(220),
                    label = "month-chip-offset",
                )
                AnimatedVisibility(
                    visible = monthPickerExpanded && currentSnapshot != null,
                    enter = fadeIn(tween(220)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(180)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().offset(y = monthChipOffset),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        currentSnapshot?.months?.sortedBy { it.yearMonth }?.takeLast(3)?.forEach { month ->
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
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding).pointerInput(loading, refreshThresholdPx) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, amount ->
                        pullDistance = (pullDistance + amount).coerceIn(0f, refreshThresholdPx * 1.5f)
                    },
                    onDragEnd = {
                        val shouldRefresh = pullDistance >= refreshThresholdPx && !loading
                        pullDistance = 0f
                        if (shouldRefresh) refreshKey++
                    },
                    onDragCancel = { pullDistance = 0f },
                )
            },
        ) {
        when {
            loading && currentSnapshot == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            currentSnapshot == null -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(loadError ?: "Attendance is unavailable.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(14.dp))
                TextButton(onClick = { refreshKey++ }) { Text("Try again") }
            }

            selectedMonth == null -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No attendance months are available yet.", textAlign = TextAlign.Center)
                TextButton(onClick = { refreshKey++ }) { Text("Refresh") }
            }

            else -> Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
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
        AnimatedVisibility(
            visible = pullDistance > 0f || (loading && currentSnapshot != null),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
            enter = fadeIn(tween(120)) + expandVertically(tween(160)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(140)),
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (loading && currentSnapshot != null) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text(if (pullDistance >= refreshThresholdPx) "↻" else "↓", style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            loading && currentSnapshot != null -> "Refreshing attendance…"
                            pullDistance >= refreshThresholdPx -> "Release to refresh"
                            else -> "Pull down to refresh"
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        }
    }
}

