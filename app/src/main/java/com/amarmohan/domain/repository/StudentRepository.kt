package com.amarmohan.domain.repository

import com.amarmohan.domain.model.*
import kotlinx.coroutines.flow.StateFlow

interface StudentRepository {
    val students: StateFlow<List<Student>>
    val isSupabaseOnline: StateFlow<Boolean>
    val supabaseError: StateFlow<String?>

    suspend fun refreshFromSupabase(): Boolean
    fun updateSupabaseConfig(url: String, key: String)
    suspend fun addStudent(student: Student): Boolean
    suspend fun updateStudent(student: Student): Boolean
    suspend fun deleteStudent(slNo: String): Boolean

    suspend fun fetchAllSessionSchedules(): List<SessionSchedule>
    suspend fun fetchSessionSchedules(studentSlNo: String): List<SessionSchedule>
    suspend fun createSessionSchedule(schedule: SessionSchedule): Boolean
    suspend fun removeSessionSchedule(id: String): Boolean

    suspend fun fetchAllAttendanceRecords(): List<AttendanceRecord>
    suspend fun fetchAttendanceRecords(studentSlNo: String): List<AttendanceRecord>
    suspend fun createAttendanceRecord(record: AttendanceRecord): Boolean
    suspend fun removeAttendanceRecord(id: String): Boolean

    fun getStudentBySlNo(slNo: String): Student?
    fun getStudentByVerificationPhone(phone: String): Student?
}
