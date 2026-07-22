package com.xinsu.moe.ui.theme

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.xinsu.moe.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// A portable, shareable theme = a single ".xnsutheme" file (a plain ZIP) carrying a JSON manifest
// of the visual settings plus the background image BYTES, so it survives being sent to another
// device where the original content-uri would resolve to nothing. No copyrighted art ships with
// the app: every image inside a bundle is whatever the user themself picked. Export snapshots the
// current look; import writes the values back through SettingsRepository, whose SharedPreferences
// writes the MainActivity change-listener observes to recolor the whole app live.
object ThemeBundle {
    const val EXTENSION = "xnsutheme"
    const val MIME = "application/octet-stream"
    const val DEFAULT_FILENAME = "XinovaSU.$EXTENSION"

    private const val MANIFEST_ENTRY = "manifest.json"
    private const val BACKGROUND_ENTRY = "background.img"
    private const val SCHEMA = 1

    suspend fun export(
        context: Context,
        repo: SettingsRepository,
        target: Uri,
        name: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val bgStyle = repo.backgroundStyle
            val bgUri = repo.backgroundImageUri
            val hasBg = BackgroundStyle.fromName(bgStyle).usesImage && bgUri.isNotBlank()
            val manifest = JSONObject().apply {
                put("schema", SCHEMA)
                put("name", name)
                put("themePreset", repo.themePreset)
                put("keyColor", repo.keyColor)
                put("colorStyle", repo.colorStyle)
                put("colorSpec", repo.colorSpec)
                put("backgroundStyle", bgStyle)
                put("hasBackgroundImage", hasBg)
            }
            val out = context.contentResolver.openOutputStream(target)
                ?: error("cannot open output stream")
            out.use { os ->
                ZipOutputStream(BufferedOutputStream(os)).use { zos ->
                    zos.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                    zos.write(manifest.toString().toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                    if (hasBg) {
                        context.contentResolver.openInputStream(bgUri.toUri())?.use { input ->
                            zos.putNextEntry(ZipEntry(BACKGROUND_ENTRY))
                            input.copyTo(zos)
                            zos.closeEntry()
                        }
                    }
                }
            }
        }
    }

    // Returns the imported theme's display name on success.
    suspend fun import(
        context: Context,
        repo: SettingsRepository,
        source: Uri,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            var manifestJson: String? = null
            var background: ByteArray? = null
            val input = context.contentResolver.openInputStream(source)
                ?: error("cannot open input stream")
            input.use { stream ->
                ZipInputStream(BufferedInputStream(stream)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        when (entry.name) {
                            MANIFEST_ENTRY -> manifestJson = zis.readBytes().toString(Charsets.UTF_8)
                            BACKGROUND_ENTRY -> background = zis.readBytes()
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            val manifest = JSONObject(manifestJson ?: error("missing manifest"))

            manifest.optString("themePreset").takeIf { it.isNotEmpty() }?.let { repo.themePreset = it }
            if (manifest.has("keyColor")) repo.keyColor = manifest.getInt("keyColor")
            manifest.optString("colorStyle").takeIf { it.isNotEmpty() }?.let { repo.colorStyle = it }
            manifest.optString("colorSpec").takeIf { it.isNotEmpty() }?.let { repo.colorSpec = it }

            // Persist the image (if any) first, then the style, so the live update reads a
            // consistent pair.
            background?.let { bytes ->
                saveBackgroundBytes(context, bytes)?.let { repo.backgroundImageUri = it }
            }
            repo.backgroundStyle = manifest.optString("backgroundStyle").ifEmpty { BackgroundStyle.None.name }

            manifest.optString("name").ifEmpty { "Imported theme" }
        }
    }
}
