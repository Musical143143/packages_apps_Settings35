/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.homepage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Largest side, in pixels, of a decoded avatar photo. The avatar is only about 60dp. */
private const val AVATAR_PHOTO_MAX_SIDE = 384

/** Prefix of the private copies of the user's picked avatar photos. */
private const val AVATAR_PHOTO_PREFIX = "homepage_avatar_"

/** Only files this helper created inside the app's files folder may be deleted. */
private fun ownedPhotoFile(context: Context, path: String?): File? {
    if (path == null) return null
    val file = File(path)
    return file.takeIf {
        it.parentFile == context.filesDir && it.name.startsWith(AVATAR_PHOTO_PREFIX)
    }
}

/**
 * Copies an image the user picked from their storage into the app's private files, so it stays
 * readable after the picker's access ends. The previous copy is removed once the new one is
 * valid. Returns the new file's path, or null if the file is not a readable image.
 */
internal fun importAvatarPhoto(context: Context, source: Uri, previousPath: String?): String? {
    return try {
        val target = File(
            context.filesDir,
            AVATAR_PHOTO_PREFIX + System.currentTimeMillis() + ".img",
        )
        val copied = context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
            true
        } ?: false
        if (!copied || !isDecodableImage(target)) {
            target.delete()
            return null
        }
        ownedPhotoFile(context, previousPath)?.delete()
        target.path
    } catch (e: Exception) {
        null
    }
}

/** Removes the private copy of an avatar photo, if this helper created it. */
internal fun deleteAvatarPhoto(context: Context, path: String?) {
    ownedPhotoFile(context, path)?.delete()
}

private fun isDecodableImage(file: File): Boolean {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    return bounds.outWidth > 0 && bounds.outHeight > 0
}

/** Decodes a stored avatar photo at a size suitable for the avatar, or returns null. */
internal fun loadAvatarPhoto(path: String?): Bitmap? {
    if (path == null) return null
    val file = File(path)
    if (!file.isFile) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val largest = maxOf(bounds.outWidth, bounds.outHeight)
    var sample = 1
    while (largest / (sample * 2) >= AVATAR_PHOTO_MAX_SIDE) sample *= 2
    return BitmapFactory.decodeFile(
        file.path,
        BitmapFactory.Options().apply { inSampleSize = sample },
    )
}

/**
 * The user's own photo for the header avatar, or null when the photo is turned off or missing.
 * It is decoded off the main thread and only again when the setting or the file changes.
 */
@Composable
internal fun rememberAvatarPhoto(config: HeaderConfig): Bitmap? {
    val enabled = config.customAvatarEnabled
    val path = config.customAvatarPath
    return produceState<Bitmap?>(initialValue = null, enabled, path) {
        value = if (enabled) withContext(Dispatchers.IO) { loadAvatarPhoto(path) } else null
    }.value
}
