package com.stkrfx.poshak.data

import java.time.LocalDate
import java.time.YearMonth

data class SessionCredentials(
    val accessToken: String,
    val refreshToken: String,
    val sessionId: String,
)

data class EmployeeProfile(
    val id: String,
    val name: String,
    val username: String,
    val mobileNumber: String,
    val gender: String,
    val email: String?,
    val dateOfBirth: String?,
    val isDateOfBirthLocked: Boolean,
    val address: String?,
    val avatarUrl: String?,
    val canEditProfile: Boolean,
    val canUploadDocuments: Boolean,
    val isBlocked: Boolean,
    val blockedReason: String?,
)

data class AttendanceDay(
    val date: LocalDate,
    val status: String,
    val note: String?,
)

data class AttendanceSummary(
    val present: String,
    val absent: String,
    val payable: String,
    val paid: String,
)

data class AttendanceMonth(
    val sheetName: String,
    val yearMonth: YearMonth,
    val summary: AttendanceSummary?,
    val days: List<AttendanceDay>,
)

data class AttendanceSnapshot(
    val allowedMonthNames: List<String>,
    val months: List<AttendanceMonth>,
)

data class AppRelease(
    val versionName: String,
    val versionCode: Int,
    val apkDownloadUrl: String,
    val releaseNotes: String,
    val isForceUpdate: Boolean,
)

data class AppUpdateStatus(
    val forceUpdateRequired: Boolean,
    val latestRelease: AppRelease?,
    val requiredRelease: AppRelease?,
)

class ApiException(
    val statusCode: Int,
    val apiCode: String,
    override val message: String,
    val appRelease: AppRelease? = null,
) : Exception(message)
