package de.germania.prittlbach

import android.os.Bundle
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

// Set this to your public GitHub Pages URL after deployment.
private const val FEED_URL = "https://YOUR_GITHUB_USERNAME.github.io/GermaniaPrittlbach/news.json"
private val green = Color(0xFF123C31)
private val gold = Color(0xFFDAAC55)
private val cream = Color(0xFFF6F5EF)

data class News(val title: String, val source: String, val date: String, val summary: String, val url: String)
private val fallbackNews = listOf(
    News("Hans Scholz als „Stiller Star“ ausgezeichnet", "Münchner Merkur", "21.09.2026", "Auszeichnung des Deutschen Schützenbundes für langjähriges Engagement. Das Preisgeld soll der Vereinsjugend zugutekommen.", "https://www.merkur.de/lokales/dachau/dachau-ort28553/52-jahre-schriftfuehrer-hans-scholz-vom-sv-germania-prittlbach-bundesweit-geehrt-94503220.html"),
    News("Germania I beendet die Saison auf Platz sechs", "Germania Prittlbach", "Saison 2025/26", "Vereinsbericht zum Bundesliga-Heimwochenende und zum Saisonabschluss.", "https://www.germania-prittlbach.de/aktuelles/2025-2026_bl1_006.htm"),
    News("Germania-Heimwettkampf in Hebertshausen", "Germania Prittlbach", "10.–11.01.2026", "Bericht und Informationen zum Bundesliga-Heimkampf.", "https://germania-prittlbach.de/aktuelles/2025-2026_bl1_heim_vor.htm"),
    News("Germania am Ziel", "Süddeutsche Zeitung", "06.01.2019 · Archiv", "Archivbericht zur Qualifikation für die Endrunde der Luftgewehr-Bundesliga.", "https://www.sueddeutsche.de/muenchen/sportschiessen-germania-am-ziel-1.4276843")
)
data class NewsSource(val title: String, val detail: String, val url: String)
private val newsSources = listOf(
    NewsSource("Vereinswebsite", "Offizielle Nachrichten", "https://www.germania-prittlbach.de/"),
    NewsSource("Münchner Merkur", "Weitere Artikel suchen", "https://www.google.com/search?q=site%3Amerkur.de+%22Germania+Prittlbach%22"),
    NewsSource("Süddeutsche Zeitung", "Weitere Artikel suchen", "https://www.google.com/search?q=site%3Asueddeutsche.de+%22Germania+Prittlbach%22"),
    NewsSource("Instagram", "Beiträge suchen · offizielles Profil noch zu bestätigen", "https://www.instagram.com/explore/search/keyword/?q=Germania%20Prittlbach")
)
data class Team(val name: String, val league: String, val category: String, val url: String)
private val teams = listOf(
    Team("Luftgewehr 1", "1. Bundesliga Süd", "Luftgewehr", "https://bundesliga.dsb.de/luftgewehr/1-bl-sued"),
    Team("Luftgewehr 2", "BSSB – Ergebnislisten", "Luftgewehr", "https://www.rwk-onlinemelder.de/online/listen/000bssb?sel_group_id=7&sel_discipline_id=1&sel_class_id=4&showCombined=0"),
    Team("Luftgewehr 3", "RWK – Wettkampfplan", "Luftgewehr", "https://www.rwk-onlinemelder.de/online/listen/407?sel_group_id=72&sel_discipline_id=1&sel_class_id=1&sel_list_type=competitionSchedule&sel_turn_date=&showCombined=0"),
    Team("Luftgewehr 4", "RWK – Wettkampfplan", "Luftgewehr", "https://www.rwk-onlinemelder.de/online/listen/407?sel_group_id=72&sel_discipline_id=1&sel_class_id=14&sel_list_type=competitionSchedule&sel_turn_date=&showCombined=0"),
    Team("Luftgewehr 5", "RWK – Wettkampfplan", "Luftgewehr", "https://www.rwk-onlinemelder.de/online/listen/407?sel_group_id=72&sel_discipline_id=1&sel_class_id=32&sel_list_type=competitionSchedule&sel_turn_date=&showCombined=0"),
    Team("Luftpistole 1", "RWK – Ergebnislisten", "Luftpistole", "https://www.rwk-onlinemelder.de/online/listen/407?sel_group_id=74&sel_discipline_id=2&sel_class_id=6&showCombined=0"),
    Team("Luftpistole 2", "RWK – Ergebnislisten", "Luftpistole", "https://www.rwk-onlinemelder.de/online/listen/407?sel_group_id=74&sel_discipline_id=2&sel_class_id=7&showCombined=0")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { GermaniaApp() } }
}

