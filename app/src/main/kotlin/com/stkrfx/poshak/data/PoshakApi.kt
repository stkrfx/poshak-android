package com.stkrfx.poshak.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.stkrfx.poshak.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** API adapter for the existing Poshak Go backend. All blocking networking runs on IO. */
class PoshakApi(context: Context) {
    private val appContext = context.applicationContext
    private val sessionStore = SecureSessionStore(appContext)
    private val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')

    suspend fun checkAppVersion(): AppUpdateStatus = withContext(Dispatchers.IO) {
        val response = execute(
            "GET",
            "/api/v1/system/version?version_code=${BuildConfig.APP_VERSION_CODE}",
        )
        val data = response.optJSONObject("data")
            ?: throw ApiException(500, "INVALID_RESPONSE", "The server returned an incomplete version response.")
        AppUpdateStatus(
            forceUpdateRequired = data.optBoolean("update_required", false),
            latestRelease = data.optJSONObject("latest_release")?.let(::releaseFrom),
            requiredRelease = data.optJSONObject("required_release")?.let(::releaseFrom),
        )
    }

    suspend fun login(identifier: String, password: String): EmployeeProfile = withContext(Dispatchers.IO) {
        val hardwareId = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ANDROID_ID,
        )?.takeIf { it.isNotBlank() } ?: "android-${Build.FINGERPRINT.hashCode()}"

        val request = JSONObject()
            .put("identifier", identifier.trim())
            .put("password", password)
            .put("device_hardware_id", hardwareId)
            .put("device_brand", Build.MANUFACTURER.ifBlank { "Android" })
            .put("device_model", Build.MODEL.ifBlank { "Android device" })
            .put("os_version", "Android ${Build.VERSION.RELEASE}")

