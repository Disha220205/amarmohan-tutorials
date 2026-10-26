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
        SupabaseProvider.init(context)
        loadLocalStudents()
        // Initialize sample demo credentials if completely empty (helps preview immediately)
        if (_students.value.isEmpty()) {
            seedDemoData()
        }
    }

    override fun updateSupabaseConfig(url: String, key: String) {
        SupabaseProvider.updateConfig(context, url, key)
        isSessionSchedulesTableAvailable = true
        isAttendanceRecordsTableAvailable = true
        _isSupabaseOnline.value = SupabaseProvider.isConfigured
        _supabaseError.value = if (SupabaseProvider.isConfigured) null else "Supabase URL or Key is invalid."
    }

    private var isSessionSchedulesTableAvailable: Boolean = true
    private var isAttendanceRecordsTableAvailable: Boolean = true

    private fun loadLocalSessionSchedules(): List<SessionSchedule> {
        val json = prefs.getString("session_schedules_cache", null) ?: return emptyList()
        return try {
            JsonUtils.jsonToList(json, SessionSchedule::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveLocalSessionSchedules(list: List<SessionSchedule>) {
        val json = JsonUtils.listToJson(list, SessionSchedule::class.java)
        prefs.edit().putString("session_schedules_cache", json).apply()
    }

    private fun loadLocalAttendanceRecords(): List<AttendanceRecord> {
        val json = prefs.getString("attendance_records_cache", null) ?: return emptyList()
        return try {
            JsonUtils.jsonToList(json, AttendanceRecord::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveLocalAttendanceRecords(list: List<AttendanceRecord>) {
        val json = JsonUtils.listToJson(list, AttendanceRecord::class.java)
        prefs.edit().putString("attendance_records_cache", json).apply()
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
        // Ensure local cached data is loaded into memory
        loadLocalStudents()

        val api = SupabaseProvider.api
        if (api == null) {
            _isSupabaseOnline.value = false
            _supabaseError.value = "Supabase API initialization failed. Operating in offline local mode."
            return@withContext false
        }
        try {
            val remoteList = api.getStudents()
            val currentLocal = _students.value

            val remoteIds = remoteList.map { it.slNo }.toSet()
            val syncedIds = prefs.getStringSet("synced_student_ids", emptySet())?.toMutableSet() ?: mutableSetOf()

            // Any student that was previously on Supabase (syncedIds contains it) but is not in remoteList (remoteIds does not contain it)
            // is considered deleted on the server. So we remove them locally.
            val toRemove = currentLocal.filter { localStudent ->
                !remoteIds.contains(localStudent.slNo) && syncedIds.contains(localStudent.slNo)
            }.map { it.slNo }.toSet()

            // Build a merged collection to preserve locally created students
            // that are not yet on the remote DB, while updating with remote database changes.
            val mergedMap = currentLocal.filter { !toRemove.contains(it.slNo) }.associateBy { it.slNo }.toMutableMap()
            remoteList.forEach { remoteStudent ->
                mergedMap[remoteStudent.slNo] = remoteStudent
            }

            saveLocalStudents(mergedMap.values.toList())

            // Sync any local students that aren't yet on Supabase
            val unsyncedStudents = mergedMap.values.filter { !syncedIds.contains(it.slNo) && !remoteIds.contains(it.slNo) }
            for (unsynced in unsyncedStudents) {
                try {
                    val resp = api.insertStudent(unsynced)
                    if (resp.isSuccessful) {
                        syncedIds.add(unsynced.slNo)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Syncing unsynced local student ${unsynced.slNo} deferred: ${e.message}")
                }
            }

            // Update the set of synced student IDs
            val updatedSyncedIds = (syncedIds + remoteIds - toRemove).toSet()
            prefs.edit().putStringSet("synced_student_ids", updatedSyncedIds).apply()
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
            Log.w(TAG, "$errMsg: ${e.message()}")
            _supabaseError.value = errMsg
            _isSupabaseOnline.value = false
            loadLocalStudents()
            false
        } catch (e: java.io.IOException) {
            val errMsg = if (e is java.net.SocketTimeoutException) {
                "Network connection timed out. Supabase is in offline cache mode."
            } else if (e is java.net.UnknownHostException) {
                "Unable to resolve Supabase server. Supabase is in offline cache mode."
            } else {
                "Network unavailable (${e.message ?: "offline"}). Supabase is in offline cache mode."
            }
            Log.w(TAG, "$errMsg: ${e.message}")
            _supabaseError.value = errMsg
            _isSupabaseOnline.value = false
            loadLocalStudents()
            false
        } catch (e: Exception) {
            val errMsg = "Supabase offline: ${e.localizedMessage ?: "Offline cache active"}"
            Log.w(TAG, "$errMsg: ${e.message}")
            _supabaseError.value = errMsg
            _isSupabaseOnline.value = false
            loadLocalStudents()
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
        if (api != null && _isSupabaseOnline.value) {
            try {
                val response = api.insertStudent(student)
                if (response.isSuccessful) {
                    val syncedIds = prefs.getStringSet("synced_student_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
                    syncedIds.add(student.slNo)
                    prefs.edit().putStringSet("synced_student_ids", syncedIds).apply()

                    _isSupabaseOnline.value = true
                    _supabaseError.value = null
                    return@withContext true
                } else {
                    val errMsg = "Supabase insert failed. HTTP Code: ${response.code()}"
                    Log.w(TAG, errMsg)
                    _supabaseError.value = errMsg
                }
            } catch (e: Exception) {
                Log.w(TAG, "Student saved locally, remote push deferred: ${e.message}")
                _supabaseError.value = "Saved locally. Supabase offline (${e.localizedMessage ?: "network issue"})"
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
        if (api != null && _isSupabaseOnline.value) {
            try {
                val query = "eq.${student.slNo}"
                api.updateStudent(query, student)
                _isSupabaseOnline.value = true
                _supabaseError.value = null
                return@withContext true
            } catch (e: Exception) {
                Log.w(TAG, "Student updated locally, remote sync deferred: ${e.message}")
                _supabaseError.value = "Updated locally. Supabase offline (${e.localizedMessage ?: "network issue"})"
                _isSupabaseOnline.value = false
            }
        }
        true
    }

    override suspend fun deleteStudent(slNo: String): Boolean = withContext(Dispatchers.IO) {
        val current = _students.value.filter { it.slNo != slNo }
        saveLocalStudents(current)

        val syncedIds = prefs.getStringSet("synced_student_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        syncedIds.remove(slNo)
        prefs.edit().putStringSet("synced_student_ids", syncedIds).apply()

        val api = SupabaseProvider.api
        if (api != null && _isSupabaseOnline.value) {
            try {
                val query = "eq.$slNo"
                api.deleteStudent(query)
                _isSupabaseOnline.value = true
                _supabaseError.value = null
                return@withContext true
            } catch (e: Exception) {
                Log.w(TAG, "Student deleted locally, remote sync deferred: ${e.message}")
                _supabaseError.value = "Deleted locally. Supabase offline (${e.localizedMessage ?: "network issue"})"
                _isSupabaseOnline.value = false
            }
        }
        true
    }

    // --- Session Schedules ---
    override suspend fun fetchAllSessionSchedules(): List<SessionSchedule> = withContext(Dispatchers.IO) {
        val localSchedules = loadLocalSessionSchedules()
        val api = SupabaseProvider.api
        if (api == null || !isSessionSchedulesTableAvailable || !_isSupabaseOnline.value) {
            return@withContext localSchedules
        }
        try {
            val remoteList = api.getSessionSchedules()
            val remoteMap = remoteList.associateBy { it.id }.toMutableMap()
            localSchedules.forEach { local ->
                if (!remoteMap.containsKey(local.id)) {
                    remoteMap[local.id] = local
                }
            }
            val merged = remoteMap.values.toList()
            saveLocalSessionSchedules(merged)
            merged
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 404 || e.code() == 400) {
                isSessionSchedulesTableAvailable = false
                Log.w(TAG, "Supabase session_schedules table not available (${e.code()}). Using local cache.")
            } else {
                Log.w(TAG, "HttpException while fetching session schedules: ${e.code()}. Using local cache.")
            }
            localSchedules
        } catch (e: Exception) {
            Log.w(TAG, "Unable to fetch remote session schedules: ${e.message}. Using local cache.")
            localSchedules
        }
    }

    override suspend fun fetchSessionSchedules(studentSlNo: String): List<SessionSchedule> = withContext(Dispatchers.IO) {
        val local = loadLocalSessionSchedules().filter { it.studentSlNo == studentSlNo }
        val api = SupabaseProvider.api
        if (api == null || !isSessionSchedulesTableAvailable || !_isSupabaseOnline.value) {
            return@withContext local
        }
        try {
            val remote = api.getSchedulesForStudent("eq.$studentSlNo")
            val combined = (local.filter { loc -> remote.none { it.id == loc.id } } + remote)
            combined
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 404 || e.code() == 400) {
                isSessionSchedulesTableAvailable = false
            }
            Log.w(TAG, "Supabase session_schedules table not available (${e.code()}). Using local.")
            local
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching schedules for student $studentSlNo: ${e.message}. Using local.")
            local
        }
    }

    override suspend fun createSessionSchedule(schedule: SessionSchedule): Boolean = withContext(Dispatchers.IO) {
        val local = loadLocalSessionSchedules().filter { it.id != schedule.id } + schedule
        saveLocalSessionSchedules(local)
        val api = SupabaseProvider.api
        if (api == null || !isSessionSchedulesTableAvailable || !_isSupabaseOnline.value) {
            return@withContext true
        }
        try {
            val response = api.insertSessionSchedule(schedule)
            if (!response.isSuccessful && (response.code() == 404 || response.code() == 400)) {
                isSessionSchedulesTableAvailable = false
            }
            true
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 404 || e.code() == 400) {
                isSessionSchedulesTableAvailable = false
            }
            Log.w(TAG, "Remote insertSessionSchedule failed (${e.code()}), safely saved in local storage.")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Remote insertSessionSchedule exception: ${e.message}, safely saved in local storage.")
            true
        }
    }

    override suspend fun removeSessionSchedule(id: String): Boolean = withContext(Dispatchers.IO) {
        val local = loadLocalSessionSchedules().filter { it.id != id }
        saveLocalSessionSchedules(local)
        val api = SupabaseProvider.api
        if (api == null || !isSessionSchedulesTableAvailable || !_isSupabaseOnline.value) {
            return@withContext true
        }
        try {
            api.deleteSessionSchedule("eq.$id")
            true
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 404 || e.code() == 400) {
                isSessionSchedulesTableAvailable = false
            }
            Log.w(TAG, "Remote deleteSessionSchedule failed (${e.code()}), removed from local storage.")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Remote deleteSessionSchedule exception: ${e.message}, removed from local storage.")
            true
        }
    }

    // --- Attendance Records ---
    override suspend fun fetchAllAttendanceRecords(): List<AttendanceRecord> = withContext(Dispatchers.IO) {
        val localRecords = loadLocalAttendanceRecords()
        val api = SupabaseProvider.api
        if (api == null || !isAttendanceRecordsTableAvailable || !_isSupabaseOnline.value) {
            return@withContext localRecords
        }
        try {
            val remoteList = api.getAttendanceRecords()
            val remoteMap = remoteList.associateBy { it.id }.toMutableMap()
            localRecords.forEach { local ->
                if (!remoteMap.containsKey(local.id)) {
                    remoteMap[local.id] = local
                }
            }
            val merged = remoteMap.values.toList()
            saveLocalAttendanceRecords(merged)
            merged
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 404 || e.code() == 400) {
                isAttendanceRecordsTableAvailable = false
                Log.w(TAG, "Supabase attendance_records table not available (${e.code()}). Using local cache.")
            } else {
                Log.w(TAG, "HttpException while fetching attendance records: ${e.code()}. Using local cache.")
            }
            localRecords
        } catch (e: Exception) {
            Log.w(TAG, "Unable to fetch remote attendance records: ${e.message}. Using local cache.")
            localRecords
        }
    }

    override suspend fun fetchAttendanceRecords(studentSlNo: String): List<AttendanceRecord> = withContext(Dispatchers.IO) {
        val local = loadLocalAttendanceRecords().filter { it.studentSlNo == studentSlNo }
        val api = SupabaseProvider.api
        if (api == null || !isAttendanceRecordsTableAvailable || !_isSupabaseOnline.value) {
            return@withContext local
        }
        try {
            val remote = api.getAttendanceForStudent("eq.$studentSlNo")
            val combined = (local.filter { loc -> remote.none { it.id == loc.id } } + remote)
            combined
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 404 || e.code() == 400) {
                isAttendanceRecordsTableAvailable = false
            }
            Log.w(TAG, "Supabase attendance_records table not available (${e.code()}). Using local.")
            local
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching attendance records for student $studentSlNo: ${e.message}. Using local.")
            local
        }
    }

    override suspend fun createAttendanceRecord(record: AttendanceRecord): Boolean = withContext(Dispatchers.IO) {
        val local = loadLocalAttendanceRecords().filter { it.id != record.id } + record
        saveLocalAttendanceRecords(local)
        val api = SupabaseProvider.api
        if (api == null || !isAttendanceRecordsTableAvailable || !_isSupabaseOnline.value) {
            return@withContext true
        }
        try {
            val response = api.insertAttendanceRecord(record)
            if (!response.isSuccessful && (response.code() == 404 || response.code() == 400)) {
                isAttendanceRecordsTableAvailable = false
            }
            true
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 404 || e.code() == 400) {
                isAttendanceRecordsTableAvailable = false
            }
            Log.w(TAG, "Remote insertAttendanceRecord failed (${e.code()}), safely saved in local storage.")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Remote insertAttendanceRecord exception: ${e.message}, safely saved in local storage.")
            true
        }
    }

    override suspend fun removeAttendanceRecord(id: String): Boolean = withContext(Dispatchers.IO) {
        val local = loadLocalAttendanceRecords().filter { it.id != id }
        saveLocalAttendanceRecords(local)
        val api = SupabaseProvider.api
        if (api == null || !isAttendanceRecordsTableAvailable || !_isSupabaseOnline.value) {
            return@withContext true
        }
        try {
            api.deleteAttendanceRecord("eq.$id")
            true
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 404 || e.code() == 400) {
                isAttendanceRecordsTableAvailable = false
            }
            Log.w(TAG, "Remote deleteAttendanceRecord failed (${e.code()}), removed from local storage.")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Remote deleteAttendanceRecord exception: ${e.message}, removed from local storage.")
            true
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
