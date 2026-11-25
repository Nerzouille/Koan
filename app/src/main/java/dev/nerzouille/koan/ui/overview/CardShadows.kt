package dev.nerzouille.koan.ui.overview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import dev.nerzouille.koan.ui.theme.CardTokens
import dev.nerzouille.koan.ui.theme.KoanTheme

/**
 * A stack card's shadows (one ambient light, direction 3): a layer of its own, drawn just under
 * the card with the same size and transform ([modifier]) and not clipped, so it moves with the
 * card through every position. The front card casts the front pair (a long soft key and a short
 * ambient), every other card a small contact shadow; [front] (0…1, read in layers only) hands one
 * over to the other. Each shadow is fixed, so its blurred image is made once and shared by every
 * card of the same size; only layer alphas change.
 */
@Composable
fun CardShadows(front: () -> Float, modifier: Modifier) {
    val dark = KoanTheme.colors.isDark
    val shape = remember { RoundedCornerShape(CardTokens.Radius) }
    val key = remember(dark) { (if (dark) CardTokens.FrontKeyDark else CardTokens.FrontKeyLight).shadow }
    val ambient = remember(dark) { (if (dark) CardTokens.FrontAmbientDark else CardTokens.FrontAmbientLight).shadow }
    val back = remember(dark) { (if (dark) CardTokens.BackDark else CardTokens.BackLight).shadow }
    // Only the front card and the one taking over compose the front pair.
    val casting by remember { derivedStateOf { front() > 0f } }
    Box(modifier) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = 1f - front()
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
                .dropShadow(shape, back),
        )
        if (casting) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = front()
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    }
                    .dropShadow(shape, key)
                    .dropShadow(shape, ambient),
            )
        }
    }
}
