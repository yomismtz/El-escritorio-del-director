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
        val prefs = getSharedPreferences("director_ui", MODE_PRIVATE)
        setContent {
            val appLanguage = remember { AppLanguagePrefs.load(this@MainActivity) }
            var theme by remember {
                mutableStateOf(
                    AgendaThemeStyle.entries.firstOrNull { it.key == prefs.getString("theme", null) }
                        ?: AgendaThemeStyle.MINT_LAVENDER
                )
            }
            CompositionLocalProvider(LocalAppLanguage provides appLanguage) {
                DirectorTheme(theme) {
                    DirectorApp(
                        currentTheme = theme,
                        onThemeChange = {
                            theme = it
                            prefs.edit().putString("theme", it.key).apply()
                        }
                    )
                }
            }
        }
    }
}

enum class DirectorSection { HOME, TEACHERS, SCHEDULES, NOTICES, MORE }

@Composable
fun DirectorApp(currentTheme: AgendaThemeStyle, onThemeChange: (AgendaThemeStyle) -> Unit) {
    val language = LocalAppLanguage.current
    var section by remember { mutableStateOf(DirectorSection.HOME) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val expandedNavigation = maxWidth >= 700.dp
        Row(Modifier.fillMaxSize()) {
            if (expandedNavigation) {
                NavigationRail {
                    Spacer(Modifier.height(8.dp))
                    NavigationRailItem(section == DirectorSection.HOME, { section = DirectorSection.HOME }, { Icon(Icons.Default.Home, null) }, label = { Text(language.text("Inicio", "Home")) })
                    NavigationRailItem(section == DirectorSection.TEACHERS, { section = DirectorSection.TEACHERS }, { Icon(Icons.Default.Badge, null) }, label = { Text(language.text("Docentes", "Teachers")) })
                    NavigationRailItem(section == DirectorSection.SCHEDULES, { section = DirectorSection.SCHEDULES }, { Icon(Icons.Default.CalendarMonth, null) }, label = { Text(language.text("Horarios", "Schedules")) })
                    NavigationRailItem(section == DirectorSection.NOTICES, { section = DirectorSection.NOTICES }, { Icon(Icons.Default.Notifications, null) }, label = { Text(language.text("Avisos", "Notices")) })
                    NavigationRailItem(section == DirectorSection.MORE, { section = DirectorSection.MORE }, { Icon(Icons.Default.MoreHoriz, null) }, label = { Text("Más") })
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (!expandedNavigation) {
                        NavigationBar {
                            NavigationBarItem(section == DirectorSection.HOME, { section = DirectorSection.HOME }, { Icon(Icons.Default.Home, null) }, label = { Text(language.text("Inicio", "Home")) })
                            NavigationBarItem(section == DirectorSection.TEACHERS, { section = DirectorSection.TEACHERS }, { Icon(Icons.Default.Badge, null) }, label = { Text(language.text("Docentes", "Teachers")) })
                            NavigationBarItem(section == DirectorSection.SCHEDULES, { section = DirectorSection.SCHEDULES }, { Icon(Icons.Default.CalendarMonth, null) }, label = { Text(language.text("Horarios", "Schedules")) })
                            NavigationBarItem(section == DirectorSection.NOTICES, { section = DirectorSection.NOTICES }, { Icon(Icons.Default.Notifications, null) }, label = { Text(language.text("Avisos", "Notices")) })
                            NavigationBarItem(section == DirectorSection.MORE, { section = DirectorSection.MORE }, { Icon(Icons.Default.MoreHoriz, null) }, label = { Text("Más") })
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.fillMaxSize().widthIn(max = 1200.dp)) {
                        when (section) {
                            DirectorSection.HOME -> DashboardScreen()
                            DirectorSection.TEACHERS -> TeachersScreen()
                            DirectorSection.SCHEDULES -> ScheduleBuilderScreen()
                            DirectorSection.NOTICES -> DirectorNoticesScreen()
                            DirectorSection.MORE -> MoreScreen(currentTheme, onThemeChange)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardScreen() {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("El Escritorio del Director", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Misma familia visual de La Carpeta del Docente")
        }
        item {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = if (maxWidth < 600.dp) 2 else 4
                val metrics = listOf("15" to "Docentes", "8" to "Grupos", "42" to "Módulos", "92%" to "Cobertura")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    metrics.chunked(columns).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { (value, label) -> MetricCard(value, label, Modifier.weight(1f)) }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Prioridad: horarios", fontWeight = FontWeight.Bold)
                    Text("Construye la semana usando disponibilidad docente, materias, grupos, duración de módulo y horas requeridas.")
                    Text("⚠ Detecta doble asignación de docente, choque de grupo y horas sin cubrir.")
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Incidencias", fontWeight = FontWeight.Bold)
                    Text("• 1 conflicto de horario por revisar")
                    Text("• 3 módulos pendientes de cubrir")
                    Text("• 2 docentes con carga menor a la planeada")
                    Text("La vista se mantiene a nivel de docentes, grupos y horarios; no muestra estudiantes individuales.")
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Privacidad", fontWeight = FontWeight.Bold)
                    Text("Dirección no ve alumnos individuales, calificaciones ni asistencias. Trabaja con docentes, grupos, horarios y avisos.")
                }
            }
        }
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label)
        }
    }
}

@Composable
private fun TeachersScreen() {
    val teachers = listOf(
        "Ana Torres · Matemáticas · 18/20 h",
        "Luis García · Ciencias · 16/20 h",
        "María López · Español · 20/20 h",
        "Carlos Ruiz · Inglés · 14/18 h"
    )
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Docentes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { Text("Solo personal docente y carga horaria; no hay perfiles individuales de estudiantes.") }
        teachers.forEach { teacher ->
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Badge, null)
                        Spacer(Modifier.width(10.dp))
                        Text(teacher, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleBuilderScreen() {
    var moduleMinutes by remember { mutableStateOf("50") }
    var schoolStart by remember { mutableStateOf("07:00") }
    var schoolEnd by remember { mutableStateOf("14:00") }
    var generated by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Creador de horarios", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Este es el módulo principal de Dirección.")
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Parámetros institucionales", fontWeight = FontWeight.Bold)
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        if (maxWidth < 600.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(moduleMinutes, { moduleMinutes = it.filter(Char::isDigit).take(3) }, label = { Text("Minutos por módulo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(schoolStart, { schoolStart = it.take(5) }, label = { Text("Entrada") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(schoolEnd, { schoolEnd = it.take(5) }, label = { Text("Salida") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(moduleMinutes, { moduleMinutes = it.filter(Char::isDigit).take(3) }, label = { Text("Minutos por módulo") }, singleLine = true, modifier = Modifier.weight(1f))
                                OutlinedTextField(schoolStart, { schoolStart = it.take(5) }, label = { Text("Entrada") }, singleLine = true, modifier = Modifier.weight(1f))
                                OutlinedTextField(schoolEnd, { schoolEnd = it.take(5) }, label = { Text("Salida") }, singleLine = true, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    Button(onClick = { generated = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.AutoAwesome, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Generar propuesta")
                    }
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Reglas que usará el generador", fontWeight = FontWeight.Bold)
                    Text("• disponibilidad de cada docente")
                    Text("• materias que puede impartir")
                    Text("• horas semanales requeridas por grupo")
                    Text("• duración de módulo y descansos")
                    Text("• impedir dos clases al mismo tiempo para un docente o grupo")
                }
            }
        }
        if (generated) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Propuesta DEMO", fontWeight = FontWeight.Bold)
                        Text("92% de cobertura · 1 conflicto · 3 módulos por cubrir")
                        Text("La propuesta nunca se aplica sola; Dirección debe revisarla y confirmarla.")
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectorNoticesScreen() {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Avisos a docentes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Los avisos de Dirección llegan a los docentes seleccionados. No aparecen directamente en la app del estudiante.")
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(message, { message = it }, label = { Text("Mensaje") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    Text("Destinatarios: docentes de la institución")
                    Button(onClick = { sent = true }, enabled = title.isNotBlank() && message.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Send, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Publicar para docentes")
                    }
                }
            }
        }
        if (sent) {
            item {
                AssistChip(onClick = {}, label = { Text("Aviso preparado para sincronización con La Carpeta del Docente") }, leadingIcon = { Icon(Icons.Default.CheckCircle, null) })
            }
        }
        item {
            Text("Ejemplo: si Dirección publica ‘Suspensión el viernes’, el docente recibe el aviso y decide cómo comunicarlo a sus grupos. Si el docente publica ‘Mañana clase al aire libre’ en una clase, lo reciben directamente los alumnos vinculados al código de esa clase.")
        }
    }
}

@Composable
private fun MoreScreen(currentTheme: AgendaThemeStyle, onThemeChange: (AgendaThemeStyle) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val language = LocalAppLanguage.current
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text(language.text("Más", "More"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(language.text("Idioma", "Language"), fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { item ->
                            FilterChip(
                                selected = language == item,
                                onClick = {
                                    if (language != item) {
                                        AppLanguagePrefs.save(context, item)
                                        (context as? android.app.Activity)?.recreate()
                                    }
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
        item { DirectorDocumentsCard() }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Color de la interfaz", fontWeight = FontWeight.Bold)
                    Text("Los mismos temas que La Carpeta del Docente y El Cuaderno del Estudiante.")
                    AgendaThemeStyle.entries.forEach { style ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = style == currentTheme, onClick = { onThemeChange(style) })
                            Column {
                                Text(style.title)
                                Text(style.subtitle, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Estructura compartida", fontWeight = FontWeight.Bold)
                    Text("Institución → ciclo → docente → clase/grupo → código de clase. El código será la llave de vinculación entre docente y alumnos cuando se conecte el backend.")
                }
            }
        }
    }
}
