package com.jehadalomour.flowvan.feature.customer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject

/**
 * iOS picker.
 *
 * UIImagePickerController covers both sources with one delegate — camera and
 * photo library differ only by sourceType. It is soft-deprecated in favour of
 * PHPicker for the library, but PHPicker cannot do camera, and one control with
 * one delegate is less to get wrong than two.
 *
 * The image is re-encoded as JPEG at 0.85 rather than passed through: a modern
 * iPhone HEIC is both a format the backend rejects and several megabytes the rep
 * would upload over cellular.
 */
@OptIn(ExperimentalForeignApi::class)
private fun UIImage.toJpegBytes(): ByteArray? {
    val data: NSData = UIImageJPEGRepresentation(this, 0.85) ?: return null
    val length = data.length.toInt()
    if (length == 0) return null
    val bytes = ByteArray(length)
    bytes.usePinned { pinned ->
        platform.posix.memcpy(pinned.addressOf(0), data.bytes, data.length)
    }
    return bytes
}

private class IosDocumentPicker : DocumentPicker {

    override suspend fun capture(): PickedDocument? =
        present(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)

    override suspend fun pickFromGallery(): PickedDocument? =
        present(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary)

    private suspend fun present(
        source: UIImagePickerControllerSourceType,
    ): PickedDocument? = suspendCancellableCoroutine { cont ->
        val root = UIApplication.sharedApplication.keyWindow?.rootViewController
        if (root == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        val controller = UIImagePickerController()
        controller.sourceType = source

        // Held in a local so ARC does not collect the delegate while the sheet is
        // up — UIImagePickerController keeps only a weak reference to it.
        val delegate = object : NSObject(),
            UIImagePickerControllerDelegateProtocol,
            UINavigationControllerDelegateProtocol {

            override fun imagePickerController(
                picker: UIImagePickerController,
                didFinishPickingMediaWithInfo: Map<Any?, *>,
            ) {
                val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage]
                    as? UIImage
                picker.dismissViewControllerAnimated(true, null)
                val bytes = image?.toJpegBytes()
                cont.resume(
                    bytes?.let { PickedDocument(it, "customer-doc.jpg", "image/jpeg") },
                )
            }

            override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                picker.dismissViewControllerAnimated(true, null)
                cont.resume(null)
            }
        }
        controller.delegate = delegate
        cont.invokeOnCancellation { controller.dismissViewControllerAnimated(true, null) }
        root.presentViewController(controller, animated = true, completion = null)
    }
}

@Composable
actual fun rememberDocumentPicker(): DocumentPicker = remember { IosDocumentPicker() }
