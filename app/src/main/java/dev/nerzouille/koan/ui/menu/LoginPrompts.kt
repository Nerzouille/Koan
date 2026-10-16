package dev.nerzouille.koan.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.PrimaryButton
import dev.nerzouille.koan.ui.kit.QuietButton
import dev.nerzouille.koan.ui.overview.Favicon
import dev.nerzouille.koan.ui.sheet.SheetDivider
import dev.nerzouille.koan.ui.sheet.SheetGroup
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SearchTokens
import dev.nerzouille.koan.ui.theme.SheetTokens
import mozilla.components.concept.storage.Login

/** "alex.rivera@example.com" → "alex.r••••@example.com": recognisable, not readable over a shoulder. */
fun masked(username: String): String {
    if (username.isEmpty()) return "No username"
    val at = username.indexOf('@')
    val name = if (at > 0) username.substring(0, at) else username
    val domain = if (at > 0) username.substring(at) else ""
    val keep = (name.length / 2).coerceIn(1, 6)
    return name.take(keep) + "••••" + domain
}

/**
 * A login field has focus and the site has saved logins (design 7f): one chip per account above
 * the keyboard, a key at the start opens the full list (7e). Tap a chip = fill with it.
 */
@Composable
fun LoginChips(logins: List<Login>, onPick: (Login) -> Unit, onMore: () -> Unit, modifier: Modifier = Modifier) {
    val palette = KoanTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = SearchTokens.Margin, vertical = SearchTokens.ChipGap),
        horizontalArrangement = Arrangement.spacedBy(SearchTokens.ChipGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(SearchTokens.ChipHeight)
                .clip(CircleShape)
                .background(palette.Card)
                .clickable(onClick = onMore),
            contentAlignment = Alignment.Center,
        ) { Icon(KoanIcons.Key, null, tint = palette.TextPrimary, modifier = Modifier.size(SearchTokens.ChipIcon + SearchTokens.ChipGap / 2)) }
        logins.forEach { login ->
            Row(
                Modifier
                    .height(SearchTokens.ChipHeight)
                    .clip(CircleShape)
                    .background(palette.Card)
                    .clickable { onPick(login) }
                    .padding(horizontal = SearchTokens.RowPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Initial(login.username)
                Spacer(Modifier.width(SearchTokens.ChipGap))
                Text(masked(login.username), color = palette.TextPrimary, fontSize = SearchTokens.ChipTextSize, maxLines = 1)
            }
        }
    }
}

/** "Sign in to example.com with" (design 7e): the site's accounts, pick one, Use or Not now. */
@Composable
fun LoginSheet(origin: String, logins: List<Login>, interactive: Boolean, onUse: (Login) -> Unit, onNotNow: () -> Unit) {
    val palette = KoanTheme.colors
    var chosen by remember(logins) { mutableStateOf(logins.maxByOrNull { it.timeLastUsed }) }
    Column(verticalArrangement = Arrangement.spacedBy(SheetTokens.Gap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Favicon(origin, SheetTokens.RowIconBox + SheetTokens.Gap)
            Spacer(Modifier.width(SheetTokens.RowPadding))
            Column {
                Text("Sign in to ${displayHost(origin)} with", color = palette.TextPrimary, fontSize = SheetTokens.CardTitleSize, fontWeight = FontWeight.SemiBold)
                Text("Koan passwords", color = palette.TextSecondary, fontSize = SheetTokens.RowSubtitleSize)
            }
        }
        SheetGroup {
            logins.forEachIndexed { i, login ->
                if (i > 0) SheetDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(SheetTokens.RowTallHeight)
                        .then(if (interactive) Modifier.clickable { chosen = login } else Modifier)
                        .padding(horizontal = SheetTokens.RowPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Initial(login.username)
                    Spacer(Modifier.width(SheetTokens.RowPadding))
                    Text(masked(login.username), color = palette.TextPrimary, fontSize = SheetTokens.RowTitleSize, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Radio(selected = chosen == login)
                }
            }
        }
        PrimaryButton("Use", onClick = { if (interactive) chosen?.let(onUse) }, modifier = Modifier.fillMaxWidth())
        QuietButton("Not now", onClick = { if (interactive) onNotNow() }, modifier = Modifier.fillMaxWidth())
    }
}

/** A page saved a new login: keep it on the phone (never sent to the account in M5). */
@Composable
fun SaveLoginCard(origin: String, username: String, update: Boolean, interactive: Boolean, onSave: () -> Unit, onNotNow: () -> Unit) {
    val palette = KoanTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(SheetTokens.Gap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Favicon(origin, SheetTokens.RowIconBox + SheetTokens.Gap)
            Spacer(Modifier.width(SheetTokens.RowPadding))
            Column {
                Text(
                    if (update) "Update the password for ${displayHost(origin)}?" else "Save the password for ${displayHost(origin)}?",
                    color = palette.TextPrimary, fontSize = SheetTokens.CardTitleSize, fontWeight = FontWeight.SemiBold,
                )
                Text(masked(username), color = palette.TextSecondary, fontSize = SheetTokens.RowSubtitleSize)
            }
        }
        Text(
            "Kept encrypted on this phone only; it isn't added to your Mozilla account.",
            color = palette.TextSecondary, fontSize = SheetTokens.CardTextSize,
        )
        PrimaryButton("Save in Koan", onClick = { if (interactive) onSave() }, modifier = Modifier.fillMaxWidth())
        QuietButton("Not now", onClick = { if (interactive) onNotNow() }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Initial(username: String) {
    val palette = KoanTheme.colors
    Box(
        Modifier
            .size(SearchTokens.Favicon)
            .clip(CircleShape)
            .background(palette.GroupedRow),
        contentAlignment = Alignment.Center,
    ) {
        Text(username.firstOrNull()?.uppercase() ?: "?", color = palette.TextPrimary, fontSize = SearchTokens.UrlSize, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Radio(selected: Boolean) {
    val palette = KoanTheme.colors
    Box(
        Modifier
            .size(SearchTokens.Favicon - SearchTokens.ChipGap / 2)
            .clip(CircleShape)
            .background(if (selected) palette.Primary else palette.GroupedRow),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(KoanIcons.Check, null, tint = palette.OnPrimary, modifier = Modifier.size(SearchTokens.ChipIcon))
    }
}
