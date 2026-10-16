package dev.nerzouille.koan.ui.settings

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
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

/** What the Passwords pages need. [unlocked]: the logins opened after the fingerprint or PIN, else null. */
class PasswordsUi(
    val summaries: List<LoginSummary>,
    val unlocked: List<Login>?,
    val onUnlock: () -> Unit,
)

/**
 * Settings › Passwords: the sites only, until the owner unlocks with the fingerprint or the device
 * PIN; then usernames show and a login opens to reveal or copy its password.
 */
@Composable
internal fun PasswordsPage(ui: PasswordsUi, on: Boolean, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val palette = KoanTheme.colors
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
        if (unlocked == null) {
            Text(
                "${ui.summaries.size} saved. Unlock to see usernames and passwords.",
                color = palette.TextSecondary,
                fontSize = SettingsTokens.RowValueSize,
            )
            Spacer(Modifier.height(SettingsTokens.SectionTop / 2))
            PrimaryButton("Unlock", onClick = { if (on) ui.onUnlock() }, modifier = Modifier.fillMaxWidth())
            SettingsSection("Sites")
            ui.summaries.forEachIndexed { i, s ->
                if (i > 0) SettingsDivider()
                SettingsRow(KoanIcons.Key, displayHost(s.origin).ifEmpty { s.origin }, on, value = if (s.local) "This phone" else null)
            }
        } else {
            unlocked.forEachIndexed { i, login ->
                if (i > 0) SettingsDivider()
                SettingsRow(
                    KoanIcons.Key, displayHost(login.origin).ifEmpty { login.origin }, on,
                    subtitle = login.username.ifEmpty { "No username" },
                    chevron = true,
                    onClick = { onOpen(login.guid) },
                )
            }
        }
    }
}

/** One login: where, who, and the password hidden until Reveal; Copy marks it sensitive. */
@Composable
internal fun PasswordDetailPage(login: Login?, on: Boolean, onBack: () -> Unit) {
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
