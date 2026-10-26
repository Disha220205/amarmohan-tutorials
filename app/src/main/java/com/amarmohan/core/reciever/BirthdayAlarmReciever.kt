package com.amarmohan.core.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
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

                try {
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
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    }
                } catch (se: SecurityException) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setAndAllowWhileIdle(
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
                }
                Log.d(TAG, "Scheduled next midnight 00:00 birthday check alarm for: ${calendar.time}")
            } catch (e: Exception) {
                Log.w(TAG, "Could not schedule midnight alarm: ${e.message}")
            }
        }

        private const val CHANNEL_ID = "tutor_birthday_reminders"

        fun checkAndDispatchBirthdaySms(context: Context) {
            // Automated background SMS removed per user instructions.
            // Dispatch in-app reminders and notifications for the tutor instead.
            checkAndNotifyBirthdays(context)
        }

        fun checkAndNotifyBirthdays(context: Context) {
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
                    if (student.birthDate.isNotBlank() && isBirthdayToday(student.birthDate)) {
                        showBirthdayReminderNotification(context, student)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Notice in checkAndNotifyBirthdays: ${e.message}")
            }
        }

        fun showBirthdayReminderNotification(context: Context, student: Student) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = android.app.NotificationChannel(
                        CHANNEL_ID,
                        "Birthday & Student Reminders",
                        android.app.NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Reminders for student birthdays and new messages"
                    }
                    notificationManager.createNotificationChannel(channel)
                }

                val cleanPhone = student.parentPhone.trim().filter { it.isDigit() || it == '+' }
                val targetPhone = if (cleanPhone.isNotEmpty()) cleanPhone else "9876543210"
                val birthdayMessage = " happy Birthday dear ${student.name}, wish you a very successfull and prosporous life ahead , stay healthy and happy , regards Shree amarmohan tutions ."

                // Explicit Intent to MainActivity to guarantee reliable dispatch and activity launch
                val notifyIntent = Intent(context, com.amarmohan.MainActivity::class.java).apply {
                    putExtra("ACTION_DISPATCH_SMS", true)
                    putExtra("EXTRA_PHONE", targetPhone)
                    putExtra("EXTRA_MESSAGE", birthdayMessage)
                    putExtra("EXTRA_STUDENT_SL_NO", student.slNo)
                    putExtra("EXTRA_IS_BIRTHDAY", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    student.name.hashCode(),
                    notifyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("🎂 Student Birthday Today: ${student.name}")
                    .setContentText("Today is ${student.name}'s birthday (SL: ${student.slNo}). Tap to send SMS!")
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .addAction(android.R.drawable.ic_menu_send, "Wish via SMS", pendingIntent)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(student.name.hashCode(), notification)
                Log.d(TAG, "Birthday reminder notification posted for ${student.name}")
            } catch (e: Exception) {
                Log.w(TAG, "Could not show birthday notification: ${e.message}")
            }
        }

        fun showStudentMessageNotification(context: Context, student: Student, reminderText: String, reminderId: String? = null) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = android.app.NotificationChannel(
                        CHANNEL_ID,
                        "Birthday & Student Reminders",
                        android.app.NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Reminders for student birthdays and new messages"
                    }
                    notificationManager.createNotificationChannel(channel)
                }

                val cleanPhone = student.parentPhone.trim().filter { it.isDigit() || it == '+' }
                val targetPhone = if (cleanPhone.isNotEmpty()) cleanPhone else "9876543210"
                val replyMessage = "Hello, regarding message for ${student.name} ('$reminderText'): "

                val notifyIntent = Intent(context, com.amarmohan.MainActivity::class.java).apply {
                    putExtra("ACTION_DISPATCH_SMS", true)
                    putExtra("EXTRA_PHONE", targetPhone)
                    putExtra("EXTRA_MESSAGE", replyMessage)
                    putExtra("EXTRA_STUDENT_SL_NO", student.slNo)
                    if (reminderId != null) {
                        putExtra("EXTRA_REMINDER_ID", reminderId)
                    }
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    (student.name + reminderText).hashCode(),
                    notifyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_email)
                    .setContentTitle("📩 New Message regarding ${student.name}")
                    .setContentText(reminderText)
                    .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText("Message for ${student.name} (SL: ${student.slNo}): $reminderText. Tap to reply via SMS."))
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .addAction(android.R.drawable.ic_menu_send, "Reply via SMS", pendingIntent)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify((student.name + reminderText).hashCode(), notification)
                Log.d(TAG, "Student message notification posted for ${student.name}")
            } catch (e: Exception) {
                Log.w(TAG, "Could not show student message notification: ${e.message}")
            }
        }

        fun showNewStudentNotification(
            context: Context,
            student: Student,
            reminderId: String? = null,
            isSignIn: Boolean = reminderId?.startsWith("signin") == true
        ) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = android.app.NotificationChannel(
                        CHANNEL_ID,
                        "Birthday & Student Reminders",
                        android.app.NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Reminders for student birthdays, enrollments, and messages"
                    }
                    notificationManager.createNotificationChannel(channel)
                }

                val cleanPhone = student.parentPhone.trim().filter { it.isDigit() || it == '+' }
                val targetPhone = if (cleanPhone.isNotEmpty()) cleanPhone else "9876543210"
                val welcomeMessage = if (isSignIn) {
                    "Welcome to Shree AmarMohan Tuition! Dear Parent, student ${student.name} (Reg No: ${student.slNo}) has signed into the student portal. Regards, Shree AmarMohan Tuition."
                } else {
                    "Welcome to Shree AmarMohan Tuition! Dear Parent, student ${student.name} (Reg No: ${student.slNo}, Class: ${student.standard}) has been successfully enrolled. Regards, Shree AmarMohan Tuition."
                }
                val notifId = if (isSignIn) (student.slNo + "_signin").hashCode() else (student.slNo + "_enroll").hashCode()

                val notifyIntent = Intent(context, com.amarmohan.MainActivity::class.java).apply {
                    putExtra("ACTION_DISPATCH_SMS", true)
                    putExtra("EXTRA_PHONE", targetPhone)
                    putExtra("EXTRA_MESSAGE", welcomeMessage)
                    putExtra("EXTRA_STUDENT_SL_NO", student.slNo)
                    if (reminderId != null) {
                        putExtra("EXTRA_REMINDER_ID", reminderId)
                    }
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    notifId,
                    notifyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val title = if (isSignIn) "🎉 Student Signed In: ${student.name}" else "🎉 New Student Enrolled: ${student.name}"
                val content = if (isSignIn) "Student ${student.name} signed in. Tap to review & send SMS to parent." else "Enrolled in ${student.standard}. Tap to review & send welcome SMS to parent."
                val bigText = if (isSignIn) {
                    "Student ${student.name} (Reg No: ${student.slNo}) has signed in. Tap to open SMS app with pre-written message for tutor to review and send."
                } else {
                    "New student ${student.name} (Reg No: ${student.slNo}) enrolled. Tap to open SMS app with pre-written welcome message for tutor to review and send."
                }

                val notification = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(bigText))
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .addAction(android.R.drawable.ic_menu_send, if (isSignIn) "Send SMS to Parent" else "Send Welcome SMS", pendingIntent)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(notifId, notification)
                Log.d(TAG, "New student notification posted for ${student.name}: isSignIn=$isSignIn")
            } catch (e: Exception) {
                Log.w(TAG, "Could not show new student notification: ${e.message}")
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
