package dev.nerzouille.koan.ui.settings

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dev.nerzouille.koan.data.Login
import dev.nerzouille.koan.data.LoginSummary
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.PrimaryButton
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
)

/**
 * Settings › Passwords: one row per site, searchable. Tapping a site asks for the fingerprint or
 * the device PIN (once per visit), then opens its page with every account on it.
 */
@Composable
internal fun PasswordsPage(ui: PasswordsUi, on: Boolean, onBack: () -> Unit, onOpenSite: (String) -> Unit) {
    val palette = KoanTheme.colors
    var query by remember { mutableStateOf("") }
    SettingsPageFrame("Passwords", on, onBack) {
        if (ui.summaries.isEmpty()) {
            Text(
                "Passwords saved in your Mozilla account show up here after the next sync.",
                color = palette.TextSecondary,
                fontSize = SettingsTokens.RowValueSize,
                modifier = Modifier.padding(top = SettingsTokens.SectionTop),
            )
            return@SettingsPageFrame
        }
        val unlocked = ui.unlocked
        SearchBox(query, "Search passwords") { query = it }
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
        shown.groupBy(::siteOf).entries.forEachIndexed { i, (site, logins) ->
            if (i > 0) SettingsDivider()
            val single = logins.singleOrNull()
            SettingsRow(
                KoanIcons.Key, site, on,
                // One account: its username once unlocked; several: how many.
                subtitle = single?.let { s ->
                    when {
                        unlocked == null -> null
                        else -> unlocked[s.guid]?.username?.ifEmpty { "No username" } ?: "…"
                    }
                },
                value = if (single == null) "${logins.size} accounts" else if (single.local) "This phone" else null,
                chevron = true,
                // Locked: the fingerprint first, then straight to the site's accounts.
                onClick = { if (unlocked == null) ui.onUnlock(site, logins.map { it.guid }) else onOpenSite(site) },
            )
        }
    }
}

/** A search field in a settings page. */
@Composable
internal fun SearchBox(value: String, hint: String, onChange: (String) -> Unit) {
    val palette = KoanTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(SettingsTokens.SegmentHeight + SettingsTokens.RowInset / 2)
            .clip(CircleShape)
            .background(palette.Glass)
            .padding(horizontal = SettingsTokens.RowInset + SettingsTokens.Divider * 4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(KoanIcons.Search, null, tint = palette.FieldHint, modifier = Modifier.size(SettingsTokens.RowGlyph))
        Spacer(Modifier.width(SettingsTokens.RowInset))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(hint, color = palette.FieldHint, fontSize = SettingsTokens.RowTitleSize)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = palette.TextPrimary, fontSize = SettingsTokens.RowTitleSize),
                cursorBrush = SolidColor(palette.TextPrimary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Box(Modifier.size(SettingsTokens.RowGlyph + SettingsTokens.RowInset).clip(CircleShape).clickable { onChange("") }, contentAlignment = Alignment.Center) {
                Icon(KoanIcons.Close, null, tint = palette.FieldHint, modifier = Modifier.size(SettingsTokens.RowGlyph * 0.8f))
            }
        }
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
