package io.github.pokhriyal_anmol.f1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.tween
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
import io.github.pokhriyal_anmol.f1.data.Repo
import io.github.pokhriyal_anmol.f1.ui.*

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
                        popEnterTransition = { fadeIn(tween(180)) },
                        popExitTransition = { fadeOut(tween(140)) },
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
                            composable("home/race/{round}") { e ->
                                ResultScreen(repo, e.arguments?.getString("round")?.toIntOrNull() ?: 0,
                                    onDriver = { nav.navigate("home/driver/$it") },
                                    onCircuit = { nav.navigate("home/circuit/$it") })
                            }
                            composable("home/circuit/{id}") { e -> CircuitScreen(repo, e.arguments?.getString("id").orEmpty()) }
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
                            composable("news") { NewsScreen() }
                        }
                    }

                    // Floating pill nav. Icons only, 27dp, in 48dp touch targets. Compose has no
                    // backdrop blur, so the design's translucent-blur pill can't be faked -
                    // it goes opaque (p.nav) and gets its lift from a hairline ring
                    // (the inset-light trick that works on OLED black) plus a shadow.
                    Row(
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
                            .padding(vertical = 4.5.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Tab.values().forEach { t ->
                            NavIcon(t, on = t == tab) { nav.openTab(t, selected = t == tab) }
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
        DriverScreen(repo, e.arguments?.getString("id").orEmpty())
    }
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
private fun NavIcon(tab: Tab, on: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Tab, onClickLabel = tab.label, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Icon(painterResource(tab.icon), contentDescription = tab.label,
            tint = if (on) p.accent else p.sec, modifier = Modifier.size(27.dp))
    }
}