@Composable
fun GermaniaApp() {
    var selected by remember { mutableStateOf<Team?>(null) }
    var article by remember { mutableStateOf<News?>(null) }
    var source by remember { mutableStateOf<NewsSource?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    var filter by remember { mutableStateOf("Alle") }
    var news by remember { mutableStateOf(fallbackNews) }
    var newsStatus by remember { mutableStateOf("Beispielmeldungen – Feed noch nicht eingerichtet") }
    LaunchedEffect(Unit) {
        thread {
            try {
                val json = JSONObject(URL(FEED_URL).openText())
                val items = json.getJSONArray("items")
                val incoming = (0 until items.length()).map { i ->
                    val o = items.getJSONObject(i)
                    News(o.optString("title"),o.optString("source"),o.optString("date"),o.optString("summary"),o.optString("url"))
                }
                if (incoming.isNotEmpty()) { news = incoming; newsStatus = "Automatisch aktualisierte Meldungen" }
                else newsStatus = "Keine neuen Meldungen gefunden"
            } catch (_: Exception) { newsStatus = "Feed nicht erreichbar – Beispielmeldungen" }
        }
    }
    BackHandler(selected != null || article != null || source != null) { selected = null; article = null; source = null }
    MaterialTheme(colorScheme = lightColorScheme(primary = green, secondary = gold, background = cream)) {
        Column(Modifier.fillMaxSize().background(cream)) {
            Row(Modifier.fillMaxWidth().background(green).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                if (selected != null || article != null || source != null) IconButton(onClick = { selected = null; article = null; source = null }) { Icon(Icons.Default.ArrowBack, "Zurück", tint = Color.White) }
                Column(Modifier.weight(1f)) {
                    Text("GERMANIA", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
                    Text(selected?.name ?: article?.source ?: source?.title ?: "PRITTLBACH  •  SCHÜTZENVEREIN", color = gold, fontSize = 12.sp)
                }
                Icon(Icons.Default.TrackChanges, null, tint = gold, modifier = Modifier.size(36.dp))
            }
            if (selected != null || article != null || source != null) {
                val url = selected?.url ?: article?.url ?: source!!.url
                Text(selected?.league ?: article?.title ?: source?.detail ?: "", Modifier.padding(16.dp), color = green, fontWeight = FontWeight.Bold)
                var loading by remember(url) { mutableStateOf(true) }
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                AndroidView(factory = { context -> WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() { override fun onPageFinished(view: WebView?, url: String?) { loading = false } }
                    loadUrl(url)
                } }, modifier = Modifier.fillMaxSize())
            } else {
                when (tab) {
                    0 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = green), shape = RoundedCornerShape(20.dp)) {
                                Column(Modifier.fillMaxWidth().padding(22.dp)) {
                                    Text("SAISON 2026 / 27", color = gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(10.dp))
                                    Text("Gemeinsam ins Schwarze.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                                    Text("Alle Mannschaften. Alle Wettkämpfe. Ein Verein.", color = Color.White.copy(alpha = .85f), fontSize = 13.sp)
                                    Spacer(Modifier.height(18.dp))
                                    Text("5 Luftgewehr   •   2 Luftpistole", color = gold, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        item { Text("Unsere Mannschaften", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = green) }
                        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Alle", "Luftgewehr", "Luftpistole").forEach { option -> FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(option, fontSize = 11.sp) }) }
                        } }
                        items(teams.filter { filter == "Alle" || it.category == filter }) { team -> TeamCard(team) { selected = team } }
                        item { Text("Ergebnisse werden direkt bei DSB bzw. RWK-Onlinemelder geladen.", fontSize = 12.sp, color = Color.Gray) }
                    }
                    1 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item { Text("Ergebnisse & Wettkämpfe", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = green) }
                        items(teams) { team -> TeamCard(team) { selected = team } }
                    }
                    else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item { Text("Aktuelles aus dem Verein", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = green) }
                        item { Text("Ausgewählte Artikel mit Quellenangabe. Keine automatische Live-Synchronisierung; für weitere Meldungen die Quellen öffnen.", fontSize = 12.sp, color = Color.DarkGray) }
                        items(news) { entry ->
                            Card(onClick = { article = entry }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                    Text(entry.source.uppercase() + "  •  " + entry.date, color = green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(6.dp))
                                    Text(entry.title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                    Text(entry.summary, fontSize = 13.sp, color = Color.DarkGray)
                                    Spacer(Modifier.height(6.dp))
                                    Text("Artikel öffnen →", color = green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                        item { Text("Nachrichtenquellen", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = green) }
                        items(newsSources) { entry ->
                            Card(onClick = { source = entry }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(entry.title, fontWeight = FontWeight.Bold, color = green)
                                        Text(entry.detail, fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Icon(Icons.Default.OpenInNew, "Öffnen", tint = green)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Start") })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.EmojiEvents, null) }, label = { Text("Ergebnisse") })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.Campaign, null) }, label = { Text("Aktuelles") })
                }
            }
        }
    }
}

@Composable
private fun TeamCard(team: Team, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = cream) { Icon(Icons.Default.GpsFixed, null, tint = green, modifier = Modifier.padding(12.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(team.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = green)
                Text(team.league, fontSize = 12.sp, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, "Öffnen", tint = green)
        }
    }
}
