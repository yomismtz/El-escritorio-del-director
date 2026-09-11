package com.profecuaderno.director.network

import android.content.Context
import com.google.gson.annotations.SerializedName
import com.profecuaderno.director.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

class AuthTokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("central_auth", Context.MODE_PRIVATE)
    var accessToken: String?
        get() = prefs.getString("access_token", null)
        set(value) { prefs.edit().apply { if (value.isNullOrBlank()) remove("access_token") else putString("access_token", value) }.apply() }
    fun clear() { prefs.edit().clear().apply() }
}

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val email: String, val password: String, @SerializedName("full_name") val fullName: String, val role: String = "director")
data class UserDto(val id: Int, val email: String, @SerializedName("full_name") val fullName: String, val role: String, @SerializedName("institution_id") val institutionId: Int?)
data class TokenResponse(@SerializedName("access_token") val accessToken: String, @SerializedName("token_type") val tokenType: String = "bearer", val user: UserDto)
data class InstitutionRequest(val name: String)
data class InstitutionDto(val id: Int, val name: String)
data class NoticeRequest(val title: String, val body: String)

data class DirectorNoticeDto(
    val id: Int,
    val title: String,
    val body: String,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
)

data class ClassDto(
    val id: Int,
    val name: String,
    val subject: String,
    @SerializedName("period_name") val periodName: String,
    @SerializedName("class_code") val classCode: String,
    @SerializedName("teacher_id") val teacherId: Int,
    @SerializedName("institution_id") val institutionId: Int?,
    val active: Boolean
)

data class ScheduleRequest(
    @SerializedName("teacher_id") val teacherId: Int,
    @SerializedName("class_id") val classId: Int?,
    val weekday: Int,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    val room: String = "",
)

data class ScheduleDto(
    val id: Int,
    @SerializedName("teacher_id") val teacherId: Int,
    @SerializedName("class_id") val classId: Int?,
    val weekday: Int,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    val room: String,
)

data class InstitutionalAttendanceSummaryDto(
    @SerializedName("class_id") val classId: Int,
    @SerializedName("class_name") val className: String,
    val subject: String,
    @SerializedName("teacher_id") val teacherId: Int,
    @SerializedName("enrolled_students") val enrolledStudents: Int,
    @SerializedName("attendance_percent") val attendancePercent: Double,
    val records: Int,
    val counts: Map<String, Int>,
    @SerializedName("effective_absences") val effectiveAbsences: Int,
)

interface DirectorCentralApi {
    @GET("health") suspend fun health(): Map<String, String>
    @POST("auth/login") suspend fun login(@Body request: LoginRequest): TokenResponse
    @POST("auth/register") suspend fun register(@Body request: RegisterRequest): TokenResponse
    @GET("me") suspend fun me(): UserDto
    @GET("institution") suspend fun institution(): InstitutionDto
    @POST("institutions") suspend fun createInstitution(@Body request: InstitutionRequest): InstitutionDto
    @POST("institutions/attach-teacher") suspend fun attachTeacher(@Query("email") email: String): UserDto
    @GET("institutions/teachers") suspend fun teachers(): List<UserDto>
    @POST("institutions/notices") suspend fun createDirectorNotice(@Body request: NoticeRequest): DirectorNoticeDto
    @GET("institutions/notices") suspend fun directorNotices(): List<DirectorNoticeDto>
    @GET("institutions/attendance-summary") suspend fun attendanceSummary(): List<InstitutionalAttendanceSummaryDto>
    @GET("classes") suspend fun classes(): List<ClassDto>
    @POST("schedule") suspend fun createSchedule(@Body request: ScheduleRequest): ScheduleDto
    @GET("schedule") suspend fun schedule(): List<ScheduleDto>
    @DELETE("schedule/{scheduleId}") suspend fun deleteSchedule(@Path("scheduleId") scheduleId: Int): Map<String, String>
}

class CentralBackend(context: Context) {
    val tokenStore = AuthTokenStore(context.applicationContext)
    private val authInterceptor = Interceptor { chain ->
        val token = tokenStore.accessToken
        val request = if (token.isNullOrBlank()) chain.request() else chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        chain.proceed(request)
    }
    private val client = OkHttpClient.Builder().addInterceptor(authInterceptor).build()
    val api: DirectorCentralApi = Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL).client(client).addConverterFactory(GsonConverterFactory.create()).build().create(DirectorCentralApi::class.java)
    val isConfigured: Boolean get() = !BuildConfig.API_BASE_URL.contains("example.invalid")
    fun saveSession(response: TokenResponse) { tokenStore.accessToken = response.accessToken }
    fun signOut() = tokenStore.clear()
}
