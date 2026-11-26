package com.slowdown.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slowdown.app.R
import com.slowdown.app.data.PermissionState
import com.slowdown.app.ui.common.BrandMark
import com.slowdown.app.ui.common.PermissionActions
import com.slowdown.app.ui.common.rememberContainer
import com.slowdown.app.ui.common.rememberPermissionActions
import com.slowdown.app.ui.theme.SlowText
import kotlinx.coroutines.launch

private const val INTRO_PAGES = 3
private const val PAGES = INTRO_PAGES + 1

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val c = rememberContainer()
    val cs = MaterialTheme.colorScheme
    val perms by c.permissions.state.collectAsStateWithLifecycle()
    val actions = rememberPermissionActions(onChanged = c.permissions::refresh)
    val pager = rememberPagerState { PAGES }
    val scope = rememberCoroutineScope()
    LifecycleResumeEffect(Unit) {
        c.permissions.refresh()
        onPauseOrDispose { }
    }
    BackHandler(enabled = pager.currentPage > 0) {
        scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
    }
    val last = pager.currentPage == PAGES - 1
    val finish: () -> Unit = {
        scope.launch {
            c.settings.setOnboardingDone(true)
            if (c.permissions.state.value.usageAccess) c.setWatching(true)
            onFinished()
        }
    }

    Scaffold(containerColor = cs.background) { pad ->
        Column(Modifier.fillMaxSize().padding(pad), horizontalAlignment = Alignment.CenterHorizontally) {
            val step = pager.currentPage + 1
            Text(
                stringResource(if (last) R.string.ob_step_perm else R.string.ob_step, step, PAGES),
                Modifier
                    .padding(top = 16.dp)
                    .background(cs.secondaryContainer, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSecondaryContainer,
            )
            HorizontalPager(pager, Modifier.weight(1f).widthIn(max = 560.dp)) { page ->
                when (page) {
                    0 -> IntroPage(Icons.Filled.Insights, MaterialShapes.Cookie9Sided.toShape(), R.string.ob1_title, R.string.ob1_body)
                    1 -> IntroPage(Icons.Filled.Speed, MaterialShapes.Sunny.toShape(), R.string.ob2_title, R.string.ob2_body)
                    2 -> IntroPage(Icons.Filled.WifiOff, MaterialShapes.Clover4Leaf.toShape(), R.string.ob3_title, R.string.ob3_body)
                    else -> PermissionsPage(perms, actions)
                }
            }
            if (last) {
                Column(
                    Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = finish,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary),
                    ) {
                        Text(stringResource(R.string.begin), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.size(8.dp))
                        Icon(Icons.Filled.Check, null, Modifier.size(18.dp))
                    }
                    if (!perms.usageAccess) {
                        Button(
                            onClick = finish,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = cs.surfaceContainerHigh, contentColor = cs.onSurfaceVariant),
                        ) { Text(stringResource(R.string.skip_manual), style = MaterialTheme.typography.labelLarge) }
                    }
                }
            } else {
                PageDots(pager.currentPage)
                Row(
                    Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (pager.currentPage > 0) {
                        TextButton(
                            onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text(stringResource(R.string.back)) }
                    } else {
                        Spacer(Modifier.size(1.dp))
                    }
                    Button(
                        onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary),
                    ) { Text(stringResource(R.string.next)) }
                }
            }
        }
    }
}

