package com.froute.data.route

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.froute.data.database.MongoDB
import com.froute.data.model.user.AccountRequest
import com.froute.data.model.user.AccountResponse
import com.froute.data.model.fruit.FruitResponse
import com.froute.data.model.user.LoginResponse
import com.froute.data.model.user.User
import com.froute.data.model.user.UserField
import com.froute.utils.GenerateId.getNextUserId
import com.froute.utils.getHashWithSalt
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days


fun Routing.usersRoute(){

    val userStore = MongoDB().UserStore()

    val jwtConfig = environment.config.config("jwt")
    val jwtAudience = jwtConfig.property("audience").getString()
    val jwtDomain = jwtConfig.property("domain").getString()
    val jwtSecret = jwtConfig.property("secret").getString()


    post("/register") {

        val accountRequest = try {
            call.receive<AccountRequest>()
        }catch (ex: Exception){
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = FruitResponse(isSuccess = false, message = "Invalid body format")
            )
            return@post
        }

        if (userStore.checkUserExist(accountRequest.userName)){
            call.respond(
                status = HttpStatusCode.OK,
                message = FruitResponse(isSuccess = false, message = "User already exist")
            )
        }else {
            val newUser = User(
                id = getNextUserId().toString(),
                userName = accountRequest.userName,
                password = getHashWithSalt(accountRequest.password)
            )

            if (userStore.register(user = newUser)){
                call.respond(
                    status = HttpStatusCode.OK,
                    message = AccountResponse(id = newUser.id, userName = newUser.userName, password = newUser.password)
                )
            }else {
                call.respondText("Error happen please try again later")
            }
        }


    }

    post("/login") {
        val user = try {
            call.receive<AccountRequest>()
        }catch (ex: Exception){
            println(ex.message + ex.cause + ex.localizedMessage)
            call.respond(
                status = HttpStatusCode.OK,
                message = FruitResponse(isSuccess = false, message = ex.message + ex.cause)
            )
            return@post
        }


        if (userStore.checkUserExist(user.userName)){
            if (userStore.checkUserNameForPass(user.password, user.userName)){

                // if password is correct generate a new JWT token
                val expiresAt = Clock.System.now().plus(30.days)
                val token = JWT.create()
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .withClaim(UserField.USER_NAME, user.userName)
                    .withExpiresAt(expiresAt.toJavaInstant())
                    .sign(Algorithm.HMAC256(jwtSecret))
                val response = LoginResponse(user.userName, token, expiresAt.toLocalDateTime(TimeZone.UTC).toString())
                call.respond(HttpStatusCode.OK, response)

//                call.respond(
//                    status = HttpStatusCode.OK,
//                    message = FruitResponse(isSuccess = false, message = "Login Successful")
//                )
            }else {
                call.respond(
                    status = HttpStatusCode.OK,
                    message = FruitResponse(isSuccess = false, message = "wrong user name or password")
                )
            }
        }else {
            call.respond(
                status = HttpStatusCode.OK,
                message = FruitResponse(isSuccess = false, message = "user name is not exist")
            )
        }
    }

}