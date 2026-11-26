package com.slowdown.app.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slowdown.app.R
import com.slowdown.app.data.DEFAULT_WATCHED_PACKAGES
import com.slowdown.app.data.InstalledApp
import com.slowdown.app.ui.common.containerViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slowdown.app.AppContainer
import com.slowdown.app.data.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppsViewModel(private val c: AppContainer) : ViewModel() {
    val installed = MutableStateFlow<List<InstalledApp>?>(null)
    val settings: StateFlow<AppSettings> = c.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    init {
        viewModelScope.launch { installed.value = c.apps.installed() }
    }

    fun toggle(pkg: String) { viewModelScope.launch { c.settings.toggleWatchedApp(pkg) } }
    fun clear() { viewModelScope.launch { c.settings.setWatchedApps(emptySet()) } }
    fun selectDefaults() {
        viewModelScope.launch { c.settings.setWatchedApps(c.apps.defaultsInstalled()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppsScreen(onBack: () -> Unit) {
    val vm = containerViewModel { AppsViewModel(it) }
    val apps by vm.installed.collectAsStateWithLifecycle()
    val s by vm.settings.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    val cs = MaterialTheme.colorScheme
    Scaffold(
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = cs.background),
                title = { Text(stringResource(R.string.choose_apps), style = MaterialTheme.typography.headlineMedium, color = cs.primary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    label = { Text(stringResource(R.string.search_apps)) },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = cs.surfaceContainerLow,
                        focusedContainerColor = cs.surfaceContainerLow,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedBorderColor = cs.primary,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                val list = apps
                if (list == null) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                } else {
                    val shown = list.filter { it.label.contains(query.trim(), ignoreCase = true) }
                    LazyColumn(contentPadding = PaddingValues(bottom = 112.dp)) {
                        items(shown, key = { it.pkg }) { app ->
                            val checked = app.pkg in s.watchedApps
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 3.dp)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                    .background(if (checked) cs.secondaryContainer.copy(alpha = 0.6f) else cs.surfaceContainerLow)
                                    .heightIn(min = 56.dp)
                                    .clickable(role = Role.Checkbox) { vm.toggle(app.pkg) }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = null,
                                    colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = cs.primary),
                                )
                                Column {
                                    Text(app.label, style = MaterialTheme.typography.titleSmall, color = cs.onSurface)
                                    Text(app.pkg, style = com.slowdown.app.ui.theme.SlowText.metricLabel.copy(fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)), color = cs.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            HorizontalFloatingToolbar(
                expanded = true,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                colors = androidx.compose.material3.FloatingToolbarDefaults.standardFloatingToolbarColors(toolbarContainerColor = cs.surfaceContainerHigh),
            ) {
                IconButton(onClick = vm::selectDefaults) {
                    Icon(Icons.Filled.DoneAll, stringResource(R.string.select_popular))
                }
                IconButton(onClick = vm::clear) {
                    Icon(Icons.Filled.ClearAll, stringResource(R.string.clear_all))
                }
            }
        }
    }
}
