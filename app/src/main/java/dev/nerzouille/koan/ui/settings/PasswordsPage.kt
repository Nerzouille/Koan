package dev.nerzouille.koan.ui.settings

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dev.nerzouille.koan.data.Login
import dev.nerzouille.koan.data.LoginStore
import dev.nerzouille.koan.data.LoginSummary
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.KoanField
import dev.nerzouille.koan.ui.kit.KoanHaptics
import dev.nerzouille.koan.ui.kit.PrimaryButton
import dev.nerzouille.koan.ui.kit.SecureWindow
import dev.nerzouille.koan.ui.kit.SelectionAction
import dev.nerzouille.koan.ui.kit.SelectionBar
import dev.nerzouille.koan.ui.kit.SelectionCheck
import dev.nerzouille.koan.ui.kit.SelectionHeader
import dev.nerzouille.koan.ui.kit.SiteIcon
import dev.nerzouille.koan.ui.kit.SwipeRow
import dev.nerzouille.koan.ui.kit.TextLink
import dev.nerzouille.koan.ui.kit.rememberSelection
import dev.nerzouille.koan.ui.kit.selectionWash
import dev.nerzouille.koan.ui.theme.DownloadTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.PasswordTokens
import dev.nerzouille.koan.ui.theme.SelectionTokens
import dev.nerzouille.koan.ui.theme.SettingsTokens
import dev.nerzouille.koan.ui.topSafePadding
import kotlinx.coroutines.launch

/**
 * What the Passwords pages need. [unlocked]: the logins opened so far after the fingerprint or PIN
 * (they fill in progressively), null while locked. [onUnlock]: asks for the fingerprint, then
 * opens [site]'s page if given (its logins [first] are opened before the others).
 */
class PasswordsUi(
    val summaries: List<LoginSummary>,
    val unlocked: Map<String, Login>?,
    val onUnlock: (site: String?, first: List<String>) -> Unit,
    /** Asks for the fingerprint or PIN, then runs [then] (selection mode on a locked list). */
    val onUnlockThen: (then: () -> Unit) -> Unit = {},
    /** Something was copied ("Password"): confirms it where Android doesn't. */
    val onCopied: (what: String) -> Unit,
    /** Deleted (after its confirmation card): the phone's logins deleted, the account's hidden (Undo in the toast). */
    val onRemove: (guids: List<String>) -> Unit = {},
    /** Asks first, on a card: title, text, the confirm button, what it does. */
    val onConfirm: (title: String, text: String, confirm: String, action: () -> Unit) -> Unit = { _, _, _, _ -> },
    /** A delete reaches the Mozilla account too (two-way sync's "Change and delete passwords" on). */
    val deletesReachAccount: Boolean = false,
    /** "Visit": the login's site in a new tab. */
    val onOpenUrl: (String) -> Unit = {},
    /** Passwords › +: saves a login typed in by hand; false when the site isn't a web address. */
    val onAdd: suspend (site: String, username: String, password: String) -> Boolean = { _, _, _ -> false },
)

/**
 * Settings › Passwords: one row per site on the page itself (hairlines, "N accounts" and a chevron
 * at the end), the site's favicon in grey (else the key), searchable, with the library rules
 * (ui/kit/LibraryRow): a tap opens the site's accounts (after the fingerprint or the device PIN,
 * once per visit), a swipe right to left deletes, a long press enters selection mode (Visit,
 * Delete). A delete always asks first; then Undo. Locked, a swipe or a long press asks for the
 * fingerprint or PIN first. The floating + adds one by hand.
 */
