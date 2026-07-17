package dev.anpo.f1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import dev.anpo.f1.data.Repo
import dev.anpo.f1.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = Repo(applicationContext)
        setContent {
            F1Theme {
                val p = LocalPalette.current
                val nav = rememberNavController()
                var tab by remember { mutableStateOf(2) }

                Box(Modifier.fillMaxSize().background(p.canvas)) {
                    NavHost(nav, startDestination = "home") {
                        composable("home") {
                            HomeScreen(repo,
                                onDriver = { nav.navigate("driver/$it") },
                                onStandings = { nav.navigate("standings") })
                        }
                        composable("standings") {
                            StandingsScreen(repo, onDriver = { nav.navigate("driver/$it") })
                        }
                        composable("driver/{id}") { e ->
                            DriverScreen(repo, e.arguments?.getString("id").orEmpty())
                        }
                    }

                    // Floating pill nav. Icons only, 27dp. Depth on OLED black comes
                    // from blur + a hairline ring, NOT from a drop shadow - there is
                    // nothing darker than #050506 for a shadow to fall onto.
                    Row(
                        Modifier.align(Alignment.BottomCenter)
                            .padding(horizontal = 20.dp, vertical = 30.dp)
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(p.barBg)
                            .padding(vertical = 15.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NavIcon(Icons.Filled.CheckCircle, "Tasks", tab == 0) { }
                        NavIcon(Icons.Filled.LocationSearching, "Valorant", tab == 1) { }
                        NavIcon(Icons.Filled.Speed, "F1", tab == 2) {
                            nav.popBackStack("home", inclusive = false); tab = 2
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, on: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Icon(icon, contentDescription = label, tint = if (on) p.accent else p.sec,
        modifier = Modifier.size(27.dp).clickable(onClick = onClick))
}
