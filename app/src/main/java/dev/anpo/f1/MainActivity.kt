package dev.anpo.f1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.*
import dev.anpo.f1.data.Repo
import dev.anpo.f1.ui.*

/** The three bottom-bar destinations. Each owns a nested graph, so each keeps its own back stack. */
private enum class Tab(val graph: String, val start: String, val label: String, @DrawableRes val icon: Int) {
    Home("home_tab", "home", "Home", R.drawable.ic_f1),
    Championship("championship_tab", "championship", "Championship", R.drawable.ic_trophy),
    News("news_tab", "news", "News", R.drawable.ic_news),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = Repo(applicationContext)
        setContent {
            // Follow the system theme, and drive the status/nav bar icon colour from it
            // so it flips with the canvas (light icons on dark, dark icons on light) -
            // otherwise the hardcoded white icons vanish on the light canvas.
            val dark = isSystemInDarkTheme()
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            F1Theme(dark) {
                val p = LocalPalette.current
                val motion = rememberMotionEnabled()
                val tick = rememberTick()
                val nav = rememberNavController()
                val entry by nav.currentBackStackEntryAsState()
                val tab = Tab.values().firstOrNull { t ->
                    entry?.destination?.hierarchy?.any { it.route == t.graph } == true
                } ?: Tab.Home

                Box(Modifier.fillMaxSize().background(p.canvas)) {
                    // Plain cross-fades on every route. The default slide + predictive-back
                    // preview was translating the whole screen to a corner and bouncing.
                    NavHost(
                        nav, startDestination = Tab.Home.graph,
                        enterTransition = { fadeIn(tween(180)) },
                        exitTransition = { fadeOut(tween(140)) },
                        // Predictive back: the page being left shrinks and fades while the one under it comes forward.
                        popEnterTransition = { fadeIn(tween(180)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220, easing = EaseOut)) },
                        popExitTransition = { fadeOut(tween(140)) + scaleOut(targetScale = 0.92f, animationSpec = tween(220, easing = EaseOut)) },
                    ) {
                        navigation(startDestination = Tab.Home.start, route = Tab.Home.graph) {
                            composable("home") {
                                HomeScreen(repo,
                                    onDriver = { nav.navigate("home/driver/$it") },
                                    onStandings = { nav.openTab(Tab.Championship, selected = false) },
                                    onWeekend = { nav.navigate("home/weekend") },
                                    onCalendar = { nav.navigate("home/calendar") },
                                    onConstructor = { nav.navigate("home/constructor/$it") })
                            }
                            composable("home/weekend") { WeekendScreen(repo, onCircuit = { nav.navigate("home/circuit/$it") }) }
                            composable("home/calendar") {
                                CalendarScreen(repo,
                                    onRace = { nav.navigate("home/race/$it") },
                                    onWeekend = { nav.navigate("home/weekend") },
                                    onCircuit = { nav.navigate("home/circuit/$it") })
                            }
                            details("home", nav, repo)
                        }
                        navigation(startDestination = Tab.Championship.start, route = Tab.Championship.graph) {
                            composable("championship") {
                                ChampionshipScreen(repo,
                                    onDriver = { nav.navigate("championship/driver/$it") },
                                    onTeam = { nav.navigate("championship/constructor/$it") })
                            }
                            details("championship", nav, repo)
                        }
                        navigation(startDestination = Tab.News.start, route = Tab.News.graph) {
                            composable("news") { NewsScreen(repo) }
                        }
                    }

                    // Floating pill nav. Icons only, 27dp, in 48dp touch targets. Compose has no
                    // backdrop blur, so the design's translucent-blur pill can't be faked -
                    // it goes opaque (p.nav) and gets its lift from a hairline ring
                    // (the inset-light trick that works on OLED black) plus a shadow.
                    BoxWithConstraints(
                        Modifier.align(Alignment.BottomCenter)
                            .padding(horizontal = 20.dp, vertical = 30.dp)
                            .fillMaxWidth()
                            .shadow(if (p.isDark) 8.dp else 16.dp, CircleShape, clip = false,
                                ambientColor = Color.Black.copy(alpha = 0.7f),
                                spotColor = Color.Black.copy(alpha = 0.7f))
                            .clip(CircleShape)
                            .background(p.barBg)   // fully opaque - no list content bleeding through
                            .border(0.7.dp,
                                if (p.isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f),
                                CircleShape)
                            .padding(vertical = 4.5.dp)
                    ) {
                        // A raised disc slides to the active tab with a small overshoot.
                        val cell = maxWidth / Tab.values().size
                        val x by animateDpAsState((cell * Tab.values().indexOf(tab)) + (cell - 64.dp) / 2,
                            if (motion) spring(dampingRatio = 0.72f, stiffness = 400f) else snap(), label = "thumb")
                        Box(Modifier.offset(x = x).size(width = 64.dp, height = 48.dp).clip(CircleShape).background(p.chip))
                        Row(Modifier.fillMaxWidth()) {
                            Tab.values().forEach { t ->
                                NavIcon(t, on = t == tab, Modifier.weight(1f)) { if (t != tab) tick(); nav.openTab(t, selected = t == tab) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Per-tab detail pages. The prefix keeps one tab's driver page a different back-stack entry from another's. */
private fun NavGraphBuilder.details(prefix: String, nav: NavController, repo: Repo) {
    composable("$prefix/driver/{id}") { e ->
        DriverScreen(repo, e.arguments?.getString("id").orEmpty(),
            onTeam = { nav.navigate("$prefix/constructor/$it") },
            onRace = { nav.navigate("$prefix/race/$it") })
    }
    composable("$prefix/race/{round}") { e ->
        ResultScreen(repo, e.arguments?.getString("round")?.toIntOrNull() ?: 0,
            onDriver = { nav.navigate("$prefix/driver/$it") },
            onCircuit = { nav.navigate("$prefix/circuit/$it") })
    }
    composable("$prefix/circuit/{id}") { e -> CircuitScreen(repo, e.arguments?.getString("id").orEmpty()) }
    composable("$prefix/constructor/{id}") { e ->
        ConstructorScreen(repo, e.arguments?.getString("id").orEmpty(),
            onDriver = { nav.navigate("$prefix/driver/$it") })
    }
}

/** Switching tabs saves the leaving tab's stack and restores the arriving one's. Tapping the active tab pops to its root. */
private fun NavController.openTab(tab: Tab, selected: Boolean) {
    if (selected) {
        popBackStack(tab.start, inclusive = false)
    } else {
        navigate(tab.graph) {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

@Composable
private fun NavIcon(tab: Tab, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    val tint by animateColorAsState(if (on) p.accent else p.sec, tween(200, easing = EaseOut), label = "tint")
    Box(modifier.height(48.dp).clip(CircleShape).clickable(role = Role.Tab, onClickLabel = tab.label, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Icon(painterResource(tab.icon), contentDescription = tab.label,
            tint = tint, modifier = Modifier.size(27.dp))
    }
}
