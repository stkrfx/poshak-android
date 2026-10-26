package com.stkrfx.poshak.ui.workspace

internal enum class MainTab { HOME, DOCUMENTS, UPDATES, PROFILE }
internal enum class ProfilePage { OVERVIEW, EDIT, CHANGE_PASSWORD, DEVICES, SETTINGS, APP_LOCK, SET_PIN, SWITCH_ACCOUNT, AVATAR, EMAIL_OTP, SALARY_PIN }

internal data class LocalAccount(val label: String, val subtitle: String, val isPrimary: Boolean = false)
internal data class LocalNotice(val title: String, val body: String, val date: String, val unread: Boolean = true)
internal data class LocalDevice(val name: String, val detail: String, val current: Boolean = false)
