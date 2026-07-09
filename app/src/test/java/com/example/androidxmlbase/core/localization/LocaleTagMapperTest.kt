package com.example.androidxmlbase.core.localization

import org.junit.Assert.assertEquals
import org.junit.Test

class LocaleTagMapperTest {

    @Test
    fun `vi maps to vi-VN`() {
        assertEquals("vi-VN", LocaleTagMapper.toRegionalTag("vi"))
    }

    @Test
    fun `ko maps to ko-KR`() {
        assertEquals("ko-KR", LocaleTagMapper.toRegionalTag("ko"))
    }

    @Test
    fun `zh-TW maps to zh-TW`() {
        assertEquals("zh-TW", LocaleTagMapper.toRegionalTag("zh-TW"))
    }

    @Test
    fun `unmapped code passes through unchanged`() {
        assertEquals("en", LocaleTagMapper.toRegionalTag("en"))
    }
}
