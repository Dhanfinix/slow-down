package com.slowdown.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.height as heightMod
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slowdown.app.R
import com.slowdown.app.data.AppSettings
import com.slowdown.app.data.SPEED_PRESETS
import com.slowdown.app.ui.common.BreathingCircles
import com.slowdown.app.ui.common.Format
import com.slowdown.app.ui.common.SoftSwitch
import com.slowdown.app.ui.common.StatusDot
import com.slowdown.app.ui.common.containerViewModel
import com.slowdown.app.ui.common.metricText
import com.slowdown.app.ui.common.presetName
import com.slowdown.app.ui.common.rememberPermissionActions
import com.slowdown.app.ui.common.rememberReducedMotion
import com.slowdown.app.ui.theme.SlowText
import com.slowdown.throttle.ThrottleState
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val vm = containerViewModel { HomeViewModel(it) }
    val ui by vm.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        vm.refreshPermissions()
        onPauseOrDispose { }
    }
    val perms = rememberPermissionActions(onChanged = vm::refreshPermissions, onVpnGranted = vm::startManual)

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Hero(ui, onStop = vm::stopSlowing)
            WatchCard(
                ui = ui,
                onWatch = { on ->
                    if (on && !ui.permissions.usageAccess) perms.openUsageAccess()
                    else if (on && !ui.permissions.vpn) perms.requestVpn()
                    else vm.setWatching(on)
                },
                onGrantUsage = perms.openUsageAccess,
            )
            TodayCard(ui)
            if (ui.mode != HeroMode.SLOWED) {
                ManualCard(
                    ui = ui,
                    onStart = {
                        if (ui.permissions.vpn) vm.startManual() else perms.requestVpn()
                    },
                    onSpeed = vm::setManualSpeed,
                    onTimer = vm::setManualTimer,
                )
            }
            BreathCard()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Hero(ui: HomeUiState, onStop: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val slowed = ui.mode == HeroMode.SLOWED
    val title = stringResource(
        when (ui.mode) {
            HeroMode.SLOWED -> R.string.status_slowed
            HeroMode.WATCHING -> R.string.status_watching
            HeroMode.OFF -> R.string.status_off
        },
    )
    val subtitle = when (ui.mode) {
        HeroMode.SLOWED -> stringResource(R.string.hero_slowed_sub)
        HeroMode.WATCHING -> pluralStringResource(R.plurals.hero_watching_sub, ui.settings.thresholdMin, ui.settings.thresholdMin)
        HeroMode.OFF -> stringResource(R.string.hero_off_sub)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(cs.surfaceContainerHigh)
            .semantics(mergeDescendants = true) { contentDescription = "$title. $subtitle" },
    ) {
        BreathingCircles(cs.secondaryContainer, Modifier.matchParentSize())
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(
                    Modifier
                        .weight(1f, fill = false)
                        .background(cs.surfaceContainerLowest.copy(alpha = 0.8f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusDot(if (ui.mode == HeroMode.OFF) cs.outline else if (slowed) cs.tertiary else cs.primary, pulse = ui.mode != HeroMode.OFF)
                    Text(
                        if (slowed) stringResource(R.string.pace_badge) else title,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                        color = if (slowed) cs.tertiary else cs.onSurface,
                    )
                }
                if (slowed) {
                    Text(
                        stringResource(R.string.badge_active),
                        Modifier.padding(start = 8.dp),
                        style = SlowText.metricLabel,
                        color = cs.onSurfaceVariant,
                    )
                }
            }
            if (slowed) SlowedDetails(ui, onStop) else {
                Text(title, style = MaterialTheme.typography.displaySmall, color = cs.primary)
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
            }
        }
    }
}

