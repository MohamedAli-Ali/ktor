package com.froute.data.model.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class AccountRequest(
    @SerialName("_id")
    val id: String,
    @SerialName("user_name")
    val userName: String,
    val password: String
)