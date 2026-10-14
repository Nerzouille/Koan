package dev.nerzouille.koan.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import dev.nerzouille.koan.ui.theme.CardTokens
import dev.nerzouille.koan.ui.theme.FirstRunTokens
import dev.nerzouille.koan.ui.theme.KoanColors

/** Cards the stack shows while the very first sync runs, so the overview is never blank. */
val SkeletonCards: List<CardModel> = List(FirstRunTokens.SKELETON_CARDS) { i ->
    CardModel(id = "skeleton:$i", title = "", url = "", active = false, skeleton = true)
}

/**
 * First run: one calm card in the overview that leads to the Mozilla sign-in. [compact] = the
 * phone-only space already has tabs, so it sits above them as a slim row instead.
 */
@Composable
fun ConnectCard(onConnect: () -> Unit, compact: Boolean, modifier: Modifier = Modifier) {
    if (compact) {
        Box(
            modifier
                .padding(horizontal = FirstRunTokens.SideMargin)
                .fillMaxWidth()
                .clip(CircleShape)
                .background(KoanColors.Glass)
                .clickable(onClick = onConnect)
                .padding(vertical = FirstRunTokens.CompactPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text("Connect your Zen Spaces", color = KoanColors.TextPrimary, fontSize = FirstRunTokens.ButtonSize, fontWeight = FontWeight.Medium)
        }
        return
    }
    Column(
        modifier
            .padding(horizontal = FirstRunTokens.SideMargin)
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardTokens.Radius))
            .background(KoanColors.CardPlaceholder)
            .padding(FirstRunTokens.Padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FirstRunTokens.Gap),
    ) {
        Text(
            "Connect your Zen Spaces",
            color = KoanColors.TextPrimary,
            fontSize = FirstRunTokens.TitleSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            "Sign in with the Mozilla account Zen syncs to. Your spaces, essentials, pinned and open tabs " +
                "show up here, read-only.",
            color = KoanColors.TextSecondary,
            fontSize = FirstRunTokens.BodySize,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(FirstRunTokens.Gap))
        Box(
            Modifier
                .clip(CircleShape)
                .background(KoanColors.NewTabButton)
                .clickable(onClick = onConnect)
                .padding(horizontal = FirstRunTokens.ButtonPaddingH, vertical = FirstRunTokens.ButtonPaddingV),
        ) {
            Text("Sign in", color = KoanColors.NewTabIcon, fontSize = FirstRunTokens.ButtonSize, fontWeight = FontWeight.Bold)
        }
    }
}
