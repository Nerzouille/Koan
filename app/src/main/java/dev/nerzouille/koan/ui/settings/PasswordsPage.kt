package dev.nerzouille.koan.ui.settings

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import android.view.View
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dev.nerzouille.koan.data.Login
import dev.nerzouille.koan.data.LoginSummary
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.KoanField
import dev.nerzouille.koan.ui.kit.PrimaryButton
import dev.nerzouille.koan.ui.kit.FaviconTile
import dev.nerzouille.koan.ui.kit.LibraryRowFrame
import dev.nerzouille.koan.ui.kit.LibraryRowSubtitle
import dev.nerzouille.koan.ui.kit.LibraryRowTitle
import dev.nerzouille.koan.ui.kit.RowMenu
import dev.nerzouille.koan.ui.kit.RowMenuItem
import dev.nerzouille.koan.ui.kit.SwipeToDelete
import dev.nerzouille.koan.data.LoginStore
import dev.nerzouille.koan.ui.theme.DownloadTokens
import dev.nerzouille.koan.ui.theme.PasswordTokens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.launch
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SettingsTokens

/**
 * What the Passwords pages need. [unlocked]: the logins opened so far after the fingerprint or PIN
 * (they fill in progressively), null while locked. [onUnlock]: asks for the fingerprint, then
 * opens [site]'s page if given (its logins [first] are opened before the others).
 */
class PasswordsUi(
    val summaries: List<LoginSummary>,
    val unlocked: Map<String, Login>?,
    val onUnlock: (site: String?, first: List<String>) -> Unit,
    /** Something was copied ("Password"): confirms it where Android doesn't. */
    val onCopied: (what: String) -> Unit,
    /** Swiped away or "Delete" in the menu: the phone's logins deleted, the account's hidden (Undo in the toast). */
    val onRemove: (guids: List<String>) -> Unit = {},
    /** A row's long-press menu. */
    val onMenu: (RowMenu) -> Unit = {},
    /** "Open site": the login's site in a new tab. */
    val onOpenUrl: (String) -> Unit = {},
    /** Passwords › +: saves a login typed in by hand; false when the site isn't a web address. */
    val onAdd: suspend (site: String, username: String, password: String) -> Boolean = { _, _, _ -> false },
)

/**
 * Settings › Passwords: one row per site, searchable, with the library rules (ui/kit/LibraryRow): a
 * tap opens the site's accounts (after the fingerprint or the device PIN, once per visit), a swipe
 * either way deletes (a login from the Mozilla account is only hidden on the phone; Undo), a long
 * press opens its menu. The round + adds one by hand ([onAdd]).
 */
