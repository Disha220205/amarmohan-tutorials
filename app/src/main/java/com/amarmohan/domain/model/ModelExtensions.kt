package com.amarmohan.domain.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object JsonUtils {
    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    inline fun <reified T> toJson(value: T): String {
        return try {
            val adapter = moshi.adapter(T::class.java)
            adapter.toJson(value)
        } catch (e: Exception) {
            ""
        }
    }

    inline fun <reified T> fromJson(json: String, default: T): T {
        if (json.isEmpty() || json == "[]" || json == "{}") return default
        return try {
            val adapter = moshi.adapter(T::class.java)
            adapter.fromJson(json) ?: default
        } catch (e: Exception) {
            default
        }
    }

    fun listToJson(list: List<Any>, clazz: Class<*>): String {
        return try {
            val type = Types.newParameterizedType(List::class.java, clazz)
            val adapter = moshi.adapter<List<Any>>(type)
            adapter.toJson(list)
        } catch (e: Exception) {
            "[]"
        }
    }

    fun <T> jsonToList(json: String, clazz: Class<T>): List<T> {
        if (json.isEmpty() || json == "[]") return emptyList()
        return try {
            val type = Types.newParameterizedType(List::class.java, clazz)
            val adapter = moshi.adapter<List<T>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun mapToJson(map: Map<String, Boolean>): String {
        return try {
            val type = Types.newParameterizedType(Map::class.java, String::class.java, java.lang.Boolean::class.java)
            val adapter = moshi.adapter<Map<String, Boolean>>(type)
            adapter.toJson(map)
        } catch (e: Exception) {
            "{}"
        }
    }

    fun jsonToMap(json: String): Map<String, Boolean> {
        if (json.isEmpty() || json == "{}") return emptyMap()
        return try {
            val type = Types.newParameterizedType(Map::class.java, String::class.java, java.lang.Boolean::class.java)
            val adapter = moshi.adapter<Map<String, Boolean>>(type)
            adapter.fromJson(json) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

// Student extensions
val ALLOWED_SUBJECTS = listOf(
    "Mathematics", "Biology", "Physics", "Chemistry", "Kannada", "Hindi",
    "Science", "History", "Civics", "English 1", "English 2", "Geography","Computer Science"
)

fun Student.getSubjects(): List<SubjectMark> {
    val list = JsonUtils.jsonToList(subjectsJson, SubjectMark::class.java)
    return list.map { sm ->
        val standardSubject = ALLOWED_SUBJECTS.find { it.equals(sm.subjectName.trim(), ignoreCase = true) }
        sm.copy(subjectName = standardSubject ?: sm.subjectName.trim())
    }
}

fun Student.withSubjects(subjects: List<SubjectMark>): Student {
    val processed = subjects.map { sm ->
        val standardSubject = ALLOWED_SUBJECTS.find { it.equals(sm.subjectName.trim(), ignoreCase = true) }
        sm.copy(subjectName = standardSubject ?: sm.subjectName.trim())
    }
    return this.copy(subjectsJson = JsonUtils.listToJson(processed, SubjectMark::class.java))
}

fun Student.getAttendance(): Map<String, Boolean> = JsonUtils.jsonToMap(attendanceJson)
fun Student.withAttendance(attendance: Map<String, Boolean>): Student = this.copy(attendanceJson = JsonUtils.mapToJson(attendance))

fun Student.getTestPapers(): List<TestPaper> {
    val list = JsonUtils.jsonToList(testPapersJson, TestPaper::class.java)
    return list.map { tp ->
        val standardSubject = ALLOWED_SUBJECTS.find { it.equals(tp.subjectName.trim(), ignoreCase = true) }
        tp.copy(subjectName = standardSubject ?: tp.subjectName.trim())
    }
}

fun Student.withTestPapers(papers: List<TestPaper>): Student {
    val processed = papers.map { tp ->
        val standardSubject = ALLOWED_SUBJECTS.find { it.equals(tp.subjectName.trim(), ignoreCase = true) }
        tp.copy(subjectName = standardSubject ?: tp.subjectName.trim())
    }
    return this.copy(testPapersJson = JsonUtils.listToJson(processed, TestPaper::class.java))
}

fun Student.getReminders(): List<Reminder> = JsonUtils.jsonToList(remindersJson, Reminder::class.java)
fun Student.withReminders(reminders: List<Reminder>): Student = this.copy(remindersJson = JsonUtils.listToJson(reminders, Reminder::class.java))

fun Student.getSyllabusItems(): List<SyllabusItem> {
    val trimmed = syllabus.trim()
    if (trimmed.startsWith("[")) {
        val list = JsonUtils.jsonToList(trimmed, SyllabusItem::class.java)
        if (list.isNotEmpty()) return list
    }

    // Fallback legacy parsing for comma/newline list
    if (trimmed.isEmpty()) return emptyList()
    val items = trimmed.split(Regex("[,\\n]+")).map { it.trim() }.filter { it.isNotEmpty() }
    return items.mapIndexed { index, itemText ->
        val isCompleted = itemText.contains("[x]", ignoreCase = true) || itemText.contains("✓", ignoreCase = true)
        val cleanText = itemText
            .replace("[x]", "", ignoreCase = true)
            .replace("[ ]", "", ignoreCase = true)
            .replace("✓", "", ignoreCase = true)
            .trim()
        SyllabusItem(
            id = "legacy_$index",
            subjectName = "General",
            topic = cleanText,
            isCompleted = isCompleted
        )
    }
}

fun Student.withSyllabusItems(items: List<SyllabusItem>): Student {
    return this.copy(syllabus = JsonUtils.listToJson(items, SyllabusItem::class.java))
}
