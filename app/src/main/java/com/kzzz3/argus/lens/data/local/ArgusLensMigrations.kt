package com.kzzz3.argus.lens.data.local

import androidx.room.migration.Migration

object ArgusLensMigrations {
    val latestVersion: Int = ArgusLensDatabase.CURRENT_VERSION
    val all: List<Migration> = emptyList()
}
