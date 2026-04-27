package com.kzzz3.argus.lens.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ArgusLensDatabaseMigrationTest {

    @Test
    fun migrationRegistryTracksCurrentDatabaseVersion() {
        assertEquals(ArgusLensDatabase.CURRENT_VERSION, ArgusLensMigrations.latestVersion)
    }

    @Test
    fun initialSchemaHasNoHistoricalMigrations() {
        assertEquals(emptyList<Any>(), ArgusLensMigrations.all)
    }

    @Test
    fun exportedSchemaTracksCurrentDatabaseVersion() {
        val schema = currentSchemaFile()

        assertTrue("Missing exported Room schema: ${schema.path}", schema.isFile)
        assertTrue(schema.readText().contains("\"version\": ${ArgusLensDatabase.CURRENT_VERSION}"))
    }

    @Test
    fun exportedSchemaContainsAllCurrentTables() {
        val schemaText = currentSchemaFile().readText()

        listOf(
            "local_conversation",
            "local_message",
            "local_draft_attachment",
        ).forEach { tableName ->
            assertTrue("Missing table '$tableName' from exported Room schema.", schemaText.contains("\"tableName\": \"$tableName\""))
        }
    }

    private fun currentSchemaFile(): File {
        return File(
            "schemas/com.kzzz3.argus.lens.data.local.ArgusLensDatabase/${ArgusLensDatabase.CURRENT_VERSION}.json",
        )
    }
}
