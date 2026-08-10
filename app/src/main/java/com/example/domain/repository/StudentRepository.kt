package com.example.domain.repository

import com.example.domain.model.*
import kotlinx.coroutines.flow.StateFlow

interface StudentRepository {
    val students: StateFlow<List<Student>>
    val isSupabaseOnline: StateFlow<Boolean>
    val supabaseError: StateFlow<String?>
    
    suspend fun refreshFromSupabase(): Boolean
    suspend fun addStudent(student: Student): Boolean
    suspend fun updateStudent(student: Student): Boolean
    suspend fun deleteStudent(slNo: String): Boolean
    
    suspend fun fetchSessionSchedules(studentSlNo: String): List<SessionSchedule>
    suspend fun createSessionSchedule(schedule: SessionSchedule): Boolean
    suspend fun removeSessionSchedule(id: String): Boolean
    
    suspend fun fetchAttendanceRecords(studentSlNo: String): List<AttendanceRecord>
    suspend fun createAttendanceRecord(record: AttendanceRecord): Boolean
    suspend fun removeAttendanceRecord(id: String): Boolean
    
    fun getStudentBySlNo(slNo: String): Student?
    fun getStudentByVerificationPhone(phone: String): Student?
}
