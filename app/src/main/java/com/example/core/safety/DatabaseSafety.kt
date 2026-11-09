package com.example.core.safety

import android.content.Context
import android.net.Uri
import com.example.BuildConfig
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Safety copies of the raw Room database files, plus helpers for the recovery screen.
 *
 * A copy is taken (a) right before the app installs a downloaded update and (b) on the first launch of
 * every new app version, *before* Room opens (and possibly migrates) the database. So whichever way
 * the update arrived, the pre-update data is on disk.
 */
object DatabaseSafety {

    const val DB_NAME = "flowly_database"
    private val SUFFIXES = listOf("", "-wal", "-shm")
    private const val PREFS = "flowly_safety"
    private const val KEY_LAST_VERSION_CODE = "last_seen_version_code"
    private const val KEY_BACKUP_TIME = "pre_update_backup_time"
    private const val KEY_BACKUP_FROM_VERSION = "pre_update_backup_version_name"

    private fun backupDir(context: Context) = File(context.filesDir, "pre_update_backup")

    /** Call at the very start of MainActivity.onCreate, before the database is touched. */
    fun backupIfAppVersionChanged(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getInt(KEY_LAST_VERSION_CODE, -1)
        // last == -1 means this is the first launch of a build that tracks versions, i.e. an upgrade from an
        // older build when a database already exists (a brand-new install has no file, so nothing is copied).
        if (last != BuildConfig.VERSION_CODE) {
            createPreUpdateBackup(context)
        }
        prefs.edit().putInt(KEY_LAST_VERSION_CODE, BuildConfig.VERSION_CODE).apply()
    }

    /** Copies db + wal + shm. Returns true when the main database file was copied. */
    fun createPreUpdateBackup(context: Context, checkpointFirst: Boolean = false): Boolean {
        return try {
            val main = context.getDatabasePath(DB_NAME)
            if (!main.exists()) return false
            if (checkpointFirst) {
                // Fold the write-ahead log into the main file so the copy is complete on its own.
                try {
                    com.example.data.local.AppDatabase.getDatabase(context).openHelper.writableDatabase
                        .query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
                } catch (_: Exception) { /* the -wal/-shm copies below still make the backup consistent */ }
            }
            val dir = backupDir(context).apply { mkdirs() }
            // Write to temp names first so a crash never leaves a half-replaced good backup.
            val staged = mutableListOf<Pair<File, File>>()
            for (suffix in SUFFIXES) {
                val src = File(main.path + suffix)
                val tmp = File(dir, "$DB_NAME$suffix.tmp")
                tmp.delete()
                if (src.exists()) {
                    FileInputStream(src).use { input -> FileOutputStream(tmp).use { input.copyTo(it) } }
                    staged += tmp to File(dir, "$DB_NAME$suffix")
                } else {
                    File(dir, "$DB_NAME$suffix").delete()
                }
            }
            staged.forEach { (tmp, final) -> final.delete(); tmp.renameTo(final) }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putLong(KEY_BACKUP_TIME, System.currentTimeMillis())
                .putString(KEY_BACKUP_FROM_VERSION, BuildConfig.VERSION_NAME)
                .apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun hasPreUpdateBackup(context: Context): Boolean = File(backupDir(context), DB_NAME).exists()

    fun preUpdateBackupTime(context: Context): Long? {
        if (!hasPreUpdateBackup(context)) return null
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_BACKUP_TIME, 0L).takeIf { it > 0 }
    }

    /**
     * Replaces the live database with the saved copy. The caller must make sure Room is closed
     * (the app restarts the process afterwards).
     */
    fun restorePreUpdateBackup(context: Context): Boolean {
        return try {
            val dir = backupDir(context)
            if (!File(dir, DB_NAME).exists()) return false
            val main = context.getDatabasePath(DB_NAME)
            // Keep whatever is being replaced, so restoring can never make things worse.
            val replaced = File(context.filesDir, "replaced_db").apply { mkdirs() }
            for (suffix in SUFFIXES) {
                val live = File(main.path + suffix)
                if (live.exists()) live.copyTo(File(replaced, "$DB_NAME$suffix"), overwrite = true)
            }
            for (suffix in SUFFIXES) {
                val live = File(main.path + suffix)
                val saved = File(dir, "$DB_NAME$suffix")
                live.delete()
                if (saved.exists()) {
                    FileInputStream(saved).use { input -> FileOutputStream(live).use { input.copyTo(it) } }
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Writes the raw database file (merging nothing, just a byte copy) to a user-chosen location. */
    fun exportRawDatabase(context: Context, target: Uri): Boolean {
        return try {
            val source = context.getDatabasePath(DB_NAME).takeIf { it.exists() }
                ?: File(backupDir(context), DB_NAME).takeIf { it.exists() }
                ?: return false
            context.contentResolver.openOutputStream(target)?.use { out ->
                FileInputStream(source).use { it.copyTo(out) }
            } ?: return false
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Restarts the whole process so Room reopens the (restored) files cleanly. */
    fun restartApp(context: Context) {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        context.startActivity(android.content.Intent.makeRestartActivityTask(launch.component))
        Runtime.getRuntime().exit(0)
    }
}
