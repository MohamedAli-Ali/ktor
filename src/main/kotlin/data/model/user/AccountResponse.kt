package com.froute.data.model.user

import com.froute.utils.getCurrentTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class AccountResponse(
    @SerialName("_id")
    val id: String,
    @SerialName("user_name")
    val userName: String,
    @SerialName("password")
    val password: String,
    @SerialName("create_at")
    val createdAt: String = getCurrentTime(),
)
