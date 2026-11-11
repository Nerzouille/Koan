package dev.nerzouille.koan.ui.privacy

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.KoanType
import dev.nerzouille.koan.ui.theme.PrivateTokens

/** The dock's texts during the flip: they dip out and back in where they switch (alpha only). */
fun Modifier.mist(flip: PrivateFlip?): Modifier = if (flip == null) this else graphicsLayer { alpha = flip.textAlpha() }

/**
 * The Private toggle at the end of the overview header (§4.2): a 56×32 track, a white knob with
 * the mask. Its colours and the knob follow the flip, in the draw phase. Taps are ignored while a
 * flip runs.
 */
@Composable
fun PrivateToggle(flip: PrivateFlip, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val dark = KoanTheme.colors.isDark
    val view = LocalView.current
    val on = flip.target
    Box(
        modifier
            .size(PrivateTokens.ToggleTouchWidth, PrivateTokens.ToggleTouchHeight)
            .semantics {
                role = Role.Switch
                contentDescription = "Private browsing"
                stateDescription = if (on) "On" else "Off"
            }
            .clickable(remember { MutableInteractionSource() }, indication = null) {
                if (flip.running) return@clickable
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onToggle()
            },
        contentAlignment = Alignment.Center,
    ) {
        val offTrack = if (dark) PrivateTokens.TrackOffDark else PrivateTokens.TrackOffLight
        val onTrack = if (dark) PrivateTokens.TrackOnDark else PrivateTokens.TrackOnLight
        val offMask = if (dark) PrivateTokens.MaskOffDark else PrivateTokens.MaskOffLight
        val onMask = if (dark) PrivateTokens.MaskOnDark else PrivateTokens.MaskOnLight
        Box(
            Modifier
                .size(PrivateTokens.TrackWidth, PrivateTokens.TrackHeight)
                .drawBehind {
                    val k = flip.knob()
                    drawRoundRect(lerp(offTrack, onTrack, k), cornerRadius = CornerRadius(size.height / 2f))
                    val r = PrivateTokens.Knob.toPx() / 2f
                    val x = (PrivateTokens.KnobOff + (PrivateTokens.KnobOn - PrivateTokens.KnobOff) * k).toPx() + r
                    // Shadow 0 / 1 / 3dp, black 25%.
                    drawCircle(Color.Black.copy(alpha = 0.12f), r + 1.5.dp.toPx(), Offset(x, size.height / 2f + 1.dp.toPx()))
                    drawCircle(Color.Black.copy(alpha = 0.13f), r + 0.5.dp.toPx(), Offset(x, size.height / 2f + 1.dp.toPx()))
                    drawCircle(Color.White, r, Offset(x, size.height / 2f))
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                KoanIcons.Private,
                null,
                tint = if (on) onMask else offMask,
                modifier = Modifier
                    .graphicsLayer {
                        val k = flip.knob()
                        val knobX = (PrivateTokens.KnobOff + (PrivateTokens.KnobOn - PrivateTokens.KnobOff) * k).toPx()
                        translationX = knobX + (PrivateTokens.Knob - PrivateTokens.KnobGlyph).toPx() / 2f
                    }
                    .size(PrivateTokens.KnobGlyph),
            )
        }
    }
}

