package com.jehadalomour.flowvan.feature.customer

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Android picker.
 *
 * Gallery uses PickVisualMedia — the modern photo picker, which needs NO storage
 * permission at all (the system UI runs out-of-process and hands back a grant for
 * the single item chosen). Asking for READ_MEDIA_IMAGES here would be a
 * permission prompt for nothing.
 *
 * Camera writes to a FileProvider uri in cacheDir: TakePicture cannot return
 * bytes, only a boolean, so it needs somewhere to put them, and cacheDir is the
 * one place the OS may reclaim without us leaking the rep's photos.
 */
private class AndroidDocumentPicker(
    private val context: Context,
    private val launchGallery: (onResult: (Uri?) -> Unit) -> Unit,
    private val launchCamera: (target: Uri, onResult: (Boolean) -> Unit) -> Unit,
) : DocumentPicker {

    override suspend fun pickFromGallery(): PickedDocument? {
        val uri = suspendCancellableCoroutine<Uri?> { cont ->
            launchGallery { cont.resume(it) }
        } ?: return null
        return readUri(uri, "customer-doc.jpg")
    }

    override suspend fun capture(): PickedDocument? {
        val file = File(context.cacheDir, "customer-doc-${System.currentTimeMillis()}.jpg")
        val target = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val ok = suspendCancellableCoroutine<Boolean> { cont ->
            launchCamera(target) { cont.resume(it) }
        }
        if (!ok || !file.exists() || file.length() == 0L) {
            file.delete()
            return null
        }
        val bytes = file.readBytes()
        // The upload owns the bytes now; leaving the file behind fills cacheDir
        // with every photo the rep ever took.
        file.delete()
        return PickedDocument(bytes, file.name, "image/jpeg")
    }

    private fun readUri(uri: Uri, fallbackName: String): PickedDocument? {
        val resolver = context.contentResolver
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        if (bytes.isEmpty()) return null
        val mime = resolver.getType(uri) ?: "image/jpeg"
        return PickedDocument(bytes, fallbackName, mime)
    }
}

@Composable
actual fun rememberDocumentPicker(): DocumentPicker {
    val context = LocalContext.current

    // A launcher registered in composition carries ONE callback, so each suspend
    // call parks its continuation here before launching. Deliberately NOT Compose
    // state — a transient callback is not UI state and would recompose the screen
    // twice per pick for nothing.
    val pending = remember { PendingPicks() }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val cb = pending.gallery
        pending.gallery = null
        cb?.invoke(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { ok ->
        val cb = pending.camera
        pending.camera = null
        cb?.invoke(ok)
    }

    return remember(context) {
        AndroidDocumentPicker(
            context = context,
            launchGallery = { onResult ->
                pending.gallery = onResult
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            launchCamera = { target, onResult ->
                pending.camera = onResult
                cameraLauncher.launch(target)
            },
        )
    }
}

private class PendingPicks {
    var gallery: ((Uri?) -> Unit)? = null
    var camera: ((Boolean) -> Unit)? = null
}
