package com.slowdown.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import com.slowdown.app.ui.theme.SlowText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.slowdown.app.AppContainer
import com.slowdown.app.R
import com.slowdown.app.data.DayStats
import com.slowdown.app.data.StatsCodec
import com.slowdown.app.ui.common.Format
import com.slowdown.app.ui.common.containerViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class StatsViewModel(c: AppContainer) : ViewModel() {
    /** Last 7 days, oldest first. */
    val week: StateFlow<List<DayStats>> = c.stats.days
        .map { StatsCodec.lastDays(it, LocalDate.now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsCodec.lastDays(emptyList(), LocalDate.now()))
}

@Composable
fun StatsScreen(modifier: Modifier = Modifier) {
    val vm = containerViewModel { StatsViewModel(it) }
    val week by vm.week.collectAsStateWithLifecycle()
    val labels = week.map { LocalDate.parse(it.date).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()) }
    val scrollMin = week.map { it.scrollMs / 60_000f }
    val slowed = week.map { it.slowedCount.toFloat() }
    val cs = MaterialTheme.colorScheme

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineLarge, color = cs.onSurface)
            Row(
                Modifier.background(cs.secondaryContainer, CircleShape).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.Spa, null, Modifier.size(18.dp), tint = cs.primary)
                Text(stringResource(R.string.stats_week_badge), style = MaterialTheme.typography.labelSmall, color = cs.onSecondaryContainer)
                Text(
                    stringResource(R.string.stats_week_value, Format.duration(week.sumOf { it.slowedMs })),
                    style = MaterialTheme.typography.titleSmall,
                    color = cs.primary,
                )
            }
            ChartCard(
                title = stringResource(R.string.chart_scroll),
                sub = stringResource(R.string.chart_scroll_sub),
                badge = stringResource(R.string.chart_avg, Format.duration((scrollMin.average() * 60_000).toLong())),
                values = scrollMin,
                labels = labels,
                tall = true,
                valueText = { Format.duration((it * 60_000).toLong()) },
            )
            ChartCard(
                title = stringResource(R.string.chart_slowed),
                sub = stringResource(R.string.chart_slowed_sub),
                badge = week.sumOf { it.slowedCount }.toString() + " " + stringResource(R.string.chart_total),
                values = slowed,
                labels = labels,
                tall = false,
                valueText = { it.toInt().toString() },
            )
            Row(
                Modifier.fillMaxWidth().background(cs.surfaceContainerLowest, RoundedCornerShape(16.dp)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Filled.Lock, null, Modifier.size(20.dp), tint = cs.onSurfaceVariant)
                Text(stringResource(R.string.stats_privacy), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
        }
    }
}

/** Seven bars. The peak day gets the clay tone. [tall] bars are charts with a dashed average line; the others carry the count inside. */
@Composable
private fun ChartCard(
    title: String,
    sub: String,
    badge: String,
    values: List<Float>,
    labels: List<String>,
    tall: Boolean,
    valueText: (Float) -> String,
) {
    val cs = MaterialTheme.colorScheme
    val desc = labels.indices.joinToString(", ") { "${labels[it]} ${valueText(values[it])}" }
    val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    val peak = if ((values.maxOrNull() ?: 0f) > 0f) values.indexOf(values.max()) else -1
    val avg = values.average().toFloat()
    val base = cs.primary.copy(alpha = 0.22f)
    val hot = cs.tertiaryContainer.copy(alpha = 0.45f)
    val line = cs.onSurfaceVariant.copy(alpha = 0.6f)
    val chartH = if (tall) 140.dp else 96.dp
    Column(
        Modifier.fillMaxWidth().background(cs.surfaceContainerLow, RoundedCornerShape(16.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = cs.onSurface)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            Text(
                badge,
                Modifier.padding(start = 8.dp).background(cs.surfaceContainer, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
                style = SlowText.metricLabel,
                color = cs.onSurface,
            )
        }
        Column(Modifier.semantics { contentDescription = "$title. $desc" }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(chartH)
                    .drawBehind {
                        if (tall && avg > 0f) {
                            val y = size.height * (1f - avg / max)
                            drawLine(line, Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                        }
                    }
                    .clearAndSetSemantics { },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                values.forEachIndexed { i, v ->
                    val frac = if (v <= 0f) 0.06f else (v / max).coerceIn(0.12f, 1f)
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                        if (tall && i == peak) {
                            Text(valueText(v), style = SlowText.metricLabel, color = cs.tertiary, maxLines = 1)
                            Spacer(Modifier.height(2.dp))
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(if (tall && i == peak) frac * 0.88f else frac)
                                .background(if (i == peak) hot else base, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp)),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            if (!tall && v > 0f) Text(valueText(v), Modifier.padding(top = 4.dp), style = SlowText.metricLabel, color = if (i == peak) cs.tertiary else cs.onSurface)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                labels.forEachIndexed { i, l ->
                    Text(
                        l,
                        Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (i == peak) cs.tertiary else cs.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
