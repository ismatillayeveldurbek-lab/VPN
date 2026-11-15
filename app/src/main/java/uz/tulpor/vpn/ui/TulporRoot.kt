package uz.tulpor.vpn.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.chrisbanes.haze.rememberHazeState
import uz.tulpor.vpn.R
import uz.tulpor.vpn.data.AppSettings
import uz.tulpor.vpn.ui.components.GlassBackground
import uz.tulpor.vpn.ui.components.GlassNavBar
import uz.tulpor.vpn.ui.components.Glow
import uz.tulpor.vpn.ui.components.LocalHazeState
import uz.tulpor.vpn.ui.components.NavItem
import uz.tulpor.vpn.ui.home.HomeScreen
import uz.tulpor.vpn.ui.premium.PremiumScreen
import uz.tulpor.vpn.ui.servers.AddServerSheet
import uz.tulpor.vpn.ui.servers.ServersScreen
import uz.tulpor.vpn.ui.settings.AppsScreen
import uz.tulpor.vpn.ui.settings.SettingsScreen
import uz.tulpor.vpn.vpn.VpnStatus

object Routes {
    const val HOME = "home"
    const val SERVERS = "servers"
    const val SETTINGS = "settings"
    const val PREMIUM = "premium"
    const val APPS = "apps"
}

@Composable
fun TulporRoot(
    vm: MainViewModel,
    pendingImport: String?,
    onImportConsumed: () -> Unit,
    onToggleVpn: () -> Unit,
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val status by vm.status.collectAsStateWithLifecycle()
    val traffic by vm.traffic.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val servers by vm.servers.collectAsStateWithLifecycle()
    val pings by vm.pings.collectAsStateWithLifecycle()
    val pinging by vm.pinging.collectAsStateWithLifecycle()
    val hazeState = rememberHazeState()
    val snackbar = remember { SnackbarHostState() }
    var showAdd by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(pendingImport) { if (pendingImport != null) showAdd = true }

    val glow = when {
        route == Routes.PREMIUM -> Glow.Premium
        route == Routes.HOME && status is VpnStatus.Connected -> Glow.Connected
        route == Routes.HOME || route == null -> Glow.Default
        else -> Glow.Cool
    }

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        GlassBackground(glow, hazeState) {
            NavHost(
                nav, startDestination = Routes.HOME, modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn() }, exitTransition = { fadeOut() },
            ) {
                composable(Routes.HOME) {
                    val shown = vm.displayServer()
                    HomeScreen(
                        status = status, traffic = traffic, server = shown,
                        serverPing = shown?.let { pings[it.id] },
                        hasServers = servers.isNotEmpty(),
                        onToggle = { if (servers.isEmpty()) showAdd = true else onToggleVpn() },
                        onOpenServers = { if (servers.isEmpty()) showAdd = true else nav.navigateTab(Routes.SERVERS) },
                        onOpenPremium = { nav.navigate(Routes.PREMIUM) },
                    )
                }
                composable(Routes.SERVERS) {
                    ServersScreen(
                        servers = servers, selectedId = settings.selectedServerId, pings = pings, pinging = pinging,
                        isPremium = settings.isPremium,
                        onSelect = vm::selectServer, onPingAll = vm::pingAll, onAdd = { showAdd = true },
                        onDelete = { vm.removeServer(it.id) }, onPremium = { nav.navigate(Routes.PREMIUM) },
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        settings = settings, onAutoConnect = vm::setAutoConnect, onBypassUz = vm::setBypassUz,
                        onOpenApps = { nav.navigate(Routes.APPS) }, onRefreshSubscription = vm::refreshSubscription,
                    )
                }
                composable(Routes.APPS) {
                    AppsScreen(
                        mode = settings.splitMode, selected = settings.splitApps,
                        onMode = vm::setSplitMode, onSelected = vm::setSplitApps, onBack = { nav.popBackStack() },
                    )
                }
                composable(Routes.PREMIUM) { PremiumScreen(onClose = { nav.popBackStack() }) }
            }

            val tabs = listOf(Routes.HOME, Routes.SERVERS, Routes.SETTINGS)
            if (route == null || route in tabs) {
                // Fade content out under the floating bar, like the mockup.
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(120.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE00C0924))))
                )
                GlassNavBar(
                    items = listOf(
                        NavItem(Routes.HOME, stringResource(R.string.nav_home), Icons.Rounded.Shield),
                        NavItem(Routes.SERVERS, stringResource(R.string.nav_servers), Icons.Rounded.Language),
                        NavItem(Routes.SETTINGS, stringResource(R.string.nav_settings), Icons.Rounded.Settings),
                    ),
                    current = route ?: Routes.HOME,
                    onSelect = { nav.navigateTab(it) },
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
                )
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.TopCenter).navigationBarsPadding())
        }
    }

    if (showAdd) {
        AddServerSheet(
            initialText = pendingImport.orEmpty(),
            initialSubscription = settings.subscriptionUrl.takeIf { it.isNotBlank() && pendingImport == null }.orEmpty(),
            onDismiss = { showAdd = false; onImportConsumed() },
            onAdd = { links, sub ->
                vm.addLinks(links, sub) { ok ->
                    if (ok) {
                        showAdd = false
                        onImportConsumed()
                        if (settings.selectedServerId == AppSettings.AUTO_SERVER && route != Routes.SERVERS) nav.navigateTab(Routes.SERVERS)
                    }
                }
            },
        )
    }
}

private fun androidx.navigation.NavController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
