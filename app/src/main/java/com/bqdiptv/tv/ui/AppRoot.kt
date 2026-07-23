package com.bqdiptv.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.bqdiptv.tv.ui.screens.*
import com.bqdiptv.tv.ui.theme.BqColors
import com.bqdiptv.tv.update.UpdateCheckResult
import com.bqdiptv.tv.update.UpdateInfo
import com.bqdiptv.tv.update.UpdateManager
import kotlinx.coroutines.launch

/**
 * Remote-key map, mirroring the prototype's keyboard handler:
 *   D-pad Up/Down       -> zap channel (no overlay open)
 *   D-pad Center/Enter  -> open channel browser
 *   D-pad Left / Menu    -> open left category menu
 *   D-pad Right / Info   -> open EPG panel for the current channel
 *   Search key           -> open search
 *   Back                 -> close whatever overlay is open
 *
 * Every overlay stays mounted (visible = false when closed) so its
 * AnimatedVisibility exit transition (mirroring the prototype's
 * rail-closing/panel removal) actually gets to play instead of the
 * content just vanishing.
 */
@Composable
fun AppRoot(vm: AppViewModel) {
    val state by vm.state.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val quality = state.graphicsQuality

    var updateUiState by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Idle) }
    val updateManager = remember { UpdateManager(vm.getApplication()) }

    LaunchedEffect(state.overlay) {
        if (state.overlay == Overlay.FIRST_RUN) vm.startProvisioning()
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(state.toast) {
        if (state.toast != null) {
            kotlinx.coroutines.delay(3000)
            vm.clearToast()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BqColors.bg0)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                if (state.lastCrashLog != null) {
                    if (event.key == Key.Back) { vm.dismissCrashLog(); true } else false
                } else if (state.overlay != Overlay.NONE) {
                    if (event.key == Key.Back) {
                        vm.setOverlay(Overlay.NONE); true
                    } else false
                } else when (event.key) {
                    Key.DirectionUp -> { vm.nextChannel(-1); true }
                    Key.DirectionDown -> { vm.nextChannel(1); true }
                    Key.DirectionCenter, Key.Enter -> { vm.setOverlay(Overlay.BROWSER); true }
                    Key.DirectionLeft -> { vm.setOverlay(Overlay.LEFT_MENU); true }
                    Key.DirectionRight, Key.Info -> { vm.setOverlay(Overlay.EPG); true }
                    Key.Menu -> { vm.setOverlay(Overlay.LEFT_MENU); true }
                    Key.Search -> { vm.setOverlay(Overlay.SEARCH); true }
                    else -> false
                }
            }
    ) {
        PlayerSurface(vm.player, modifier = Modifier.fillMaxSize())

        OsdOverlay(
            channel = state.currentChannel,
            current = vm.currentEpgFor(state.currentChannel, System.currentTimeMillis()),
            next = vm.nextEpgFor(state.currentChannel, System.currentTimeMillis()),
            visible = state.overlay == Overlay.NONE && state.currentChannel != null,
            quality = quality,
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)
        )

        LeftMenuOverlay(
            visible = state.overlay == Overlay.LEFT_MENU,
            categories = state.categories,
            quality = quality,
            onCategorySelected = { vm.setOverlay(Overlay.BROWSER) },
            onOpenSearch = { vm.setOverlay(Overlay.SEARCH) },
            onOpenSettings = { vm.setOverlay(Overlay.SETTINGS) },
            modifier = Modifier.align(Alignment.CenterStart)
        )

        BrowserOverlay(
            visible = state.overlay == Overlay.BROWSER,
            channels = state.allChannels,
            favorites = state.favorites,
            currentChannel = state.currentChannel,
            quality = quality,
            currentEpg = { ch -> vm.currentEpgFor(ch, System.currentTimeMillis()) },
            onSelect = { vm.selectChannel(it) },
            onToggleFavorite = { vm.toggleFavorite(it) }
        )

        EpgOverlay(
            visible = state.overlay == Overlay.EPG,
            channel = state.currentChannel,
            programs = state.currentChannel?.let { state.epg[it.id] } ?: emptyList(),
            quality = quality,
            nowMillis = System.currentTimeMillis(),
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        var query by remember { mutableStateOf("") }
        val hits = remember(query, state.epg) {
            if (query.isBlank()) emptyList()
            else state.allChannels.flatMap { ch ->
                (state.epg[ch.id] ?: emptyList())
                    .filter { it.title.contains(query, ignoreCase = true) }
                    .map { SearchHit(ch, it) }
            }.take(50)
        }
        SearchOverlay(
            visible = state.overlay == Overlay.SEARCH,
            query = query,
            onQueryChange = { query = it },
            results = hits,
            quality = quality,
            onSelectHit = { hit -> vm.selectChannel(hit.channel); vm.setOverlay(Overlay.NONE) }
        )

        SettingsOverlay(
            visible = state.overlay == Overlay.SETTINGS,
            playlistUrl = state.allChannels.takeIf { it.isNotEmpty() }?.let { "плейлист загружен, ${it.size} каналов" },
            epgUrl = if (state.epg.isNotEmpty()) "EPG загружен" else null,
            updateState = updateUiState,
            graphicsQuality = quality,
            graphicsAuto = state.graphicsAuto,
            onChangePlaylist = { vm.setOverlay(Overlay.FIRST_RUN) },
            onToggleEconomyMode = { vm.toggleEconomyMode() },
            onCheckUpdate = {
                updateUiState = UpdateUiState.Checking
                scope.launch {
                    updateUiState = when (val result = updateManager.checkForUpdate()) {
                        is UpdateCheckResult.Available -> UpdateUiState.Available(result.info.versionName, result.info.releaseNotes)
                        is UpdateCheckResult.UpToDate -> UpdateUiState.UpToDate
                        is UpdateCheckResult.Error -> UpdateUiState.Error(result.message)
                    }
                }
            },
            onInstallUpdate = {
                scope.launch {
                    try {
                        val fresh = updateManager.checkForUpdate()
                        if (fresh is UpdateCheckResult.Available) {
                            val file = updateManager.download(fresh.info) { pct -> updateUiState = UpdateUiState.Downloading(pct) }
                            updateManager.install(file)
                        }
                    } catch (e: Exception) {
                        updateUiState = UpdateUiState.Error(e.message ?: "Ошибка обновления")
                    }
                }
            }
        )

        FirstRunSetupOverlay(
            visible = state.overlay == Overlay.FIRST_RUN,
            provisioningAddress = state.provisioningAddress
        )

        // Rendered last so it's always on top — including over the opaque
        // full-screen first-run setup panel, where error toasts matter most.
        ToastBanner(
            message = state.toast,
            modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)
        )

        if (state.loading) {
            CircularProgressIndicator(color = BqColors.accent2, modifier = Modifier.align(Alignment.Center))
        }

        CrashLogOverlay(log = state.lastCrashLog, onDismiss = { vm.dismissCrashLog() })
    }
}
