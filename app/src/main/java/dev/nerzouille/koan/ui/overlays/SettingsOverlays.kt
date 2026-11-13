package dev.nerzouille.koan.ui.overlays

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nerzouille.koan.BuildConfig
import dev.nerzouille.koan.data.AccountState
import dev.nerzouille.koan.data.Login
import dev.nerzouille.koan.data.StartPageAfter
import dev.nerzouille.koan.data.SyncBackChoice
import dev.nerzouille.koan.data.ThemeMode
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.search.SearchEngine
import dev.nerzouille.koan.search.origin
import dev.nerzouille.koan.ui.Overlays
import dev.nerzouille.koan.ui.bar.DrawerStatus
import dev.nerzouille.koan.ui.kit.Toast
import dev.nerzouille.koan.ui.kit.copiedToast
import dev.nerzouille.koan.ui.kit.slideUpPage
import dev.nerzouille.koan.ui.menu.SignOutConfirm
import dev.nerzouille.koan.ui.menu.TranslateActions
import dev.nerzouille.koan.ui.menu.UBlockUi
import dev.nerzouille.koan.ui.menu.languageName
import dev.nerzouille.koan.ui.settings.PasswordsUi
import dev.nerzouille.koan.ui.settings.SettingsPage
import dev.nerzouille.koan.ui.settings.SettingsScreen
import dev.nerzouille.koan.ui.settings.SettingsUi
import dev.nerzouille.koan.ui.settings.TranslationSettings
import dev.nerzouille.koan.ui.settings.authenticate
import dev.nerzouille.koan.ui.sheet.Detent
import dev.nerzouille.koan.ui.sheet.KoanCard
import dev.nerzouille.koan.ui.signin.FxAWeb
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.tour.Highlights
import dev.nerzouille.koan.ui.tour.TourSheet
import dev.nerzouille.koan.ui.tour.featureVersion
import dev.nerzouille.koan.update.UpdateCheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mozilla.components.browser.state.action.TranslationsAction
import mozilla.components.browser.state.state.TranslationsBrowserState
import mozilla.components.concept.engine.translate.LanguageSetting
import mozilla.components.concept.engine.translate.TranslationOperation

