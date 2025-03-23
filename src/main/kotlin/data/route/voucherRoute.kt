package com.froute.data.route

import com.froute.data.database.MongoDB
import com.froute.data.model.voucher.Voucher
import com.froute.data.model.voucher.VoucherResponse
import com.froute.utils.GenerateId.genVoucherCode
import com.froute.utils.GenerateId.getNextVoucherId
import com.froute.utils.Result
import com.froute.utils.getCurrentTime
import com.froute.utils.getCurrentTimePlusDuration
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days


fun Routing.voucherRoute() {

    val voucherStore = MongoDB().VoucherStore()

    post("/create-vouchers") {

        try {
            val amount = call.request.queryParameters["amount"]?.toIntOrNull()

            val totalVouchers = if (amount == null) {
                return@post call.respondText(text = "Error: Missing 'amount' parameter in the request.")
            } else if (amount < 10 || amount > 10000) {
                return@post call.respondText(text = "Error: 'amount' must be between 10 and 10000")
            } else {
                amount
            }

            val batchSize = totalVouchers / 10
            val startTime = System.currentTimeMillis()
            val vouchers = mutableListOf<Voucher>()

            withContext(Dispatchers.IO) {
                (0 until totalVouchers / batchSize).map {
                    launch {
                        val batch = (0 until batchSize).map {
                            Voucher(
                                id = getNextVoucherId(),
                                code = genVoucherCode(),
                                discountPercentage = 0.0,
                                expireDate = getCurrentTimePlusDuration(20.days)
                            )
                        }
                        vouchers.addAll(batch)
                        voucherStore.createVouchers(batch)
                        println("Batch $it completed")
                    }
                }.joinAll()
            }

            val elapsed = (System.currentTimeMillis() - startTime) / 1000

            call.respond(HttpStatusCode.OK, VoucherResponse(
                list = vouchers,
                size = totalVouchers,
                message = "Vouchers created: ${vouchers.firstOrNull()?.id} - ${vouchers.lastOrNull()?.id}",
                estimateTime = VoucherResponse.Time(minutes = elapsed / 60, seconds = elapsed % 60),
                success = true
            )
            )

        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, e.message ?: "Voucher creation failed.")
        }
    }



    get("/vouchers") {
        try {
            val pageSize = call.queryParameters["page_size"]?.toIntOrNull()
            val page = call.queryParameters["page"]?.toIntOrNull()
            val query = call.queryParameters["query"]

            val vouchers = voucherStore
                .getAllVouchers(query = query, page = page, pageSize = pageSize)

            call.respond(
                status = HttpStatusCode.OK,
                message = VoucherResponse(
                    list = vouchers,
                    size = vouchers.size,
                    success = true
                )
            )
        }catch (ex: Exception){
            call.respondText(text = ex.message.toString())
        }
    }


    patch("/use_voucher") {
        try {
            val userId = call.queryParameters["user_id"] ?: return@patch call.respondText(text = "Error: Missing 'user_id' parameter in the request.")
            val code = call.queryParameters["code"] ?: return@patch call.respondText(text = "Error: Missing 'code' parameter in the request.")
            val usedAt = getCurrentTime()
            val result = voucherStore.useVoucher(code = code, userId = userId, usedAt = usedAt)
            when(result){
                is Result.Success-> {
                    call.respondText(
                        status = HttpStatusCode.OK,
                        text = "Voucher used successfully"
                    )
                }
                is Result.Error->{
                    call.respondText(
                        status = HttpStatusCode.BadRequest,
                        text = result.exception.localizedMessage
                    )
                }
            }
        }catch (ex: Exception){
            call.respondText(
                status = HttpStatusCode.BadRequest,
                text = ex.message.toString()
            )
        }
    }








}