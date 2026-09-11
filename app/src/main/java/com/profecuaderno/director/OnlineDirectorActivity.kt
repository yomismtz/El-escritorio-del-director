package com.profecuaderno.director

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.profecuaderno.director.network.CentralBackend
import com.profecuaderno.director.network.ClassDto
import com.profecuaderno.director.network.DirectorNoticeDto
import com.profecuaderno.director.network.InstitutionDto
import com.profecuaderno.director.network.UserDto
import kotlinx.coroutines.launch

class OnlineDirectorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val backend = CentralBackend(this)
        val prefs = getSharedPreferences("director_ui", MODE_PRIVATE)
        DirectorSyncScheduler.ensurePeriodic(this)
        DirectorSyncScheduler.enqueueNow(this)
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val offline = remember { DirectorOfflineStore(context) }
    val cached = remember { offline.loadCore() }
    var me by remember { mutableStateOf(cached?.current) }
    var institution by remember { mutableStateOf(cached?.institution) }
    var teachers by remember { mutableStateOf(cached?.teachers.orEmpty()) }
    var classes by remember { mutableStateOf(cached?.classes.orEmpty()) }
    var notices by remember { mutableStateOf(cached?.notices.orEmpty()) }
    var institutionName by remember { mutableStateOf("") }
    var teacherEmail by remember { mutableStateOf("") }
    var noticeTitle by remember { mutableStateOf("") }
    var noticeBody by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember {
        mutableStateOf(
            if (cached != null) "Mostrando datos guardados; comprobando conexión…"
            else "Conectando con ProfeCuaderno Online…"
        )
    }

    fun refresh() {
        scope.launch {
            loading = true
            offline.flush(backend)
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
                offline.saveCore(
                    DirectorCoreCache(
                        current = snapshot.current,
                        institution = snapshot.institution,
                        teachers = snapshot.teachers,
                        classes = snapshot.classes,
                        notices = snapshot.notices,
                    )
                )
                message = if (offline.pendingCount() == 0) {
                    "Datos institucionales actualizados"
                } else {
                    "Datos actualizados · ${offline.pendingCount()} cambio(s) pendiente(s) de sincronizar"
                }
            }.onFailure { error ->
                message = if (offline.loadCore() != null) {
                    "Sin conexión: puedes seguir consultando los últimos datos guardados. Los cambios pendientes se enviarán al volver internet."
                } else {
                    error.message ?: "No se pudo consultar el servidor"
                }
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    DisposableEffect(Unit) {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                refresh()
            }
        }
        runCatching { connectivity.registerDefaultNetworkCallback(callback) }
        onDispose { runCatching { connectivity.unregisterNetworkCallback(callback) } }
    }

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
                        val pending = offline.pendingCount()
                        if (pending > 0) Text("Cambios guardados sin enviar: $pending", style = MaterialTheme.typography.bodySmall)
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
                                    val name = institutionName.trim()
                                    scope.launch {
                                        loading = true
                                        runCatching { offline.createInstitutionOrQueue(backend, name) }
                                            .onSuccess { (result, created) ->
                                                institutionName = ""
                                                if (result == DirectorOfflineWriteResult.SENT && created != null) {
                                                    message = "Institución creada: ${created.name}"
                                                    refresh()
                                                } else {
                                                    message = "Sin conexión: la creación de la institución quedó guardada y se enviará automáticamente."
                                                }
                                            }
                                            .onFailure { message = it.message ?: "No se pudo guardar la institución" }
                                        loading = false
                                    }
                                },
                                enabled = institutionName.trim().length >= 2 && !loading,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Crear y vincular a mi cuenta") }
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
                                    val email = teacherEmail.trim()
                                    scope.launch {
                                        loading = true
                                        runCatching { offline.attachTeacherOrQueue(backend, email) }
                                            .onSuccess { (result, teacher) ->
                                                teacherEmail = ""
                                                if (result == DirectorOfflineWriteResult.SENT && teacher != null) {
                                                    message = "Docente vinculado: ${teacher.fullName.ifBlank { teacher.email }}"
                                                    refresh()
                                                } else {
                                                    message = "Sin conexión: la vinculación del docente quedó guardada para sincronizarse."
                                                }
                                            }
                                            .onFailure { message = it.message ?: "No se pudo guardar la vinculación" }
                                        loading = false
                                    }
                                },
                                enabled = teacherEmail.contains("@") && !loading,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Vincular docente") }
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
                                    val title = noticeTitle.trim()
                                    val body = noticeBody.trim()
                                    scope.launch {
                                        loading = true
                                        runCatching { offline.createNoticeOrQueue(backend, title, body) }
                                            .onSuccess { (result, _) ->
                                                noticeTitle = ""
                                                noticeBody = ""
                                                if (result == DirectorOfflineWriteResult.SENT) {
                                                    message = "Aviso enviado a los docentes de la institución"
                                                    refresh()
                                                } else {
                                                    message = "Sin conexión: el aviso quedó guardado y se enviará automáticamente."
                                                }
                                            }
                                            .onFailure { message = it.message ?: "No se pudo guardar el aviso" }
                                        loading = false
                                    }
                                },
                                enabled = noticeTitle.isNotBlank() && noticeBody.isNotBlank() && !loading,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Publicar aviso institucional") }
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
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
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
                    Text(if (me?.institutionId == null) "Crea una institución para comenzar." else "Todavía no hay grupos online vinculados a esta institución.")
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
                item { OnlineAttendanceOverview(backend = backend, offline = offline) }
                item {
                    OnlineScheduleSection(
                        backend = backend,
                        offline = offline,
                        teachers = teachers,
                        classes = classes,
                    )
                }
            }

            item {
                Text(
                    "Privacidad: Dirección puede consultar información institucional y un panorama agregado de asistencia por grupo. No recibe nombres, matrículas, calificaciones, historiales individuales de asistencia ni coevaluaciones de estudiantes.",
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
