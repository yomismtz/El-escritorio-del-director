package com.profecuaderno.director

object EcosystemLinkContract {
    const val SCHEMA_VERSION = 1

    enum class Audience { TEACHER_ONLY, CLASS_STUDENTS, TEACHER_AND_CLASS_STUDENTS }
    enum class SenderRole { DIRECTOR, TEACHER }

    data class TeacherChannel(
        val teacherId: String,
        val institutionId: String,
        val academicCycleId: String
    )

    data class ClassLink(
        val classId: String,
        val classCode: String,
        val teacherId: String,
        val groupLabel: String,
        val subjectLabel: String,
        val academicCycleId: String
    )

    data class Announcement(
        val announcementId: String,
        val senderRole: SenderRole,
        val senderId: String,
        val title: String,
        val message: String,
        val audience: Audience,
        val teacherIds: Set<String> = emptySet(),
        val classIds: Set<String> = emptySet(),
        val createdAt: Long,
        val expiresAt: Long? = null
    )

    fun directorAnnouncementForTeachers(
        id: String,
        directorId: String,
        title: String,
        message: String,
        teacherIds: Set<String>,
        createdAt: Long
    ) = Announcement(
        announcementId = id,
        senderRole = SenderRole.DIRECTOR,
        senderId = directorId,
        title = title,
        message = message,
        audience = Audience.TEACHER_ONLY,
        teacherIds = teacherIds,
        createdAt = createdAt
    )
}
