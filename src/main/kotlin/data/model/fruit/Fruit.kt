package com.froute.data.model.fruit


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


object FruitField {
    const val ID = "_id"
    const val NAME = "name"
    const val SEASON = "season"
    const val COUNTRIES = "countries"
    const val IMAGE = "image"
    const val ADDED_BY = "added_by"
}


@Serializable
data class Fruit(
    @SerialName(FruitField.ID)
    val id: String,
    val name: String,
    val season: String = Season.Unknown.name,
    val countries: List<String> = emptyList(),
    @SerialName(FruitField.IMAGE)
    val imageUrl: String? = null,
    @SerialName(FruitField.ADDED_BY)
    val addedBy: String = "Unknown"
){
    enum class Season { Summer, Spring, Winter, Autumn, Unknown }
}
