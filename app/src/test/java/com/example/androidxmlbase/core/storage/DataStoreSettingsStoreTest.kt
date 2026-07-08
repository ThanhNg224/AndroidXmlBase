package com.example.androidxmlbase.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: DataStoreSettingsStore

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = TestScope(UnconfinedTestDispatcher()),
            produceFile = { tempFolder.newFile("test.preferences_pb") },
        )
        store = DataStoreSettingsStore(dataStore)
    }

    @Test
    fun `string value round-trips through set and get`() = runTest {
        val key = SettingsKey.StringKey(name = "string_key", defaultValue = "default")

        store.set(key, "hello")

        assertEquals("hello", store.get(key))
    }

    @Test
    fun `int value round-trips through set and get`() = runTest {
        val key = SettingsKey.IntKey(name = "int_key", defaultValue = 0)

        store.set(key, 42)

        assertEquals(42, store.get(key))
    }

    @Test
    fun `long value round-trips through set and get`() = runTest {
        val key = SettingsKey.LongKey(name = "long_key", defaultValue = 0L)

        store.set(key, 123456789L)

        assertEquals(123456789L, store.get(key))
    }

    @Test
    fun `boolean value round-trips through set and get`() = runTest {
        val key = SettingsKey.BooleanKey(name = "boolean_key", defaultValue = false)

        store.set(key, true)

        assertEquals(true, store.get(key))
    }

    @Test
    fun `float value round-trips through set and get`() = runTest {
        val key = SettingsKey.FloatKey(name = "float_key", defaultValue = 0f)

        store.set(key, 3.14f)

        assertEquals(3.14f, store.get(key))
    }

    @Test
    fun `get returns the default value when the key was never set`() = runTest {
        val key = SettingsKey.StringKey(name = "unset_key", defaultValue = "fallback")

        assertEquals("fallback", store.get(key))
    }

    @Test
    fun `remove clears a previously set value back to the default`() = runTest {
        val key = SettingsKey.IntKey(name = "removable_key", defaultValue = -1)
        store.set(key, 99)

        store.remove(key)

        assertEquals(-1, store.get(key))
    }

    @Test
    fun `observe emits the new value after set`() = runTest {
        val key = SettingsKey.IntKey(name = "observed_key", defaultValue = 0)

        store.observe(key).test {
            assertEquals(0, awaitItem())
            store.set(key, 7)
            assertEquals(7, awaitItem())
        }
    }
}
