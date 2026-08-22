package com.amarmohan.data.repository

import android.content.Context
import android.util.Log
import com.amarmohan.domain.model.*
import com.amarmohan.domain.repository.StudentRepository
import com.amarmohan.core.database.SupabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class StudentRepositoryImpl(private val context: Context) : StudentRepository {
    private val TAG = "StudentRepositoryImpl"
    private val prefs = context.getSharedPreferences("TutorPortalPrefs", Context.MODE_PRIVATE)
    
    private val _students = MutableStateFlow<List<Student>>(emptyList())
    override val students: StateFlow<List<Student>> = _students.asStateFlow()

    private val _isSupabaseOnline = MutableStateFlow(SupabaseProvider.isConfigured)
    override val isSupabaseOnline: StateFlow<Boolean> = _isSupabaseOnline.asStateFlow()

    private val _supabaseError = MutableStateFlow<String?>(if (SupabaseProvider.isConfigured) null else "Supabase URL or Key is empty.")
    override val supabaseError: StateFlow<String?> = _supabaseError.asStateFlow()

    init {
        loadLocalStudents()
        // Initialize sample demo credentials if completely empty (helps preview immediately)
        if (_students.value.isEmpty()) {
            seedDemoData()
        }
    }

    private fun loadLocalStudents() {
        val json = prefs.getString("students_cache", "[]") ?: "[]"
        val cached = JsonUtils.jsonToList(json, Student::class.java)
        _students.value = cached
    }

    private fun saveLocalStudents(list: List<Student>) {
        _students.value = list
        val json = JsonUtils.listToJson(list, Student::class.java)
        prefs.edit().putString("students_cache", json).apply()
    }

    private fun seedDemoData() {
        val initialDemo = emptyList<Student>()
        saveLocalStudents(initialDemo)
    }

    override suspend fun refreshFromSupabase(): Boolean = withContext(Dispatchers.IO) {
        val api = SupabaseProvider.api
        if (api == null) {
            _isSupabaseOnline.value = false
            _supabaseError.value = "Supabase API initialization failed. Please check your URL and Anon Key configurations."
            return@withContext false
        }
        try {
            val remoteList = api.getStudents()
            val currentLocal = _students.value
            
            // Build a merged collection to preserve locally created students (like Ward A/B drafts or offline profiles)
            // that are not yet on the remote DB, while updating with remote database changes.
            val mergedMap = currentLocal.associateBy { it.slNo }.toMutableMap()
            remoteList.forEach { remoteStudent ->
                mergedMap[remoteStudent.slNo] = remoteStudent
            }
            
            saveLocalStudents(mergedMap.values.toList())
            _isSupabaseOnline.value = true
            _supabaseError.value = null
            true
        } catch (e: retrofit2.HttpException) {
            val errMsg = when (e.code()) {
                401 -> "401 Unauthorized: Invalid public Anon Key."
                404 -> "404 Not Found: 'students' table does not exist. Did you run the SQL schema in Supabase?"
                400 -> "400 Bad Request: PostgREST relation error or wrong parameters."
                else -> "HTTP Error ${e.code()}: ${e.message()}"
            }
            Log.e(TAG, errMsg, e)
            _supabaseError.value = errMsg
            _isSupabaseOnline.value = false
            false
        } catch (e: java.io.IOException) {
            val errMsg = "Network connection timed out. Please check your internet connection and Supabase endpoint."
            Log.e(TAG, errMsg, e)
            _supabaseError.value = errMsg
            _isSupabaseOnline.value = false
            false
        } catch (e: Exception) {
            val errMsg = "Connection Fail: ${e.localizedMessage ?: "Check table or credentials"}"
            Log.e(TAG, errMsg, e)
            _supabaseError.value = errMsg
            _isSupabaseOnline.value = false
            false
        }
    }

    override suspend fun addStudent(student: Student): Boolean = withContext(Dispatchers.IO) {
        // Enforce update to local cache first
        val current = _students.value.toMutableList()
        current.removeAll { it.slNo == student.slNo }
        current.add(student)
        saveLocalStudents(current)

        val api = SupabaseProvider.api
        if (api != null) {
            try {
                val response = api.insertStudent(student)
                if (response.isSuccessful) {
                    _isSupabaseOnline.value = true
                    _supabaseError.value = null
                    return@withContext true
                } else {
                    val errMsg = "Supabase insert failed. HTTP Code: ${response.code()}"
                    Log.w(TAG, errMsg)
                    _supabaseError.value = errMsg
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error pushing new student to Supabase", e)
                _supabaseError.value = "Insert failed: ${e.localizedMessage ?: "check table access"}"
                _isSupabaseOnline.value = false
            }
        }
        true // Locally succeeded
    }

    override suspend fun updateStudent(student: Student): Boolean = withContext(Dispatchers.IO) {
        val current = _students.value.map {
            if (it.slNo == student.slNo) student else it
        }
        saveLocalStudents(current)

        val api = SupabaseProvider.api
        if (api != null) {
            try {
                val query = "eq.${student.slNo}"
                api.updateStudent(query, student)
                _isSupabaseOnline.value = true
                _supabaseError.value = null
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Error updating student in Supabase", e)
                _supabaseError.value = "Update failed: ${e.localizedMessage ?: "check table schema"}"
                _isSupabaseOnline.value = false
            }
        }
        true
    }

    override suspend fun deleteStudent(slNo: String): Boolean = withContext(Dispatchers.IO) {
        val current = _students.value.filter { it.slNo != slNo }
        saveLocalStudents(current)

        val api = SupabaseProvider.api
        if (api != null) {
            try {
                val query = "eq.$slNo"
                api.deleteStudent(query)
                _isSupabaseOnline.value = true
                _supabaseError.value = null
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting student on Supabase", e)
                _supabaseError.value = "Delete failed: ${e.localizedMessage ?: "check table schema"}"
                _isSupabaseOnline.value = false
            }
        }
        true
    }

    // --- Session Schedules ---
    override suspend fun fetchSessionSchedules(studentSlNo: String): List<SessionSchedule> = withContext(Dispatchers.IO) {
        val api = SupabaseProvider.api ?: return@withContext emptyList()
        try {
            api.getSchedulesForStudent("eq.$studentSlNo")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching session schedules for student $studentSlNo", e)
            emptyList()
        }
    }

    override suspend fun createSessionSchedule(schedule: SessionSchedule): Boolean = withContext(Dispatchers.IO) {
        val api = SupabaseProvider.api ?: return@withContext false
        try {
            val response = api.insertSessionSchedule(schedule)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error creating session schedule", e)
            false
        }
    }

    override suspend fun removeSessionSchedule(id: String): Boolean = withContext(Dispatchers.IO) {
        val api = SupabaseProvider.api ?: return@withContext false
        try {
            api.deleteSessionSchedule("eq.$id")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error removing session schedule $id", e)
            false
        }
    }

    // --- Attendance Records ---
    override suspend fun fetchAttendanceRecords(studentSlNo: String): List<AttendanceRecord> = withContext(Dispatchers.IO) {
        val api = SupabaseProvider.api ?: return@withContext emptyList()
        try {
            api.getAttendanceForStudent("eq.$studentSlNo")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching attendance records for student $studentSlNo", e)
            emptyList()
        }
    }

    override suspend fun createAttendanceRecord(record: AttendanceRecord): Boolean = withContext(Dispatchers.IO) {
        val api = SupabaseProvider.api ?: return@withContext false
        try {
            val response = api.insertAttendanceRecord(record)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error creating attendance record", e)
            false
        }
    }

    override suspend fun removeAttendanceRecord(id: String): Boolean = withContext(Dispatchers.IO) {
        val api = SupabaseProvider.api ?: return@withContext false
        try {
            api.deleteAttendanceRecord("eq.$id")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error removing attendance record $id", e)
            false
        }
    }

    override fun getStudentBySlNo(slNo: String): Student? {
        return _students.value.firstOrNull { it.slNo == slNo }
    }

    override fun getStudentByVerificationPhone(phone: String): Student? {
        val cleanPhone = phone.trim().filter { it.isDigit() }
        return _students.value.firstOrNull {
            val dbPhone = it.parentPhone.trim().filter { c -> c.isDigit() }
            dbPhone.contains(cleanPhone) || cleanPhone.contains(dbPhone)
        }
    }
}