private fun fontWeightSemi() = androidx.compose.ui.text.font.FontWeight.SemiBold

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SlowedDetails(ui: HomeUiState, onStop: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val reduced = rememberReducedMotion()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val t = ui.throttle
    // Big number and the line under it.
    when (t) {
        is ThrottleState.Active -> {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t.downKbps.toString(), style = SlowText.metricHero, color = cs.primary)
                Text(
                    stringResource(R.string.kbps_down),
                    Modifier.padding(bottom = 6.dp),
                    style = SlowText.metricLabel,
                    color = cs.primary,
                )
            }
            Text(
                stringResource(R.string.pace_line, Format.bytes(t.bytesDown), Format.bytes(t.bytesUp)),
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
            )
        }
        is ThrottleState.Starting -> Text(stringResource(R.string.live_starting), style = MaterialTheme.typography.headlineMedium, color = cs.primary)
        is ThrottleState.Error -> Text(t.message, style = MaterialTheme.typography.bodyLarge, color = cs.error)
        ThrottleState.Idle -> Text(stringResource(R.string.hero_slowed_sub), style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(cs.surfaceContainerLowest.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val s = ui.session
        val end = s?.endsAt
        if (s != null && end != null) {
            val total = (end - s.startedAt).coerceAtLeast(1)
            val left = (end - now).coerceIn(0, total)
            val totalMin = (total / 60_000).toInt()
            val doneMin = ((total - left) / 60_000).toInt()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Air, null, Modifier.size(18.dp), tint = cs.tertiary)
                    Text(stringResource(R.string.timer_left, Format.duration(left + 59_999)), style = MaterialTheme.typography.labelMedium, color = cs.onSurface)
                }
                Text("$doneMin / ${totalMin}m", style = SlowText.metricLabel.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = cs.tertiary)
            }
            LinearWavyProgressIndicator(
                progress = { 1f - left.toFloat() / total },
                modifier = Modifier.fillMaxWidth(),
                color = cs.tertiaryContainer,
                trackColor = cs.surfaceVariant,
                waveSpeed = if (reduced) 0.dp else 20.dp,
            )
        }
        Text(
            "“" + stringResource(R.string.pause_quote) + "”",
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
    Button(
        onClick = onStop,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = cs.primaryContainer, contentColor = cs.onPrimaryContainer),
    ) {
        Icon(Icons.Filled.LockOpen, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.stop_slowing), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun IconBubble(icon: ImageVector, bg: Color, tint: Color, box: Dp = 40.dp, iconSize: Dp = 22.dp, radius: Dp = 16.dp) {
    Box(Modifier.size(box).background(bg, RoundedCornerShape(radius)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(iconSize), tint = tint)
    }
}

@Composable
private fun WatchCard(ui: HomeUiState, onWatch: (Boolean) -> Unit, onGrantUsage: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val on = ui.settings.watching && ui.permissions.usageAccess
    Column(
        Modifier.fillMaxWidth().background(cs.surfaceContainer, RoundedCornerShape(24.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBubble(Icons.Filled.Visibility, cs.secondaryContainer, cs.primary, radius = 16.dp)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.watch_title), style = MaterialTheme.typography.titleSmall, color = cs.onSurface)
                Text(
                    if (ui.permissions.usageAccess) pluralStringResource(R.plurals.watch_active, ui.settings.watchedApps.size, ui.settings.watchedApps.size)
                    else stringResource(R.string.watch_manual_only),
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
            val watchLabel = stringResource(R.string.watch_title)
            SoftSwitch(on, onWatch, Modifier.semantics { contentDescription = watchLabel })
        }
        if (!ui.permissions.usageAccess) {
            Button(
                onClick = onGrantUsage,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cs.secondaryContainer, contentColor = cs.onSecondaryContainer),
            ) { Text(stringResource(R.string.grant_usage)) }
        }
    }
}

