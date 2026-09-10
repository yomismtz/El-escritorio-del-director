package com.profecuaderno.director.data

/** Shared contract for El Cuaderno ecosystem. Backend rules must enforce these permissions. */
object EcosystemContract {
    const val PROTOCOL_VERSION = 2
    const val TEACHER_APP_ID = "com.profecuaderno.app"
    const val STUDENT_APP_ID = "com.profecuaderno.student"
    const val DIRECTOR_APP_ID = "com.profecuaderno.director"

    enum class Role { TEACHER, STUDENT, DIRECTOR }
    enum class NoticeSource { DIRECTOR, TEACHER }
    enum class NoticeTarget { TEACHERS, STUDENTS, TEACHERS_AND_STUDENTS, GROUP }

    object DirectorCapabilities {
        const val READ_TEACHERS = true
        const val MANAGE_TEACHER_HOURS = true
        const val MANAGE_TEACHER_AVAILABILITY = true
        const val MANAGE_GROUP_STRUCTURE = true
        const val MANAGE_SUBJECT_REQUIREMENTS = true
        const val MANAGE_SCHEDULES = true
        const val PUBLISH_INSTITUTIONAL_NOTICES = true

        const val READ_INDIVIDUAL_STUDENTS = false
        const val READ_STUDENT_GRADES = false
        const val READ_STUDENT_ATTENDANCE = false
        const val EDIT_STUDENT_GRADES = false
        const val EDIT_STUDENT_ATTENDANCE = false
    }
}