@Composable
internal fun PasswordsPage(ui: PasswordsUi, on: Boolean, onBack: () -> Unit, onOpenSite: (String) -> Unit, onAdd: () -> Unit) {
    val palette = KoanTheme.colors
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize()) {
        SettingsPageFrame("Passwords", on, onBack) {
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
            val unlocked = ui.unlocked
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
            val q = query.trim()
            val shown = ui.summaries.filter { s ->
                q.isEmpty() || s.origin.contains(q, ignoreCase = true) ||
                    unlocked?.get(s.guid)?.username?.contains(q, ignoreCase = true) == true
            }
            if (shown.isEmpty()) {
                Text("No password matches “$q”.", color = palette.TextSecondary, fontSize = SettingsTokens.RowValueSize)
            }
            val sites = shown.groupBy(::siteOf).entries.toList()
            sites.forEachIndexed { i, (site, logins) ->
                key(site) {
                    val single = logins.singleOrNull()
                    val login = single?.let { unlocked?.get(it.guid) }
                    // Locked: the fingerprint first, then straight to the site's accounts.
                    val open = { if (unlocked == null) ui.onUnlock(site, logins.map { it.guid }) else onOpenSite(site) }
                    val remove = { ui.onRemove(logins.map { it.guid }) }
                    SwipeToDelete(first = i == 0, last = i == sites.lastIndex, onRemove = remove) {
                        LibraryRowFrame(
                            first = i == 0,
                            onClick = { if (on) open() },
                            onLongClick = {
                                ui.onMenu(
                                    RowMenu(
                                        title = site,
                                        subtitle = logins.first().origin,
                                        iconUrl = logins.first().origin,
                                        groups = listOf(
                                            listOf(
                                                RowMenuItem(KoanIcons.Key, if (single != null) "Open password" else "Open ${logins.size} accounts") { open() },
                                                RowMenuItem(KoanIcons.Plus, "Open site in new tab") { ui.onOpenUrl(logins.first().origin) },
                                            ),
                                            listOfNotNull(
                                                login?.takeIf { it.username.isNotEmpty() }?.let { l ->
                                                    RowMenuItem(KoanIcons.Link, "Copy username") { copy(context, "Username", l.username, sensitive = false, ui.onCopied) }
                                                },
                                                login?.let { l -> RowMenuItem(KoanIcons.Key, "Copy password") { copy(context, "Password", l.password, sensitive = true, ui.onCopied) } },
                                            ),
                                            listOf(RowMenuItem(KoanIcons.Trash, if (logins.size > 1) "Delete ${logins.size} passwords" else "Delete password", destructive = true) { remove() }),
                                        ),
                                    ),
                                )
                            },
                        ) {
                            FaviconTile(logins.first().origin)
                            Spacer(Modifier.width(DownloadTokens.RowGap))
                            Column(Modifier.weight(1f)) {
                                LibraryRowTitle(site)
                                // One account: its username once unlocked; several: how many.
                                val sub = when {
                                    single == null -> "${logins.size} accounts"
                                    unlocked == null -> if (single.local) "Saved on this phone" else "From your Mozilla account"
                                    else -> unlocked[single.guid]?.username?.ifEmpty { "No username" } ?: "…"
                                }
                                LibraryRowSubtitle(sub)
                            }
                            if (single?.local == true && unlocked != null) {
                                Spacer(Modifier.width(DownloadTokens.ActionGap))
                                Text("This phone", color = palette.TextSecondary, fontSize = SettingsTokens.RowSubtitleSize)
                            }
                        }
                    }
                }
            }
            // Room for the + button over the last row.
            Spacer(Modifier.height(PasswordTokens.AddButton + PasswordTokens.AddMargin))
        }
        if (ui.summaries.isNotEmpty()) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(PasswordTokens.AddMargin)
                    .size(PasswordTokens.AddButton)
                    .clip(CircleShape)
                    .background(palette.Primary)
                    .clickable(enabled = on, onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(KoanIcons.Plus, "Add a password", tint = palette.OnPrimary, modifier = Modifier.size(SettingsTokens.RowGlyph))
            }
        }
    }
}

/** Passwords › +: a login typed in by hand (site, username, password), saved encrypted on the phone. */
@Composable
internal fun AddPasswordPage(ui: PasswordsUi, on: Boolean, onBack: () -> Unit) {
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
 * with one account also has the big Copy password button.
 */
@Composable
internal fun PasswordSitePage(ui: PasswordsUi, site: String, on: Boolean, onBack: () -> Unit) {
    SettingsPageFrame(site, on, onBack) {
        val accounts = ui.summaries.filter { siteOf(it) == site }
        val unlocked = ui.unlocked
        if (unlocked == null) {
            PrimaryButton("Unlock", onClick = { if (on) ui.onUnlock(site, accounts.map { it.guid }) }, modifier = Modifier.fillMaxWidth())
        } else {
            accounts.forEachIndexed { i, s ->
                // Keyed: the accounts open one by one, each one's rows change as it does.
                key(s.guid) {
                    if (i > 0) Spacer(Modifier.height(SettingsTokens.SectionTop))
                    Account(s, unlocked[s.guid], on, ui.onCopied)
                }
            }
            // One account: its password one big tap away.
            val single = accounts.singleOrNull()?.let { unlocked[it.guid] }
            if (single != null) {
                val context = LocalContext.current
                Spacer(Modifier.height(SettingsTokens.SectionTop))
                PrimaryButton("Copy password", onClick = { if (on) copy(context, "Password", single.password, sensitive = true, ui.onCopied) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** One account of a site: where it comes from, its username, its password ([login] null = still opening). */
@Composable
private fun Account(s: LoginSummary, login: Login?, on: Boolean, onCopied: (String) -> Unit) {
    val context = LocalContext.current
    var shown by remember { mutableStateOf(false) }
    SettingsRow(KoanIcons.Globe, s.origin, on, subtitle = if (s.local) "Saved on this phone" else "From your Mozilla account")
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
    }
}

/** A quiet word at a row's end ("Copy", "Reveal"); tappable on its own when [onClick] is given. */
@Composable
internal fun RowAction(label: String, onClick: (() -> Unit)? = null) {
    Text(
        label,
        color = KoanTheme.colors.TextSecondary,
        fontSize = SettingsTokens.RowValueSize,
        modifier = if (onClick == null) Modifier else Modifier.clip(CircleShape).clickable(onClick = onClick).padding(SettingsTokens.Divider * 6),
    )
}

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
