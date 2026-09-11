package com.profecuaderno.director

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.profecuaderno.director.network.CentralBackend
import com.profecuaderno.director.network.InstitutionalAttendanceSummaryDto
import kotlinx.coroutines.launch

@Composable
fun OnlineAttendanceOverview(
    backend: CentralBackend,
    offline: DirectorOfflineStore,
) {
    val scope = rememberCoroutineScope()
    var rows by remember {
        mutableStateOf<List<InstitutionalAttendanceSummaryDto>>(
            offline.loadAttendance().sortedBy { it.className.lowercase() }
        )
    }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(if (rows.isNotEmpty()) "Datos guardados disponibles sin conexión" else null) }

    fun refresh() {
        scope.launch {
            loading = true
            runCatching { backend.api.attendanceSummary() }
                .onSuccess {
                    rows = it.sortedBy { row -> row.className.lowercase() }
                    offline.saveAttendance(rows)
                    message = null
                }
                .onFailure {
                    message = if (rows.isNotEmpty()) {
                        "Sin conexión: se muestra el último panorama guardado."
                    } else {
                        it.message ?: "No se pudo consultar el panorama de asistencia"
                    }
                }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FactCheck, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Panorama de asistencia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = ::refresh, enabled = !loading) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar asistencia")
                }
            }
            Text(
                "Vista institucional agregada por grupo. No muestra nombres, matrículas ni historiales individuales de estudiantes.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            if (!loading && rows.isEmpty() && message == null) {
                Text("Todavía no hay asistencias sincronizadas.")
            }

            rows.forEach { row ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(row.className, fontWeight = FontWeight.SemiBold)
                        if (row.subject.isNotBlank()) Text(row.subject, style = MaterialTheme.typography.bodySmall)
                        Text("Asistencia global: ${"%.1f".format(row.attendancePercent)}%")
                        Text("Alumnos inscritos: ${row.enrolledStudents} · Registros: ${row.records}", style = MaterialTheme.typography.bodySmall)
                        val p = row.counts["present"] ?: 0
                        val f = row.counts["absent"] ?: 0
                        val r = row.counts["late"] ?: 0
                        val j = row.counts["justified"] ?: 0
                        Text("P $p · F $f · R $r · J $j", style = MaterialTheme.typography.bodySmall)
                        if (row.effectiveAbsences > 0) {
                            Text("Faltas efectivas según regla: ${row.effectiveAbsences}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