@Composable
internal fun PasswordsPage(ui: PasswordsUi, on: Boolean, onBack: () -> Unit, onOpenSite: (String) -> Unit, onAdd: () -> Unit) {
    // No screenshot, recording or Recents preview of this page.
    SecureWindow()
    val palette = KoanTheme.colors
    var query by remember { mutableStateOf("") }
    val selection = rememberSelection<String>()
    val unlocked = ui.unlocked
    val q = query.trim()
    val shown = ui.summaries.filter { s ->
        q.isEmpty() || s.origin.contains(q, ignoreCase = true) ||
            unlocked?.get(s.guid)?.username?.contains(q, ignoreCase = true) == true
    }
    val sites = shown.groupBy(::siteOf)
    SideEffect { selection.keepOnly(sites.keys) }
    val selecting = selection.active

    /** The confirmation card, then the delete. */
    fun askDelete(picked: List<Pair<String, List<LoginSummary>>>) =
        confirmDelete(ui, picked) { selection.clear() }

    Box(Modifier.fillMaxSize()) {
        SettingsPageFrame("Passwords", on, onBack, titleHidden = selecting) {
            BackHandler(enabled = selecting, onBack = selection::clear)
            if (ui.summaries.isEmpty()) {
                Spacer(Modifier.height(SettingsTokens.SectionTop))
                Text("No saved passwords yet", color = palette.TextPrimary, fontSize = SettingsTokens.RowTitleSize, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(SettingsTokens.FieldLabelGap))
                Text(
                    "Passwords from your Mozilla account show up here after the next sync. You can also add one yourself.",
                    color = palette.TextSecondary,
                    fontSize = SettingsTokens.RowValueSize,
                )
                Spacer(Modifier.height(SettingsTokens.SectionTop))
                PrimaryButton("Add a password", onClick = { if (on) onAdd() }, modifier = Modifier.fillMaxWidth())
                return@SettingsPageFrame
            }
            KoanField(query, { query = it }, "Search passwords", Modifier.fillMaxWidth(), search = true)
            Spacer(Modifier.height(SettingsTokens.RowInset))
            if (unlocked == null) {
                Text(
                    "${ui.summaries.size} saved. Unlock to see usernames and passwords.",
                    color = palette.TextSecondary,
                    fontSize = SettingsTokens.RowValueSize,
                )
                Spacer(Modifier.height(SettingsTokens.RowInset))
                PrimaryButton("Unlock", onClick = { if (on) ui.onUnlock(null, emptyList()) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(SettingsTokens.RowInset))
            }
            if (shown.isEmpty()) {
                Text("No password matches “$q”.", color = palette.TextSecondary, fontSize = SettingsTokens.RowValueSize)
            }
            sites.entries.forEachIndexed { i, (site, logins) ->
                key(site) {
                    if (i > 0) SettingsDivider()
                    val single = logins.singleOrNull()
                    // Locked: the fingerprint first, then straight to the site's accounts.
                    val open = { if (unlocked == null) ui.onUnlock(site, logins.map { it.guid }) else onOpenSite(site) }
                    // Locked: nothing is deleted or selected before the fingerprint or PIN; then the
                    // swipe's delete card, or selection mode for a long press.
                    SwipeRow(
                        onDelete = {
                            if (unlocked != null) askDelete(listOf(site to logins)) else ui.onUnlockThen { askDelete(listOf(site to logins)) }
                        },
                        enabled = on && !selecting,
                        springBack = true,
                    ) {
                        SiteRow(
                            origin = logins.first().origin,
                            site = site,
                            // One account: its username once unlocked; several: how many, at the end.
                            subtitle = single?.let { s -> unlocked?.let { it[s.guid]?.username?.ifEmpty { "No username" } ?: "…" } },
                            value = if (single == null) "${logins.size} accounts" else if (single.local) "This phone" else null,
                            checked = if (selecting) site in selection else null,
                            on = on,
                            onTap = { if (selecting) selection.toggle(site) else open() },
                            onLong = {
                                when {
                                    unlocked == null -> ui.onUnlockThen { selection.start(site) }
                                    selecting -> selection.toggle(site)
                                    else -> selection.start(site)
                                }
                            },
                        )
                    }
                }
            }
            // Room for the selection bar, or the floating +, over the last rows.
            Spacer(Modifier.height(if (selecting) SelectionTokens.BarHeight + SettingsTokens.SectionTop else PasswordTokens.AddButton + PasswordTokens.AddMargin))
        }
        if (!selecting && ui.summaries.isNotEmpty()) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(PasswordTokens.AddMargin)
                    .size(PasswordTokens.AddButton)
                    .shadow(PasswordTokens.AddShadow, CircleShape)
                    .clip(CircleShape)
                    .background(palette.Primary)
                    .clickable(enabled = on, onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(KoanIcons.Plus, "Add a password", tint = palette.OnPrimary, modifier = Modifier.size(SettingsTokens.RowGlyph))
            }
        }
        if (selecting) {
            val picked = sites.filterKeys { it in selection }.toList()
            SelectionHeader(
                picked.size, interactive = on, onCancel = selection::clear,
                onSelectAll = { selection.set(sites.keys) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(palette.Base)
                    .settingsWash(palette.isDark)
                    .topSafePadding(),
            )
            val actions = if (picked.size == 1) {
                listOf(
                    SelectionAction(KoanIcons.Globe, "Visit") {
                        selection.clear()
                        ui.onOpenUrl(picked[0].second.first().origin)
                    },
                    SelectionAction(KoanIcons.Trash, "Delete", destructive = true) { askDelete(picked) },
                )
            } else {
                listOf(SelectionAction(KoanIcons.Trash, "Delete", destructive = true) { askDelete(picked) })
            }
            SelectionBar(
                actions, interactive = on,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = SettingsTokens.Padding, vertical = DownloadTokens.BottomGap),
            )
        }
    }
}

