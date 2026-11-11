package dev.nerzouille.koan.ui.overlays

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.engine.downloads.DuplicateDownloads
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.Overlays
import dev.nerzouille.koan.ui.bar.PageInfo
import dev.nerzouille.koan.ui.browser.observeSlice
import dev.nerzouille.koan.ui.downloads.host
import dev.nerzouille.koan.ui.downloads.sizeText
import dev.nerzouille.koan.ui.folderLabel
import dev.nerzouille.koan.ui.kit.KoanClipboard
import dev.nerzouille.koan.ui.kit.KoanHaptics
import dev.nerzouille.koan.ui.kit.Toast
import dev.nerzouille.koan.ui.kit.copiedToast
import dev.nerzouille.koan.ui.menu.DownloadPrompt
import dev.nerzouille.koan.ui.menu.DuplicateDownloadPrompt
import dev.nerzouille.koan.ui.menu.LinkActions
import dev.nerzouille.koan.ui.menu.LinkMenu
import dev.nerzouille.koan.ui.menu.LinkTarget
import dev.nerzouille.koan.ui.sheet.Detent
import dev.nerzouille.koan.ui.sheet.KoanCard
import dev.nerzouille.koan.ui.theme.SheetTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.state.content.DownloadState

/**
 * Long press on a link or an image, and the one floating card for it, a download to confirm, or a
 * download of a file already there: when one leads to the next (Save image → already downloaded),
 * the same card morphs.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun LinkCardOverlay(
    components: Components,
    overlays: Overlays,
    scope: CoroutineScope,
    page: PageInfo,
    activeTabId: String?,
    clipboard: KoanClipboard,
    onOpenNew: (url: String, select: Boolean, nextToCurrent: Boolean) -> Unit,
    onToast: (Toast) -> Unit,
    share: (url: String, subject: String) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    // ---- long press on a link or an image ----
    val hit by components.store.observeSlice(activeTabId) { s -> activeTabId?.let { s.findTab(it) }?.content?.hitResult }
    LaunchedEffect(hit) {
        val result = hit ?: return@LaunchedEffect
        activeTabId?.let(components.pageTools::consumeHitResult)
        val target = LinkTarget.of(result) ?: return@LaunchedEffect
        // A light tick as the card comes up: the press landed.
        KoanHaptics.tick(view)
        overlays.link = target
        overlays.linkCard.animateTo(Detent.Compact)
    }
    // ---- one floating card for a pressed link or image, a download to confirm, a download of a
    // file already there: when one leads to the next (Save image → already downloaded), the same
    // card morphs: its height springs to the new size, the image shrinks into the thumbnail, the
    // content crossfades. ----
    val actionCard = overlays.linkCard
    val downloadAsk by components.downloadAsk.collectAsStateWithLifecycle()
    val dupAsk by components.duplicateAsk.collectAsStateWithLifecycle()
    LaunchedEffect(downloadAsk) { if (downloadAsk != null) actionCard.animateTo(Detent.Compact) }
    LaunchedEffect(dupAsk) { if (dupAsk != null) actionCard.animateTo(Detent.Compact) }
    SideEffect { overlays.actionBusy = downloadAsk != null || dupAsk != null }
    /** A step that may lead to the duplicate card: the card stays a moment, then closes if nothing came. */
    fun closeUnlessNext() {
        scope.launch {
            delay(SheetTokens.CARD_NEXT_WAIT_MS)
            if (components.duplicateAsk.value == null && components.downloadAsk.value == null) actionCard.animateTo(Detent.Closed)
        }
    }
    fun answerDownload(start: Boolean) {
        val ask = components.downloadAsk.value ?: return
        components.downloadAsk.value = null
        if (start) {
            ask.start()
            closeUnlessNext()
        } else {
            ask.cancel()
            actionCard.animateTo(Detent.Closed)
        }
    }
    fun answerDuplicate(start: DownloadState?) {
        val ask = components.duplicateAsk.value ?: return
        components.duplicateAsk.value = null
        actionCard.animateTo(Detent.Closed)
        if (start != null) ask.start(start) else ask.cancel()
    }
    // Dismissed (tap outside, Back): what it was asking is cancelled.
    LaunchedEffect(actionCard.open) {
        if (actionCard.open) return@LaunchedEffect
        if (components.duplicateAsk.value != null) answerDuplicate(null)
        if (components.downloadAsk.value != null) answerDownload(start = false)
    }
    KoanCard(actionCard) { interactive ->
        val cardPage: ActionPage? = dupAsk?.let { ActionPage.Duplicate } ?: downloadAsk?.let { ActionPage.Download } ?: overlays.link?.let { ActionPage.Link }
        SharedTransitionLayout {
            AnimatedContent(
                cardPage,
                transitionSpec = {
                    (fadeIn(tween(SheetTokens.CARD_MORPH_MS)) togetherWith fadeOut(tween(SheetTokens.CARD_MORPH_MS / 2)))
                        .using(SizeTransform(clip = false) { _, _ -> spring(dampingRatio = 0.85f, stiffness = 420f) })
                },
                label = "card",
            ) { shown ->
                // The pressed image and the duplicate's thumbnail are one element across the morph.
                val image = Modifier.sharedBounds(rememberSharedContentState("card-image"), this@AnimatedContent)
                when (shown) {
                    ActionPage.Link -> {
                        val target = overlays.link ?: return@AnimatedContent
                        LinkMenu(
                            target, interactive,
                            LinkActions(
                                openNew = { url -> actionCard.animateTo(Detent.Closed); onOpenNew(url, true, true) },
                                openBackground = { url -> actionCard.animateTo(Detent.Closed); onOpenNew(url, false, true) },
                                copy = { url -> clipboard.setText(AnnotatedString(url)); copiedToast("Link")?.let(onToast); actionCard.animateTo(Detent.Closed) },
                                share = { url -> actionCard.animateTo(Detent.Closed); share(url, "") },
                                playVideo = { url, label ->
                                    actionCard.animateTo(Detent.Closed)
                                    components.player.open(dev.nerzouille.koan.ui.player.videoOfLink(url, label, page.url))
                                },
                                saveImage = { url ->
                                    // Goes through the same downloads as a page's: no dialog, the toast says when
                                    // it's saved; a file already there turns this card into the duplicate card.
                                    val tab = activeTabId
                                    fun save(src: String) {
                                        tab ?: return
                                        components.store.dispatch(
                                            ContentAction.UpdateDownloadAction(tab, DownloadState(src, skipConfirmation = true, referrerUrl = page.url)),
                                        )
                                        closeUnlessNext()
                                    }
                                    if (tab != null && LinkTarget.isPreview(url)) {
                                        // Google's small preview: the page says which full image is there, once loaded.
                                        overlays.imageWaiting = true
                                        scope.launch {
                                            val full = components.pageImages.fullImage(tab)
                                            overlays.imageWaiting = false
                                            when {
                                                !actionCard.open -> Unit
                                                full != null -> save(full)
                                                else -> {
                                                    actionCard.animateTo(Detent.Closed)
                                                    onToast(Toast("The full image didn't load", detail = "Try again once it shows"))
                                                }
                                            }
                                        }
                                    } else {
                                        save(url)
                                    }
                                },
                            ),
                            imageModifier = image,
                            waiting = overlays.imageWaiting,
                        )
                    }
                    ActionPage.Download -> {
                        val ask = downloadAsk ?: return@AnimatedContent
                        val d = ask.download
                        val site = components.store.state.findTab(d.sessionId.orEmpty())?.content?.url?.let(::displayHost)?.ifBlank { null } ?: host(d)
                        DownloadPrompt(
                            name = d.fileName?.ifBlank { null } ?: "File",
                            detail = listOfNotNull(d.contentLength?.takeIf { it > 0 }?.let(::sizeText), site).joinToString(" · "),
                            interactive = interactive,
                            onCancel = { answerDownload(start = false) },
                            onDownload = { answerDownload(start = true) },
                        )
                    }
                    ActionPage.Duplicate -> {
                        val ask = dupAsk ?: return@AnimatedContent
                val dup = ask.dup
                val files = components.downloadFiles
                val thumb by produceState<ImageBitmap?>(null, dup.existingName, dup.existingFolder) {
                    value = withContext(Dispatchers.IO) { fileThumbnail(context, files, dup.existingName, dup.existingFolder, dup.download.contentType) }
                }
                val folder = dup.existingFolder.takeIf { it.startsWith("content://") }?.let(::folderLabel) ?: dup.existingFolder.substringAfterLast('/').ifEmpty { "Download" }
                DuplicateDownloadPrompt(
                    name = dup.existingName,
                    // A new name nobody has is kept as it is; else the first free "name (1).ext".
                    keepBothName = { n -> if (!files.fileExists(dup.existingFolder, n)) n else DuplicateDownloads.keepBothName(n) { files.fileExists(dup.existingFolder, it) } },
                    folder = folder,
                    thumbnail = thumb,
                    contentType = dup.download.contentType,
                    thumbModifier = image,
                    size = dup.download.contentLength?.takeIf { it > 0 }?.let(::sizeText),
                    interactive = interactive,
                    onOpen = {
                        val opened = files.openFile(dup.existingName, dup.existingFolder, dup.earlier?.contentType ?: dup.download.contentType)
                        if (!opened) onToast(Toast("Couldn't open the file"))
                        answerDuplicate(null)
                    },
                    onReplace = { n ->
                        // The old file goes (and its row), then the new one takes its name.
                        files.deleteMediaFile(context.contentResolver, dup.existingName, dup.existingFolder)
                        dup.earlier?.let { components.downloadsUseCases.removeDownload(it.id) }
                        if (n != dup.existingName && files.fileExists(dup.existingFolder, n)) files.deleteMediaFile(context.contentResolver, n, dup.existingFolder)
                        answerDuplicate(dup.download.copy(fileName = n))
                    },
                    onKeepBoth = { n ->
                        val name = if (files.fileExists(dup.existingFolder, n)) DuplicateDownloads.keepBothName(n) { files.fileExists(dup.existingFolder, it) } else n
                        answerDuplicate(dup.download.copy(fileName = name))
                    },
                    onCancel = { answerDuplicate(null) },
                )
                    }
                    null -> Unit
                }
            }
        }
    }
}

