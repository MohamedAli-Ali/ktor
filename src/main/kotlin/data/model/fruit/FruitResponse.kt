package com.froute.data.model.fruit

import kotlinx.serialization.Serializable


@Serializable
data class FruitResponse(
    val list: List<Fruit> = emptyList(),
    val isSuccess: Boolean,
    val message: String?
)
