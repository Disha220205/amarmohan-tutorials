package com.amarmohan.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Student(
    @Json(name = "sl_no") val slNo: String, // Primary Key (SL No)
    @Json(name = "name") val name: String,

    // Parent Details
    @Json(name = "parent_name") val parentName: String = "",
    @Json(name = "parent_phone") val parentPhone: String = "",
    @Json(name = "parent_flat_no") val parentFlatNo: String = "",
    @Json(name = "parent_email") val parentEmail: String = "",

    // Student School/Standard Details
    @Json(name = "standard") val standard: String = "",
    @Json(name = "school") val school: String = "",
    @Json(name = "board") val board: String = "",
    @Json(name = "student_email") val studentEmail: String = "",
    @Json(name = "birth_date") val birthDate: String = "",
    @Json(name = "subjects_json") val subjectsJson: String = "[]", // Serialized list of SubjectMark objects

    // Attendance data
    @Json(name = "attendance_json") val attendanceJson: String = "{}", // Serialized map: "yyyy-MM-dd" to Boolean (True = Present, False = Absent)

    // Test papers
    @Json(name = "test_papers_json") val testPapersJson: String = "[]", // Serialized list of TestPaper

    // Reminders data
    @Json(name = "reminders_json") val remindersJson: String = "[]", // Serialized list of Reminder

    // Syllabus for next test
    @Json(name = "syllabus") val syllabus: String = "",

    // Payment controls
    @Json(name = "is_fees_paid") val isFeesPaid: Boolean = false,
    @Json(name = "tuition_fee") val tuitionFee: Double = 0.0,
    @Json(name = "due_amount") val dueAmount: Double = 0.0,
    @Json(name = "demo_fee") val demoFee: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class SubjectMark(
    @Json(name = "subject_name") val subjectName: String,
    @Json(name = "marks") val marks: Double,
    @Json(name = "max_marks") val maxMarks: Double = 100.0,
    @Json(name = "test_date") val testDate: String = "",
    @Json(name = "test_type") val testType: String = "Monthly" // "Monthly" or "Annual"
)

@JsonClass(generateAdapter = true)
data class TestPaper(
    @Json(name = "id") val id: String,
    @Json(name = "subject_name") val subjectName: String,
    @Json(name = "test_date") val testDate: String,
    @Json(name = "parent_photo_uri") val parentPhotoUri: String? = null,
    @Json(name = "tutor_photo_uri") val tutorPhotoUri: String? = null,
    @Json(name = "status") val status: String = "Pending Capture", // "Pending Tutor Upload", "Completed"
    @Json(name = "marks_obtained") val marksObtained: Double? = null,
    @Json(name = "total_marks") val totalMarks: Double? = null
)

@JsonClass(generateAdapter = true)
data class Reminder(
    @Json(name = "id") val id: String,
    @Json(name = "text") val text: String,
    @Json(name = "date") val date: String = "",
    @Json(name = "is_demos") val isDemos: Boolean = false,
    @Json(name = "is_pay_day") val isPayDay: Boolean = false,
    @Json(name = "is_important") val isImportant: Boolean = false,
    @Json(name = "is_by_parent") val isByParent: Boolean = false,
    @Json(name = "is_read_by_tutor") val isReadByTutor: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SessionSchedule(
    @Json(name = "id") val id: String,
    @Json(name = "student_sl_no") val studentSlNo: String,
    @Json(name = "topic") val topic: String,
    @Json(name = "date") val date: String,
    @Json(name = "time") val time: String = "",
    @Json(name = "duration_minutes") val durationMinutes: Int = 60,
    @Json(name = "is_completed") val isCompleted: Boolean = false
)

@JsonClass(generateAdapter = true)
data class AttendanceRecord(
    @Json(name = "id") val id: String,
    @Json(name = "student_sl_no") val studentSlNo: String,
    @Json(name = "date") val date: String,
    @Json(name = "is_present") val isPresent: Boolean,
    @Json(name = "remarks") val remarks: String = ""
)

@JsonClass(generateAdapter = true)
data class SyllabusItem(
    @Json(name = "id") val id: String,
    @Json(name = "subject_name") val subjectName: String,
    @Json(name = "topic") val topic: String,
    @Json(name = "is_completed") val isCompleted: Boolean = false
)
