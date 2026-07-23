package com.example.featureBook.ui

import com.example.featureBook.R
import com.example.core.domain.DataError
import com.example.core.presentation.UiText

fun DataError.toUiText(): UiText = when (this) {
    DataError.Network.NOT_FOUND, DataError.Local.NOT_FOUND -> UiText.StringResource(R.string.error_book_not_found)
    DataError.Network.SERIALIZATION -> UiText.StringResource(R.string.error_serialization)
    DataError.Local.DISK_FULL -> UiText.StringResource(R.string.error_disk_full)
    else -> UiText.StringResource(R.string.error_unknown)
}
