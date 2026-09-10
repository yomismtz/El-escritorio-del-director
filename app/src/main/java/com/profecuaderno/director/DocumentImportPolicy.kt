package com.profecuaderno.director

object DocumentImportPolicy {
    val mimeTypes = arrayOf(
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

    val supportedExtensions = setOf("pdf", "csv", "xls", "xlsx", "doc", "docx", "txt")

    fun isSupported(fileName: String?): Boolean =
        fileName?.substringAfterLast('.', "")?.lowercase().orEmpty() in supportedExtensions
}
