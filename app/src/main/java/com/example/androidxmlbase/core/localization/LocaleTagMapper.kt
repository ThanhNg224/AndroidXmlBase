package com.example.androidxmlbase.core.localization

object LocaleTagMapper {
    private val REGIONAL_OVERRIDES =
        mapOf(
            "vi" to "vi-VN",
            "ko" to "ko-KR",
            "zh-TW" to "zh-TW",
        )

    fun toRegionalTag(languageCode: String): String = REGIONAL_OVERRIDES[languageCode] ?: languageCode
}
