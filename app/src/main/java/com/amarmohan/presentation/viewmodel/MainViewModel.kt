package com.amarmohan.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amarmohan.domain.model.*
import com.amarmohan.domain.repository.StudentRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface UserSession {
    object Guest : UserSession
    object Tutor : UserSession
    data class Parent(val studentSlNo: String) : UserSession
}

class MainViewModel(private val repository: StudentRepository, private val context: Context) : ViewModel() {
    private val TAG = "MainViewModel"
    private val prefs = context.getSharedPreferences("tutorsync_prefs", Context.MODE_PRIVATE)

    private val _session = MutableStateFlow<UserSession>(UserSession.Guest)
    val session: StateFlow<UserSession> = _session.asStateFlow()

    private val _isTutorTestMode = MutableStateFlow(prefs.getBoolean("is_tutor_test_mode", false))
    val isTutorTestMode: StateFlow<Boolean> = _isTutorTestMode.asStateFlow()

    private fun createInitialTestStudent(): Student {
        return Student(
            slNo = "SL_TEST_01",
            name = "test",
            parentPhone = "9191919191",
            parentName = "Parent of test",
            parentFlatNo = "101",
            parentEmail = "test@example.com",
            studentEmail = "test@example.com",
            school = "AmarMohan Learning Centre",
            board = "CBSE",
            standard = "Class 10",
            subjectsJson = "[\"Mathematics\",\"Physics\",\"Chemistry\"]",
            syllabus = "Chapter 1: Real Numbers\nChapter 2: Polynomials\nChapter 3: Linear Equations",
            testPapersJson = "[]",
            remindersJson = "[]",
            attendanceJson = "{}",
            isFeesPaid = false,
            dueAmount = 1500.0,
            tuitionFee = 1500.0,
            demoFee = 0.0
        )
    }

    private fun loadTestStudents(): List<Student> {
        val json = prefs.getString("test_students_cache", null)
        if (json.isNullOrEmpty()) {
            return listOf(createInitialTestStudent())
        }
        return try {
            val list = JsonUtils.jsonToList(json, Student::class.java)
            if (list.isEmpty()) listOf(createInitialTestStudent()) else list
        } catch (e: Exception) {
            listOf(createInitialTestStudent())
        }
    }

    private fun saveTestStudents(list: List<Student>) {
        val json = JsonUtils.listToJson(list, Student::class.java)
        prefs.edit().putString("test_students_cache", json).apply()
    }

