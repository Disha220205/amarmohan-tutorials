package com.amarmohan.core.receiver

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.amarmohan.domain.model.JsonUtils
import com.amarmohan.domain.model.Student
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BirthdayAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "BirthdayAlarmReceiver triggered: checking birthdays at 00:00...")

        // 1. Check and dispatch birthday SMS
        checkAndDispatchBirthdaySms(context)

        // 2. Schedule next 00:00 midnight alarm
        scheduleNextMidnightAlarm(context)
    }

    companion object {
        private const val TAG = "BirthdayAlarmReceiver"
        private const val ALARM_REQ_CODE = 8821

        fun scheduleNextMidnightAlarm(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, BirthdayAlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    ALARM_REQ_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Calculate next 00:00:00 midnight
                val now = System.currentTimeMillis()
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= now) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

                val triggerTime = calendar.timeInMillis

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
                Log.d(TAG, "Scheduled next midnight 00:00 birthday check alarm for: ${calendar.time}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule midnight alarm", e)
            }
        }

        fun checkAndDispatchBirthdaySms(context: Context) {
            try {
                val prefs = context.getSharedPreferences("TutorPortalPrefs", Context.MODE_PRIVATE)
                val syncPrefs = context.getSharedPreferences("tutorsync_prefs", Context.MODE_PRIVATE)

                val json1 = prefs.getString("students_cache", "[]") ?: "[]"
                val json2 = prefs.getString("test_students_key", "[]") ?: "[]"
                val json3 = syncPrefs.getString("students_cache", "[]") ?: "[]"

                val list1 = JsonUtils.jsonToList(json1, Student::class.java)
                val list2 = JsonUtils.jsonToList(json2, Student::class.java)
                val list3 = JsonUtils.jsonToList(json3, Student::class.java)

                val allCombinedStudents = (list1 + list2 + list3).distinctBy { it.slNo }

                allCombinedStudents.forEach { student ->
                    dispatchBirthdaySmsIfToday(context, student)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in checkAndDispatchBirthdaySms", e)
            }
        }

        fun dispatchBirthdaySmsIfToday(context: Context, student: Student, force: Boolean = false) {
            if (student.birthDate.isNotBlank() && isBirthdayToday(student.birthDate) && student.parentPhone.isNotBlank()) {
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val syncPrefs = context.getSharedPreferences("tutorsync_prefs", Context.MODE_PRIVATE)
                val sentKey = "birthday_sms_sent_${student.slNo}_$todayDate"
                val alreadySent = syncPrefs.getBoolean(sentKey, false)
                if (!alreadySent || force) {
                    val cleanPhone = student.parentPhone.trim().filter { it.isDigit() }
                    if (cleanPhone.isNotEmpty()) {
                        val message = " happy Birthday dear ${student.name}, wish you a very successfull and prosporous life ahead , stay healthy and happy , regards Shree amarmohan tutions ."
                        sendSmsSilently(context, cleanPhone, message)
                        syncPrefs.edit().putBoolean(sentKey, true).apply()
                        Log.d(TAG, "Birthday SMS dispatched for ${student.name} ($cleanPhone)")
                    }
                }
            }
        }

        private fun sendSmsSilently(context: Context, cleanPhone: String, message: String) {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "SEND_SMS permission not granted; skipping background silent SMS")
                    return
                }
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                val parts = smsManager.divideMessage(message)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(cleanPhone, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(cleanPhone, null, message, null, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send SMS in background: ${e.message}", e)
            }
        }

        fun isBirthdayToday(birthDateStr: String): Boolean {
            if (birthDateStr.isBlank()) return false
            val todayCal = Calendar.getInstance()
            val todayMonth = todayCal.get(Calendar.MONTH) + 1 // 1-12
            val todayDay = todayCal.get(Calendar.DAY_OF_MONTH) // 1-31

            val parts = birthDateStr.trim().split(Regex("[\\-\\/.\\s]")).filter { it.isNotEmpty() }
            if (parts.size >= 3) {
                if (parts[0].length == 4) {
                    // YYYY-MM-DD
                    val m = parts[1].toIntOrNull() ?: return false
                    val d = parts[2].toIntOrNull() ?: return false
                    return m == todayMonth && d == todayDay
                } else if (parts[2].length == 4) {
                    // DD-MM-YYYY
                    val d = parts[0].toIntOrNull() ?: return false
                    val m = parts[1].toIntOrNull() ?: return false
                    return m == todayMonth && d == todayDay
                }
            } else if (parts.size == 2) {
                // DD-MM
                val d = parts[0].toIntOrNull() ?: return false
                val m = parts[1].toIntOrNull() ?: return false
                return m == todayMonth && d == todayDay
            }
            return false
        }
    }
}
