package com.slowdown.app.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import com.slowdown.app.R
import com.slowdown.app.ui.home.HeroMode
import com.slowdown.app.ui.theme.SlowText

/** Name shown for a speed in the UI. Settings keep the raw preset labels. */
fun presetName(kbps: Int, settingsStyle: Boolean): String = when (kbps) {
    56 -> "Dial-up"
    128 -> if (settingsStyle) "2G GSM" else "2G Vintage"
    384 -> if (settingsStyle) "3G Basic" else "3G Patient"
    1024 -> if (settingsStyle) "Slow 4G" else "Lite Stream"
    else -> Format.speed(kbps)
}

/** A metric like "1h 5m": digits in the mono display style, unit letters smaller. */
fun metricText(value: String, unitSize: Int = 14): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < value.length) {
        val letter = value[i].isLetter()
        var j = i
        while (j < value.length && value[j].isLetter() == letter) j++
        val part = value.substring(i, j)
        if (letter) withStyle(SpanStyle(fontSize = unitSize.sp, fontWeight = FontWeight.Normal)) { append(part) } else append(part)
        i = j
    }
}

@Composable
fun StatusDot(color: Color, pulse: Boolean = false, size: Int = 8) {
    val reduced = rememberReducedMotion()
    val a = if (pulse && !reduced) {
        val t = rememberInfiniteTransition(label = "dot")
        val v by t.animateFloat(1f, 0.35f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "dotA")
        v
    } else 1f
    Box(Modifier.size(size.dp).alpha(a).background(color, CircleShape))
}

/** Wordmark, status chip. Sits under the status bar on every tab screen. */
@Composable
fun AppHeader(mode: HeroMode, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(64.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.app_name), style = SlowText.wordmark, color = cs.primary)
        val (label, dot) = when (mode) {
            HeroMode.SLOWED -> R.string.status_slowed to cs.tertiary
            HeroMode.WATCHING -> R.string.status_watching to cs.primary
            HeroMode.OFF -> R.string.status_off to cs.outline
        }
        Row(
            Modifier.background(cs.secondaryContainer.copy(alpha = 0.7f), CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StatusDot(dot, pulse = mode != HeroMode.OFF, size = 6)
            Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = cs.onSecondaryContainer)
        }
    }
}

/** App mark: a ring with a slow wave inside. Same drawing as the launcher icon. */
@Composable
fun BrandMark(size: Int, ring: Color, wave: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size.dp)) {
        val s = this.size.minDimension
        val w = s * 0.07f
        drawCircle(ring, radius = s * 0.40f, style = Stroke(w))
        val p = Path().apply {
            val x0 = s * 0.28f
            val x1 = s * 0.72f
            val y = s * 0.5f
            val a = s * 0.09f
            moveTo(x0, y)
            cubicTo(x0 + (x1 - x0) * 0.17f, y - a * 2f, x0 + (x1 - x0) * 0.33f, y - a * 2f, x0 + (x1 - x0) * 0.5f, y)
            cubicTo(x0 + (x1 - x0) * 0.67f, y + a * 2f, x0 + (x1 - x0) * 0.83f, y + a * 2f, x1, y)
        }
        drawPath(p, wave, style = Stroke(w, cap = StrokeCap.Round))
    }
}

/** Switch with a check in the thumb, as in the design. */
@Composable
fun SoftSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        thumbContent = {
            if (checked) Icon(Icons.Filled.Check, null, Modifier.size(SwitchDefaults.IconSize), tint = cs.primary)
        },
        colors = SwitchDefaults.colors(
            checkedTrackColor = cs.primary,
            checkedThumbColor = cs.surfaceContainerLowest,
            checkedBorderColor = Color.Transparent,
            uncheckedTrackColor = cs.surfaceVariant,
            uncheckedThumbColor = cs.outline,
            uncheckedBorderColor = cs.outline,
        ),
    )
}

/** Draws two soft circles, the outer one breathing on a 4 s in, 4 s out cycle. */
@Composable
fun BreathingCircles(color: Color, modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    val k = if (reduced) 1f else {
        val t = rememberInfiniteTransition(label = "breath")
        val v by t.animateFloat(0.9f, 1.15f, infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "breathK")
        v
    }
    Canvas(modifier) {
        val c = Offset(size.width, 0f)
        drawCircle(color.copy(alpha = 0.40f), radius = 96.dp.toPx() * k, center = c)
        drawCircle(color.copy(alpha = 0.50f), radius = 70.dp.toPx(), center = c)
    }
}