/**
 * Deleting [picked] (site → its logins) asks first, on a card: who (site · username, or the
 * sites), what it affects (this phone only, or the Mozilla account too), Cancel / Delete. Then the
 * delete ([PasswordsUi.onRemove]: an Undo toast) and [after].
 */
internal fun confirmDelete(ui: PasswordsUi, picked: List<Pair<String, List<LoginSummary>>>, after: () -> Unit = {}) {
    val logins = picked.flatMap { it.second }
    if (logins.isEmpty()) return
    val unlocked = ui.unlocked
    val reaches = ui.deletesReachAccount && logins.any { !it.local }
    val effect = if (reaches) "Removed from this phone and your Mozilla account." else "Removed from this phone."
    val one = logins.singleOrNull()
    val title = if (one != null) "Delete this password?" else "Delete ${logins.size} passwords?"
    val who = when {
        one != null -> listOfNotNull(siteOf(one), unlocked?.get(one.guid)?.username?.ifEmpty { null }).joinToString(" · ")
        picked.size == 1 -> "${picked[0].first} · ${logins.size} accounts"
        else -> picked.map { it.first }.let { names ->
            if (names.size <= 3) names.joinToString(", ") else names.take(3).joinToString(", ") + " and ${names.size - 3} more"
        }
    }
    ui.onConfirm(title, "$who\n$effect", "Delete") {
        after()
        ui.onRemove(logins.map { it.guid })
    }
}

