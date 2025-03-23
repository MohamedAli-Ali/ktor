package com.froute.data.model.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

object UserField {
    const val MONGO_ID = "_id"
    const val USER_ID = "id"
    const val USER_NAME = "user_name"
    const val PASSWORD = "password"
}


@Serializable
data class User(
    @SerialName(UserField.MONGO_ID)
    val mongoId: String = ObjectId().toString(),
    @SerialName(UserField.USER_ID)
    val id: String,
    @SerialName(UserField.USER_NAME)
    val userName: String,
    val password: String
)
