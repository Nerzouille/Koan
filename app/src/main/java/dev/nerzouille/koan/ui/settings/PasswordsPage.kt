package dev.nerzouille.koan.ui.settings

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
 * opens the login [then] if given.
 */
class PasswordsUi(
    val summaries: List<LoginSummary>,
    val unlocked: Map<String, Login>?,
    val onUnlock: (then: String?) -> Unit,
)

/**
 * Settings › Passwords: the sites, searchable; usernames and passwords only once unlocked with the
 * fingerprint or the device PIN. Tapping a site while locked unlocks and opens it.
 */
@Composable
internal fun PasswordsPage(ui: PasswordsUi, on: Boolean, onBack: () -> Unit, onOpen: (String) -> Unit) {
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
            PrimaryButton("Unlock", onClick = { if (on) ui.onUnlock(null) }, modifier = Modifier.fillMaxWidth())
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
        shown.forEachIndexed { i, s ->
            if (i > 0) SettingsDivider()
            val login = unlocked?.get(s.guid)
            SettingsRow(
                KoanIcons.Key, displayHost(s.origin).ifEmpty { s.origin }, on,
                subtitle = when {
                    unlocked == null -> null
                    login == null -> "…"
                    else -> login.username.ifEmpty { "No username" }
                },
                value = if (s.local) "This phone" else null,
                chevron = true,
                // Locked: the fingerprint first, then straight to this login.
                onClick = { if (unlocked == null) ui.onUnlock(s.guid) else onOpen(s.guid) },
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

/** One login: where, who, and the password hidden until Reveal; Copy marks it sensitive. */
@Composable
internal fun PasswordDetailPage(login: Login?, on: Boolean, onBack: () -> Unit) {
    // Opened right after the fingerprint: the login may arrive a moment after the page.
    val context = LocalContext.current
    var shown by remember(login?.guid) { mutableStateOf(false) }
    SettingsPageFrame(login?.let { displayHost(it.origin) } ?: "Password", on, onBack) {
        if (login == null) return@SettingsPageFrame
        SettingsRow(KoanIcons.Globe, login.origin, on, subtitle = if (login.local) "Saved on this phone" else "From your Mozilla account")
        SettingsDivider()
        SettingsRow(null, login.username.ifEmpty { "No username" }, on, subtitle = "Username", onClick = if (login.username.isEmpty()) null else { { copy(context, "Username", login.username, sensitive = false) } }) {
            if (login.username.isNotEmpty()) CopyLabel()
        }
        SettingsDivider()
        SettingsRow(null, if (shown) login.password else "••••••••••", on, subtitle = "Password", onClick = { shown = !shown }) {
            Text(if (shown) "Hide" else "Reveal", color = KoanTheme.colors.TextSecondary, fontSize = SettingsTokens.RowValueSize)
        }
        Spacer(Modifier.height(SettingsTokens.SectionTop))
        Row(horizontalArrangement = Arrangement.spacedBy(SettingsTokens.RowInset)) {
            PrimaryButton("Copy password", onClick = { if (on) copy(context, "Password", login.password, sensitive = true) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CopyLabel() {
    Text("Copy", color = KoanTheme.colors.TextSecondary, fontSize = SettingsTokens.RowValueSize)
}

/** Copies [value]; a password is flagged sensitive so Android hides it in clipboard previews. */
private fun copy(context: Context, label: String, value: String, sensitive: Boolean) {
    val clip = ClipData.newPlainText(label, value)
    if (sensitive) clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
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
