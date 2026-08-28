package jiamin.chen.orangecloud.ui.toolbox

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import jiamin.chen.orangecloud.ui.push.PushCenterScreen
import jiamin.chen.orangecloud.ui.push.PushInboxScreen

/** 工具箱内部路由（与 ToolboxHubScreen 的 TOOLBOX_ENTRIES 一致）。 */
object ToolRoutes {
    const val HUB = "hub"
    const val PUSH = "push"
    const val PUSH_INBOX = "push/inbox"
    const val DNS = "dns"
    const val SSL = "ssl"
    const val HTTP = "http"
    const val WHOIS = "whois"
    const val GEOIP = "geoip"
    const val CIDR = "cidr"
    const val TRACE = "trace"
}

/**
 * 免登录工具箱的自包含导航宿主（自带 NavController）。
 * 同一份在两处复用：登录页（未登录覆盖层）与设置页（登录后路由）。
 * onExit = 从工具箱整体退出（hub 的返回触发）。
 */
@Composable
fun ToolboxNavHost(onExit: () -> Unit) {
    val nav = rememberNavController()
    val back: () -> Unit = { nav.popBackStack() }
    NavHost(navController = nav, startDestination = ToolRoutes.HUB) {
        composable(ToolRoutes.HUB) {
            ToolboxHubScreen(onExit = onExit, onOpenTool = { route -> nav.navigate(route) })
        }
        composable(ToolRoutes.PUSH) {
            PushCenterScreen(onBack = back, onOpenInbox = { nav.navigate(ToolRoutes.PUSH_INBOX) })
        }
        composable(ToolRoutes.PUSH_INBOX) { PushInboxScreen(onBack = back) }
        composable(ToolRoutes.DNS) { DnsLookupToolScreen(onBack = back) }
        composable(ToolRoutes.SSL) { SslInspectToolScreen(onBack = back) }
        composable(ToolRoutes.HTTP) { HttpProbeToolScreen(onBack = back) }
        composable(ToolRoutes.WHOIS) { WhoisToolScreen(onBack = back) }
        composable(ToolRoutes.GEOIP) { GeoIpToolScreen(onBack = back) }
        composable(ToolRoutes.CIDR) { CidrToolScreen(onBack = back) }
        composable(ToolRoutes.TRACE) { CfTraceToolScreen(onBack = back) }
    }
}
