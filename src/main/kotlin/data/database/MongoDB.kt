package com.froute.data.database


import com.froute.data.model.fruit.Fruit
import com.froute.data.model.fruit.FruitField
import com.froute.data.model.user.User
import com.froute.data.model.user.UserField
import com.froute.data.model.voucher.Voucher
import com.froute.data.model.voucher.VoucherField
import com.froute.utils.checkHashForPassword
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import com.froute.utils.Result.Error
import com.froute.utils.Result.Success
import com.froute.utils.Result

class MongoDB {

     companion object {
         private const val PROD_CONNECTION_STRING = "mongodb+srv://mohamed20250099:BO2bUmtH4jB2OgG5@cluster0.s0tmm.mongodb.net"
         private const val DEV_CONNECTION_STRRING = "mongodb://localhost:27017"
         private const val DATABASE_NAME = "FruitsDB"
         private const val FRUIT_COLLECTION = "fruits"
         private const val USER_COLLECTION = "users"
         private const val VOUCHER_COLLECTION = "vouchers"
    }

    private val mongoClient = MongoClient.create(PROD_CONNECTION_STRING)
    private val db = mongoClient.getDatabase(DATABASE_NAME)
    private val fruitCollection = db.getCollection<Fruit>(FRUIT_COLLECTION)
    private val userCollection = db.getCollection<User>(USER_COLLECTION)
    private val voucherCollection = db.getCollection<Voucher>(VOUCHER_COLLECTION)

    inner class FruitStore() {

        suspend fun createListFruit(fruits: List<Fruit>): Boolean {
            return try {
                fruitCollection.insertMany(fruits).wasAcknowledged()
            }catch (ex: Exception){
                ex.printStackTrace()
                false
            }
        }
        suspend fun createFruit(fruit: Fruit): Boolean {
            return try {
                fruitCollection.insertOne(fruit).wasAcknowledged()
            }catch (ex: Exception){
                ex.printStackTrace()
                false
            }
        }

        suspend fun updateFruit(fruit: Fruit): Boolean{
            val filter = Filters.eq(  "_id",fruit.id)
            return fruitCollection.replaceOne(filter,fruit).modifiedCount == 1L
        }




        suspend fun deleteFruitById(id: String): Boolean{
            val filter = Filters.eq("_id",id)
            val result = fruitCollection.deleteOne(filter)
            return result.deletedCount == 1L
        }

        suspend fun getAllFruits(
            sortedBy: String? = FruitField.NAME,
            sortDirection: Int = 1,
            query: String? = null,
            seasons: List<Fruit.Season> = emptyList(),
            countries: List<String> = emptyList(),
            addedBy: String? = null
        ): List<Fruit> {
            val seasonFilter = if (seasons.isNotEmpty()) Filters.`in`(FruitField.SEASON, seasons) else Filters.empty()
            val countriesFilter = if (countries.isNotEmpty()) Filters.`in`(FruitField.COUNTRIES, (countries)) else Filters.empty()
            val queryFilter = if (query != null) Filters.regex(FruitField.NAME , query, "i") else Filters.empty()
            val addedByFilter = if (addedBy != null) Filters.eq(FruitField.ADDED_BY, addedBy) else Filters.empty()
            val combinedFilter = Filters.and(seasonFilter, queryFilter, countriesFilter,addedByFilter)
            val sort = if (sortDirection < 0 ) Sorts.descending(sortedBy) else Sorts.ascending(sortedBy)
            return fruitCollection.find().filter(combinedFilter).sort(sort).toList()
        }


    }

    inner class UserStore(){

        suspend fun register(user: User): Boolean{
            return try {
                userCollection.insertOne(user).wasAcknowledged()
            }catch (ex: Exception){
                ex.printStackTrace()
                false
            }
        }

        suspend fun checkUserExist(userName: String): Boolean {
            val filter = Filters.eq(UserField.USER_NAME, userName)
            return userCollection.find(filter).firstOrNull() != null
        }

        suspend fun checkUserNameForPass(passToCheck: String, userName: String): Boolean{
            val filter = Filters.eq(UserField.USER_NAME, userName)
            val actualPass = userCollection.find(filter).firstOrNull()?.password ?: ""
            return  checkHashForPassword(passToCheck, actualPass)
        }



    }

    inner class VoucherStore() {

        suspend fun useVoucher(code: String, userId: String, usedAt: String): Result<Boolean> {
            return try {
                val voucher = voucherCollection.find(Filters.eq(VoucherField.CODE, code)).firstOrNull()
                    ?: return Error(Exception("Voucher not found"))
                val user = userCollection.find(Filters.eq(UserField.USER_ID, userId)).firstOrNull()
                    ?: return Error(Exception("User not found"))

                val usedBy = voucher.usedBy.toMutableList()
                if (usedBy.any { it.username == user.userName }) {
                    return Error(Exception("User already used this voucher"))
                }
                usedBy.add(Voucher.UsedByInfo(username = user.userName, usedAt = usedAt))
                val updated = Updates.set(VoucherField.USER_BY, usedBy)
                val result = voucherCollection.updateOne(Filters.eq(VoucherField.CODE, code), updated).modifiedCount == 1L
                if (result) {
                    Success(true)
                } else {
                    Error(Exception("Unable to use voucher"))
                }
            } catch (ex: Exception) {
                Error(ex)
            }
        }


        suspend fun createVouchers(vouchers: List<Voucher>): Boolean{
            return try {
                voucherCollection.insertMany(vouchers).wasAcknowledged()
            }catch (ex: Exception){
                ex.printStackTrace()
                false
            }
        }


        suspend fun getAllVouchers(
            query: String? = null,
            page: Int? = null,
            pageSize: Int? = null
        ): List<Voucher> {
            val queryFilter = if (query != null) Filters.regex(Voucher::code.name, query, "i") else Filters.empty()
            val sort = Sorts.ascending(VoucherField.VOUCHER_ID)
            var findIterable = voucherCollection.find(queryFilter).sort(sort)

            if (page != null && pageSize != null) {
                val skip = (page - 1) * pageSize
                findIterable = findIterable.skip(skip).limit(pageSize)
            }

            if (pageSize != null){
                findIterable = findIterable.limit(pageSize)
            }

            return findIterable.toList()
        }



    }


}