@Composable
private fun TodayCard(ui: HomeUiState) {
    val cs = MaterialTheme.colorScheme
    val dark = cs.background.red < 0.3f
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.today_title),
            Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.headlineSmall,
            color = cs.onSurface,
        )
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile(Icons.Filled.HourglassTop, Format.duration(ui.today.scrollMs), stringResource(R.string.stat_screen_time),
                cs.surfaceContainerLow, cs.surfaceContainer, cs.primary, cs.onSurface, cs.onSurfaceVariant, Modifier.weight(1f))
            Tile(Icons.Filled.Waves, ui.today.slowedCount.toString(), stringResource(R.string.stat_slowed),
                cs.surfaceContainerLow, cs.secondaryContainer, cs.onSecondaryContainer, cs.primary, cs.onSurfaceVariant, Modifier.weight(1f))
            Tile(Icons.Filled.Spa, Format.duration(ui.today.slowedMs), stringResource(R.string.stat_saved),
                if (dark) cs.tertiaryContainer.copy(alpha = 0.35f) else cs.tertiaryFixed.copy(alpha = 0.3f),
                if (dark) cs.tertiaryContainer else cs.tertiaryFixed,
                if (dark) cs.onTertiaryContainer else cs.onTertiaryFixed,
                cs.tertiary, cs.onSurfaceVariant, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Tile(
    icon: ImageVector,
    value: String,
    label: String,
    bg: Color,
    bubble: Color,
    bubbleTint: Color,
    valueColor: Color,
    labelColor: Color,
    modifier: Modifier,
) {
    Column(
        modifier
            .fillMaxHeight()
            .background(bg, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value" },
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        IconBubble(icon, bubble, bubbleTint, box = 28.dp, iconSize = 16.dp, radius = 14.dp)
        Spacer(Modifier.height(16.dp))
        Column {
            Text(metricText(value), style = SlowText.metricDisplay, color = valueColor, maxLines = 1)
            Text(label, style = MaterialTheme.typography.bodySmall, color = labelColor)
        }
    }
}

@Composable
private fun ManualCard(ui: HomeUiState, onStart: () -> Unit, onSpeed: (Int) -> Unit, onTimer: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().background(cs.surfaceContainerLow, RoundedCornerShape(24.dp)).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.manual_title), style = MaterialTheme.typography.headlineSmall, color = cs.onSurface)
                Text(stringResource(R.string.manual_sub), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            IconBubble(Icons.Filled.Speed, cs.secondaryContainer, cs.onSecondaryContainer, box = 32.dp, iconSize = 18.dp, radius = 16.dp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.manual_speed_label), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            SPEED_PRESETS.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { p ->
                        val sel = ui.settings.manualSpeedKbps == p.kbps
                        Row(
                            Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) cs.secondaryContainer else cs.surfaceContainerHighest)
                                .selectable(sel, role = Role.RadioButton, onClick = { onSpeed(p.kbps) })
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    presetName(p.kbps, false),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (sel) cs.onSecondaryContainer else cs.onSurface,
                                )
                                Text(
                                    "${p.kbps} kbps",
                                    style = SlowText.metricLabel,
                                    color = if (sel) (if (cs.background.red < 0.3f) cs.onSecondaryContainer else cs.primary) else cs.onSurfaceVariant,
                                )
                            }
                            if (sel) Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp), tint = cs.primary)
                        }
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.manual_timer_label), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            Row(
                Modifier.fillMaxWidth().background(cs.surfaceContainerHighest, RoundedCornerShape(16.dp)).padding(4.dp),
            ) {
                // 15, 30, 60, then "Indefinite" (0) last, as in the design.
                AppSettings.TIMERS.sortedBy { if (it == 0) Int.MAX_VALUE else it }.forEach { min ->
                    val sel = ui.settings.manualTimerMin == min
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) (if (cs.background.red < 0.3f) cs.secondaryContainer else cs.surfaceContainerLowest) else Color.Transparent)
                            .selectable(sel, role = Role.RadioButton, onClick = { onTimer(min) }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (min == 0) stringResource(R.string.timer_none) else stringResource(R.string.timer_short, min),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (sel) (if (cs.background.red < 0.3f) cs.onSecondaryContainer else cs.primary) else cs.onSurfaceVariant,
                            maxLines = 1,
                            autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 14.sp),
                        )
                    }
                }
            }
        }
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary),
        ) {
            Icon(Icons.Filled.FilterDrama, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.slow_me_now), style = MaterialTheme.typography.labelLarge)
        }
        if (!ui.permissions.vpn) {
            Text(stringResource(R.string.vpn_needed_hint), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun BreathCard() {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().background((if (cs.background.red < 0.3f) cs.secondaryContainer.copy(alpha = 0.4f) else cs.secondaryFixed.copy(alpha = 0.5f)), RoundedCornerShape(24.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBubble(Icons.Filled.SelfImprovement, cs.secondaryContainer, cs.primary, box = 48.dp, iconSize = 24.dp)
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.breath_title), style = MaterialTheme.typography.titleSmall, color = cs.onSurface)
            Text(stringResource(R.string.breath_body), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
    }
}
