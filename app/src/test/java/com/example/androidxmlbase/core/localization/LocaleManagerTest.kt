package com.example.androidxmlbase.core.localization

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeLocaleStore(initial: String = "") : LocaleStore {
    private val state = MutableStateFlow(initial)

    override fun observeLanguageCode(): Flow<String> = state

    override suspend fun setLanguageCode(code: String) {
        state.value = code
    }

    fun current(): String = state.value
}

private class FakeAppLocaleApplier : AppLocaleApplier {
    val appliedTags = mutableListOf<String>()

    override fun applyLocales(tag: String) {
        appliedTags.add(tag)
    }
}

class LocaleManagerTest {

    @Test
    fun `setLanguage vi persists code and applies mapped regional tag`() = runTest {
        val store = FakeLocaleStore()
        val applier = FakeAppLocaleApplier()
        val manager = LocaleManager(store, applier)

        manager.setLanguage("vi")

        assertEquals("vi", store.current())
        assertEquals(listOf("vi-VN"), applier.appliedTags)
    }

    @Test
    fun `setLanguage blank persists blank code and applies blank passthrough`() = runTest {
        val store = FakeLocaleStore(initial = "vi")
        val applier = FakeAppLocaleApplier()
        val manager = LocaleManager(store, applier)

        manager.setLanguage("")

        assertEquals("", store.current())
        assertEquals(listOf(""), applier.appliedTags)
    }
}