/** Settings, full screen, sliding up over everything (and the account's sign-out card, [SignOutOverlay]). */
@Composable
internal fun SettingsOverlays(
    components: Components,
    overlays: Overlays,
    scope: CoroutineScope,
    engines: List<SearchEngine>,
    drawerStatus: DrawerStatus,
    folderName: String?,
    ublockUi: UBlockUi?,
    translateEngine: TranslationsBrowserState,
    translateActions: TranslateActions,
    activeTabId: String?,
    onSignIn: () -> Unit,
    onOpenNew: (url: String, select: Boolean, nextToCurrent: Boolean) -> Unit,
    onToast: (Toast) -> Unit,
) {
    val context = LocalContext.current
    fun dispatchTranslate(action: mozilla.components.browser.state.action.BrowserAction) = components.store.dispatch(action)
    val account by components.accounts.state.collectAsStateWithLifecycle()
    val theme by components.settings.themeMode.collectAsStateWithLifecycle(ThemeMode.System)
    val engine by components.settings.searchEngine.collectAsStateWithLifecycle(SearchEngine.DuckDuckGo)
    val startPageAfter by components.settings.startPageAfter.collectAsStateWithLifecycle(StartPageAfter.Hour)
    val translateOffer by components.settings.translateOffer.collectAsStateWithLifecycle(true)
    val suggestionsOn by components.settings.searchSuggestions.collectAsStateWithLifecycle(true)
    val hideStatusBar by components.settings.hideStatusBar.collectAsStateWithLifecycle(false)
    val syncBack by components.settings.syncBack.collectAsStateWithLifecycle(SyncBackChoice())
    val syncBackEdits by components.settings.syncBackPasswordEdits.collectAsStateWithLifecycle(false)
    val syncWriteStatus by components.syncWrite.status.collectAsStateWithLifecycle()
    val pictureInPicture by components.settings.pictureInPicture.collectAsStateWithLifecycle(true)
    val blockedWeek by components.settings.blockedByDay.collectAsStateWithLifecycle(emptyMap())
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
        scope.launch { components.settings.setDownloadLocation(uri.toString()) }
    }
    // ---- Settings, full screen, sliding up over everything ----
    val roles = remember { context.getSystemService(Context.ROLE_SERVICE) as RoleManager }
    var isDefault by remember { mutableStateOf(roles.isRoleHeld(RoleManager.ROLE_BROWSER)) }
    LifecycleResumeEffect(Unit) {
        isDefault = roles.isRoleHeld(RoleManager.ROLE_BROWSER)
        onPauseOrDispose {}
    }
    val askDefault = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault = roles.isRoleHeld(RoleManager.ROLE_BROWSER)
    }
    // Passwords: the sites are listed freely; usernames and passwords only once unlocked, and only
    // while the Passwords pages are open.
    val loginSummaries by components.logins.summaries.collectAsStateWithLifecycle(emptyList())
    var unlockedLogins by remember { mutableStateOf<Map<String, Login>?>(null) }
    val inPasswords = overlays.settingsOpen && overlays.settingsNav.pages.any { it == SettingsPage.Passwords }
    LaunchedEffect(inPasswords) { if (!inPasswords) unlockedLogins = null }
    val slide = remember { Animatable(0f) }
    LaunchedEffect(overlays.settingsOpen) {
        slide.animateTo(if (overlays.settingsOpen) 1f else 0f, Motion.Sheet)
        if (!overlays.settingsOpen) overlays.settingsNav.reset()
    }
    val settingsVisible by remember { derivedStateOf { overlays.settingsOpen || slide.value > 0f } }
    if (settingsVisible) {
        val signedIn = account as? AccountState.SignedIn
        SettingsScreen(
            ui = SettingsUi(
                settings = components.settings,
                theme = theme,
                onTheme = { mode -> scope.launch { components.settings.setThemeMode(mode) } },
                engine = engine,
                onEngine = { e -> scope.launch { components.settings.setSearchEngine(e) } },
                engines = engines,
                onSaveEngine = { e ->
                    val added = engines.none { it.id == e.id }
                    scope.launch { components.settings.saveCustomEngine(e) }
                    onToast(Toast(if (added) "${e.label} added" else "${e.label} saved"))
                },
                onDeleteEngine = { e ->
                    // Undo puts it back, as the default again if it was.
                    val wasDefault = e.id == engine.id
                    scope.launch { components.settings.deleteCustomEngine(e.id) }
                    onToast(
                        Toast("${e.label} deleted", "Undo") {
                            scope.launch {
                                components.settings.saveCustomEngine(e)
                                if (wasDefault) components.settings.setSearchEngine(e)
                            }
                        },
                    )
                },
                startPageAfter = startPageAfter,
                onStartPageAfter = { v -> scope.launch { components.settings.setStartPageAfter(v) } },
                suggestions = suggestionsOn,
                onSuggestions = { on -> scope.launch { components.settings.setSearchSuggestions(on) } },
                syncBack = syncBack,
                onSyncBack = { c -> scope.launch { components.settings.setSyncBack(c) } },
                syncBackPasswordEdits = syncBackEdits,
                onSyncBackPasswordEdits = { on -> scope.launch { components.settings.setSyncBackPasswordEdits(on) } },
                syncWriteStatus = syncWriteStatus,
                hideStatusBar = hideStatusBar,
                onHideStatusBar = { on -> scope.launch { components.settings.setHideStatusBar(on) } },
                pictureInPicture = pictureInPicture,
                onPictureInPicture = { on -> scope.launch { components.settings.setPictureInPicture(on) } },
                email = signedIn?.email,
                status = drawerStatus,
                onSyncNow = { components.sync.syncNow() },
                onSignIn = {
                    overlays.settingsOpen = false
                    onSignIn()
                },
                onSignOut = { overlays.signOutCard.animateTo(Detent.Compact) },
                isDefault = isDefault,
                onMakeDefault = { askDefault.launch(roles.createRequestRoleIntent(RoleManager.ROLE_BROWSER)) },
                version = BuildConfig.VERSION_NAME,
                checkUpdate = { UpdateCheck.check(BuildConfig.VERSION_NAME) },
                onTour = { overlays.showTour(TourSheet.Tour) },
                onWhatsNew = featureVersion(BuildConfig.VERSION_NAME).takeIf { it in Highlights }
                    ?.let { version -> { overlays.showTour(TourSheet.WhatsNew(version)) } },
                onOpenUrl = { url ->
                    overlays.settingsOpen = false
                    onOpenNew(url, true, false)
                },
                downloadFolder = folderName,
                onPickDownloadFolder = { pickFolder.launch(null) },
                onDefaultDownloadFolder = { scope.launch { components.settings.setDownloadLocation(null) } },
                onShowDownloads = { overlays.downloadsOpen = true },
                ublock = ublockUi,
                ublockWeek = blockedWeek.values.sum(),
                onUblock = components.ublock::setEnabled,
                onInstallUblock = components.ublock::install,
                translation = TranslationSettings(
                    always = translateEngine.languageSettings.orEmpty().filterValues { it == LanguageSetting.ALWAYS }.keys.map { it to languageName(it) }.sortedBy { it.second },
                    never = translateEngine.languageSettings.orEmpty().filterValues { it == LanguageSetting.NEVER }.keys.map { it to languageName(it) }.sortedBy { it.second },
                    neverSites = translateEngine.neverTranslateSites.orEmpty(),
                    onRemoveLanguage = { code ->
                        dispatchTranslate(TranslationsAction.UpdateLanguageSettingsAction(code, LanguageSetting.OFFER))
                        (activeTabId ?: components.store.state.selectedTabId)?.let { dispatchTranslate(TranslationsAction.OperationRequestedAction(it, TranslationOperation.FETCH_AUTOMATIC_LANGUAGE_SETTINGS)) }
                    },
                    onRemoveSite = { origin -> dispatchTranslate(TranslationsAction.RemoveNeverTranslateSiteAction(origin)) },
                    onPhone = translateEngine.languageModels.orEmpty()
                        .filter { it.status == mozilla.components.concept.engine.translate.ModelState.DOWNLOADED }
                        .mapNotNull { m -> m.language?.let { Triple(it.code, it.localizedDisplayName?.ifBlank { null } ?: languageName(it.code), m.size) } }
                        .sortedBy { it.second },
                    onDeleteLanguage = translateActions.deleteLanguage,
                    offer = translateOffer,
                    onOffer = { on -> scope.launch { components.settings.setTranslateOffer(on) } },
                    onOpen = {
                        (activeTabId ?: components.store.state.selectedTabId)?.let { id ->
                            dispatchTranslate(TranslationsAction.OperationRequestedAction(id, TranslationOperation.FETCH_LANGUAGE_MODELS))
                            dispatchTranslate(TranslationsAction.OperationRequestedAction(id, TranslationOperation.FETCH_AUTOMATIC_LANGUAGE_SETTINGS))
                            dispatchTranslate(TranslationsAction.OperationRequestedAction(id, TranslationOperation.FETCH_NEVER_TRANSLATE_SITES))
                        }
                    },
                ),
                passwords = PasswordsUi(
                    summaries = loginSummaries,
                    unlocked = unlockedLogins,
                    onUnlock = { site, first ->
                        (context as? FragmentActivity)?.let { activity ->
                            authenticate(activity, "Unlock your passwords") {
                                // Unlocked at once; the logins fill in as they're opened (the tapped site's first).
                                unlockedLogins = emptyMap()
                                val onSite = overlays.settingsNav.pages.lastOrNull() == SettingsPage.PasswordSite(site ?: "")
                                if (site != null && !onSite) overlays.settingsNav.push(SettingsPage.PasswordSite(site))
                                scope.launch {
                                    fun add(logins: List<Login>) {
                                        val current = unlockedLogins ?: return
                                        unlockedLogins = current + logins.associateBy { it.guid }
                                    }
                                    add(first.mapNotNull { components.logins.openOne(it) })
                                    components.logins.openAll(::add)
                                }
                            }
                        }
                    },
                    onUnlockThen = { then ->
                        (context as? FragmentActivity)?.let { activity ->
                            authenticate(activity, "Unlock your passwords") {
                                unlockedLogins = emptyMap()
                                then()
                                scope.launch {
                                    components.logins.openAll { logins ->
                                        val current = unlockedLogins ?: return@openAll
                                        unlockedLogins = current + logins.associateBy { it.guid }
                                    }
                                }
                            }
                        }
                    },
                    onCopied = { what -> copiedToast(what)?.let(onToast) },
                    onRemove = { guids ->
                        scope.launch {
                            val removed = components.logins.remove(guids)
                            if (removed.count == 0) return@launch
                            val what = if (removed.count == 1) "Password deleted" else "${removed.count} passwords deleted"
                            val detail = if (removed.hidden.isNotEmpty()) "Hidden on this phone; still in your Mozilla account" else null
                            onToast(Toast(what, "Undo", detail = detail) { scope.launch { components.logins.restore(removed) } })
                        }
                    },
                    onConfirm = { title, text, confirm, action -> overlays.ask(title, text, confirm, action = action) },
                    // Deleting an account login sends its tombstone only with both switches on.
                    deletesReachAccount = syncBack.passwords && syncBackEdits,
                    onOpenUrl = { url ->
                        overlays.settingsOpen = false
                        onOpenNew(url, true, false)
                    },
                    onAdd = { site, username, password ->
                        val ok = components.logins.addManual(site, username, password)
                        if (ok) {
                            onToast(Toast("Password saved"))
                            // Unlocked: the new one opens too, so its row shows its username.
                            if (unlockedLogins != null) {
                                components.logins.openAll { logins ->
                                    val current = unlockedLogins ?: return@openAll
                                    unlockedLogins = current + logins.associateBy { it.guid }
                                }
                            }
                        }
                        ok
                    },
                ),
            ),
            nav = overlays.settingsNav,
            onClose = { overlays.settingsOpen = false },
            modifier = Modifier.slideUpPage { slide.value },
        )
    }
}

/** Sign out asks first. */
@Composable
internal fun SignOutOverlay(components: Components, overlays: Overlays) {
    val signOutCard = overlays.signOutCard
    KoanCard(signOutCard) { interactive ->
        SignOutConfirm(
            interactive = interactive,
            onSignOut = {
                signOutCard.animateTo(Detent.Closed)
                overlays.settingsNav.pop()
                FxAWeb.wipeWebSession()
                components.sync.signOut()
            },
            onCancel = { signOutCard.animateTo(Detent.Closed) },
        )
    }
}
