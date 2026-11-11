package dev.nerzouille.koan.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * Something that shows and hides with a 0…1 animation (working rule 9: the animated value is never
 * read in a composable body).
 *
 * - [progress]: read it in `graphicsLayer`, draw or layout lambdas only.
 * - [composed]: whether it belongs in the composition (open, or still on its way out). It changes
 *   only at the two ends of the animation, so reading it recomposes twice, not every frame.
 */
@Stable
class Presence internal constructor(private val anim: Animatable<Float, AnimationVector1D>, isOpen: () -> Boolean) {
    val progress: Float get() = anim.value

    private val composedState = derivedStateOf { isOpen() || anim.value > 0f }
    val composed: Boolean get() = composedState.value
}

/**
 * A [Presence] that animates itself towards [open]: [enter] on the way in, [exit] on the way out.
 * [snapIn]: shows at once (no animation in), fades out with [exit].
 */
@Composable
fun rememberPresence(
    open: Boolean,
    enter: AnimationSpec<Float>,
    exit: AnimationSpec<Float> = enter,
    snapIn: Boolean = false,
): Presence {
    val anim = remember { Animatable(0f) }
    val openState = rememberUpdatedState(open)
    LaunchedEffect(open) {
        when {
            !open -> anim.animateTo(0f, exit)
            snapIn -> anim.snapTo(1f)
            else -> anim.animateTo(1f, enter)
        }
    }
    return remember(anim) { Presence(anim) { openState.value } }
}

/**
 * A [Presence] over an animation the caller drives itself (e.g. a toast's timed show and hide).
 * [isOpen] reads snapshot state; it is called inside a derived state, never in composition.
 */
@Composable
fun rememberPresence(anim: Animatable<Float, AnimationVector1D>, isOpen: () -> Boolean): Presence {
    val open = rememberUpdatedState(isOpen)
    return remember(anim) { Presence(anim) { open.value() } }
}
