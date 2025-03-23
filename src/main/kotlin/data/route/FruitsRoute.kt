package com.froute.data.route

import com.froute.data.database.MongoDB
import com.froute.data.model.fruit.Fruit
import com.froute.data.model.fruit.FruitField
import com.froute.data.model.fruit.FruitResponse
import com.froute.data.model.user.UserField
import com.froute.utils.Constants
import com.froute.utils.GenerateId.getNextFruitId
import com.froute.utils.save
import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File


fun Routing.fruitsRoute(){
    val fruitStore = MongoDB().FruitStore()


    get("/fruits/{sortedBy?}/{sort_direction?}/{season[]?}/{country[]?}") {
        val sortedBy = when (call.queryParameters["sortedBy"]?.lowercase() ?: "name") {
            "name" -> Fruit::name.name
            "id" -> Fruit::id.name
            else -> return@get call.respond(
                status = HttpStatusCode.BadRequest,
                message = FruitResponse(isSuccess = false, message = "invalid parameter for sorted field")
            )
        }
        val sortDirection = when (call.parameters["sort_direction"]?.lowercase() ?: "asc") {
            "dec" -> -1
            "asc" -> 1
            else -> return@get call.respond(
                HttpStatusCode.BadRequest,
                FruitResponse(isSuccess = false, message = "invalid parameter for sort_direction")
            )
        }
        val addedBy = call.queryParameters[FruitField.ADDED_BY]

        val countries = call.parameters.getAll("country") ?: emptyList()
        val query = call.queryParameters["query"]

        val seasons = mutableListOf<Fruit.Season>()
        call.parameters.getAll("season")?.forEach { name ->
            seasons.add(
                when(name.lowercase()){
                    "summer" -> Fruit.Season.Summer
                    "spring" -> Fruit.Season.Spring
                    "winter" -> Fruit.Season.Winter
                    "autumn" -> Fruit.Season.Autumn
                    else -> return@get call.respond(
                        HttpStatusCode.BadRequest,
                        FruitResponse(isSuccess = false, message = "invalid parameter $name for season")
                    )
                }
            )
        }

        val fruits = fruitStore.getAllFruits(
            sortedBy = sortedBy,
            sortDirection = sortDirection,
            query = query,
            countries = countries,
            seasons = seasons,
            addedBy = addedBy
        )

        call.respond(
            status = HttpStatusCode.OK,
            message = FruitResponse(list = fruits ,isSuccess = true, message = "items: ${fruits.size}")
        )
    }


    post("add-fruits") {
        try {
            val fruits = call.receive<List<Fruit>>()
            if (fruitStore.createListFruit(fruits)){
                call.respond(message = fruits, status = HttpStatusCode.Created)
            }else {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = FruitResponse(isSuccess = false, message = "can't create list of fruits")
                )
            }
        }
        catch (ex: Exception){
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = FruitResponse(isSuccess = false, message = ex.message
                )
            )
        }
    }


    authenticate {
        post("add-fruit") {
            try {


                // get the username for the authenticated user
                val username = call.principal<JWTPrincipal>()?.get(UserField.USER_NAME) ?: throw Exception("Can't get username")


                val multipart = call.receiveMultipart()
                var fileName: String? = null
                var name: String? = null
                val countries = mutableListOf<String>()
                var season: String? = null
                var imageUrl: String? = null


                multipart.forEachPart { partData ->
                    when (partData) {
                        is PartData.FormItem -> {
                            when (partData.name) {
                                FruitField.NAME -> name = partData.value
                                FruitField.SEASON -> season = partData.value
                                FruitField.COUNTRIES -> countries.add(partData.value)

                            }
                        }

                        is PartData.FileItem -> {
                            if (partData.name == FruitField.IMAGE) {
                                fileName = partData.save(path = Constants.FRUIT_IMAGE_PATH)
                                imageUrl = "${Constants.EXTERNAL_FRUIT_IMAGE_PATH}/$fileName"
                            }
                        }

                        else -> Unit
                    }
                }

                // Check for null values and respond with an error if any are missing
                if (name.isNullOrEmpty() || season.isNullOrEmpty()) {
                    File("${Constants.FRUIT_IMAGE_PATH}/$fileName").delete()
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        FruitResponse(isSuccess = false, message = "Missing required fields: name, or season")
                    )
                }

                val newFruit = Fruit(
                    id = getNextFruitId().toString(),
                    name = name!!,
                    season = season!!,
                    countries = countries,
                    imageUrl = imageUrl,
                    addedBy = username
                )

                if (!fruitStore.createFruit(newFruit)) {
                    File("${Constants.FRUIT_IMAGE_PATH}/$fileName").delete()
                    return@post call.respond(
                        HttpStatusCode.Conflict,
                        FruitResponse(isSuccess = false, message = "Item already exists")
                    )
                }

                call.respond(HttpStatusCode.Created, newFruit)

            } catch (ex: Exception) {
                ex.printStackTrace()
                call.respond(
                    HttpStatusCode.BadRequest,
                    FruitResponse(isSuccess = false, message = "Invalid data or error processing request: ${ex.message}")
                )
            }
        }
    }

    patch("add-fruit") {
        try {

            var id: String? = null
            val multipart = call.receiveMultipart()
            var fileName: String? = null
            var name: String? = null
            val countries = mutableListOf<String>()
            var season: String? = null
            var imageUrl: String? = null
            var addedBy: String? = null
            var oldImageUrl: String = ""



            multipart.forEachPart { partData ->
                when (partData) {
                    is PartData.FormItem -> {
                        when (partData.name) {
                            FruitField.NAME -> name = partData.value
                            FruitField.SEASON -> season = partData.value
                            FruitField.COUNTRIES -> countries.add(partData.value)
                            FruitField.ID -> id = partData.value
                            FruitField.ADDED_BY -> addedBy = partData.value
                            FruitField.IMAGE -> oldImageUrl = partData.value
                        }
                    }

                    is PartData.FileItem -> {
                        if (partData.name == FruitField.IMAGE) {
                            fileName = partData.save(path = Constants.FRUIT_IMAGE_PATH)
                            imageUrl = "${Constants.EXTERNAL_FRUIT_IMAGE_PATH}/$fileName"
                        }
                    }

                    else -> Unit
                }
            }

            // Check for null values and respond with an error if any are missing
            if (name.isNullOrEmpty() || season.isNullOrEmpty()) {
                return@patch call.respond(
                    HttpStatusCode.BadRequest,
                    FruitResponse(isSuccess = false, message = "Missing required fields: name, or season")
                )
            }

            val updatedFruit = Fruit(
                id = id!!,
                name = name!!,
                season = season!!,
                countries = countries,
                imageUrl = imageUrl,
                addedBy = addedBy!!
            )



            if (fruitStore.updateFruit(updatedFruit)){
                File("${Constants.FRUIT_IMAGE_PATH}/${oldImageUrl.removePrefix("/images")}").delete()
//                File(oldImageUrl.replace(Constants.EXTERNAL_FRUIT_IMAGE_PATH, Constants.FRUIT_IMAGE_PATH)).delete()
                return@patch call.respond(status = HttpStatusCode.OK, message = updatedFruit)
            }else {
                return@patch call.respond(
                    HttpStatusCode.Conflict,
                    FruitResponse(isSuccess = false, message = "updated failed, may messing some field")
                )
            }


//            if (mongoDb.updateFruit(updatedFruit)) {
//                File("${Constants.FRUIT_IMAGE_PATH}/$fileName").delete()
//                return@patch call.respond(
//                    HttpStatusCode.Conflict,
//                    FruitResponse(isSuccess = false, message = "Item already exists")
//                )
//            }



        } catch (ex: Exception) {
            ex.printStackTrace()
            call.respond(
                HttpStatusCode.BadRequest,
                FruitResponse(isSuccess = false, message = "Invalid data or error processing request: ${ex.message}")
            )
        }
    }


