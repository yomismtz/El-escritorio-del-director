package com.profecuaderno.director

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.profecuaderno.director.network.CentralBackend
import com.profecuaderno.director.network.ClassDto
import com.profecuaderno.director.network.DirectorNoticeDto
import com.profecuaderno.director.network.InstitutionDto
import com.profecuaderno.director.network.InstitutionRequest
import com.profecuaderno.director.network.NoticeRequest
import com.profecuaderno.director.network.UserDto
import kotlinx.coroutines.launch

class OnlineDirectorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val backend = CentralBackend(this)
        val prefs = getSharedPreferences("director_ui", MODE_PRIVATE)
        setContent {
            val appLanguage = remember { AppLanguagePrefs.load(this@OnlineDirectorActivity) }
            val theme = remember {
                AgendaThemeStyle.entries.firstOrNull { it.key == prefs.getString("theme", null) }
                    ?: AgendaThemeStyle.MINT_LAVENDER
            }
            CompositionLocalProvider(LocalAppLanguage provides appLanguage) {
                DirectorTheme(theme) {
                    OnlineDirectorScreen(
                        backend = backend,
                        onSignOut = {
                            backend.signOut()
                            finish()
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnlineDirectorScreen(
    backend: CentralBackend,
    onSignOut: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var me by remember { mutableStateOf<UserDto?>(null) }
    var institution by remember { mutableStateOf<InstitutionDto?>(null) }
    var teachers by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    var classes by remember { mutableStateOf<List<ClassDto>>(emptyList()) }
    var notices by remember { mutableStateOf<List<DirectorNoticeDto>>(emptyList()) }
    var institutionName by remember { mutableStateOf("") }
    var teacherEmail by remember { mutableStateOf("") }
    var noticeTitle by remember { mutableStateOf("") }
    var noticeBody by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Conectando con ProfeCuaderno Online…") }

    fun refresh() {
        scope.launch {
            loading = true
            runCatching {
                val current = backend.api.me()
                if (current.institutionId == null) {
                    InstitutionalSnapshot(current, null, emptyList(), emptyList(), emptyList())
                } else {
                    InstitutionalSnapshot(
                        current = current,
                        institution = backend.api.institution(),
                        teachers = backend.api.teachers(),
                        classes = backend.api.classes(),
                        notices = backend.api.directorNotices(),
                    )
                }
            }.onSuccess { snapshot ->
                me = snapshot.current
                institution = snapshot.institution
                teachers = snapshot.teachers
                classes = snapshot.classes
                notices = snapshot.notices
                message = "Datos institucionales actualizados"
            }.onFailure { error ->
                message = error.message ?: "No se pudo consultar el servidor"
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("El Escritorio del Director · Online") },
                actions = {
                    IconButton(onClick = ::refresh, enabled = !loading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDone, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Servidor central", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(message, color = MaterialTheme.colorScheme.primary)
                        me?.let { director ->
                            Text(director.fullName.ifBlank { director.email })
                            Text(director.email, style = MaterialTheme.typography.bodySmall)
                            Text(
                                institution?.name ?: if (director.institutionId == null) "Sin institución creada" else "Institución vinculada",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }

            if (me?.institutionId == null) {
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AddBusiness, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Crear institución", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            OutlinedTextField(
                                value = institutionName,
                                onValueChange = { institutionName = it },
                                label = { Text("Nombre de la institución") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            Button(
                                onClick = {
                                    scope.launch {
                                        loading = true
                                        runCatching { backend.api.createInstitution(InstitutionRequest(institutionName.trim())) }
                                            .onSuccess { created ->
                                                message = "Institución creada: ${created.name}"
                                                institutionName = ""
                                                refresh()
                                            }
                                            .onFailure { message = it.message ?: "No se pudo crear la institución" }
                                        loading = false
                                    }
                                },
                                enabled = institutionName.trim().length >= 2 && !loading,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Crear y vincular a mi cuenta")
                            }
                        }
                    }
                }
            } else {
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Badge, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Vincular docente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text("El docente debe haber creado primero su cuenta online. La vinculación se hace por correo exacto.")
                            OutlinedTextField(
                                value = teacherEmail,
                                onValueChange = { teacherEmail = it },
                                label = { Text("Correo del docente") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            Button(
                                onClick = {
                                    scope.launch {
                                        loading = true
                                        runCatching { backend.api.attachTeacher(teacherEmail.trim()) }
                                            .onSuccess { teacher ->
                                                teacherEmail = ""
                                                message = "Docente vinculado: ${teacher.fullName.ifBlank { teacher.email }}"
                                                refresh()
                                            }
                                            .onFailure { message = it.message ?: "No se pudo vincular al docente" }
                                        loading = false
                                    }
                                },
                                enabled = teacherEmail.contains("@") && !loading,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Vincular docente")
                            }
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Badge, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Docentes vinculados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                if (teachers.isEmpty()) {
                    item { Text("Todavía no hay docentes vinculados a esta institución.") }
                } else {
                    items(teachers, key = { "teacher-${it.id}" }) { teacher ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(teacher.fullName.ifBlank { teacher.email }, fontWeight = FontWeight.Bold)
                                Text(teacher.email, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Aviso para docentes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text("Este canal es institucional: los estudiantes no reciben estos avisos.")
                            OutlinedTextField(
                                value = noticeTitle,
                                onValueChange = { noticeTitle = it },
                                label = { Text("Título") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = noticeBody,
                                onValueChange = { noticeBody = it },
                                label = { Text("Mensaje") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                            )
                            Button(
                                onClick = {
                                    scope.launch {
                                        loading = true
                                        runCatching {
                                            backend.api.createDirectorNotice(NoticeRequest(noticeTitle.trim(), noticeBody.trim()))
                                        }.onSuccess {
                                            noticeTitle = ""
                                            noticeBody = ""
                                            message = "Aviso enviado a los docentes de la institución"
                                            refresh()
                                        }.onFailure {
                                            message = it.message ?: "No se pudo enviar el aviso"
                                        }
                                        loading = false
                                    }
                                },
                                enabled = noticeTitle.isNotBlank() && noticeBody.isNotBlank() && !loading,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Publicar aviso institucional")
                            }
                        }
                    }
                }

                if (notices.isNotEmpty()) {
                    item { Text("Avisos institucionales recientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    items(notices, key = { "notice-${it.id}" }) { notice ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(notice.title, fontWeight = FontWeight.Bold)
                                Text(notice.body)
                            }
                        }
                    }
                }
            }

            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(classes.size.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Grupos online")
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(teachers.size.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Docentes vinculados")
                        }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Groups, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Grupos de la institución", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            if (classes.isEmpty()) {
                item {
                    Text(
                        if (me?.institutionId == null) "Crea una institución para comenzar." else "Todavía no hay grupos online vinculados a esta institución."
                    )
                }
            } else {
                items(classes, key = { "class-${it.id}" }) { classroom ->
                    val teacher = teachers.firstOrNull { it.id == classroom.teacherId }
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(classroom.name, fontWeight = FontWeight.Bold)
                            Text(classroom.subject.ifBlank { "Sin materia" })
                            Text("Periodo: ${classroom.periodName.ifBlank { "Sin periodo" }}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Docente: ${teacher?.fullName?.ifBlank { teacher.email } ?: classroom.teacherId.toString()} · Código ${classroom.classCode}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            if (me?.institutionId != null) {
                item {
                    OnlineScheduleSection(
                        backend = backend,
                        teachers = teachers,
                        classes = classes,
                    )
                }
            }

            item {
                Text(
                    "Privacidad: Dirección trabaja con institución, docentes, grupos, horarios y avisos institucionales. No consulta listas individuales de alumnos, calificaciones, asistencias ni coevaluaciones.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private data class InstitutionalSnapshot(
    val current: UserDto,
    val institution: InstitutionDto?,
    val teachers: List<UserDto>,
    val classes: List<ClassDto>,
    val notices: List<DirectorNoticeDto>,
)