@Composable
private fun PageDots(current: Int) {
    val cs = MaterialTheme.colorScheme
    val desc = stringResource(R.string.page_of, current + 1, PAGES)
    Row(
        Modifier.semantics { contentDescription = desc },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(PAGES) { i ->
            Box(
                Modifier
                    .size(width = if (i == current) 24.dp else 8.dp, height = 8.dp)
                    .background(if (i == current) cs.primary else cs.outlineVariant, CircleShape),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IntroPage(icon: ImageVector, shape: androidx.compose.ui.graphics.Shape, title: Int, body: Int) {
    val cs = MaterialTheme.colorScheme
    // Scrolls so large fonts and short screens never clip the body text.
    BoxWithConstraints(Modifier.fillMaxSize()) {
    Column(
        Modifier.fillMaxWidth().heightIn(min = maxHeight).verticalScroll(rememberScrollState()).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(shape = shape, color = cs.secondaryContainer, modifier = Modifier.size(180.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(72.dp), tint = cs.primary)
            }
        }
        Spacer(Modifier.size(32.dp))
        Text(stringResource(title), style = MaterialTheme.typography.headlineLarge, color = cs.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.size(12.dp))
        Text(stringResource(body), style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
    }
    }
}

@Composable
private fun PermissionsPage(p: PermissionState, a: PermissionActions) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(88.dp).background(cs.surfaceContainerLow, CircleShape), contentAlignment = Alignment.Center) {
            BrandMark(56, cs.primary, cs.primary)
        }
        Text(stringResource(R.string.perm_headline), style = MaterialTheme.typography.headlineLarge, color = cs.onSurface, textAlign = TextAlign.Center)
        Text(stringResource(R.string.perm_intro), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
        Row(
            Modifier.fillMaxWidth().background(cs.surfaceContainer, RoundedCornerShape(16.dp)).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBox(Icons.Filled.Train)
            Column {
                Text(stringResource(R.string.perm_commute_title), style = MaterialTheme.typography.titleSmall, color = cs.onSurface)
                Text(stringResource(R.string.perm_commute_body), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
        }
        PermCard(
            icon = Icons.Filled.Speed,
            title = R.string.perm_vpn,
            tag = R.string.perm_required,
            tagColor = cs.tertiary,
            body = R.string.perm_vpn_why,
            granted = p.vpn,
            grantedLabel = R.string.perm_granted,
            onGrant = a.requestVpn,
        ) {
            Text(stringResource(R.string.perm_vpn_mono).uppercase(), style = SlowText.metricLabel, color = cs.onSurfaceVariant)
        }
        PermCard(
            icon = Icons.Filled.NotificationsActive,
            title = R.string.perm_notif,
            tag = R.string.perm_recommended,
            tagColor = cs.onSurfaceVariant,
            body = R.string.perm_notif_why,
            granted = p.notifications,
            grantedLabel = R.string.perm_enabled,
            onGrant = a.requestNotifications,
        ) {
            Text(
                stringResource(R.string.perm_notif_quote),
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = cs.onSurfaceVariant,
            )
        }
        PermCard(
            icon = Icons.Filled.Visibility,
            title = R.string.perm_usage,
            tag = R.string.perm_optional,
            tagColor = cs.onSurfaceVariant,
            body = R.string.perm_usage_long,
            granted = p.usageAccess,
            grantedLabel = R.string.perm_granted,
            onGrant = a.openUsageAccess,
        ) {
            if (!p.usageAccess) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Spa, null, Modifier.size(16.dp), tint = cs.tertiary)
                    Text(
                        stringResource(R.string.perm_skippable),
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = cs.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun IconBox(icon: ImageVector) {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.size(40.dp).background(cs.secondaryContainer, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(22.dp), tint = cs.primary)
    }
}

@Composable
private fun PermCard(
    icon: ImageVector,
    title: Int,
    tag: Int,
    tagColor: Color,
    body: Int,
    granted: Boolean,
    grantedLabel: Int,
    onGrant: () -> Unit,
    extra: @Composable () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val status = stringResource(if (granted) grantedLabel else R.string.perm_not_granted)
    Column(
        Modifier
            .fillMaxWidth()
            .background(cs.surfaceContainer, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .semantics(mergeDescendants = true) { contentDescription = status },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBox(icon)
            Column(Modifier.weight(1f)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                Text(stringResource(tag), style = MaterialTheme.typography.bodySmall, color = tagColor)
            }
            if (granted) {
                Row(
                    Modifier.background(cs.secondaryContainer, CircleShape).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Filled.Check, null, Modifier.size(14.dp), tint = cs.primary)
                    Text(stringResource(grantedLabel), style = MaterialTheme.typography.labelMedium, color = cs.primary)
                }
            } else {
                Button(
                    onClick = onGrant,
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = cs.surfaceContainerLowest, contentColor = cs.onSurface),
                ) {
                    Text(stringResource(R.string.grant), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.size(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(16.dp))
                }
            }
        }
        Text(stringResource(body), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        extra()
    }
}
