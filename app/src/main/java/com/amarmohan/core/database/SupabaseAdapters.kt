package com.amarmohan.core.database

import com.amarmohan.domain.model.AttendanceRecord
import com.amarmohan.domain.model.Reminder
import com.amarmohan.domain.model.SessionSchedule
import com.amarmohan.domain.model.Student
import com.amarmohan.domain.model.SubjectMark
import com.amarmohan.domain.model.TestPaper
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * Custom Moshi Adapter for Student that guarantees null-safety across all fields.
 * PostgREST / Supabase REST often returns `null` for unfilled SQL columns, which causes
 * default KotlinJsonAdapter to throw JsonDataException on non-nullable fields.
 */
class StudentNullSafeAdapter : JsonAdapter<Student>() {

    @FromJson
    override fun fromJson(reader: JsonReader): Student {
        var slNo = ""
        var name = ""
        var parentName = ""
        var parentPhone = ""
        var parentFlatNo = ""
        var parentEmail = ""
        var standard = ""
        var school = ""
        var board = ""
        var studentEmail = ""
        var birthDate = ""
        var subjectsJson = "[]"
        var attendanceJson = "{}"
        var testPapersJson = "[]"
        var remindersJson = "[]"
        var syllabus = ""
        var isFeesPaid = false
        var tuitionFee = 0.0
        var dueAmount = 0.0
        var demoFee = 0.0

        reader.beginObject()
        while (reader.hasNext()) {
            val fieldName = reader.nextName()
            if (reader.peek() == JsonReader.Token.NULL) {
                reader.nextNull<Any>()
                continue
            }
            when (fieldName) {
                "sl_no" -> slNo = reader.nextString()
                "name" -> name = reader.nextString()
                "parent_name" -> parentName = reader.nextString()
                "parent_phone" -> parentPhone = reader.nextString()
                "parent_flat_no" -> parentFlatNo = reader.nextString()
                "parent_email" -> parentEmail = reader.nextString()
                "standard" -> standard = reader.nextString()
                "school" -> school = reader.nextString()
                "board" -> board = reader.nextString()
                "student_email" -> studentEmail = reader.nextString()
                "birth_date" -> birthDate = reader.nextString()
                "subjects_json" -> {
                    if (reader.peek() == JsonReader.Token.STRING) {
                        subjectsJson = reader.nextString()
                    } else {
                        // In case PostgREST returned raw json array
                        subjectsJson = reader.nextSource().readUtf8()
                    }
                }
                "attendance_json" -> {
                    if (reader.peek() == JsonReader.Token.STRING) {
                        attendanceJson = reader.nextString()
                    } else {
                        attendanceJson = reader.nextSource().readUtf8()
                    }
                }
                "test_papers_json" -> {
                    if (reader.peek() == JsonReader.Token.STRING) {
                        testPapersJson = reader.nextString()
                    } else {
                        testPapersJson = reader.nextSource().readUtf8()
                    }
                }
                "reminders_json" -> {
                    if (reader.peek() == JsonReader.Token.STRING) {
                        remindersJson = reader.nextString()
                    } else {
                        remindersJson = reader.nextSource().readUtf8()
                    }
                }
                "syllabus" -> syllabus = reader.nextString()
                "is_fees_paid" -> {
                    isFeesPaid = when (reader.peek()) {
                        JsonReader.Token.BOOLEAN -> reader.nextBoolean()
                        JsonReader.Token.NUMBER -> reader.nextInt() != 0
                        JsonReader.Token.STRING -> reader.nextString().toBoolean()
                        else -> {
                            reader.skipValue()
                            false
                        }
                    }
                }
                "tuition_fee" -> {
                    tuitionFee = when (reader.peek()) {
                        JsonReader.Token.NUMBER -> reader.nextDouble()
                        JsonReader.Token.STRING -> reader.nextString().toDoubleOrNull() ?: 0.0
                        else -> {
                            reader.skipValue()
                            0.0
                        }
                    }
                }
                "due_amount" -> {
                    dueAmount = when (reader.peek()) {
                        JsonReader.Token.NUMBER -> reader.nextDouble()
                        JsonReader.Token.STRING -> reader.nextString().toDoubleOrNull() ?: 0.0
                        else -> {
                            reader.skipValue()
                            0.0
                        }
                    }
                }
                "demo_fee" -> {
                    demoFee = when (reader.peek()) {
                        JsonReader.Token.NUMBER -> reader.nextDouble()
                        JsonReader.Token.STRING -> reader.nextString().toDoubleOrNull() ?: 0.0
                        else -> {
                            reader.skipValue()
                            0.0
                        }
                    }
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return Student(
            slNo = slNo,
            name = name,
            parentName = parentName,
            parentPhone = parentPhone,
            parentFlatNo = parentFlatNo,
            parentEmail = parentEmail,
            standard = standard,
            school = school,
            board = board,
            studentEmail = studentEmail,
            birthDate = birthDate,
            subjectsJson = subjectsJson.ifEmpty { "[]" },
            attendanceJson = attendanceJson.ifEmpty { "{}" },
            testPapersJson = testPapersJson.ifEmpty { "[]" },
            remindersJson = remindersJson.ifEmpty { "[]" },
            syllabus = syllabus,
            isFeesPaid = isFeesPaid,
            tuitionFee = tuitionFee,
            dueAmount = dueAmount,
            demoFee = demoFee
        )
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: Student?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("sl_no").value(value.slNo)
        writer.name("name").value(value.name)
        writer.name("parent_name").value(value.parentName)
        writer.name("parent_phone").value(value.parentPhone)
        writer.name("parent_flat_no").value(value.parentFlatNo)
        writer.name("parent_email").value(value.parentEmail)
        writer.name("standard").value(value.standard)
        writer.name("school").value(value.school)
        writer.name("board").value(value.board)
        writer.name("student_email").value(value.studentEmail)
        writer.name("birth_date").value(value.birthDate)
        writer.name("subjects_json").value(value.subjectsJson)
        writer.name("attendance_json").value(value.attendanceJson)
        writer.name("test_papers_json").value(value.testPapersJson)
        writer.name("reminders_json").value(value.remindersJson)
        writer.name("syllabus").value(value.syllabus)
        writer.name("is_fees_paid").value(value.isFeesPaid)
        writer.name("tuition_fee").value(value.tuitionFee)
        writer.name("due_amount").value(value.dueAmount)
        writer.name("demo_fee").value(value.demoFee)
        writer.endObject()
    }
}

class SessionScheduleNullSafeAdapter : JsonAdapter<SessionSchedule>() {
    @FromJson
    override fun fromJson(reader: JsonReader): SessionSchedule {
        var id = ""
        var studentSlNo = ""
        var topic = ""
        var date = ""
        var time = ""
        var durationMinutes = 60
        var isCompleted = false

        reader.beginObject()
        while (reader.hasNext()) {
            val fieldName = reader.nextName()
            if (reader.peek() == JsonReader.Token.NULL) {
                reader.nextNull<Any>()
                continue
            }
            when (fieldName) {
                "id" -> id = reader.nextString()
                "student_sl_no" -> studentSlNo = reader.nextString()
                "topic" -> topic = reader.nextString()
                "date" -> date = reader.nextString()
                "time" -> time = reader.nextString()
                "duration_minutes" -> {
                    durationMinutes = when (reader.peek()) {
                        JsonReader.Token.NUMBER -> reader.nextInt()
                        JsonReader.Token.STRING -> reader.nextString().toIntOrNull() ?: 60
                        else -> { reader.skipValue(); 60 }
                    }
                }
                "is_completed" -> {
                    isCompleted = when (reader.peek()) {
                        JsonReader.Token.BOOLEAN -> reader.nextBoolean()
                        JsonReader.Token.NUMBER -> reader.nextInt() != 0
                        JsonReader.Token.STRING -> reader.nextString().toBoolean()
                        else -> { reader.skipValue(); false }
                    }
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return SessionSchedule(
            id = id,
            studentSlNo = studentSlNo,
            topic = topic,
            date = date,
            time = time,
            durationMinutes = durationMinutes,
            isCompleted = isCompleted
        )
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: SessionSchedule?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("id").value(value.id)
        writer.name("student_sl_no").value(value.studentSlNo)
        writer.name("topic").value(value.topic)
        writer.name("date").value(value.date)
        writer.name("time").value(value.time)
        writer.name("duration_minutes").value(value.durationMinutes)
        writer.name("is_completed").value(value.isCompleted)
        writer.endObject()
    }
}

class AttendanceRecordNullSafeAdapter : JsonAdapter<AttendanceRecord>() {
    @FromJson
    override fun fromJson(reader: JsonReader): AttendanceRecord {
        var id = ""
        var studentSlNo = ""
        var date = ""
        var isPresent = false
        var remarks = ""

        reader.beginObject()
        while (reader.hasNext()) {
            val fieldName = reader.nextName()
            if (reader.peek() == JsonReader.Token.NULL) {
                reader.nextNull<Any>()
                continue
            }
            when (fieldName) {
                "id" -> id = reader.nextString()
                "student_sl_no" -> studentSlNo = reader.nextString()
                "date" -> date = reader.nextString()
                "is_present" -> {
                    isPresent = when (reader.peek()) {
                        JsonReader.Token.BOOLEAN -> reader.nextBoolean()
                        JsonReader.Token.NUMBER -> reader.nextInt() != 0
                        JsonReader.Token.STRING -> reader.nextString().toBoolean()
                        else -> { reader.skipValue(); false }
                    }
                }
                "remarks" -> remarks = reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return AttendanceRecord(
            id = id,
            studentSlNo = studentSlNo,
            date = date,
            isPresent = isPresent,
            remarks = remarks
        )
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: AttendanceRecord?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("id").value(value.id)
        writer.name("student_sl_no").value(value.studentSlNo)
        writer.name("date").value(value.date)
        writer.name("is_present").value(value.isPresent)
        writer.name("remarks").value(value.remarks)
        writer.endObject()
    }
}
