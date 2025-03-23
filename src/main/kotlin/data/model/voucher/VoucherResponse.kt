package com.froute.data.model.voucher

import kotlinx.serialization.Serializable

@Serializable
data class VoucherResponse(
    val list: List<Voucher> = emptyList(),
    val size: Int = 0,
    val message: String = "",
    val estimateTime: Time = Time(),
    val success: Boolean,
){
    @Serializable
     data class Time(
        val minutes: Long = 0,
        val seconds: Long = 0
    )
}