/**
 * A small picture of a file already downloaded: the image itself (scaled down), an APK's app icon,
 * else null (the card shows the file's type).
 */
private fun fileThumbnail(context: Context, files: mozilla.components.support.utils.DownloadFileUtils, name: String, folder: String, type: String?): ImageBitmap? = runCatching {
    val uri = files.findDownloadFileUri(name, folder) ?: return null
    val lower = name.lowercase()
    when {
        type?.startsWith("image/") == true || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif") -> {
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
            val sample = generateSequence(1) { it * 2 }.first { maxOf(bounds.outWidth, bounds.outHeight) / it <= 256 }
            context.contentResolver.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })
            }?.asImageBitmap()
        }
        lower.endsWith(".apk") -> {
            // The package manager reads an archive from a path: the folder's own (not a picked tree).
            val path = (if (uri.scheme == "file") uri.path else java.io.File(folder, name).takeIf { folder.startsWith("/") && it.canRead() }?.path) ?: return null
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(path, 0)?.applicationInfo ?: return null
            info.sourceDir = path
            info.publicSourceDir = path
            pm.getApplicationIcon(info).toBitmap(128, 128).asImageBitmap()
        }
        else -> null
    }
}.getOrNull()

/** What the one action card shows (KoanOverlays): a pressed link / image, a download to confirm, a duplicate. */
private enum class ActionPage { Link, Download, Duplicate }
