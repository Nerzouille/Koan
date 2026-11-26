package dev.nerzouille.koan.ui.overlays

import android.view.autofill.AutofillManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nerzouille.koan.data.Login
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.search.origin
import dev.nerzouille.koan.ui.Overlays
import dev.nerzouille.koan.ui.bar.barZone
import dev.nerzouille.koan.ui.browser.observeSlice
import dev.nerzouille.koan.ui.menu.FilePrompt
import dev.nerzouille.koan.ui.menu.LoginChips
import dev.nerzouille.koan.ui.menu.LoginSheet
import dev.nerzouille.koan.ui.menu.PagePrompts
import dev.nerzouille.koan.ui.menu.SaveLoginCard
import dev.nerzouille.koan.ui.sheet.Detent
import dev.nerzouille.koan.ui.sheet.KoanCard
import dev.nerzouille.koan.ui.sheet.SheetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.concept.engine.prompt.PromptRequest

/**
 * Logins: fill from a chip (7f) or the full list (7e), save a new one on the phone; and what else a
 * page asks: its file field (Android's picker), <select>, pickers, alert/confirm, HTTP sign-in, pop-ups.
 */
@Composable
internal fun LoginOverlays(
    components: Components,
    overlays: Overlays,
    scope: CoroutineScope,
    activeTabId: String?,
) {
    // ---- logins: fill from a chip (7f) or the full list (7e); save a new one on the phone ----
    val loginState by components.store.observeSlice(activeTabId) { s ->
        val content = activeTabId?.let { s.findTab(it) }?.content
        val prompt = content?.promptRequests?.firstOrNull { it is PromptRequest.SelectLoginPrompt || it is PromptRequest.SaveLoginPrompt }
        prompt to (content?.private == false)
    }
    val (loginPrompt, regularTab) = loginState
    // A page's file field: Android's picker (and the camera).
    FilePrompt(components, activeTabId)
    // Everything else a page asks: <select>, pickers, alert/confirm, HTTP sign-in, pop-ups.
    PagePrompts(components, activeTabId, overlays.promptCard)
    fun consumePrompt(p: PromptRequest) { activeTabId?.let { components.store.dispatch(ContentAction.ConsumePromptRequestAction(it, p)) } }
    val select = loginPrompt as? PromptRequest.SelectLoginPrompt
    if (select != null && select.logins.isNotEmpty()) {
        fun use(login: mozilla.components.concept.storage.Login) {
            overlays.loginCard.animateTo(Detent.Closed)
            select.onConfirm(login)
            consumePrompt(select)
        }
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
            LoginChips(
                select.logins, onPick = ::use,
                onMore = { overlays.loginCard.animateTo(Detent.Compact) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .padding(bottom = barZone(overview = false)),
            )
        }
        KoanCard(overlays.loginCard) { interactive ->
            LoginSheet(
                origin = select.logins.first().origin, logins = select.logins, interactive = interactive,
                onUse = ::use,
                onNotNow = {
                    overlays.loginCard.animateTo(Detent.Closed)
                    select.onDismiss()
                    consumePrompt(select)
                },
            )
        }
    }
    SaveLoginOverlay(components, overlays.saveLoginCard, scope, loginPrompt as? PromptRequest.SaveLoginPrompt, regularTab, ::consumePrompt)
}

@Composable
private fun SaveLoginOverlay(
    components: Components,
    card: SheetState,
    scope: CoroutineScope,
    save: PromptRequest.SaveLoginPrompt?,
    regularTab: Boolean,
    consumePrompt: (PromptRequest) -> Unit,
) {
    val systemAutofill by components.settings.systemAutofill.collectAsStateWithLifecycle(false)
    val context = LocalContext.current
    val autofillManager = remember(context) { context.getSystemService(AutofillManager::class.java) }
    val entry = save?.logins?.firstOrNull()
    val systemHandlesSave = save != null && regularTab && systemAutofill && autofillManager?.isEnabled == true
    var savedBefore by remember { mutableStateOf(false) }
    LaunchedEffect(save, systemHandlesSave) {
        if (save == null || entry == null) return@LaunchedEffect
        if (systemHandlesSave) {
            card.animateTo(Detent.Closed)
            save.onDismiss()
            consumePrompt(save)
            return@LaunchedEffect
        }
        savedBefore = components.logins.forOrigin(entry.origin).any { it.username == entry.username }
        card.animateTo(Detent.Compact)
    }
    KoanCard(card) { interactive ->
        if (save == null || entry == null || systemHandlesSave) return@KoanCard
        SaveLoginCard(
            origin = entry.origin, username = entry.username, update = savedBefore, interactive = interactive,
            onSave = {
                card.animateTo(Detent.Closed)
                scope.launch {
                    components.logins.saveLocal(
                        entry.origin, entry.formActionOrigin, entry.httpRealm, entry.usernameField, entry.passwordField,
                        entry.username, entry.password,
                    )
                }
                save.onConfirm(entry)
                consumePrompt(save)
            },
            onNotNow = {
                card.animateTo(Detent.Closed)
                save.onDismiss()
                consumePrompt(save)
            },
        )
    }
}
