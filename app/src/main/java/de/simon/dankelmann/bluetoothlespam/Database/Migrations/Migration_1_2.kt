package de.simon.dankelmann.bluetoothlespam.Database.Migrations

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val Migration_1_2 = object : Migration(1,2) {
    private val _logTag = "Migration_1_2"


    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d(_logTag, "Executing Migration...")

        // The idempotent catalog sync runs after the database opens and adds new entries.
        Log.d(_logTag, "Finished Executing Migration...")
    }
}
