package com.amarmohan.core.database

import android.util.Log
import com.amarmohan.BuildConfig
import com.amarmohan.domain.model.Student
import com.amarmohan.domain.model.SessionSchedule
import com.amarmohan.domain.model.AttendanceRecord
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface SupabaseApi {
    @POST("storage/v1/object/testpapers/{filename}")
    @Headers("Content-Type: image/jpeg")
    suspend fun uploadTestPaper(
        @Path("filename") filename: String,
        @Body imageBytes: okhttp3.RequestBody
    ): okhttp3.ResponseBody

    @GET("rest/v1/students")
    suspend fun getStudents(
        @Query("select") select: String = "*"
    ): List<Student>

    @GET("rest/v1/students")
    suspend fun getStudentByPhone(
        @Query("parent_phone") phoneEq: String, // format: "eq.101"
        @Query("select") select: String = "*"
    ): List<Student>

    @GET("rest/v1/students")
    suspend fun getStudentBySlNo(
        @Query("sl_no") slNoEq: String, // format: "eq.5"
        @Query("select") select: String = "*"
    ): List<Student>

    @POST("rest/v1/students")
    suspend fun insertStudent(
        @Body student: Student
    ): retrofit2.Response<Unit>

    @PATCH("rest/v1/students")
    suspend fun updateStudent(
        @Query("sl_no") slNoEq: String, // format: "eq.5"
        @Body student: Student
    ): okhttp3.ResponseBody

    @DELETE("rest/v1/students")
    suspend fun deleteStudent(
        @Query("sl_no") slNoEq: String // format: "eq.5"
    ): okhttp3.ResponseBody

    // --- Session Schedules Endpoints ---
    @GET("rest/v1/session_schedules")
    suspend fun getSessionSchedules(
        @Query("select") select: String = "*"
    ): List<SessionSchedule>

    @GET("rest/v1/session_schedules")
    suspend fun getSchedulesForStudent(
        @Query("student_sl_no") studentSlNoEq: String, // format: "eq.101"
        @Query("select") select: String = "*"
    ): List<SessionSchedule>

    @POST("rest/v1/session_schedules")
    suspend fun insertSessionSchedule(
        @Body schedule: SessionSchedule
    ): retrofit2.Response<Unit>

    @PATCH("rest/v1/session_schedules")
    suspend fun updateSessionSchedule(
        @Query("id") idEq: String, // format: "eq.sched_123"
        @Body schedule: SessionSchedule
    ): okhttp3.ResponseBody

    @DELETE("rest/v1/session_schedules")
    suspend fun deleteSessionSchedule(
        @Query("id") idEq: String // format: "eq.sched_123"
    ): okhttp3.ResponseBody

    // --- Attendance Records Endpoints ---
    @GET("rest/v1/attendance_records")
    suspend fun getAttendanceRecords(
        @Query("select") select: String = "*"
    ): List<AttendanceRecord>

    @GET("rest/v1/attendance_records")
    suspend fun getAttendanceForStudent(
        @Query("student_sl_no") studentSlNoEq: String, // format: "eq.101"
        @Query("select") select: String = "*"
    ): List<AttendanceRecord>

    @POST("rest/v1/attendance_records")
    suspend fun insertAttendanceRecord(
        @Body record: AttendanceRecord
    ): retrofit2.Response<Unit>

    @PATCH("rest/v1/attendance_records")
    suspend fun updateAttendanceRecord(
        @Query("id") idEq: String, // format: "eq.att_123"
        @Body record: AttendanceRecord
    ): okhttp3.ResponseBody

    @DELETE("rest/v1/attendance_records")
    suspend fun deleteAttendanceRecord(
        @Query("id") idEq: String // format: "eq.att_123"
    ): okhttp3.ResponseBody
}

object SupabaseProvider {
    private const val TAG = "SupabaseProvider"

    private var customUrl: String? = null
    private var customAnonKey: String? = null
    private var cachedApi: SupabaseApi? = null

    fun init(context: android.content.Context) {
        val prefs = context.getSharedPreferences("TutorPortalPrefs", android.content.Context.MODE_PRIVATE)
        customUrl = prefs.getString("supabase_custom_url", null)
        customAnonKey = prefs.getString("supabase_custom_anon_key", null)
    }

    val supabaseUrl: String
        get() {
            val custom = customUrl?.trim()
            if (!custom.isNullOrEmpty()) return custom
            return try {
                BuildConfig.SUPABASE_URL.trim()
            } catch (e: Exception) {
                ""
            }
        }

    val supabaseAnonKey: String
        get() {
            val custom = customAnonKey?.trim()
            if (!custom.isNullOrEmpty()) return custom
            return try {
                BuildConfig.SUPABASE_ANON_KEY.trim()
            } catch (e: Exception) {
                ""
            }
        }

    val isConfigured: Boolean
        get() {
            val url = supabaseUrl
            val key = supabaseAnonKey
            return url.isNotEmpty() &&
                    key.isNotEmpty() &&
                    !url.contains("MY_SUPABASE_URL") &&
                    !url.contains("your-project.supabase.co") &&
                    !key.contains("your-anon-public-key")
        }

    fun updateConfig(context: android.content.Context, url: String, key: String) {
        val cleanUrl = url.trim()
        val cleanKey = key.trim()
        customUrl = cleanUrl
        customAnonKey = cleanKey
        cachedApi = null
        val prefs = context.getSharedPreferences("TutorPortalPrefs", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString("supabase_custom_url", cleanUrl)
            .putString("supabase_custom_anon_key", cleanKey)
            .apply()
    }

    val api: SupabaseApi?
        get() {
            if (!isConfigured) {
                Log.w(TAG, "Supabase base URL or Anon Key is not properly configured. Operating in offline local mode.")
                return null
            }
            if (cachedApi != null) return cachedApi

            return try {
                val url = supabaseUrl
                val key = supabaseAnonKey
                val baseUrl = if (url.endsWith("/")) url else "$url/"

                val loggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }

                val headerInterceptor = Interceptor { chain ->
                    val original = chain.request()
                    val requestBuilder = original.newBuilder()
                        .header("apikey", key)
                        .header("Authorization", "Bearer $key")
                        .header("Content-Type", "application/json")
                        .header("Prefer", "return=representation")
                    val request = requestBuilder.build()
                    chain.proceed(request)
                }

                val okHttpClient = OkHttpClient.Builder()
                    .addInterceptor(headerInterceptor)
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .build()

                val moshi = Moshi.Builder()
                    .add(Student::class.java, StudentNullSafeAdapter())
                    .add(SessionSchedule::class.java, SessionScheduleNullSafeAdapter())
                    .add(AttendanceRecord::class.java, AttendanceRecordNullSafeAdapter())
                    .addLast(KotlinJsonAdapterFactory())
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()

                val instance = retrofit.create(SupabaseApi::class.java)
                cachedApi = instance
                instance
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Supabase Retrofit client.", e)
                null
            }
        }
}