        val response = execute("POST", "/api/v1/auth/login", request)
        val data = response.optJSONObject("data")
            ?: throw ApiException(500, "INVALID_RESPONSE", "The server returned an incomplete login response.")
        val credentials = credentialsFrom(data)
        val profileJson = data.optJSONObject("user")
            ?: throw ApiException(500, "INVALID_RESPONSE", "The server did not return your account details.")
        sessionStore.save(credentials)
        profileFrom(profileJson)
    }

    suspend fun restoreSession(): EmployeeProfile? = withContext(Dispatchers.IO) {
        if (sessionStore.load() == null) return@withContext null
        try {
            val response = authenticatedRequest("GET", "/api/v1/profile")
            val profile = response.optJSONObject("data")
                ?: throw ApiException(500, "INVALID_RESPONSE", "The server returned an incomplete profile response.")
            profileFrom(profile)
        } catch (error: ApiException) {
            if (error.statusCode == 401 || error.apiCode == "ACCOUNT_BLOCKED") {
                sessionStore.clear()
                if (error.statusCode == 401) return@withContext null
            }
            throw error
        }
    }

    /** Fetches the latest account profile without changing the current navigation state. */
    suspend fun fetchProfile(): EmployeeProfile = withContext(Dispatchers.IO) {
        val response = authenticatedRequest("GET", "/api/v1/profile")
        val data = response.optJSONObject("data")
            ?: throw ApiException(500, "INVALID_RESPONSE", "The server returned an incomplete profile response.")
        profileFrom(data)
    }

    suspend fun fetchAttendance(): AttendanceSnapshot = withContext(Dispatchers.IO) {
        val response = authenticatedRequest("GET", "/api/v1/attendance")
        val data = response.optJSONObject("data")
            ?: throw ApiException(500, "INVALID_RESPONSE", "The server returned an incomplete attendance response.")

        val allowedNames = data.optJSONArray("allowed_months").toStringList()
        val monthArray = data.optJSONArray("months") ?: JSONArray()
        val months = buildList {
            for (index in 0 until monthArray.length()) {
                val monthJson = monthArray.optJSONObject(index) ?: continue
                val sheetName = monthJson.optString("month_sheet_name").trim()
                val yearMonth = parseYearMonth(sheetName) ?: continue
                val summaryJson = monthJson.optJSONObject("summary")
                val summary = summaryJson?.let {
                    AttendanceSummary(
                        present = it.optString("total_present_p", "0"),
                        absent = it.optString("total_absent_a", "0"),
                        payable = it.optString("total_payable_g", "0"),
                        paid = it.optString("total_paid_t", "0"),
                    )
                }
                val dayArray = monthJson.optJSONArray("days") ?: JSONArray()
                val days = buildList {
                    for (dayIndex in 0 until dayArray.length()) {
                        val day = dayArray.optJSONObject(dayIndex) ?: continue
                        val date = runCatching { LocalDate.parse(day.optString("attendance_date")) }.getOrNull()
                            ?: continue
                        add(
                            AttendanceDay(
                                date = date,
                                status = day.optString("status", "UNMARKED").uppercase(Locale.ROOT),
                                note = day.nullableString("note"),
                            ),
                        )
                    }
                }
                add(AttendanceMonth(sheetName, yearMonth, summary, days))
            }
        }.sortedByDescending { it.yearMonth }

        AttendanceSnapshot(
            allowedMonthNames = allowedNames,
            months = months,
        )
    }

    suspend fun sendPasswordResetOtp(email: String): String = withContext(Dispatchers.IO) {
        val response = execute(
            "POST",
            "/api/v1/auth/forgot-password/email-otp",
            JSONObject().put("email", email.trim()),
        )
        response.optString("message", "A verification code has been sent to your email.")
    }

    suspend fun resetPasswordWithEmailOtp(email: String, otp: String, newPassword: String): String =
        withContext(Dispatchers.IO) {
            val response = execute(
                "POST",
                "/api/v1/auth/reset-password",
                JSONObject()
                    .put("method", "EMAIL_OTP")
                    .put("email", email.trim())
                    .put("otp_code", otp.trim())
                    .put("new_password", newPassword),
            )
            sessionStore.clear()
            response.optString("message", "Password updated. You can now log in.")
        }

    suspend fun logoutCurrentDevice() = withContext(Dispatchers.IO) {
        try {
            if (sessionStore.load() != null) {
                authenticatedRequest("POST", "/api/v1/profile/devices/logout-current")
            }
        } finally {
            sessionStore.clear()
        }
    }

    fun hasLocalSession(): Boolean = sessionStore.load() != null

    private fun authenticatedRequest(
        method: String,
        path: String,
        body: JSONObject? = null,
    ): JSONObject {
        val existing = sessionStore.load()
            ?: throw ApiException(401, "SESSION_EXPIRED", "Your session has ended. Please log in again.")

        try {
            return execute(method, path, body, existing.accessToken)
        } catch (error: ApiException) {
            if (error.statusCode == 403 && error.apiCode == "ACCOUNT_BLOCKED") {
                sessionStore.clear()
                throw error
            }
            if (error.statusCode != 401) throw error
        }

        val refreshed = try {
            refresh(existing)
        } catch (error: ApiException) {
            if (error.statusCode == 401 || error.statusCode == 403) sessionStore.clear()
            throw error
        }

        return try {
            execute(method, path, body, refreshed.accessToken)
        } catch (error: ApiException) {
            if (error.statusCode == 401 || error.apiCode == "ACCOUNT_BLOCKED") sessionStore.clear()
            throw error
        }
    }

    private fun refresh(existing: SessionCredentials): SessionCredentials {
        val response = execute(
            "POST",
            "/api/v1/auth/refresh",
            JSONObject()
                .put("session_id", existing.sessionId)
                .put("refresh_token", existing.refreshToken),
        )
        val data = response.optJSONObject("data")
            ?: throw ApiException(500, "INVALID_RESPONSE", "The server returned an incomplete session refresh response.")
        val updated = credentialsFrom(data)
        sessionStore.save(updated)
        return updated
    }

    private fun credentialsFrom(data: JSONObject): SessionCredentials {
        val access = data.optString("access_token")
        val refresh = data.optString("refresh_token")
        val sessionId = data.optString("session_id")
        if (access.isBlank() || refresh.isBlank() || sessionId.isBlank()) {
            throw ApiException(500, "INVALID_RESPONSE", "The server returned incomplete session credentials.")
        }
        return SessionCredentials(access, refresh, sessionId)
    }

    private fun execute(
        method: String,
        path: String,
        body: JSONObject? = null,
        accessToken: String? = null,
    ): JSONObject {
        val connection = try {
            (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("X-App-Version-Code", BuildConfig.APP_VERSION_CODE.toString())
                accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
            }
        } catch (error: Exception) {
            throw ApiException(0, "NETWORK_ERROR", "Could not connect to the Poshak server. Please try again.")
        }

        try {
            if (body != null) {
                connection.outputStream.use { output ->
                    output.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val response = try {
                if (raw.isBlank()) JSONObject() else JSONObject(raw)
            } catch (_: Exception) {
                throw ApiException(status, "INVALID_RESPONSE", "The server returned an unreadable response. Please try again.")
            }

            val success = response.optBoolean("success", status in 200..299)
            if (status !in 200..299 || !success) {
                val code = response.optString("code", "HTTP_$status")
                val message = response.optString("message").takeIf { it.isNotBlank() }
                    ?: "The request failed. Please try again."
                val appRelease = response.optJSONObject("data")
                    ?.optJSONObject("latest_release")
                    ?.let(::releaseFrom)
                throw ApiException(status, code, message, appRelease)
            }
            return response
        } catch (error: ApiException) {
            throw error
        } catch (error: IOException) {
            throw ApiException(0, "NETWORK_ERROR", "The server could not be reached. Check your connection and try again.")
        } catch (error: Exception) {
            throw ApiException(0, "NETWORK_ERROR", "Something went wrong while contacting the server. Please try again.")
        } finally {
            connection.disconnect()
        }
    }

    private fun releaseFrom(json: JSONObject) = AppRelease(
        versionName = json.optString("version_name"),
        versionCode = json.optInt("version_code", 0),
        apkDownloadUrl = json.optString("apk_download_url"),
        releaseNotes = json.optString("release_notes"),
        isForceUpdate = json.optBoolean("is_force_update", false),
    )

    private fun profileFrom(json: JSONObject) = EmployeeProfile(
        id = json.optString("id"),
        name = json.optString("name", "Employee"),
        username = json.optString("username"),
        mobileNumber = json.optString("mobile_number"),
        gender = json.optString("gender"),
        email = json.nullableString("email"),
        dateOfBirth = json.nullableString("dob"),
        isDateOfBirthLocked = json.optBoolean("is_dob_locked", false),
        address = json.nullableString("address"),
        avatarUrl = json.nullableString("avatar_url"),
        canEditProfile = json.optBoolean("can_edit_profile", true),
        canUploadDocuments = json.optBoolean("can_upload_documents", true),
        isBlocked = json.optBoolean("is_blocked", false),
        blockedReason = json.nullableString("blocked_reason"),
    )

    private fun JSONObject.nullableString(key: String): String? =
        optString(key).takeUnless { it.isBlank() || it == "null" }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val value = optString(index).trim()
                if (value.isNotEmpty()) add(value)
            }
        }
    }

    private fun parseYearMonth(name: String): YearMonth? {
        val formats = listOf(
            DateTimeFormatter.ofPattern("MMM uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH),
        )
        return formats.firstNotNullOfOrNull { format ->
            runCatching { YearMonth.parse(name.trim(), format) }.getOrNull()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 20_000
    }
}