/** A site of the list: its grey favicon (or the key), the site, a username or "N accounts", the chevron (or the check mark). */
@Composable
private fun SiteRow(origin: String, site: String, subtitle: String?, value: String?, checked: Boolean?, on: Boolean, onTap: () -> Unit, onLong: () -> Unit) {
    val palette = KoanTheme.colors
    val view = LocalView.current
    val wash = selectionWash(checked == true)
    Row(
        Modifier
            .rowArea(
                if (on) onTap else null,
                if (on) { { KoanHaptics.hold(view); onLong() } } else null,
                wash = { wash.value },
            )
            .height(if (subtitle != null) SettingsTokens.RowTallHeight else SettingsTokens.RowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SiteIcon(origin, PasswordTokens.Favicon, fallback = KoanIcons.Key)
        Spacer(Modifier.width(SettingsTokens.RowGlyphGap))
        Column(Modifier.weight(1f)) {
            Text(site, color = palette.TextPrimary, fontSize = SettingsTokens.RowTitleSize, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, color = palette.TextSecondary, fontSize = SettingsTokens.RowSubtitleSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (value != null) Text(value, color = palette.TextSecondary, fontSize = SettingsTokens.RowValueSize, maxLines = 1)
        Spacer(Modifier.width(SettingsTokens.Divider * 6))
        if (checked != null) {
            SelectionCheck(checked)
        } else {
            Icon(KoanIcons.ChevronRight, null, tint = palette.TextSecondary, modifier = Modifier.size(SettingsTokens.RowGlyph))
        }
    }
}

/** Passwords › +: a login typed in by hand (site, username, password), saved encrypted on the phone. */
@Composable
internal fun AddPasswordPage(ui: PasswordsUi, on: Boolean, onBack: () -> Unit) {
    // No screenshot, recording or Recents preview of this page.
    SecureWindow()
    val palette = KoanTheme.colors
    val scope = rememberCoroutineScope()
    var site by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    var tried by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val siteError = when {
        site.isBlank() -> "Type the site, like example.com"
        LoginStore.originOf(site) == null -> "That isn't a web address"
        else -> null
    }
    val passwordError = if (password.isEmpty()) "Type the password" else null
    // Koan keeps this one itself: no "Save to <password manager>?" from Android's autofill.
    val view = LocalView.current
    DisposableEffect(view) {
        val before = view.importantForAutofill
        view.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        onDispose { view.importantForAutofill = before }
    }
    SettingsPageFrame("Add a password", on, onBack) {
        Field("Site", site, "example.com", siteError.takeIf { tried }, keyboard = KeyboardType.Uri) { site = it }
        Field("Username", username, "name@example.com", null, help = "Leave empty if the site only asks for a password.", keyboard = KeyboardType.Email) { username = it }
        Field(
            "Password", password, "Password", passwordError.takeIf { tried },
            keyboard = KeyboardType.Password,
            visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
        ) { password = it }
        Row(Modifier.padding(top = SettingsTokens.FieldLabelGap)) {
            RowAction(if (reveal) "Hide password" else "Show password") { if (on) reveal = !reveal }
        }
        Spacer(Modifier.height(SettingsTokens.FieldGap))
        Text("Saved encrypted on this phone.", color = palette.TextSecondary, fontSize = SettingsTokens.RowSubtitleSize)
        Spacer(Modifier.height(SettingsTokens.FieldGap))
        PrimaryButton(
            "Save",
            onClick = {
                if (!on || saving) return@PrimaryButton
                tried = true
                if (siteError == null && passwordError == null) {
                    saving = true
                    scope.launch {
                        if (ui.onAdd(site, username.trim(), password)) onBack() else saving = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Settings › Passwords › a site: each account with where it comes from, its username and its
 * password, hidden until Reveal. Tapping either copies it (a password is marked sensitive); a site
 * with one account also has the big Copy password button. Locked, only where each account comes
 * from shows, its favicon in grey; unlocked, the favicon turns to colour.
 */
@Composable
internal fun PasswordSitePage(ui: PasswordsUi, site: String, on: Boolean, onBack: () -> Unit) {
    // No screenshot, recording or Recents preview of this page.
    SecureWindow()
    SettingsPageFrame(site, on, onBack) {
        val accounts = ui.summaries.filter { siteOf(it) == site }
        val unlocked = ui.unlocked
        accounts.forEachIndexed { i, s ->
            // Keyed: the accounts open one by one, each one's rows change as it does.
            key(s.guid) {
                if (i > 0) Spacer(Modifier.height(SettingsTokens.SectionTop))
                Account(
                    s, unlocked?.get(s.guid), open = unlocked != null, on, ui.onCopied,
                    // The site's last account: back to the list once it's gone.
                    onDelete = { confirmDelete(ui, listOf(site to listOf(s))) { if (accounts.size == 1) onBack() } },
                )
            }
        }
        Spacer(Modifier.height(SettingsTokens.SectionTop))
        if (unlocked == null) {
            PrimaryButton("Unlock", onClick = { if (on) ui.onUnlock(site, accounts.map { it.guid }) }, modifier = Modifier.fillMaxWidth())
        } else {
            // One account: its password one big tap away.
            val single = accounts.singleOrNull()?.let { unlocked[it.guid] }
            if (single != null) {
                val context = LocalContext.current
                PrimaryButton("Copy password", onClick = { if (on) copy(context, "Password", single.password, sensitive = true, ui.onCopied) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** One account of a site: where it comes from, then (once [open]) its username, its password and Delete ([login] null = still opening). */
@Composable
private fun Account(s: LoginSummary, login: Login?, open: Boolean, on: Boolean, onCopied: (String) -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    var shown by remember { mutableStateOf(false) }
    SettingsRow(
        null, s.origin, on, subtitle = if (s.local) "Saved on this phone" else "From your Mozilla account",
        leading = { SiteIcon(s.origin, PasswordTokens.Favicon, fallback = KoanIcons.Globe, colour = open) },
    )
    if (!open) return
    SettingsDivider()
    if (login == null) {
        SettingsRow(null, "…", on, subtitle = "Username")
    } else {
        SettingsRow(
            null, login.username.ifEmpty { "No username" }, on, subtitle = "Username",
            onClick = if (login.username.isEmpty()) null else { { copy(context, "Username", login.username, sensitive = false, onCopied) } },
        ) { if (login.username.isNotEmpty()) RowAction("Copy") }
        SettingsDivider()
        // Tapping the password copies it, hidden or shown; Reveal / Hide is its own tap.
        SettingsRow(null, if (shown) login.password else "••••••••••", on, subtitle = "Password", onClick = { copy(context, "Password", login.password, sensitive = true, onCopied) }) {
            RowAction(if (shown) "Hide" else "Reveal") { if (on) shown = !shown }
        }
        SettingsDivider()
        SettingsRow(null, "Delete password", on, titleColor = KoanTheme.colors.Destructive, onClick = onDelete)
    }
}

/** A quiet word at a row's end ("Copy", "Reveal"); tappable on its own when [onClick] is given. */
@Composable
internal fun RowAction(label: String, onClick: (() -> Unit)? = null) =
    TextLink(label, onClick, SettingsTokens.RowValueSize, PaddingValues(SettingsTokens.Divider * 6))

/** The site a login is for, as the list shows it. */
private fun siteOf(s: LoginSummary): String = displayHost(s.origin).ifEmpty { s.origin }

/**
 * Copies [value]; a password is flagged sensitive so Android 13+ hides it in clipboard previews
 * (older versions ignore the flag).
 */
@SuppressLint("InlinedApi")
private fun copy(context: Context, label: String, value: String, sensitive: Boolean, onCopied: (String) -> Unit) {
    val clip = ClipData.newPlainText(label, value)
    if (sensitive) clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
    onCopied(label)
}

/** Asks for the fingerprint (or the device PIN / pattern) before passwords show. */
fun authenticate(activity: FragmentActivity, title: String, onSuccess: () -> Unit) {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        },
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL)
            .build(),
    )
}
