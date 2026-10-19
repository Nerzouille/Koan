package dev.nerzouille.koan.ui.menu

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.ui.browser.observeSlice
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.concept.engine.prompt.PromptRequest
import java.io.File

/**
 * A page's file field (<input type=file>): Android's picker (files, photos), with the camera
 * offered too when the field takes images or videos; straight to the camera when the field asks
 * for a capture. The camera needs Koan's camera permission (asked once, here). Nothing picked =
 * the field stays empty.
 */
@Composable
fun FilePrompt(components: Components, tabId: String?) {
    val context = LocalContext.current
    val request by components.store.observeSlice(tabId) { s ->
        tabId?.let { s.findTab(it) }?.content?.promptRequests?.firstOrNull { it is PromptRequest.File } as PromptRequest.File?
    }
    val pending = remember { PendingCapture() }

    fun finish(r: PromptRequest.File) {
        tabId?.let { components.store.dispatch(ContentAction.ConsumePromptRequestAction(it, r)) }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val r = request ?: return@rememberLauncherForActivityResult
        val data = result.data
        val picked = if (result.resultCode != Activity.RESULT_OK) {
            emptyList()
        } else {
            data?.clipData?.uris()
                ?: data?.data?.let(::listOf)
                // The camera wrote to the file Koan gave it, and returned nothing else.
                ?: pending.photo?.takeIf { pending.file?.length() ?: 0L > 0L }?.let(::listOf)
                ?: emptyList()
        }
        when {
            picked.isEmpty() -> r.onDismiss()
            r.isMultipleFilesSelection -> r.onMultipleFilesSelected(context, picked.toTypedArray())
            else -> r.onSingleFileSelected(context, picked.first())
        }
        finish(r)
    }

    fun launch(r: PromptRequest.File, camera: Boolean) {
        val captures = if (camera) captureIntents(context, r, pending) else emptyList()
        val direct = r.captureMode != PromptRequest.File.FacingMode.NONE && captures.isNotEmpty()
        val intent = if (direct) {
            captures.first()
        } else {
            Intent.createChooser(pickIntent(r), null).putExtra(Intent.EXTRA_INITIAL_INTENTS, captures.toTypedArray())
        }
        runCatching { picker.launch(intent) }.onFailure {
            r.onDismiss()
            finish(r)
        }
    }

    val askCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        request?.let { launch(it, camera = granted) }
    }

    LaunchedEffect(request) {
        val r = request ?: return@LaunchedEffect
        val wantsCamera = takesImages(r) || takesVideos(r)
        val cameraAllowed = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (wantsCamera && !cameraAllowed) askCamera.launch(Manifest.permission.CAMERA) else launch(r, camera = wantsCamera)
    }
}

/** Where the camera writes the photo it takes. */
private class PendingCapture {
    var file: File? = null
    var photo: Uri? = null
}

private fun pickIntent(r: PromptRequest.File): Intent {
    val types = r.mimeTypes.filter { it.isNotBlank() }
    return Intent(Intent.ACTION_GET_CONTENT)
        .addCategory(Intent.CATEGORY_OPENABLE)
        .setType(types.singleOrNull() ?: "*/*")
        .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, r.isMultipleFilesSelection)
        .apply { if (types.size > 1) putExtra(Intent.EXTRA_MIME_TYPES, types.toTypedArray()) }
}

private fun captureIntents(context: Context, r: PromptRequest.File, pending: PendingCapture): List<Intent> {
    val intents = ArrayList<Intent>()
    if (takesImages(r)) {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "photo-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        pending.file = file
        pending.photo = uri
        intents += Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, uri)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .also { it.clipData = ClipData.newRawUri("", uri) }
    }
    if (takesVideos(r)) intents += Intent(MediaStore.ACTION_VIDEO_CAPTURE)
    return intents.filter { it.resolveActivity(context.packageManager) != null }
}

private fun takesImages(r: PromptRequest.File) = r.mimeTypes.isEmpty() || r.mimeTypes.any { it.isBlank() || it == "*/*" || it.startsWith("image/") }

private fun takesVideos(r: PromptRequest.File) = r.mimeTypes.any { it.startsWith("video/") }

private fun ClipData.uris(): List<Uri> = (0 until itemCount).mapNotNull { getItemAt(it).uri }