    private fun loadTestSchedules(): List<SessionSchedule> {
        val json = prefs.getString("test_schedules_cache", null)
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            JsonUtils.jsonToList(json, SessionSchedule::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveTestSchedules(list: List<SessionSchedule>) {
        val json = JsonUtils.listToJson(list, SessionSchedule::class.java)
        prefs.edit().putString("test_schedules_cache", json).apply()
    }

    private fun loadTestAttendanceRecords(): List<AttendanceRecord> {
        val json = prefs.getString("test_attendance_records_cache", null)
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            JsonUtils.jsonToList(json, AttendanceRecord::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveTestAttendanceRecords(list: List<AttendanceRecord>) {
        val json = JsonUtils.listToJson(list, AttendanceRecord::class.java)
        prefs.edit().putString("test_attendance_records_cache", json).apply()
    }

    private val _testStudents = MutableStateFlow<List<Student>>(loadTestStudents())

    // Students flow dynamically linked to Repository or Test Mode
    val students: StateFlow<List<Student>> = combine(
        _session,
        _isTutorTestMode,
        repository.students,
        _testStudents
    ) { sessionState, isTest, repoStudents, testStudentsList ->
        if (isTest || (sessionState is UserSession.Parent && testStudentsList.any { it.slNo == sessionState.studentSlNo })) {
            testStudentsList
        } else {
            repoStudents
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, repository.students.value)

    fun getStudent(slNo: String): Student? {
        if (_isTutorTestMode.value || (_session.value is UserSession.Parent && (_session.value as UserSession.Parent).studentSlNo == slNo) || _testStudents.value.any { it.slNo == slNo }) {
            val testFound = _testStudents.value.firstOrNull { it.slNo == slNo }
            if (testFound != null) return testFound
        }
        return repository.getStudentBySlNo(slNo)
    }

    suspend fun persistStudentUpdate(student: Student) {
        if (_isTutorTestMode.value || _testStudents.value.any { it.slNo == student.slNo }) {
            val current = _testStudents.value.toMutableList()
            val idx = current.indexOfFirst { it.slNo == student.slNo }
            if (idx >= 0) {
                current[idx] = student
            } else {
                current.add(student)
            }
            _testStudents.value = current
            saveTestStudents(current)
        } else {
            repository.updateStudent(student)
        }
    }

    suspend fun persistStudentAdd(student: Student) {
        if (_isTutorTestMode.value || _testStudents.value.any { it.slNo == student.slNo }) {
            val current = _testStudents.value.toMutableList()
            current.removeAll { it.slNo == student.slNo }
            current.add(student)
            _testStudents.value = current
            saveTestStudents(current)
        } else {
            repository.addStudent(student)
        }
    }

    suspend fun persistStudentDelete(slNo: String) {
        if (_isTutorTestMode.value || _testStudents.value.any { it.slNo == studentSlNo(slNo) }) {
            val current = _testStudents.value.filter { it.slNo != slNo }
            _testStudents.value = current
            saveTestStudents(current)
        } else {
            repository.deleteStudent(slNo)
        }
    }

    private fun studentSlNo(slNo: String): String = slNo

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Dynamic stats counters for reminders/dashboard metrics
    private val _demosCount = MutableStateFlow(prefs.getInt("reminders_demos_count", 5))
    val demosCount: StateFlow<Int> = _demosCount.asStateFlow()

    private val _checkedInsCount = MutableStateFlow(prefs.getInt("reminders_checked_ins_count", 12))
    val checkedInsCount: StateFlow<Int> = _checkedInsCount.asStateFlow()

    private val _enquiriesCount = MutableStateFlow(prefs.getInt("reminders_enquiries_count", 18))
    val enquiriesCount: StateFlow<Int> = _enquiriesCount.asStateFlow()

    private val _unitTestsCount = MutableStateFlow(prefs.getInt("reminders_unit_tests_count", 7))
    val unitTestsCount: StateFlow<Int> = _unitTestsCount.asStateFlow()

    fun updateDemosCount(delta: Int) {
        val newVal = (_demosCount.value + delta).coerceAtLeast(0)
        _demosCount.value = newVal
        prefs.edit().putInt("reminders_demos_count", newVal).apply()
    }

    fun updateCheckedInsCount(delta: Int) {
        val newVal = (_checkedInsCount.value + delta).coerceAtLeast(0)
        _checkedInsCount.value = newVal
        prefs.edit().putInt("reminders_checked_ins_count", newVal).apply()
    }

    fun updateEnquiriesCount(delta: Int) {
        val newVal = (_enquiriesCount.value + delta).coerceAtLeast(0)
        _enquiriesCount.value = newVal
        prefs.edit().putInt("reminders_enquiries_count", newVal).apply()
    }

    fun updateUnitTestsCount(delta: Int) {
        val newVal = (_unitTestsCount.value + delta).coerceAtLeast(0)
        _unitTestsCount.value = newVal
        prefs.edit().putInt("reminders_unit_tests_count", newVal).apply()
    }

    val isSupabaseOnline: StateFlow<Boolean> = repository.isSupabaseOnline
    val supabaseError: StateFlow<String?> = repository.supabaseError

    // Tutor UI Search & Filtering states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeTutorTab = MutableStateFlow(0) // 0: Attendance, 1: Pay, 2: Students, 3: Reminders
    val activeTutorTab: StateFlow<Int> = _activeTutorTab.asStateFlow()

    // Parent states
    val parentStudents: StateFlow<List<Student>> = combine(session, students) { sessionState, studentList ->
        if (sessionState is UserSession.Parent) {
            val phone = prefs.getString("session_parent_phone", "") ?: ""
            if (phone.isNotEmpty()) {
                val cleanPhone = phone.trim().filter { it.isDigit() }
                studentList.filter {
                    val dbPhone = it.parentPhone.trim().filter { c -> c.isDigit() }
                    dbPhone.contains(cleanPhone) || cleanPhone.contains(dbPhone)
                }
            } else {
                studentList.filter { it.slNo == sessionState.studentSlNo }
            }
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentParentStudent: StateFlow<Student?> = combine(session, students) { sessionState, studentList ->
        if (sessionState is UserSession.Parent) {
            studentList.firstOrNull { it.slNo == sessionState.studentSlNo }
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Setup dates for attendance (last 6 months)
    private val _attendanceDates = MutableStateFlow(getPastSixMonthsDates())
    val attendanceDates: StateFlow<List<String>> = _attendanceDates.asStateFlow()

    // Active selected date for daily attendance mark (default is today)
    private val _attendanceSelectedDate = MutableStateFlow(getFormattedTodayDate())
    val attendanceSelectedDate: StateFlow<String> = _attendanceSelectedDate.asStateFlow()

    // Private/Public state flows for Session Schedules and Attendance Records
    private val _sessionSchedules = MutableStateFlow<List<SessionSchedule>>(emptyList())
    val sessionSchedules: StateFlow<List<SessionSchedule>> = _sessionSchedules.asStateFlow()

    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()

    init {
        val sessionType = prefs.getString("session_type", "GUEST")
        val studentSlNo = prefs.getString("session_student_sl_no", "") ?: ""
        val isTest = prefs.getBoolean("is_tutor_test_mode", false)
        _isTutorTestMode.value = isTest

        if (isTest) {
            _testStudents.value = loadTestStudents()
            _sessionSchedules.value = loadTestSchedules()
            _attendanceRecords.value = loadTestAttendanceRecords()
        }

        _session.value = when (sessionType) {
            "TUTOR" -> UserSession.Tutor
            "PARENT" -> if (studentSlNo.isNotEmpty()) UserSession.Parent(studentSlNo) else UserSession.Guest
            else -> UserSession.Guest
        }

        viewModelScope.launch {
            students.collect { studentList ->
                val currentSession = _session.value
                if (currentSession is UserSession.Parent) {
                    val exists = studentList.any { it.slNo == currentSession.studentSlNo }
                    if (!exists) {
                        Log.d(TAG, "Logged in student ${currentSession.studentSlNo} is no longer in students list. Logging out parent.")
                        logOut()
                    }
                }
            }
        }

        if (!_isTutorTestMode.value) {
            refreshData()
        }
    }

    fun refreshData(onComplete: ((Boolean) -> Unit)? = null) {
        if (_isTutorTestMode.value && _session.value is UserSession.Tutor) {
            _isRefreshing.value = false
            onComplete?.invoke(true)
            return
        }
        viewModelScope.launch {
            _isRefreshing.value = true
            // Reload preferences counters
            _demosCount.value = prefs.getInt("reminders_demos_count", 5)
            _checkedInsCount.value = prefs.getInt("reminders_checked_ins_count", 12)
            _enquiriesCount.value = prefs.getInt("reminders_enquiries_count", 18)
            _unitTestsCount.value = prefs.getInt("reminders_unit_tests_count", 7)
            _attendanceDates.value = getPastSixMonthsDates()

            val success = try {
                repository.refreshFromSupabase()
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing from repository", e)
                false
            }

            try {
                refreshSchedulesAndAttendance()
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing extra schedules and attendance records", e)
            }

            _isRefreshing.value = false
            onComplete?.invoke(success)
        }
    }

    private fun projectSchedules(baseSchedules: List<SessionSchedule>): List<SessionSchedule> {
        val rawSchedules = baseSchedules.filter { !it.id.contains("_projected_") }
        if (rawSchedules.isEmpty()) return emptyList()

        val result = mutableListOf<SessionSchedule>()
        val groupedByStudent = rawSchedules.groupBy { it.studentSlNo }
        val formatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
        val today = java.time.LocalDate.now()
        val startDate = today.minusMonths(3)
        val endDate = today.plusMonths(4)

        groupedByStudent.forEach { (studentSlNo, studentScheds) ->
            val sortedScheds = studentScheds.sortedBy { it.date }
            if (sortedScheds.isEmpty()) return@forEach

            val firstNonCanceled = sortedScheds.firstOrNull { it.topic != "[CANCELED]" }
            val firstSchedDate = if (firstNonCanceled != null) {
                try {
                    java.time.LocalDate.parse(firstNonCanceled.date, formatter)
                } catch (e: Exception) {
                    startDate
                }
            } else {
                startDate
            }
            val startProjectionDate = if (firstSchedDate.isBefore(startDate)) startDate else firstSchedDate

            val explicitByDate = studentScheds.groupBy { it.date }

            var currentDate = startProjectionDate
            while (!currentDate.isAfter(endDate)) {
                val dateStr = currentDate.format(formatter)
                val explicitOnDay = explicitByDate[dateStr]

                if (explicitOnDay != null) {
                    result.addAll(explicitOnDay)
                } else {
                    val dayOfWeek = currentDate.dayOfWeek
                    val isWorkingDay = dayOfWeek != java.time.DayOfWeek.SUNDAY

                    if (isWorkingDay) {
                        val mostRecent = sortedScheds.lastOrNull { it.date < dateStr && it.topic != "[CANCELED]" }
                        if (mostRecent != null) {
                            result.add(
                                mostRecent.copy(
                                    id = "${mostRecent.id}_projected_$dateStr",
                                    date = dateStr,
                                    isCompleted = false
                                )
                            )
                        }
                    }
                }
                currentDate = currentDate.plusDays(1)
            }
        }
        return result.filter { it.topic != "[CANCELED]" }
    }

    suspend fun refreshSchedulesAndAttendance() {
        if (_isTutorTestMode.value) return
        val allSchedules = repository.fetchAllSessionSchedules().toMutableList()
        val allRecords = repository.fetchAllAttendanceRecords().toMutableList()
        if (allSchedules.isEmpty() && students.value.isNotEmpty()) {
            students.value.forEach { s ->
                val sScheds = repository.fetchSessionSchedules(s.slNo)
                allSchedules.addAll(sScheds)
            }
        }
        if (allRecords.isEmpty() && students.value.isNotEmpty()) {
            students.value.forEach { s ->
                val sRecs = repository.fetchAttendanceRecords(s.slNo)
                allRecords.addAll(sRecs)
            }
        }
        _sessionSchedules.value = projectSchedules(allSchedules)
        _attendanceRecords.value = allRecords
    }

    fun getSupabaseUrl(): String = com.amarmohan.core.database.SupabaseProvider.supabaseUrl
    fun getSupabaseAnonKey(): String = com.amarmohan.core.database.SupabaseProvider.supabaseAnonKey
    fun isSupabaseConfigured(): Boolean = com.amarmohan.core.database.SupabaseProvider.isConfigured

    fun updateSupabaseConfig(url: String, anonKey: String, onComplete: ((Boolean) -> Unit)? = null) {
        repository.updateSupabaseConfig(url, anonKey)
        refreshData(onComplete)
    }

    // --- Session Schedules ---
    fun loadSchedules(studentSlNo: String) {
        if (_isTutorTestMode.value || _testStudents.value.any { it.slNo == studentSlNo }) {
            _sessionSchedules.value = loadTestSchedules().filter { it.studentSlNo == studentSlNo }
            return
        }
        viewModelScope.launch {
            val scheds = repository.fetchSessionSchedules(studentSlNo)
            val unfiltered = _sessionSchedules.value.filter { it.studentSlNo != studentSlNo && !it.id.contains("_projected_") } + scheds
            _sessionSchedules.value = projectSchedules(unfiltered.sortedBy { it.date })
        }
    }

    fun addSessionSchedule(studentSlNo: String, topic: String, date: String, time: String = "", durationMinutes: Int = 60) {
        viewModelScope.launch {
            val id = "sched_" + System.currentTimeMillis()
            val schedule = SessionSchedule(id, studentSlNo, topic, date, time, durationMinutes, false)
            if (_isTutorTestMode.value || _testStudents.value.any { it.slNo == studentSlNo }) {
                val updated = _sessionSchedules.value + schedule
                _sessionSchedules.value = updated
                saveTestSchedules(updated)
                scheduleClassAlarmDirect(topic, date, time)
                return@launch
            }
            val success = repository.createSessionSchedule(schedule)
            if (success) {
                refreshSchedulesAndAttendance()
                // Auto register alarm on tutor-saved class schedule time
                scheduleClassAlarmDirect(topic, date, time)
            }
        }
    }

    fun scheduleClassAlarmDirect(topic: String, dateStr: String, timeStr: String) {
        try {
            val partsTime = timeStr.split(":")
            val hour = if (partsTime.isNotEmpty()) partsTime[0].toIntOrNull() ?: 17 else 17
            val minute = if (partsTime.size >= 2) partsTime[1].toIntOrNull() ?: 0 else 0

            val days = arrayListOf(
                java.util.Calendar.MONDAY,
                java.util.Calendar.TUESDAY,
                java.util.Calendar.WEDNESDAY,
                java.util.Calendar.THURSDAY,
                java.util.Calendar.FRIDAY,
                java.util.Calendar.SATURDAY
            )

            val intent = Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, "Amarmohan Tuition Schedule: $topic")
                putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour)
                putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute)
                putExtra(android.provider.AlarmClock.EXTRA_DAYS, days)
                putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun markScheduleCompleted(schedule: SessionSchedule, completed: Boolean) {
        viewModelScope.launch {
            if (_isTutorTestMode.value || _testStudents.value.any { it.slNo == schedule.studentSlNo }) {
                val updated = _sessionSchedules.value.map {
                    if (it.id == schedule.id) it.copy(isCompleted = completed) else it
                }
                _sessionSchedules.value = updated
                saveTestSchedules(updated)
                return@launch
            }
            if (schedule.id.contains("_projected_")) {
                // Create a real completed schedule for this date
                val newId = "sched_" + java.util.UUID.randomUUID().toString()
                val realSched = schedule.copy(id = newId, isCompleted = completed)
                repository.createSessionSchedule(realSched)
            } else {
                repository.removeSessionSchedule(schedule.id)
                repository.createSessionSchedule(schedule.copy(isCompleted = completed))
            }
            refreshSchedulesAndAttendance()
        }
    }

    fun deleteSessionSchedule(schedule: SessionSchedule) {
        viewModelScope.launch {
            if (_isTutorTestMode.value || _testStudents.value.any { it.slNo == schedule.studentSlNo }) {
                val updated = _sessionSchedules.value.filter { it.id != schedule.id }
                _sessionSchedules.value = updated
                saveTestSchedules(updated)
                return@launch
            }
            if (schedule.id.contains("_projected_")) {
                // It's a projected schedule. To delete/cancel it, we save a [CANCELED] placeholder
                val newId = "sched_" + java.util.UUID.randomUUID().toString()
                val cancelSched = schedule.copy(
                    id = newId,
                    topic = "[CANCELED]",
                    isCompleted = false
                )
                repository.createSessionSchedule(cancelSched)
            } else {
                // It's a real schedule. We remove it, but replace with [CANCELED] to prevent prior projection
                repository.removeSessionSchedule(schedule.id)
                val newId = "sched_" + java.util.UUID.randomUUID().toString()
                val cancelSched = schedule.copy(
                    id = newId,
                    topic = "[CANCELED]",
                    isCompleted = false
                )
                repository.createSessionSchedule(cancelSched)
            }
            refreshSchedulesAndAttendance()
        }
    }

    // --- Attendance Records (Granular Table) ---
    fun loadAttendanceRecords(studentSlNo: String) {
        if (_isTutorTestMode.value || _testStudents.value.any { it.slNo == studentSlNo }) {
            _attendanceRecords.value = loadTestAttendanceRecords().filter { it.studentSlNo == studentSlNo }
            return
        }
        viewModelScope.launch {
            val recs = repository.fetchAttendanceRecords(studentSlNo)
            _attendanceRecords.value = (_attendanceRecords.value.filter { it.studentSlNo != studentSlNo } + recs)
        }
    }

    fun addAttendanceRecord(studentSlNo: String, date: String, isPresent: Boolean, remarks: String = "") {
        viewModelScope.launch {
            val id = "att_${studentSlNo}_${date.replace("-", "")}"
            val record = AttendanceRecord(id, studentSlNo, date, isPresent, remarks)
            if (_isTutorTestMode.value || _testStudents.value.any { it.slNo == studentSlNo }) {
                val updated = _attendanceRecords.value.filter { it.id != id } + record
                _attendanceRecords.value = updated
                saveTestAttendanceRecords(updated)
                return@launch
            }
            repository.removeAttendanceRecord(id)
            val success = repository.createAttendanceRecord(record)
            if (success) {
                refreshSchedulesAndAttendance()
            }
        }
    }

    fun deleteAttendanceRecord(id: String) {
        viewModelScope.launch {
            if (_isTutorTestMode.value) {
                val updated = _attendanceRecords.value.filter { it.id != id }
                _attendanceRecords.value = updated
                saveTestAttendanceRecords(updated)
                return@launch
            }
            val success = repository.removeAttendanceRecord(id)
            if (success) {
                refreshSchedulesAndAttendance()
            }
        }
    }

    // Role-switching APIs
    fun logOut() {
        _isTutorTestMode.value = false
        prefs.edit()
            .remove("session_type")
            .remove("session_parent_phone")
            .remove("session_student_sl_no")
            .remove("is_tutor_test_mode")
            .apply()
        _session.value = UserSession.Guest
    }

    fun loginAsTutor(onSuccess: () -> Unit, onError: (String) -> Unit) {
        _isTutorTestMode.value = false
        prefs.edit().putBoolean("is_tutor_test_mode", false).apply()

        var currentMyId = prefs.getString("my_tutor_session_id", "") ?: ""
        if (currentMyId.isEmpty()) {
            currentMyId = "tutor_device_" + System.currentTimeMillis()
            prefs.edit().putString("my_tutor_session_id", currentMyId).apply()
        }

        val activeSessions = (prefs.getStringSet("tutor_active_sessions", emptySet()) ?: emptySet()).toMutableSet()
        activeSessions.add(currentMyId)
        prefs.edit()
            .putStringSet("tutor_active_sessions", activeSessions)
            .putString("session_type", "TUTOR")
            .apply()
        _session.value = UserSession.Tutor
        refreshData {
            // Refreshed backend
        }
        onSuccess()
    }

    /**
     * Test mode login when PIN 123456 is entered.
     * Opens a fully fresh and empty dashboard with only the student named "test".
     */
    fun loginAsTutorTestMode(onSuccess: () -> Unit, onError: (String) -> Unit) {
        _isTutorTestMode.value = true
        val testList = loadTestStudents()
        _testStudents.value = testList
        _sessionSchedules.value = loadTestSchedules()
        _attendanceRecords.value = loadTestAttendanceRecords()
        _demosCount.value = 0
        _checkedInsCount.value = 0
        _enquiriesCount.value = 0
        _unitTestsCount.value = 0
        _searchQuery.value = ""
        _activeTutorTab.value = 0

        prefs.edit()
            .putString("session_type", "TUTOR")
            .putBoolean("is_tutor_test_mode", true)
            .apply()
        _session.value = UserSession.Tutor
        onSuccess()
    }

    /**
     * Parent sign-up logic.
     * Records parent phone number, saves profile info, and either updates matched student profiles
     * or seeds 2 default students (A and B) under that number.
     */
    fun signUpParent(
        name: String,
        phone: String,
        flatNo: String,
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (name.trim().isEmpty() || phone.trim().isEmpty()) {
            onError("Name and Phone number are required.")
            return
        }

        viewModelScope.launch {
            val cleanPhone = phone.trim().filter { it.isDigit() }

            val isPreEnrolled = students.value.any {
                it.parentPhone.trim().filter { c -> c.isDigit() } == cleanPhone
            }
            if (!isPreEnrolled) {
                onError("This phone number ($phone) is not registered by the tutor. Please have the tutor register your child first.")
                return@launch
            }

            // Save registered parent details
            val registeredPhonesSet = (prefs.getStringSet("registered_parents_phones", emptySet()) ?: emptySet()).toMutableSet()
            registeredPhonesSet.add(cleanPhone)

            prefs.edit()
                .putStringSet("registered_parents_phones", registeredPhonesSet)
                .putString("parent_name_$cleanPhone", name.trim())
                .putString("parent_flat_$cleanPhone", flatNo.trim().ifEmpty { "N/A" })
                .putString("parent_email_$cleanPhone", email.trim().ifEmpty { "parent@example.com" })
                .apply()

            // Find matching students already created by Tutor
            val matchingStudents = students.value.filter {
                val dbPhone = it.parentPhone.trim().filter { c -> c.isDigit() }
                dbPhone.isNotEmpty() && (dbPhone.contains(cleanPhone) || cleanPhone.contains(dbPhone))
            }

            if (matchingStudents.isNotEmpty()) {
                // Update existing children's parent information with signup details
                matchingStudents.forEach { student ->
                    val updated = student.copy(
                        parentName = name.trim(),
                        parentFlatNo = flatNo.trim().ifEmpty { "N/A" },
                        parentEmail = email.trim().ifEmpty { "parent@example.com" },
                        parentPhone = phone.trim()
                    )
                    repository.updateStudent(updated)
                }
            } else {
                // No existing children found, create 2 default kids (Student A & Student B) as requested!
                val slNoA = "SL_" + (100000..999999).random()
                val slNoB = "SL_" + (100000..999999).random()

                val kidA = Student(
                    slNo = slNoA,
                    name = "${name.trim()}'s Student A",
                    parentPhone = phone.trim(),
                    parentName = name.trim(),
                    parentFlatNo = flatNo.trim().ifEmpty { "N/A" },
                    parentEmail = email.trim().ifEmpty { "parent@example.com" },
                    school = "AmarMohan School",
                    board = "CBSE",
                    standard = "Class 10"
                )
                val kidB = Student(
                    slNo = slNoB,
                    name = "${name.trim()}'s Student B",
                    parentPhone = phone.trim(),
                    parentName = name.trim(),
                    parentFlatNo = flatNo.trim().ifEmpty { "N/A" },
                    parentEmail = email.trim().ifEmpty { "parent@example.com" },
                    school = "AmarMohan School",
                    board = "CBSE",
                    standard = "Class 10"
                )

                repository.addStudent(kidA)
                repository.addStudent(kidB)
            }
            onSuccess()
        }
    }

    fun verifyParentPhoneForOtp(
        phone: String,
        onValid: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (phone.trim().isEmpty()) {
            onError("Phone number is required.")
            return
        }
        val cleanPhone = phone.trim().filter { it.isDigit() }
        val isPreEnrolled = students.value.any {
            it.parentPhone.trim().filter { c -> c.isDigit() } == cleanPhone
        }
        if (!isPreEnrolled) {
            onError("This phone number ($phone) is not enrolled for any student. Please contact the tutor.")
            return
        }
        onValid()
    }

    fun sendOtpSms(context: android.content.Context, phone: String, otp: String) {
        val cleanPhone = phone.trim().filter { it.isDigit() }
        if (cleanPhone.isEmpty()) return

        try {
            val smsManager: android.telephony.SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                context.getSystemService(android.telephony.SmsManager::class.java)
            } else {
                android.telephony.SmsManager.getDefault()
            }
            val message = if (cleanPhone == "9986736360") {
                "Your Shree Amarmohan Tutor verification OTP is: $otp"
            } else {
                "Your Shree Amarmohan Parent verification OTP is: $otp. (Sent from Tutor: 9986736360)"
            }
            smsManager.sendTextMessage(cleanPhone, null, message, null, null)
            android.util.Log.d("SMS_OTP", "SMS OTP Sent successfully to $cleanPhone: $otp")
        } catch (e: Exception) {
            android.util.Log.e("SMS_OTP", "Failed to send SMS programmatically", e)
        }
    }

    /**
     * Parent login checking for signed-up phone numbers and verifying password.
     * The default password is the name of their student (case-insensitive, trimmed).
     */
    fun loginAsParent(
        phone: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (phone.trim().isEmpty()) {
            onError("Phone number is required.")
            return
        }
        if (password.trim().isEmpty()) {
            onError("Password is required.")
            return
        }

        viewModelScope.launch {
            val cleanPhone = phone.trim().filter { it.isDigit() }
            val cleanPass = password.trim().lowercase()

            // Check if test student login from parent side (e.g. phone: 9191919191 or 123456, password: test or student name)
            val testList = loadTestStudents()
            val matchingTestStudent = testList.firstOrNull {
                val dbPhone = it.parentPhone.trim().filter { c -> c.isDigit() }
                cleanPhone == "9191919191" || cleanPhone == "123456" || cleanPhone == "1234567890" || dbPhone == cleanPhone || cleanPhone.contains("9191919191")
            } ?: if (cleanPhone == "9191919191" || cleanPhone == "123456" || cleanPhone == "1234567890") testList.firstOrNull() else null

            if (matchingTestStudent != null) {
                val isTestPassValid = cleanPass == "test" ||
                        cleanPass == "123456" ||
                        cleanPass == matchingTestStudent.name.trim().lowercase() ||
                        cleanPass == matchingTestStudent.parentPhone.trim().filter { it.isDigit() }
                if (isTestPassValid) {
                    _isTutorTestMode.value = true
                    _testStudents.value = testList
                    _sessionSchedules.value = loadTestSchedules()
                    _attendanceRecords.value = loadTestAttendanceRecords()
                    prefs.edit()
                        .putString("session_type", "PARENT")
                        .putString("session_parent_phone", phone.trim())
                        .putString("session_student_sl_no", matchingTestStudent.slNo)
                        .putBoolean("is_tutor_test_mode", true)
                        .apply()
                    _session.value = UserSession.Parent(matchingTestStudent.slNo)
                    onSuccess()
                    return@launch
                }
            }

            // Check if pre-enrolled
            val isPreEnrolled = students.value.any {
                it.parentPhone.trim().filter { c -> c.isDigit() } == cleanPhone
            }

            val parentName = prefs.getString("parent_name_$cleanPhone", null)
            val hasSignedUp = parentName != null

            if (!isPreEnrolled && !hasSignedUp) {
                onError("This phone number ($phone) is not enrolled. Please contact the tutor.")
                return@launch
            }

            // Get matching students
            val matchingStudents = students.value.filter {
                val dbPhone = it.parentPhone.trim().filter { c -> c.isDigit() }
                dbPhone == cleanPhone
            }

            if (matchingStudents.isEmpty() && !hasSignedUp) {
                onError("No students registered for this phone number.")
                return@launch
            }

            // Verify password
            val savedPassword = prefs.getString("parent_password_$cleanPhone", null)
            val isPasswordValid = if (savedPassword != null) {
                password.trim() == savedPassword.trim()
            } else {
                if (matchingStudents.isNotEmpty()) {
                    matchingStudents.any { student ->
                        password.trim().lowercase() == student.name.trim().lowercase()
                    }
                } else {
                    // Fallback to name if no student found but signed up
                    password.trim().lowercase() == (parentName ?: "").trim().lowercase()
                }
            }

            if (!isPasswordValid) {
                onError("Incorrect password. The default password is your student's name.")
                return@launch
            }

            if (matchingStudents.isNotEmpty()) {
                val primaryStudent = matchingStudents.first()
                prefs.edit()
                    .putString("session_type", "PARENT")
                    .putString("session_parent_phone", phone.trim())
                    .putString("session_student_sl_no", primaryStudent.slNo)
                    .apply()
                _session.value = UserSession.Parent(primaryStudent.slNo)
                onSuccess()
            } else {
                // Automatically create default student profiles so they can login.
                val safeParentName = parentName ?: "Parent"
                val slNoA = "SL_" + (100000..999999).random()
                val slNoB = "SL_" + (100000..999999).random()

                val parentFlatNo = prefs.getString("parent_flat_$cleanPhone", "N/A") ?: "N/A"
                val parentEmail = prefs.getString("parent_email_$cleanPhone", "parent@example.com") ?: "parent@example.com"

                val kidA = Student(
                    slNo = slNoA,
                    name = "$safeParentName's Student A",
                    parentPhone = phone.trim(),
                    parentName = safeParentName,
                    parentFlatNo = parentFlatNo,
                    parentEmail = parentEmail,
                    school = "AmarMohan School",
                    board = "CBSE",
                    standard = "Class 10"
                )
                val kidB = Student(
                    slNo = slNoB,
                    name = "$safeParentName's Student B",
                    parentPhone = phone.trim(),
                    parentName = safeParentName,
                    parentFlatNo = parentFlatNo,
                    parentEmail = parentEmail,
                    school = "AmarMohan School",
                    board = "CBSE",
                    standard = "Class 10"
                )

                repository.addStudent(kidA)
                repository.addStudent(kidB)

                delay(100)

                prefs.edit()
                    .putString("session_type", "PARENT")
                    .putString("session_parent_phone", phone.trim())
                    .putString("session_student_sl_no", slNoA)
                    .apply()
                _session.value = UserSession.Parent(slNoA)
                onSuccess()
            }
        }
    }

    /**
     * Change parent login password.
     */
    fun changeParentPassword(
        phone: String,
        oldPass: String,
        newPass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val sessionPhone = (prefs.getString("session_parent_phone", "") ?: "").trim().filter { it.isDigit() }
        val inputPhone = phone.trim().filter { it.isDigit() }
        val cleanPhone = if (inputPhone.isNotEmpty()) inputPhone else sessionPhone

        val savedPassword = if (cleanPhone.isNotEmpty()) {
            prefs.getString("parent_password_$cleanPhone", null)
                ?: if (sessionPhone.isNotEmpty()) prefs.getString("parent_password_$sessionPhone", null) else null
        } else {
            null
        }

        val matchingStudents = students.value.filter {
            val dbPhone = it.parentPhone.trim().filter { c -> c.isDigit() }
            (cleanPhone.isNotEmpty() && dbPhone == cleanPhone) ||
                    (sessionPhone.isNotEmpty() && dbPhone == sessionPhone)
        }

        val parentName = if (cleanPhone.isNotEmpty()) prefs.getString("parent_name_$cleanPhone", "Parent") else "Parent"

        val currentStudentSlNo = prefs.getString("session_student_sl_no", "")
        val currentStudent = students.value.firstOrNull { it.slNo == currentStudentSlNo }

        val isOldValid = if (savedPassword != null) {
            oldPass.trim() == savedPassword.trim()
        } else {
            if (matchingStudents.isNotEmpty()) {
                matchingStudents.any { student ->
                    oldPass.trim().lowercase() == student.name.trim().lowercase()
                }
            } else if (currentStudent != null) {
                oldPass.trim().lowercase() == currentStudent.name.trim().lowercase()
            } else {
                oldPass.trim().lowercase() == (parentName ?: "").trim().lowercase()
            }
        }

        if (!isOldValid) {
            onError("Incorrect current password. Default password is the student's name.")
            return
        }

        val editor = prefs.edit()
        if (cleanPhone.isNotEmpty()) {
            editor.putString("parent_password_$cleanPhone", newPass.trim())
        }
        if (sessionPhone.isNotEmpty()) {
            editor.putString("parent_password_$sessionPhone", newPass.trim())
        }
        if (currentStudent != null) {
            val studentPhone = currentStudent.parentPhone.trim().filter { it.isDigit() }
            if (studentPhone.isNotEmpty()) {
                editor.putString("parent_password_$studentPhone", newPass.trim())
            }
        }
        editor.apply()
        onSuccess()
    }

    /**
     * Switch context/profile to another child for the parent.
     */
    fun selectParentStudent(slNo: String) {
        if (_session.value is UserSession.Parent) {
            _session.value = UserSession.Parent(slNo)
            prefs.edit().putString("session_student_sl_no", slNo).apply()
        }
    }

    // Tutor student manipulation operations
    fun addOrUpdateStudentDetails(
        slNo: String,
        name: String,
        phone: String,
        school: String = "",
        board: String = "",
        standard: String = "",
        email: String = "",
        subjects: List<SubjectMark> = emptyList(),
        syllabus: String = "",
        tuitionFee: Double = 0.0,
        isPaid: Boolean = false,
        demoFee: Double = 0.0,
        parentFlatNo: String = ""
    ) {
        if (slNo.trim().isEmpty() || name.trim().isEmpty() || phone.trim().isEmpty()) {
            return
        }

        viewModelScope.launch {
            val existing = getStudent(slNo)
            val base = existing ?: Student(slNo = slNo, name = name, parentPhone = phone, parentFlatNo = parentFlatNo)

            val updated = base.copy(
                name = name,
                parentPhone = phone,
                school = school,
                board = board,
                standard = standard,
                studentEmail = email,
                syllabus = syllabus,
                tuitionFee = tuitionFee,
                isFeesPaid = isPaid,
                demoFee = demoFee,
                parentFlatNo = parentFlatNo
            ).withSubjects(subjects)

            val calculatedDue = if (isPaid) 0.0 else calculateStudentDueAmount(updated)
            val finalUpdated = updated.copy(dueAmount = calculatedDue)

            if (existing != null) {
                persistStudentUpdate(finalUpdated)
            } else {
                persistStudentAdd(finalUpdated)
            }
            if (!_isTutorTestMode.value) {
                refreshData()
            }
        }
    }

    fun updateStudent(student: Student) {
        viewModelScope.launch {
            persistStudentUpdate(student)
            if (!_isTutorTestMode.value) {
                refreshData()
            }
        }
    }

    fun deleteStudent(slNo: String) {
        viewModelScope.launch {
            persistStudentDelete(slNo)
            if (!_isTutorTestMode.value) {
                refreshData()
            }
        }
    }

    // Daily Attendance Management
    fun selectAttendanceDate(date: String) {
        _attendanceSelectedDate.value = date
        if (!_attendanceDates.value.contains(date)) {
            _attendanceDates.value = listOf(date) + _attendanceDates.value
        }
    }

    fun toggleAttendance(studentSlNo: String, date: String) {
        viewModelScope.launch {
            val student = getStudent(studentSlNo) ?: return@launch
            val attendance = student.getAttendance().toMutableMap()
            val currentStatus = attendance[date] ?: false
            val newStatus = !currentStatus
            attendance[date] = newStatus

            val updated = student.withAttendance(attendance)
            persistStudentUpdate(updated)

            // Sync with attendance_records table
            addAttendanceRecord(studentSlNo, date, newStatus, "Recorded from Tutor Console")
        }
    }

    fun setAttendanceStatus(studentSlNo: String, date: String, status: Boolean?) {
        viewModelScope.launch {
            val student = getStudent(studentSlNo) ?: return@launch
            val attendance = student.getAttendance().toMutableMap()
            if (status == null) {
                attendance.remove(date)
            } else {
                attendance[date] = status
            }

            val updated = student.withAttendance(attendance)
            persistStudentUpdate(updated)

            // Sync with attendance_records table
            addAttendanceRecord(studentSlNo, date, status ?: false, "Recorded from Tutor Console")
        }
    }

    fun getAttendanceForDate(student: Student, date: String): Boolean? {
        return student.getAttendance()[date]
    }

    // Payment operations (Teacher / Tutor manual inputs)
    fun updateStudentPayment(studentSlNo: String, isPaid: Boolean, feeValue: Double) {
        viewModelScope.launch {
            val student = getStudent(studentSlNo) ?: return@launch
            val updatedStudent = student.copy(
                isFeesPaid = isPaid,
                tuitionFee = feeValue
            )
            val calculatedDue = if (isPaid) 0.0 else calculateStudentDueAmount(updatedStudent)
            val updated = updatedStudent.copy(
                dueAmount = calculatedDue
            )
            persistStudentUpdate(updated)
        }
    }

    fun updateStudentDueAmount(studentSlNo: String, dueValue: Double) {
        viewModelScope.launch {
            val student = getStudent(studentSlNo) ?: return@launch
            val updated = student.copy(
                dueAmount = dueValue
            )
            persistStudentUpdate(updated)
        }
    }

    fun modifyDemoFee(studentSlNo: String, isIncrement: Boolean) {
        viewModelScope.launch {
            val student = getStudent(studentSlNo) ?: return@launch
            val delta = if (isIncrement) 500.0 else -500.0
            val currentDemo = student.demoFee
            val newDemo = (currentDemo + delta).coerceAtLeast(0.0)
            val updated = student.copy(demoFee = newDemo)
            persistStudentUpdate(updated)
        }
    }

    // Monthly Fee & Attendance Utilities
    fun getPastTwelveMonths(): List<String> {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        for (i in 0 until 12) {
            list.add(sdf.format(cal.time))
            cal.add(Calendar.MONTH, -1)
        }
        return list
    }

    fun getStudentFeePaidForMonth(student: Student, month: String): Boolean {
        val key = "fee_$month"
        val map = student.getAttendance()
        if (map.containsKey(key)) {
            return map[key] ?: false
        }
        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        if (month == currentMonthStr) {
            return student.isFeesPaid
        }
        return false
    }

    fun calculateStudentDueAmount(student: Student): Double {
        if (student.isFeesPaid) {
            return 0.0
        }
        val months = getPastTwelveMonths()
        var totalDue = 0.0
        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        for (month in months) {
            val isPaid = getStudentFeePaidForMonth(student, month)
            if (!isPaid) {
                if (month == currentMonthStr) {
                    totalDue += student.tuitionFee
                } else {
                    // For past months, only add if they had active attendance in that month
                    val hasAttendance = student.getAttendance().keys.any { key ->
                        key.startsWith(month) && !key.startsWith("fee_")
                    }
                    if (hasAttendance) {
                        totalDue += student.tuitionFee
                    }
                }
            }
        }
        return totalDue
    }

    fun updateStudentPaymentForMonth(studentSlNo: String, month: String, isPaid: Boolean, feeValue: Double? = null) {
        viewModelScope.launch {
            val student = getStudent(studentSlNo) ?: return@launch
            val attendance = student.getAttendance().toMutableMap()
            val key = "fee_$month"
            attendance[key] = isPaid

            val finalFee = feeValue ?: student.tuitionFee
            val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

            var updated = student.withAttendance(attendance)
            if (month == currentMonthStr) {
                updated = updated.copy(
                    isFeesPaid = isPaid,
                    tuitionFee = finalFee
                )
            } else {
                if (feeValue != null) {
                    updated = updated.copy(tuitionFee = feeValue)
                }
            }

            val calculatedDue = if (updated.isFeesPaid) 0.0 else calculateStudentDueAmount(updated)
            updated = updated.copy(dueAmount = calculatedDue)

            persistStudentUpdate(updated)
        }
    }

    fun formatMonthYear(monthStr: String): String {
        return try {
            val sdfSource = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val sdfDest = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val date = sdfSource.parse(monthStr)
            if (date != null) sdfDest.format(date) else monthStr
        } catch (e: Exception) {
            monthStr
        }
    }

    private fun formatToDdMmYyyy(dateStr: String): String {
        return try {
            val sdfSource = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val sdfDest = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val date = sdfSource.parse(dateStr)
            if (date != null) sdfDest.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    fun downloadLedgerExcelForMonth(context: Context, month: String): String {
        val studentList = students.value
        val displayMonth = formatMonthYear(month)
        val sb = java.lang.StringBuilder()

        var totalEarnedForMonth = 0.0
        studentList.forEach { s ->
            val isPaid = getStudentFeePaidForMonth(s, month)
            if (isPaid) {
                totalEarnedForMonth += s.tuitionFee
            }
        }
        val totalEarnedFormatted = String.format(java.util.Locale.US, "%,.2f", totalEarnedForMonth)

        sb.append("""
            <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
            <head>
            <meta http-equiv="content-type" content="text/html; charset=UTF-8">
            <!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets><x:ExcelWorksheet><x:Name>Fees Ledger - $month</x:Name><x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions></x:ExcelWorksheet></x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]-->
            <style>
                table { border-collapse: collapse; font-family: Arial, sans-serif; }
                th { color: black; font-weight: bold; padding: 8px; border: 1px solid #ddd; text-align: left; }
                td { padding: 8px; border: 1px solid #ddd; text-align: left; }
                .due { color: #DC2626; font-weight: bold; background-color: #FEE2E2; }
                .paid { color: #10B981; font-weight: bold; background-color: #D1FAE5; }
            </style>
            </head>
            <body>
            <table>
                <!-- Row 1: Title -->
                <tr>
                    <td colspan="4" style="font-size: 16pt; font-weight: bold; border: none; padding: 10px 0;">Tuition Fees Ledger ($displayMonth)</td>
                </tr>
                <!-- Row 2: Spacer -->
                <tr>
                    <td colspan="4" style="border: none; height: 15px;"></td>
                </tr>
                <!-- Row 3: Table Headers -->
                <thead>
                    <tr>
                        <th>Student ID</th>
                        <th>Student Name</th>
                        <th>Fees Amount (₹)</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
        """.trimIndent())

        studentList.forEach { s ->
            val isPaid = getStudentFeePaidForMonth(s, month)
            val statusStr = if (isPaid) "PAID" else "DUE/UNPAID"
            val statusClass = if (isPaid) "paid" else "due"
            val feeAmt = s.tuitionFee
            val feeFormatted = if (feeAmt % 1.0 == 0.0) feeAmt.toInt().toString() else feeAmt.toString()

            sb.append("""
                <tr>
                    <td>${s.slNo}</td>
                    <td><b>${s.name}</b></td>
                    <td>$feeFormatted</td>
                    <td class="$statusClass">$statusStr</td>
                </tr>
            """.trimIndent())
        }

        sb.append("""
                <!-- Row Spacer before Total -->
                <tr>
                    <td colspan="4" style="border: none; height: 10px;"></td>
                </tr>
                <!-- Row Total -->
                <tr>
                    <td style="border: none;"></td>
                    <td style="text-align: right; font-weight: bold; border: none; padding: 8px;">Total Earned for $displayMonth:</td>
                    <td style="color: #10B981; font-weight: bold; border: none; padding: 8px; text-align: left;">₹ $totalEarnedFormatted</td>
                    <td style="border: none;"></td>
                </tr>
                </tbody>
            </table>
            </body>
            </html>
        """.trimIndent())

        val excelContent = sb.toString()

        try {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val filename = "Tuition_Ledger_${month}_${System.currentTimeMillis()}.xls"
            val file = File(dir, filename)
            val writer = FileWriter(file)
            writer.write(excelContent)
            writer.close()

            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.ms-excel"
                putExtra(Intent.EXTRA_SUBJECT, "Tuition Fees Ledger - $displayMonth")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Save/Share Fees Ledger Excel"))

            return "Saved to $filename! Excel sharing chooser launched."
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write Excel ledger", e)
            return "Failed to save Excel file!"
        }
    }

    fun downloadAttendanceExcelForMonth(context: Context, month: String): String {
        val studentList = students.value
        val displayMonth = formatMonthYear(month)
        val sb = java.lang.StringBuilder()

        val allDatesInMonth = studentList.flatMap { s ->
            s.getAttendance().keys.filter { it.startsWith(month) && !it.startsWith("fee_") }
        }.distinct().sorted()

        val totalCols = allDatesInMonth.size + 1

        sb.append("""
            <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
            <head>
            <meta http-equiv="content-type" content="text/html; charset=UTF-8">
            <!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets><x:ExcelWorksheet><x:Name>Attendance - $month</x:Name><x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions></x:ExcelWorksheet></x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]-->
            <style>
                table { border-collapse: collapse; font-family: Arial, sans-serif; }
                th { color: black; font-weight: bold; padding: 8px; border: 1px solid #ddd; }
                td { padding: 8px; border: 1px solid #ddd; }
                .present { color: #137333; font-weight: bold; background-color: #E6F4EA; text-align: center; }
                .absent { color: #C5221F; font-weight: bold; background-color: #FCE8E6; text-align: center; }
            </style>
            </head>
            <body>
            <table>
                <!-- Row 1: Title -->
                <tr>
                    <td colspan="$totalCols" style="font-size: 18pt; font-weight: bold; border: none; padding: 10px 0;">Monthwise Attendance Sheet</td>
                </tr>
                <!-- Row 2: Spacer -->
                <tr>
                    <td colspan="$totalCols" style="border: none; height: 15px;"></td>
                </tr>
                <!-- Row 3: Subtitle -->
                <tr>
                    <td colspan="$totalCols" style="font-size: 14pt; font-weight: bold; border: none; padding: 5px 0;">Month: $displayMonth</td>
                </tr>
                <!-- Row 4: Spacer -->
                <tr>
                    <td colspan="$totalCols" style="border: none; height: 15px;"></td>
                </tr>
                <!-- Row 5: Table Header -->
                <thead>
                    <tr>
                        <th style="text-align: left; font-weight: bold; border: 1px solid #ddd; padding: 8px;">Student Name</th>
        """.trimIndent())

        allDatesInMonth.forEach { dateStr ->
            val formattedDate = formatToDdMmYyyy(dateStr)
            sb.append("<th style=\"text-align: center; font-weight: bold; border: 1px solid #ddd; padding: 8px;\">$formattedDate</th>\n")
        }

        sb.append("""
                    </tr>
                </thead>
                <tbody>
        """.trimIndent())

        if (allDatesInMonth.isEmpty()) {
            sb.append("""
                <tr>
                    <td colspan="$totalCols" style="text-align: center; color: #9CA3AF; padding: 20px; border: 1px solid #ddd;">No attendance records found for this month.</td>
                </tr>
            """.trimIndent())
        } else {
            studentList.forEach { s ->
                sb.append("<tr>\n")
                sb.append("<td style=\"text-align: left; font-weight: bold; border: 1px solid #ddd; padding: 8px;\">${s.name}</td>\n")

                allDatesInMonth.forEach { dateStr ->
                    val isPresent = s.getAttendance()[dateStr]
                    when (isPresent) {
                        true -> sb.append("<td class=\"present\">P</td>\n")
                        false -> sb.append("<td class=\"absent\">A</td>\n")
                        null -> sb.append("<td style=\"text-align: center; border: 1px solid #ddd; padding: 8px;\">-</td>\n")
                    }
                }
                sb.append("</tr>\n")
            }

            // Row Spacer before Total
            sb.append("""
                <tr>
                    <td colspan="$totalCols" style="border: none; height: 15px;"></td>
                </tr>
            """.trimIndent())

            // Total Row
            sb.append("<tr>\n")
            sb.append("<td style=\"text-align: left; font-weight: normal; border: none; padding: 8px;\">Total</td>\n")
            allDatesInMonth.forEach { dateStr ->
                val presentCount = studentList.count { s -> s.getAttendance()[dateStr] == true }
                sb.append("<td style=\"text-align: center; font-weight: normal; border: none; padding: 8px;\">$presentCount</td>\n")
            }
            sb.append("</tr>\n")
        }

        sb.append("""
                </tbody>
            </table>
            </body>
            </html>
        """.trimIndent())

        val excelContent = sb.toString()

        try {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val filename = "Attendance_Sheet_${month}_${System.currentTimeMillis()}.xls"
            val file = File(dir, filename)
            val writer = FileWriter(file)
            writer.write(excelContent)
            writer.close()

            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.ms-excel"
                putExtra(Intent.EXTRA_SUBJECT, "Attendance Sheet - $displayMonth")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Save/Share Attendance Excel"))

            return "Saved to $filename! Excel sharing chooser launched."
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write Attendance Excel", e)
            return "Failed to save Attendance Excel file!"
        }
    }

    // Export/Download Pay Sheets
    fun downloadLedgerText(context: Context): String {
        val studentList = students.value
        val sb = java.lang.StringBuilder()
        sb.append("=========================================\n")
        sb.append("      SHREE AMARMOHAN PAYMENT LEDGER     \n")
        sb.append("         Date: ${getFormattedTodayDate()}\n")
        sb.append("=========================================\n\n")

        var totalEarned = 0.0
        var totalDues = 0.0
        var totalDemo = 0.0

        studentList.forEach { s ->
            val statusStr = if (s.isFeesPaid) "PAID" else "UNPAID"
            sb.append("SL No: ${s.slNo} - Name: ${s.name}\n")
            sb.append("  Tuition Fee: ₹${s.tuitionFee} ($statusStr)\n")
            sb.append("  Dues:       ₹${s.dueAmount}\n")
            sb.append("  Demo Fee:   ₹${s.demoFee}\n")
            sb.append("-----------------------------------------\n")

            if (s.isFeesPaid) {
                totalEarned += s.tuitionFee
            } else {
                totalDues += s.dueAmount
            }
            totalDemo += s.demoFee
        }

        val overallTotal = totalEarned + totalDemo
        sb.append("\n=========================================\n")
        sb.append("SUMMARY TOTALS:\n")
        sb.append("  ✔ Total Tuition Earned: ₹$totalEarned\n")
        sb.append("  ✖ Total Dues Owed:      ₹$totalDues\n")
        sb.append("  ➕ Total Demo Fee Earned: ₹$totalDemo\n")
        sb.append("  💰 Net Earned Income:     ₹$overallTotal\n")
        sb.append("=========================================\n")

        // Write to local cache to make it downloadable
        try {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val filename = "Tuition_Ledger_${System.currentTimeMillis()}.txt"
            val file = File(dir, filename)
            val writer = FileWriter(file)
            writer.write(sb.toString())
            writer.close()
            Log.i(TAG, "Ledger saved to local file: ${file.absolutePath}")
            return "Saved ledger to: $filename. Copy to clipboard available!"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write file ledger", e)
            return "Ledger compiled! Clipboard copy available."
        }
    }

    fun downloadLedgerCSV(context: Context): String {
        val studentList = students.value
        val sb = java.lang.StringBuilder()

        sb.append("""
            <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
            <head>
            <meta http-equiv="content-type" content="text/html; charset=UTF-8">
            <!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets><x:ExcelWorksheet><x:Name>Fees Ledger</x:Name><x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions></x:ExcelWorksheet></x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]-->
            <style>
                table { border-collapse: collapse; width: 100%; font-family: Arial, sans-serif; }
                th { background-color: #7C3AED; color: white; font-weight: bold; padding: 10px; border: 1px solid #ddd; }
                td { padding: 8px; border: 1px solid #ddd; text-align: left; }
                tr:nth-child(even) { background-color: #f9fafb; }
                .due { color: #DC2626; font-weight: bold; }
                .paid { color: #10B981; font-weight: bold; }
                .earned { color: #059669; font-weight: bold; }
            </style>
            </head>
            <body>
            <h2>Shree Amarmohan - Tuition Fees Ledger (Till Date)</h2>
            <table>
                <thead>
                    <tr>
                        <th>Student ID</th>
                        <th>Student Name</th>
                        <th>Date</th>
                        <th>Amount (₹)</th>
                        <th>Category</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
        """.trimIndent())

        studentList.forEach { s ->
            val todayDate = getFormattedTodayDate()
            val tuitionStatus = if (s.isFeesPaid) "PAID" else "DUE"
            val tuitionClass = if (s.isFeesPaid) "paid" else "due"

            // 1. Tuition Fee row
            sb.append("""
                <tr>
                    <td>${s.slNo}</td>
                    <td><b>${s.name}</b></td>
                    <td>$todayDate</td>
                    <td>${s.tuitionFee}</td>
                    <td>Tuition Fee</td>
                    <td><span class="$tuitionClass">$tuitionStatus</span></td>
                </tr>
            """.trimIndent())

            // 2. Tuition Due row
            sb.append("""
                <tr>
                    <td>${s.slNo}</td>
                    <td><b>${s.name}</b></td>
                    <td>$todayDate</td>
                    <td>${s.dueAmount}</td>
                    <td>Tuition Fee Due</td>
                    <td><span class="due">DUE</span></td>
                </tr>
            """.trimIndent())

            // 3. Demo Fee row
            sb.append("""
                <tr>
                    <td>${s.slNo}</td>
                    <td><b>${s.name}</b></td>
                    <td>$todayDate</td>
                    <td>${s.demoFee}</td>
                    <td>Demo Fee</td>
                    <td><span class="earned">EARNED</span></td>
                </tr>
            """.trimIndent())
        }

        sb.append("""
                </tbody>
            </table>
            </body>
            </html>
        """.trimIndent())

        val excelContent = sb.toString()

        try {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val filename = "Tuition_Ledger_${System.currentTimeMillis()}.xls"
            val file = File(dir, filename)
            val writer = FileWriter(file)
            writer.write(excelContent)
            writer.close()

            // Get content URI from FileProvider
            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // Trigger share intent with permission grant
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.ms-excel"
                putExtra(Intent.EXTRA_SUBJECT, "Tuition Fees Ledger Excel")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Save/Share Fees Ledger Excel"))

            return "Saved to $filename! Excel sharing chooser launched."
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write Excel ledger", e)
            return "Failed to save Excel file!"
        }
    }

    fun downloadAttendanceCSV(context: Context): String {
        val studentList = students.value
        val sb = java.lang.StringBuilder()

        sb.append("""
            <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
            <head>
            <meta http-equiv="content-type" content="text/html; charset=UTF-8">
            <!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets><x:ExcelWorksheet><x:Name>Attendance Sheet</x:Name><x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions></x:ExcelWorksheet></x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]-->
            <style>
                table { border-collapse: collapse; font-family: Arial, sans-serif; margin-bottom: 30px; }
                th { background-color: #10B981; color: white; font-weight: bold; padding: 10px; border: 1px solid #ddd; text-align: center; }
                td { padding: 8px; border: 1px solid #ddd; text-align: center; }
                .student-name { text-align: left; font-weight: bold; background-color: #f9fafb; }
                .present { background-color: #D1FAE5; color: #065F46; font-weight: bold; }
                .absent { background-color: #FEE2E2; color: #991B1B; font-weight: bold; }
                .no-record { color: #9CA3AF; }
            </style>
            </head>
            <body>
            <h2>Shree Amarmohan - Monthwise Attendance Sheet</h2>
        """.trimIndent())

        // Group all dates across all student attendances by Month (yyyy-MM)
        val allStudentDates = studentList.flatMap { s -> s.getAttendance().keys }
        val months = allStudentDates.map { dateStr ->
            try {
                java.time.LocalDate.parse(dateStr).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
            } catch (e: Exception) {
                "Unknown"
            }
        }.distinct().sortedDescending()

        if (months.isEmpty()) {
            sb.append("<p>No attendance records found.</p>")
        } else {
            months.forEach { monthKey ->
                val displayMonth = try {
                    java.time.LocalDate.parse("$monthKey-01").format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.getDefault()))
                } catch (e: Exception) {
                    monthKey
                }

                // Get all dates in this month that have any attendance record across all students
                val datesInMonth = allStudentDates.filter { dateStr ->
                    dateStr.startsWith(monthKey)
                }.distinct().sorted()

                sb.append("""
                    <h3>Month: $displayMonth</h3>
                    <table>
                        <thead>
                            <tr>
                                <th style="text-align: left;">Student Name</th>
                """.trimIndent())

                datesInMonth.forEach { dateStr ->
                    val dayStr = try {
                        java.time.LocalDate.parse(dateStr).dayOfMonth.toString()
                    } catch (e: Exception) {
                        dateStr
                    }
                    sb.append("<th>Day $dayStr</th>")
                }
                sb.append("""
                            </tr>
                        </thead>
                        <tbody>
                """.trimIndent())

                studentList.forEach { s ->
                    val attendanceMap = s.getAttendance()
                    sb.append("""
                        <tr>
                            <td class="student-name">${s.name}</td>
                    """.trimIndent())

                    datesInMonth.forEach { dateStr ->
                        val isPresent = attendanceMap[dateStr]
                        if (isPresent == null) {
                            sb.append("<td class=\"no-record\">-</td>")
                        } else if (isPresent) {
                            sb.append("<td class=\"present\">P</td>")
                        } else {
                            sb.append("<td class=\"absent\">A</td>")
                        }
                    }
                    sb.append("</tr>")
                }

                sb.append("""
                        </tbody>
                    </table>
                """.trimIndent())
            }
        }

        sb.append("""
            </body>
            </html>
        """.trimIndent())

        val excelContent = sb.toString()

        try {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val filename = "Attendance_Sheet_${System.currentTimeMillis()}.xls"
            val file = File(dir, filename)
            val writer = FileWriter(file)
            writer.write(excelContent)
            writer.close()

            // Get content URI from FileProvider
            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // Trigger share intent with permission grant
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.ms-excel"
                putExtra(Intent.EXTRA_SUBJECT, "Attendance Sheet Excel")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Save/Share Attendance Excel"))

            return "Saved to $filename! Excel sharing chooser launched."
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write Attendance Excel", e)
            return "Failed to save Attendance Excel file!"
        }
    }

    private fun formatMonth(dateStr: String): String {
        return try {
            val date = java.time.LocalDate.parse(dateStr)
            date.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.getDefault()))
        } catch (e: Exception) {
            "Unknown Month"
        }
    }

    // Parent writing syllabus
    fun saveSyllabus(studentSlNo: String, text: String) {
        viewModelScope.launch {
            val student = getStudent(studentSlNo) ?: return@launch
            val updated = student.copy(syllabus = text)
            persistStudentUpdate(updated)
        }
    }

    fun saveCapturedBitmap(bitmap: android.graphics.Bitmap): String? {
        return try {
            val file = java.io.File(context.filesDir, "captured_paper_${System.currentTimeMillis()}.jpg")
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error saving captured bitmap", e)
            null
        }
    }

    fun saveSelectedUri(uri: android.net.Uri): String? {
        return try {
            val file = java.io.File(context.filesDir, "selected_paper_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                java.io.FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error copying URI to file", e)
            null
        }
    }

    // Parent uploading test papers
    fun uploadTestPaper(
        studentSlNo: String,
        subject: String,
        marksObtained: Double?,
        totalMarks: Double?,
        sourceUri: String,
        imageBytes: ByteArray? = null
    ) {
        viewModelScope.launch {
            var finalUri = sourceUri
            var finalImageBytes = imageBytes
            if (finalImageBytes == null && !sourceUri.startsWith("http") && !sourceUri.startsWith("content://")) {
                try {
                    val file = java.io.File(sourceUri)
                    if (file.exists()) {
                        finalImageBytes = file.readBytes()
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to read local file bytes for upload", e)
                }
            }

            if (finalImageBytes != null && com.amarmohan.core.database.SupabaseProvider.isConfigured && com.amarmohan.core.database.SupabaseProvider.api != null) {
                try {
                    val fileName = "paper_${studentSlNo}_${System.currentTimeMillis()}.jpg"
                    Log.d("MainViewModel", "Uploading testpaper image to Supabase storage bucket: $fileName")

                    val requestBody = okhttp3.RequestBody.create(
                        null,
                        finalImageBytes
                    )

                    com.amarmohan.core.database.SupabaseProvider.api!!.uploadTestPaper(
                        filename = fileName,
                        imageBytes = requestBody
                    )

                    // The standard public path in Supabase Storage buckets
                    val publicUrl = "${com.amarmohan.core.database.SupabaseProvider.supabaseUrl}/storage/v1/object/public/testpapers/$fileName"
                    finalUri = publicUrl
                    Log.d("MainViewModel", "Supabase storage upload completed! Public URL: $finalUri")
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to upload image to Supabase Storage", e)
                }
            } else if (finalImageBytes != null) {
                val fileName = "local_paper_${studentSlNo}_${System.currentTimeMillis()}.jpg"
                finalUri = "https://your-supabase-project.supabase.co/storage/v1/object/public/testpapers/$fileName"
            }

            val student = getStudent(studentSlNo) ?: return@launch
            val papers = student.getTestPapers().toMutableList()
            val newId = "paper_" + System.currentTimeMillis()
            val newPaper = TestPaper(
                id = newId,
                subjectName = subject,
                testDate = getFormattedTodayDate(),
                parentPhotoUri = finalUri,
                status = "Completed",
                marksObtained = marksObtained,
                totalMarks = totalMarks
            )
            papers.add(newPaper)
            val updated = student.withTestPapers(papers)
            persistStudentUpdate(updated)

            // Also automatically append to subjects/grades in performance metrics!
            if (marksObtained != null && totalMarks != null) {
                val subjects = student.getSubjects().toMutableList()
                val newRecord = SubjectMark(
                    subjectName = subject,
                    marks = marksObtained,
                    maxMarks = totalMarks,
                    testDate = getFormattedTodayDate(),
                    testType = "Unit Test"
                )
                subjects.add(newRecord)
                val updatedWithScore = updated.withSubjects(subjects)
                persistStudentUpdate(updatedWithScore)
            }
        }
    }

    // Teacher approving/uploading their side of test papers
    fun tutorUpdateTestPaper(studentSlNo: String, paperId: String, tutorUri: String) {
        viewModelScope.launch {
            var finalTutorUri = tutorUri
            var finalImageBytes: ByteArray? = null
            if (!tutorUri.startsWith("http") && !tutorUri.startsWith("content://")) {
                try {
                    val file = java.io.File(tutorUri)
                    if (file.exists()) {
                        finalImageBytes = file.readBytes()
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to read local tutor file bytes for upload", e)
                }
            }

            if (finalImageBytes != null && com.amarmohan.core.database.SupabaseProvider.isConfigured && com.amarmohan.core.database.SupabaseProvider.api != null) {
                try {
                    val fileName = "tutor_paper_${studentSlNo}_${System.currentTimeMillis()}.jpg"
                    Log.d("MainViewModel", "Uploading tutor paper image to Supabase storage bucket: $fileName")

                    val requestBody = okhttp3.RequestBody.create(
                        null,
                        finalImageBytes
                    )

                    com.amarmohan.core.database.SupabaseProvider.api!!.uploadTestPaper(
                        filename = fileName,
                        imageBytes = requestBody
                    )

                    val publicUrl = "${com.amarmohan.core.database.SupabaseProvider.supabaseUrl}/storage/v1/object/public/testpapers/$fileName"
                    finalTutorUri = publicUrl
                    Log.d("MainViewModel", "Supabase tutor storage upload completed! Public URL: $finalTutorUri")
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to upload tutor image to Supabase Storage", e)
                }
            }

            val student = getStudent(studentSlNo) ?: return@launch
            val papers = student.getTestPapers().map { paper ->
                if (paper.id == paperId) {
                    paper.copy(tutorPhotoUri = finalTutorUri, status = "Completed")
                } else {
                    paper
                }
            }
            val updated = student.withTestPapers(papers)
            persistStudentUpdate(updated)
        }
    }

    // Add general subjects test record
    fun addTestScore(studentSlNo: String, subject: String, score: Double, maxScore: Double, testType: String) {
        if (maxScore <= 0 || score < 0 || score > maxScore) return
        viewModelScope.launch {
            val student = getStudent(studentSlNo) ?: return@launch
            val subjects = student.getSubjects().toMutableList()
            val newRecord = SubjectMark(
                subjectName = subject,
                marks = score,
                maxMarks = maxScore,
                testDate = getFormattedTodayDate(),
                testType = testType
            )
            subjects.add(newRecord)
            val updated = student.withSubjects(subjects)
            persistStudentUpdate(updated)
        }
    }

    // Update student's core subject enrollment list
    fun updateStudentSubjects(studentSlNo: String, subjectNames: List<String>) {
        viewModelScope.launch {
            val student = getStudent(studentSlNo) ?: return@launch
            val existingSubjectsMap = student.getSubjects().associateBy { it.subjectName }
            val updatedSubjects = subjectNames.map { name ->
                existingSubjectsMap[name] ?: SubjectMark(
                    subjectName = name,
                    marks = 0.0,
                    maxMarks = 100.0,
                    testDate = "Not loaded",
                    testType = "Registered"
                )
            }
            val updated = student.withSubjects(updatedSubjects)
            persistStudentUpdate(updated)
        }
    }

    // Parent/Teacher adds reminders
    fun addReminder(studentSlNo: String, text: String, isDemos: Boolean = false, isPayDay: Boolean = false, isImportant: Boolean = false, isByParent: Boolean = false) {
        viewModelScope.launch {
            val student = getStudent(studentSlNo) ?: return@launch
            val reminders = student.getReminders().toMutableList()
            val newId = "rem_" + System.currentTimeMillis()
            val newReminder = Reminder(
                id = newId,
                text = text,
                date = getFormattedTodayDate(),
                isDemos = isDemos,
                isPayDay = isPayDay,
                isImportant = isImportant,
                isByParent = isByParent,
                isReadByTutor = false
            )
            reminders.add(newReminder)
            val updated = student.withReminders(reminders)
            persistStudentUpdate(updated)
        }
    }

    fun markReminderReadByTutor(studentSlNo: String, reminderId: String) {
        viewModelScope.launch {
            val student = getStudent(studentSlNo) ?: return@launch
            val reminders = student.getReminders().map { r ->
                if (r.id == reminderId) r.copy(isReadByTutor = true) else r
            }
            val updated = student.withReminders(reminders)
            persistStudentUpdate(updated)
        }
    }

    fun removeReminder(studentSlNo: String, reminderId: String) {
        viewModelScope.launch {
            val student = getStudent(studentSlNo) ?: return@launch
            val reminders = student.getReminders().filter { it.id != reminderId }
            val updated = student.withReminders(reminders)
            persistStudentUpdate(updated)
        }
    }

    // UI Navigation triggers
    fun setTutorTab(index: Int) {
        _activeTutorTab.value = index
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Utility calendar helpers
    private fun getFormattedTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getPastSixMonthsDates(): List<String> {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Let's generate daily dates for the last 6 months (roughly 180 days)
        for (i in 0 until 180) {
            list.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return list
    }
}