/** "Not saved" next to the private header's title (§4.3). */
@Composable
fun NotSavedChip() {
    val palette = KoanTheme.colors
    Box(
        Modifier
            .height(PrivateTokens.ChipHeight)
            .clip(RoundedCornerShape(PrivateTokens.ChipHeight / 2))
            .background(palette.PrivatePill)
            .padding(horizontal = PrivateTokens.ChipPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text("Not saved", color = palette.PrivateInk, fontSize = PrivateTokens.ChipText, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** The private header's title part: "Private" and the chip (no icon tile: the toggle has the mask). */
@Composable
fun PrivateTitle(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Private",
            color = KoanTheme.colors.TextPrimary,
            style = KoanType.title(dev.nerzouille.koan.ui.theme.OverviewTokens.TitleSize),
            maxLines = 1,
        )
        Spacer(Modifier.width(dev.nerzouille.koan.ui.theme.OverviewTokens.TitleGap))
        NotSavedChip()
    }
}

/** The dock's first row in Private (§4.4): one centred "Private" pill, a label with no action for now. */
@Composable
fun PrivateDockPill(modifier: Modifier = Modifier) {
    val palette = KoanTheme.colors
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .height(PrivateTokens.PillHeight)
                .clip(RoundedCornerShape(PrivateTokens.PillHeight / 2))
                .background(palette.PrivatePill)
                .padding(start = PrivateTokens.PillPaddingStart, end = PrivateTokens.PillPaddingEnd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(KoanIcons.Private, null, tint = palette.PrivateInk, modifier = Modifier.size(PrivateTokens.PillGlyph))
            Spacer(Modifier.width(PrivateTokens.PillGap))
            Text("Private", color = palette.PrivateInk, fontSize = PrivateTokens.PillText, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

/** The search sheet's first item in Private (§4.6). */
@Composable
fun PrivateSearchNotice(modifier: Modifier = Modifier) {
    val palette = KoanTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PrivateTokens.NoticeRadius))
            .background(palette.Card)
            .background(palette.PrivatePill)
            .padding(vertical = PrivateTokens.NoticePaddingV, horizontal = PrivateTokens.NoticePaddingH),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(KoanIcons.Private, null, tint = palette.PrivateInk, modifier = Modifier.size(PrivateTokens.NoticeGlyph))
        Spacer(Modifier.width(PrivateTokens.NoticeGap))
        Text(
            "Private search: only your bookmarks are suggested, and nothing you type is kept.",
            color = palette.PrivateInk,
            fontSize = PrivateTokens.NoticeText,
            lineHeight = PrivateTokens.NoticeLine,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** What the empty state says private mode does (§4.7, docs/FEATURES.md "Private browsing"). */
val PRIVATE_FACTS = listOf(
    "History, cookies and site data are deleted when you close the last private tab",
    "Private tabs stay open, but sites sign you out when Koan restarts",
    "Downloads and bookmarks you save are kept",
    "Sites, your network and your employer can still see what you visit",
)

/** No private tabs (§4.7): the mask, a title, what Private does and doesn't, and "New private tab". */
@Composable
fun PrivateEmptyState(onNewTab: () -> Unit, modifier: Modifier = Modifier) {
    val palette = KoanTheme.colors
    Column(modifier.fillMaxWidth().padding(horizontal = PrivateTokens.EmptySide), horizontalAlignment = Alignment.CenterHorizontally) {
        PrivateIconTile()
        Spacer(Modifier.height(18.dp))
        Text("No private tabs", color = palette.TextPrimary, fontSize = PrivateTokens.EmptyTitle, lineHeight = PrivateTokens.EmptyTitleLine, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("Browse without leaving a trace on this phone.", color = palette.TextSecondary, fontSize = PrivateTokens.EmptyBody, lineHeight = PrivateTokens.EmptyBodyLine)
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(PrivateTokens.EmptyFactGap)) {
            for (fact in PRIVATE_FACTS) {
                Row {
                    Box(
                        Modifier
                            .padding(top = PrivateTokens.EmptyFactDotTop)
                            .size(PrivateTokens.EmptyFactDot)
                            .clip(CircleShape)
                            .background(palette.PrivateAccent),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(fact, color = palette.TextSecondary, fontSize = PrivateTokens.EmptyBody, lineHeight = PrivateTokens.EmptyFactLine)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier
                .height(PrivateTokens.EmptyButtonHeight)
                .clip(RoundedCornerShape(PrivateTokens.EmptyButtonHeight / 2))
                .background(palette.Primary)
                .clickable(onClick = onNewTab)
                .padding(horizontal = PrivateTokens.EmptyButtonPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(KoanIcons.Plus, null, tint = palette.OnPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("New private tab", color = palette.OnPrimary, fontSize = dev.nerzouille.koan.ui.theme.ButtonTokens.TextSize, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Over everything while the private tabs are locked (after 1 min away or the screen off): the
 * private palette's base and the mask, nothing of the tabs. [onUnlock] asks for the fingerprint;
 * [onLeave] goes back to normal tabs.
 */
@Composable
fun PrivateLockCover(onUnlock: () -> Unit, onLeave: () -> Unit) {
    val palette = KoanTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(palette.Base)
            .clickable(remember { MutableInteractionSource() }, indication = null) { },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = PrivateTokens.EmptySide)) {
            PrivateIconTile()
            Spacer(Modifier.height(18.dp))
            Text("Private tabs are locked", color = palette.TextPrimary, fontSize = PrivateTokens.EmptyTitle, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(24.dp))
            Row(
                Modifier
                    .height(PrivateTokens.EmptyButtonHeight)
                    .clip(RoundedCornerShape(PrivateTokens.EmptyButtonHeight / 2))
                    .background(palette.Primary)
                    .clickable(onClick = onUnlock)
                    .padding(horizontal = PrivateTokens.EmptyButtonPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Unlock", color = palette.OnPrimary, fontSize = dev.nerzouille.koan.ui.theme.ButtonTokens.TextSize, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .height(PrivateTokens.EmptyButtonHeight)
                    .clip(RoundedCornerShape(PrivateTokens.EmptyButtonHeight / 2))
                    .clickable(onClick = onLeave)
                    .padding(horizontal = PrivateTokens.EmptyButtonPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Back to normal tabs", color = palette.TextPrimary, fontSize = dev.nerzouille.koan.ui.theme.ButtonTokens.TextSize)
            }
        }
    }
}

/** Private's icon on its pill-coloured tile (empty state, lock cover). */
@Composable
private fun PrivateIconTile() {
    val palette = KoanTheme.colors
    Box(
        Modifier.size(PrivateTokens.EmptyIconBox).clip(RoundedCornerShape(PrivateTokens.EmptyIconRadius)).background(palette.PrivatePill),
        contentAlignment = Alignment.Center,
    ) { Icon(KoanIcons.Private, null, tint = palette.PrivateInk, modifier = Modifier.size(PrivateTokens.EmptyIcon)) }
}
