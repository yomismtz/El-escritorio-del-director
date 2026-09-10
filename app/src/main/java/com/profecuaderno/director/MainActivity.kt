package com.profecuaderno.director

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DirectorApp() }
    }
}

enum class DirectorSection(val label: String) {
    HOME("Inicio"), PEOPLE("Personas"), GROUPS("Grupos"), SCHEDULES("Horarios"), MORE("Más")
}

@Composable
fun DirectorApp() {
    var section by remember { mutableStateOf(DirectorSection.HOME) }
    MaterialTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(section == DirectorSection.HOME, { section = DirectorSection.HOME }, { Icon(Icons.Default.Home, null) }, { Text("Inicio") })
                    NavigationBarItem(section == DirectorSection.PEOPLE, { section = DirectorSection.PEOPLE }, { Icon(Icons.Default.Groups, null) }, { Text("Personas") })
                    NavigationBarItem(section == DirectorSection.GROUPS, { section = DirectorSection.GROUPS }, { Icon(Icons.Default.School, null) }, { Text("Grupos") })
                    NavigationBarItem(section == DirectorSection.SCHEDULES, { section = DirectorSection.SCHEDULES }, { Icon(Icons.Default.CalendarMonth, null) }, { Text("Horarios") })
                    NavigationBarItem(section == DirectorSection.MORE, { section = DirectorSection.MORE }, { Icon(Icons.Default.MoreHoriz, null) }, { Text("Más") })
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (section) {
                    DirectorSection.HOME -> DashboardScreen()
                    DirectorSection.PEOPLE -> PlaceholderScreen("Personas", "Docentes y estudiantes de la institución.")
                    DirectorSection.GROUPS -> PlaceholderScreen("Grupos", "Organiza grupos, materias y responsables.")
                    DirectorSection.SCHEDULES -> PlaceholderScreen("Horarios", "Construcción manual y asistida de horarios.")
                    DirectorSection.MORE -> MoreScreen()
                }
            }
        }
    }
}

@Composable
private fun DashboardScreen() {
    val cards = listOf(
        Triple("15", "Docentes", Icons.Default.Badge),
        Triple("200", "Estudiantes", Icons.Default.Person),
        Triple("8", "Grupos", Icons.Default.Groups),
        Triple("92%", "Cobertura", Icons.Default.CheckCircle)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("El Escritorio del Director", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Institución DEMO · Ciclo 2026–2027", style = MaterialTheme.typography.bodyMedium)
        }
        item {
            cards.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { (value, label, icon) ->
                        ElevatedCard(Modifier.weight(1f)) {
                            Column(Modifier.padding(14.dp)) {
                                Icon(icon, null)
                                Spacer(Modifier.height(8.dp))
                                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                Text(label)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Alertas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("⚠ 2 grupos tienen materias sin docente")
                    Text("⚠ 1 conflicto de horario detectado")
                    Text("✓ 13 docentes tienen carga asignada")
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Próximamente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Consejo técnico · viernes")
                    Text("Cierre de evaluación · próximo lunes")
                }
            }
        }
    }
}

@Composable
private fun MoreScreen() {
    val options = listOf("Materias", "Avisos", "Calendario institucional", "Cobertura académica", "Estadísticas", "Ciclos escolares", "Configuración")
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Más", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        items(options.size) { index ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(options[index], modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, subtitle: String) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(subtitle)
        ElevatedCard(Modifier.fillMaxWidth()) {
            Text("Módulo base creado. La siguiente etapa añadirá datos, edición y persistencia local.", Modifier.padding(16.dp))
        }
    }
}
