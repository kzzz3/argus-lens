package com.kzzz3.argus.lens.core.database.local

import androidx.room.migration.Migration

object ArgusLensMigrations {
    val latestVersion: Int = ArgusLensDatabase.CURRENT_VERSION
    val all: List<Migration> = emptyList()
}
