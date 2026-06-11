package com.pokechain.data.models

data class SearchStringResult(
    val baseForms: List<String>,
    val raw: String
) {
    val formatted: String
        get() = baseForms.joinToString(";") { "+$it" }
}
