package com.profecuaderno.director.data

data class Institution(
    val localId: Long,
    val syncId: String,
    val name: String,
    val schoolYear: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String = "LOCAL_ONLY"
)

data class TeacherProfile(
    val localId: Long,
    val syncId: String,
    val institutionId: String,
    val name: String,
    val degree: String,
    val subjects: List<String>,
    val contractedHours: Int,
    val assignedHours: Int,
    val availableHours: Int,
    val active: Boolean = true
)

data class SchoolGroup(
    val localId: Long,
    val syncId: String,
    val institutionId: String,
    val schoolYearId: String,
    val name: String,
    val level: String,
    val studentCount: Int,
    val tutorTeacherId: String? = null
)

data class Subject(
    val localId: Long,
    val syncId: String,
    val institutionId: String,
    val name: String,
    val code: String,
    val weeklyModules: Int,
    val moduleMinutes: Int
)

data class TeacherAvailability(
    val localId: Long,
    val syncId: String,
    val institutionId: String,
    val teacherId: String,
    val dayOfWeek: Int,
    val startMinutes: Int,
    val endMinutes: Int
)

data class TeachingRequirement(
    val localId: Long,
    val syncId: String,
    val institutionId: String,
    val schoolYearId: String,
    val groupId: String,
    val subjectId: String,
    val weeklyModules: Int,
    val moduleMinutes: Int
)

data class ScheduleBlock(
    val localId: Long,
    val syncId: String,
    val institutionId: String,
    val groupId: String,
    val teacherId: String,
    val subjectId: String,
    val dayOfWeek: Int,
    val startMinutes: Int,
    val endMinutes: Int
)

enum class NoticeAudience {
    TEACHERS,
    STUDENTS,
    TEACHERS_AND_STUDENTS
}

data class InstitutionalNotice(
    val localId: Long,
    val syncId: String,
    val institutionId: String,
    val schoolYearId: String,
    val title: String,
    val body: String,
    val audience: NoticeAudience,
    val publishedAt: Long,
    val expiresAt: Long? = null,
    val syncStatus: String = "LOCAL_ONLY"
)
