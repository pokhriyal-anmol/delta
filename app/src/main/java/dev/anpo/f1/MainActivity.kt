package dev.anpo.f1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.compose.*
import dev.anpo.f1.data.Repo
import dev.anpo.f1.ui.*

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
                var tab by remember { mutableStateOf(2) }

                Box(Modifier.fillMaxSize().background(p.canvas)) {
                    NavHost(nav, startDestination = "home") {
                        composable("home") {
                            HomeScreen(repo,
                                onDriver = { nav.navigate("driver/$it") },
                                onStandings = { nav.navigate("standings") },
                                onWeekend = { nav.navigate("weekend") })
                        }
                        composable("standings") {
                            StandingsScreen(repo, onDriver = { nav.navigate("driver/$it") })
                        }
                        composable("weekend") {
                            WeekendScreen(repo)
                        }
                        composable("driver/{id}") { e ->
                            DriverScreen(repo, e.arguments?.getString("id").orEmpty())
                        }
                    }

                    // Floating pill nav. Icons only, 27dp. Compose has no backdrop
                    // blur, so the design's translucent-blur pill can't be faked -
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
                            .padding(vertical = 15.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NavIcon(painterResource(R.drawable.ic_tasks), "Tasks", tab == 0) { }
                        NavIcon(painterResource(R.drawable.ic_valorant), "Valorant", tab == 1) { }
                        NavIcon(painterResource(R.drawable.ic_f1), "F1", tab == 2) {
                            nav.popBackStack("home", inclusive = false); tab = 2
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavIcon(icon: Painter, label: String, on: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Icon(icon, contentDescription = label, tint = if (on) p.accent else p.sec,
        modifier = Modifier.size(27.dp).clickable(onClick = onClick))
}
