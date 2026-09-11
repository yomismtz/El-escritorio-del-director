package com.profecuaderno.director

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.profecuaderno.director.network.CentralBackend
import com.profecuaderno.director.network.ClassDto
import com.profecuaderno.director.network.ScheduleDto
import com.profecuaderno.director.network.ScheduleRequest
import com.profecuaderno.director.network.UserDto
import kotlinx.coroutines.launch

private val weekdayNames = mapOf(
    1 to "Lunes",
    2 to "Martes",
    3 to "Miércoles",
    4 to "Jueves",
    5 to "Viernes",
    6 to "Sábado",
    7 to "Domingo",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineScheduleSection(
    backend: CentralBackend,
    teachers: List<UserDto>,
    classes: List<ClassDto>,
) {
    val scope = rememberCoroutineScope()
    var schedule by remember { mutableStateOf<List<ScheduleDto>>(emptyList()) }
    var selectedTeacherId by remember { mutableStateOf<Int?>(null) }
    var selectedClassId by remember { mutableStateOf<Int?>(null) }
    var selectedWeekday by remember { mutableIntStateOf(1) }
    var startTime by remember { mutableStateOf("07:00") }
    var endTime by remember { mutableStateOf("07:50") }
    var room by remember { mutableStateOf("") }
    var teacherMenu by remember { mutableStateOf(false) }
    var classMenu by remember { mutableStateOf(false) }
    var dayMenu by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val selectedTeacher = teachers.firstOrNull { it.id == selectedTeacherId }
    val teacherClasses = classes.filter { selectedTeacherId == null || it.teacherId == selectedTeacherId }
    val selectedClass = classes.firstOrNull { it.id == selectedClassId }

    fun refreshSchedule() {
        scope.launch {
            loading = true
            runCatching { backend.api.schedule() }
                .onSuccess {
                    schedule = it.sortedWith(compareBy<ScheduleDto> { row -> row.weekday }.thenBy { row -> row.startTime })
                    message = null
                }
                .onFailure { message = it.message ?: "No se pudo consultar el horario" }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refreshSchedule() }
    LaunchedEffect(teachers, classes) {
        if (selectedTeacherId != null && teachers.none { it.id == selectedTeacherId }) {
            selectedTeacherId = null
            selectedClassId = null
        }
        if (selectedClassId != null && classes.none { it.id == selectedClassId }) {
            selectedClassId = null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Horario institucional online", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = ::refreshSchedule, enabled = !loading) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualizar horario")
            }
        }
        Text(
            "Los módulos guardados aquí quedan en el servidor. El backend impide choques del mismo docente y de la misma aula.",
            style = MaterialTheme.typography.bodySmall,
        )

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Agregar módulo", fontWeight = FontWeight.Bold)

                ExposedDropdownMenuBox(
                    expanded = teacherMenu,
                    onExpandedChange = { teacherMenu = !teacherMenu },
                ) {
                    OutlinedTextField(
                        value = selectedTeacher?.let { it.fullName.ifBlank { it.email } }.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Docente") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teacherMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = teacherMenu, onDismissRequest = { teacherMenu = false }) {
                        teachers.forEach { teacher ->
                            DropdownMenuItem(
                                text = { Text(teacher.fullName.ifBlank { teacher.email }) },
                                onClick = {
                                    selectedTeacherId = teacher.id
                                    if (selectedClass?.teacherId != teacher.id) selectedClassId = null
                                    teacherMenu = false
                                },
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = classMenu,
                    onExpandedChange = { if (selectedTeacherId != null) classMenu = !classMenu },
                ) {
                    OutlinedTextField(
                        value = selectedClass?.let { "${it.name} · ${it.subject.ifBlank { it.name }}" }.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        enabled = selectedTeacherId != null,
                        label = { Text("Grupo / clase") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = classMenu, onDismissRequest = { classMenu = false }) {
                        teacherClasses.forEach { classroom ->
                            DropdownMenuItem(
                                text = { Text("${classroom.name} · ${classroom.subject.ifBlank { classroom.name }}") },
                                onClick = {
                                    selectedClassId = classroom.id
                                    classMenu = false
                                },
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = dayMenu,
                    onExpandedChange = { dayMenu = !dayMenu },
                ) {
                    OutlinedTextField(
                        value = weekdayNames[selectedWeekday].orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Día") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = dayMenu, onDismissRequest = { dayMenu = false }) {
                        weekdayNames.forEach { (day, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedWeekday = day
                                    dayMenu = false
                                },
                            )
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it.take(5) },
                        label = { Text("Inicio HH:MM") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it.take(5) },
                        label = { Text("Fin HH:MM") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it.take(120) },
                    label = { Text("Aula (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                val validTime = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")
                val canSave = selectedTeacherId != null && selectedClassId != null &&
                    validTime.matches(startTime) && validTime.matches(endTime) && endTime > startTime && !loading

                Button(
                    onClick = {
                        val teacherId = selectedTeacherId ?: return@Button
                        val classId = selectedClassId ?: return@Button
                        scope.launch {
                            loading = true
                            runCatching {
                                backend.api.createSchedule(
                                    ScheduleRequest(
                                        teacherId = teacherId,
                                        classId = classId,
                                        weekday = selectedWeekday,
                                        startTime = startTime,
                                        endTime = endTime,
                                        room = room.trim(),
                                    )
                                )
                            }.onSuccess {
                                message = "Módulo guardado en el horario online"
                                room = ""
                                schedule = backend.api.schedule().sortedWith(
                                    compareBy<ScheduleDto> { row -> row.weekday }.thenBy { row -> row.startTime }
                                )
                            }.onFailure {
                                message = it.message ?: "No se pudo guardar el módulo; revisa si hay un choque de horario"
                            }
                            loading = false
                        }
                    },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Guardar módulo")
                }
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }

        Text("Horario guardado (${schedule.size})", fontWeight = FontWeight.Bold)
        if (schedule.isEmpty()) {
            Text("Todavía no hay módulos en el horario online.")
        } else {
            schedule.forEach { row ->
                val teacher = teachers.firstOrNull { it.id == row.teacherId }
                val classroom = classes.firstOrNull { it.id == row.classId }
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            "${weekdayNames[row.weekday] ?: "Día ${row.weekday}"} · ${row.startTime}–${row.endTime}",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(classroom?.let { "${it.name} · ${it.subject.ifBlank { it.name }}" } ?: "Clase no vinculada")
                        Text(
                            "Docente: ${teacher?.fullName?.ifBlank { teacher.email } ?: row.teacherId.toString()}" +
                                if (row.room.isBlank()) "" else " · Aula ${row.room}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
