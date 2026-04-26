package com.kzzz3.argus.lens.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class ArgusLensDatabaseMigrationTest {

    @Test
    fun migrationRegistryTracksCurrentDatabaseVersion() {
        assertEquals(ArgusLensDatabase.CURRENT_VERSION, ArgusLensMigrations.latestVersion)
    }

    @Test
    fun initialSchemaHasNoHistoricalMigrations() {
        assertEquals(emptyList<Any>(), ArgusLensMigrations.all)
    }
}
