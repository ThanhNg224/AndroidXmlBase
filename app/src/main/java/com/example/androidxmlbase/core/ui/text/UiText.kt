package com.example.androidxmlbase.core.ui.text

import android.content.Context
import androidx.annotation.StringRes

/** A UI message that can remain localized until the view renders it. */
sealed interface UiText {
    data class DynamicString(
        val value: String,
    ) : UiText

    data class StringResource(
        @StringRes val resId: Int,
        val formatArgs: List<Any> = emptyList(),
    ) : UiText
}

fun UiText.resolve(context: Context): String =
    when (this) {
        is UiText.DynamicString -> value
        is UiText.StringResource -> context.getString(resId, *formatArgs.toTypedArray())
    }
