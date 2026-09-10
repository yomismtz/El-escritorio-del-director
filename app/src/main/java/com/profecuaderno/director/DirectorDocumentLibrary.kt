package com.profecuaderno.director

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val directorDocumentMimeTypes = arrayOf(
    "application/pdf",
    "text/csv",
    "text/comma-separated-values",
    "application/csv",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "text/plain"
)

@Composable
fun DirectorDocumentsCard() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("director_documents", 0) }
    var uriString by remember { mutableStateOf(prefs.getString("last_document_uri", null)) }
    var message by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            message = "No se seleccionó ningún documento."
        } else {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val readable = runCatching { context.contentResolver.openInputStream(uri)?.use { it.read() } != null }.getOrDefault(false)
            if (readable) {
                uriString = uri.toString()
                prefs.edit().putString("last_document_uri", uri.toString()).apply()
                message = "Documento institucional guardado en este dispositivo."
            } else {
                message = "No se pudo leer el archivo seleccionado."
            }
        }
    }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Description, contentDescription = null)
                Text("Documentos institucionales", style = MaterialTheme.typography.titleMedium)
            }
            Text("Admite PDF, CSV, XLS, XLSX, DOC, DOCX y TXT. Estos archivos no exponen alumnos individuales ni calificaciones.")
            Button(onClick = { picker.launch(directorDocumentMimeTypes) }) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (uriString == null) "Importar documento" else "Cambiar documento")
            }
            if (uriString != null) {
                OutlinedButton(onClick = {
                    val uri = Uri.parse(uriString)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching { context.startActivity(intent) }
                        .onFailure { message = "No hay una aplicación compatible para abrir este documento." }
                }) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Abrir documento")
                }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
