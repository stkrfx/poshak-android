package com.stkrfx.poshak

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object StartupRoute : NavKey
@Serializable data object LoginRoute : NavKey
@Serializable data object ForgotPasswordRoute : NavKey
@Serializable data object EmailOtpRoute : NavKey
@Serializable data object ResetPasswordRoute : NavKey
@Serializable data object HomeRoute : NavKey
@Serializable data object ForceUpdateRoute : NavKey
