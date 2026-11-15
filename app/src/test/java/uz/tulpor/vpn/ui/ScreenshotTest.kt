package uz.tulpor.vpn.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import dev.chrisbanes.haze.rememberHazeState
import org.junit.Test
import uz.tulpor.vpn.data.AppSettings
import uz.tulpor.vpn.data.VlessLink
import uz.tulpor.vpn.ui.components.GlassBackground
import uz.tulpor.vpn.ui.components.GlassNavBar
import uz.tulpor.vpn.ui.components.Glow
import uz.tulpor.vpn.ui.components.LocalHazeState
import uz.tulpor.vpn.ui.components.LocalMotionEnabled
import uz.tulpor.vpn.ui.components.NavItem
import uz.tulpor.vpn.ui.home.HomeScreen
import uz.tulpor.vpn.ui.premium.PremiumScreen
import uz.tulpor.vpn.ui.servers.ServersScreen
import uz.tulpor.vpn.ui.settings.AppsScreen
import uz.tulpor.vpn.ui.settings.SettingsScreen
import uz.tulpor.vpn.data.SplitMode
import uz.tulpor.vpn.ui.theme.TulporTheme
import uz.tulpor.vpn.vpn.Traffic
import uz.tulpor.vpn.vpn.VpnStatus

/**
 * Renders the real screens with sample data into app/screenshots/:
 * `./gradlew :app:testDebugUnitTest --tests '*ScreenshotTest*' -Droborazzi.test.record=true`
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "uz-w411dp-h891dp-xxhdpi", application = android.app.Application::class)
class ScreenshotTest {
    private fun shot(name: String, content: @Composable () -> Unit) =
        captureRoboImage("screenshots/$name.png") { content() }

    private fun link(country: String, ip: String) =
        "vless://b831381d-6324-4d53-ad4f-8cda48b30811@$ip:443?flow=xtls-rprx-vision&security=reality" +
            "&sni=www.microsoft.com&pbk=Z84J2IelR9ch3k8VtlVhhs5ycBUlXA7wHBWcBrjqnAw&sid=6b#$country"

    private val servers = listOf(
        VlessLink.parse(link("DE-Frankfurt", "1.1.1.1"))!!,
        VlessLink.parse(link("NL-Amsterdam", "1.1.1.2"))!!,
        VlessLink.parse(link("FI-Helsinki", "1.1.1.3"))!!,
        VlessLink.parse(link("PL-Warsaw", "1.1.1.4"))!!,
        VlessLink.parse(link("TR-Istanbul", "1.1.1.5"))!!,
    )
    private val pings = mapOf(servers[0].id to 48L, servers[1].id to 61L, servers[2].id to 39L, servers[3].id to 420L, servers[4].id to -1L)

    @Composable
    private fun Frame(glow: Glow, tab: String?, content: @Composable () -> Unit) {
        val haze = rememberHazeState()
        TulporTheme {
            CompositionLocalProvider(LocalHazeState provides haze, LocalMotionEnabled provides false) {
                GlassBackground(glow, haze) {
                    content()
                    if (tab != null) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                        GlassNavBar(
                            listOf(
                                NavItem(Routes.HOME, "Asosiy", Icons.Rounded.Shield),
                                NavItem(Routes.SERVERS, "Serverlar", Icons.Rounded.Language),
                                NavItem(Routes.SETTINGS, "Sozlamalar", Icons.Rounded.Settings),
                            ),
                            current = tab, onSelect = {},
                        )
                    }
                }
            }
        }
    }

    @Test fun home_disconnected() = shot("home_disconnected") {
        Frame(Glow.Default, Routes.HOME) {
            HomeScreen(VpnStatus.Disconnected, Traffic(), null, null, true, {}, {}, {})
        }
    }

    @Test fun home_connected() = shot("home_connected") {
        Frame(Glow.Connected, Routes.HOME) {
            HomeScreen(
                VpnStatus.Connected(servers[0], SystemClock.elapsedRealtime() - 765_000),
                Traffic(downBps = 3_075_000, upBps = 1_037_000), servers[0], 48, true, {}, {}, {},
            )
        }
    }

    @Test fun servers() = shot("servers") {
        Frame(Glow.Cool, Routes.SERVERS) {
            ServersScreen(servers, servers[0].id, pings, false, false, {}, {}, {}, {}, {})
        }
    }

    @Test fun settings() = shot("settings") {
        Frame(Glow.Cool, Routes.SETTINGS) {
            SettingsScreen(AppSettings(autoConnect = false), {}, {}, {}, {})
        }
    }

    @Test fun premium() = shot("premium") {
        Frame(Glow.Premium, null) { PremiumScreen {} }
    }

    // Narrow phone (360dp): catches labels that would wrap or overflow.
    @Config(qualifiers = "uz-w360dp-h740dp-xxhdpi")
    @Test fun small_apps() = shot("small_apps") {
        Frame(Glow.Cool, null) {
            AppsScreen(SplitMode.EXCEPT_SELECTED, emptySet(), {}, {}, {})
        }
    }

    @Config(qualifiers = "uz-w360dp-h740dp-xxhdpi")
    @Test fun small_home_connected() = shot("small_home_connected") {
        Frame(Glow.Connected, Routes.HOME) {
            HomeScreen(
                VpnStatus.Connected(servers[0], SystemClock.elapsedRealtime() - 765_000),
                Traffic(downBps = 13_075_000, upBps = 2_037_000), servers[0], 48, true, {}, {}, {},
            )
        }
    }

    @Config(qualifiers = "uz-w360dp-h740dp-xxhdpi")
    @Test fun small_settings() = shot("small_settings") {
        Frame(Glow.Cool, Routes.SETTINGS) {
            SettingsScreen(AppSettings(splitMode = SplitMode.EXCEPT_SELECTED, splitApps = setOf("a", "b", "c")), {}, {}, {}, {})
        }
    }
}
