package com.froute.data.model.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    @SerialName("user_name")
    val userName: String,
    val token: String,
    @SerialName("expired_at")
    val expiredAt: String
)