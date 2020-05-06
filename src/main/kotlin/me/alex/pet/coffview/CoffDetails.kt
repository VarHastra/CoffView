package me.alex.pet.coffview

data class CoffDetails(
    val symbolTableOffset: Long,
    val numOfSymbols: Long,
    val stringTableOffset: Long,
    val stringTableSizeInBytes: Long,
    val symbols: List<String>
)