package dev.nerzouille.koan.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nerzouille.koan.ui.overview.LabTokens
import dev.nerzouille.koan.ui.overview.StackLab
import dev.nerzouille.koan.ui.overview.StackStyle
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SettingsTokens

/** Settings › Debug › Stack style (dev builds only): the look, its sliders, the overview panel. */
@Composable
fun StackStylePage(interactive: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { StackLab.load(context) }
    SettingsPageFrame("Stack style", interactive, onBack) {
        SettingsSection("Look")
        Box(Modifier.padding(horizontal = SettingsTokens.RowInset)) { StackLabControls(interactive) }
        SettingsSection("While you try it")
        SettingsToggleRow(
            null, "Panel on the overview", interactive, checked = StackLab.panel, onChange = StackLab::showPanel,
            subtitle = "The same controls over the stack, to tune it live",
        )
    }
}

/** The look, its strength and (Tilt) its angle. */
@Composable
fun StackLabControls(interactive: Boolean) {
    Column {
        Spacer(Modifier.height(8.dp))
        Segmented(StackStyle.entries.map { it to it.short }, StackLab.style, interactive, StackLab::select)
        LabSlider(
            label = if (StackLab.style == StackStyle.Tilt) "Perspective" else "Strength",
            value = StackLab.strength,
            range = LabTokens.MIN_STRENGTH..LabTokens.MAX_STRENGTH,
            text = "×%.2f".format(StackLab.strength),
            enabled = interactive,
            onChange = StackLab::changeStrength,
        )
        if (StackLab.style == StackStyle.Tilt) {
            LabSlider(
                label = "Angle per card",
                value = StackLab.angle,
                range = 0f..LabTokens.TILT_ANGLE_MAX,
                text = "%.1f°".format(StackLab.angle),
                enabled = interactive,
                onChange = StackLab::changeAngle,
            )
        }
    }
}

@Composable
private fun LabSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    text: String,
    enabled: Boolean,
    onChange: (Float) -> Unit,
) {
    val palette = KoanTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = palette.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text, color = palette.TextSecondary, fontSize = 14.sp)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, enabled = enabled, onValueChangeFinished = StackLab::save)
    }
}

/** Over the overview, dev builds only, when turned on in Settings › Debug › Stack style. */
@Composable
fun StackLabPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { StackLab.load(context) }
    if (!StackLab.available || !StackLab.panel) return
    val palette = KoanTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
            .shadow(12.dp, shape)
            .clip(shape)
            .background(palette.Card.copy(alpha = 0.94f))
            // Touches between the controls stay in the panel, never reach the stack.
            .clickable(remember { MutableInteractionSource() }, indication = null) {}
            .padding(start = 14.dp, end = 14.dp, bottom = 4.dp),
    ) {
        StackLabControls(interactive = true)
    }
}
