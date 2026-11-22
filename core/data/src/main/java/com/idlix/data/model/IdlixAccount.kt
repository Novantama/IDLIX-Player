package com.idlix.data.model

import kotlinx.serialization.Serializable

@Serializable
data class IdlixAccount(
    val isLoggedIn: Boolean = false,
    val email: String = "",
    val username: String = "",
    val plan: String = "",
    val status: String = "Belum terhubung",
    val lastUpdated: Long = 0L
)

@Serializable
data class IdlixAuthResponse(
    val success: Boolean = false,
    val message: String = "",
    val status: String = "",
    val user: IdlixUserInfo? = null
)

@Serializable
data class IdlixUserInfo(
    val email: String = "",
    val username: String = "",
    val plan: String = ""
)

@Serializable
data class IdlixAuthStatusResponse(
    val success: Boolean = false,
    val isLoggedIn: Boolean = false,
    val status: String = "",
    val identifier: String = "",
    val user: IdlixUserInfo? = null,
    val serverUrl: String = ""
)