//    patch("add-fruit") {
//        try {
//            val fruit = call.receive<Fruit>()
//            if (mongoDb.updateFruit(fruit)){
//                call.respond(message = fruit, status = HttpStatusCode.OK)
//            }else {
//                call.respond(
//                    status = HttpStatusCode.BadRequest,
//                    message = FruitResponse(isSuccess = false, message = "can't update fruit, may object is not exist"
//                    ))
//            }
//        }catch (ex: Exception){
//            ex.printStackTrace()
//            call.respond(
//                status = HttpStatusCode.BadRequest,
//                message = FruitResponse(isSuccess = false, message = ex.message
//                )
//            )
//        }
//    }




    delete("delete-fruit"){
        try {
            val id = call.queryParameters["id"] ?: return@delete call.respond(
                status = HttpStatusCode.BadRequest,
                message = FruitResponse(isSuccess = false, message = "please provide an id")
            )
            if (fruitStore.deleteFruitById(id)){
                call.respond(
                    status = HttpStatusCode.OK,
                    message = FruitResponse(isSuccess = true, message = "Deleted Successfully")
                )
            }else {
                call.respond(status = HttpStatusCode.BadRequest,
                    message = FruitResponse(isSuccess = false, message = "Deleted Failed")
                )
            }

        }catch (ex: Exception){
            ex.printStackTrace()
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = FruitResponse(isSuccess = false, message = ex.message
                )
            )
        }
    }


}