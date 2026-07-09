package com.example.androidxmlbase.core.localization

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeAppLocaleApplier : AppLocaleApplier {
    val appliedTags = mutableListOf<String>()

    override fun applyLocales(tag: String) {
        appliedTags.add(tag)
    }
}

class LocaleManagerTest {
    @Test
    fun `setLanguage vi applies mapped regional tag`() =
        runTest {
            val applier = FakeAppLocaleApplier()
            val manager = LocaleManager(applier)

            manager.setLanguage("vi")

            assertEquals(listOf("vi-VN"), applier.appliedTags)
        }

    @Test
    fun `setLanguage blank applies blank passthrough`() =
        runTest {
            val applier = FakeAppLocaleApplier()
            val manager = LocaleManager(applier)

            manager.setLanguage("")

            assertEquals(listOf(""), applier.appliedTags)
        }
}
