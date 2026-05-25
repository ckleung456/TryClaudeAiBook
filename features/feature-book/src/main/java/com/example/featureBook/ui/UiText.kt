package com.example.featureBook.ui

sealed interface UiText {
    data class DynamicString(val value: String) : UiText
}

fun UiText.asString(): String = when (this) {
    is UiText.DynamicString -> value
}