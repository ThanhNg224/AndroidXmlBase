package com.example.androidxmlbase.core.localization

data class LanguageOption(val code: String, val displayName: String)

val SUPPORTED_LANGUAGES = listOf(
    LanguageOption("en", "English"),
    LanguageOption("vi", "Tiếng Việt"),
)
