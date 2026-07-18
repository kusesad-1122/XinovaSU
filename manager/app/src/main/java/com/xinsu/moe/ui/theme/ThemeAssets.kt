package com.xinsu.moe.ui.theme

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// App-internal home for theme assets (background images, and later per-card art). Keeping the
// actual bytes here — rather than a SAF content:// uri — is what lets a background survive a
// reboot or a source-app uninstall AND travel inside a shared theme bundle to another device,
// where the original content-uri would resolve to nothing.
private const val THEME_DIR = "theme"
private const val BACKGROUND_PREFIX = "background_"
private const val CARD_PREFIX = "card_"

fun themeAssetsDir(context: Context): File =
    File(context.filesDir, THEME_DIR).apply { mkdirs() }

// Copies a picked image (any content:// uri) into app-internal storage and returns a portable
// file:// uri string, or null on failure. A fresh timestamped filename is used every time so the
// stored uri string actually changes — the background loader keys its decode on that string, so
// reusing one filename would silently fail to refresh when the user picks a new picture.
suspend fun importBackgroundImage(context: Context, source: Uri): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val dir = themeAssetsDir(context)
            // Drop any previous background so we don't accumulate orphaned files.
            dir.listFiles { f -> f.name.startsWith(BACKGROUND_PREFIX) }?.forEach { it.delete() }
            val target = File(dir, "$BACKGROUND_PREFIX${System.currentTimeMillis()}.img")
            val copied = context.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            if (copied) Uri.fromFile(target).toString() else null
        }.getOrNull()
    }

// Same as importBackgroundImage but for the optional per-card illustration, kept under its own
// filename prefix so it and the page background can coexist.
suspend fun importCardImage(context: Context, source: Uri): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val dir = themeAssetsDir(context)
            dir.listFiles { f -> f.name.startsWith(CARD_PREFIX) }?.forEach { it.delete() }
            val target = File(dir, "$CARD_PREFIX${System.currentTimeMillis()}.img")
            val copied = context.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            if (copied) Uri.fromFile(target).toString() else null
        }.getOrNull()
    }

// Writes raw image bytes (e.g. extracted from an imported theme bundle) into app-internal storage
// and returns a portable file:// uri string, or null on failure. Blocking — call off the main
// thread. Mirrors importBackgroundImage but takes bytes instead of a source uri.
fun saveBackgroundBytes(context: Context, bytes: ByteArray): String? =
    runCatching {
        val dir = themeAssetsDir(context)
        dir.listFiles { f -> f.name.startsWith(BACKGROUND_PREFIX) }?.forEach { it.delete() }
        val target = File(dir, "$BACKGROUND_PREFIX${System.currentTimeMillis()}.img")
        target.outputStream().use { it.write(bytes) }
        Uri.fromFile(target).toString()
    }.getOrNull()
