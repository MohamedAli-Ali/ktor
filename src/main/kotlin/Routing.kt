package com.froute


import com.froute.data.route.fruitsRoute
import com.froute.data.route.usersRoute
import com.froute.data.route.voucherRoute
import com.froute.utils.Constants
import com.froute.utils.Constants.EXTERNAL_FRUIT_IMAGE_PATH
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

import java.io.File

fun Application.configureRouting() {
    routing {

        fruitsRoute()
        usersRoute()
        voucherRoute()

        staticFiles(remotePath = EXTERNAL_FRUIT_IMAGE_PATH, dir = File(Constants.FRUIT_IMAGE_PATH))

    }
}
