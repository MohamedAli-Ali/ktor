package com.froute.data.model.voucher

import com.froute.data.model.user.UserField
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

object VoucherField {
    const val MONGO_ID = "_id"
    const val VOUCHER_ID = "voucher_id"
    const val CODE = "code"
    const val EXPIRE_DATE = "expire_date"
    const val DISCOUNT_PERCENTAGE = "discount_percentage"
    const val USER_BY = "used_by"

    const val USER_NAME = "user_name"
    const val USED_AT = "used_at"
}


@Serializable
data class Voucher(
    @SerialName(VoucherField.MONGO_ID)
    val mongoId: String = ObjectId().toHexString(),

    @SerialName(VoucherField.VOUCHER_ID)
    val id: Int,

    @SerialName(VoucherField.CODE)
    val code: String,

    @SerialName(VoucherField.DISCOUNT_PERCENTAGE)
    val discountPercentage: Double,

    @SerialName(VoucherField.EXPIRE_DATE)
    val expireDate: String,

    @SerialName(VoucherField.USER_BY)
    val usedBy: List<UsedByInfo> = emptyList()
){
    @Serializable
    data class UsedByInfo(
        @SerialName(VoucherField.USER_NAME)
        val username: String,
        @SerialName(VoucherField.USED_AT)
        val usedAt: String
    )
}