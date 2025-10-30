package com.example.lifesync;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast; // Keep for other Toast messages

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Notificationhelperhabit {

    private static final String TAG = "NotificationHelperHabit";
    private static final String CHANNEL_ID = "habit_reminder_channel";
    private static final String CHANNEL_NAME = "Habit Reminders";
    private static final String CHANNEL_DESC = "Notifications for your habit reminders";

    private Context context;
    private AlarmManager alarmManager;

    public Notificationhelperhabit(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setSound(android.media.RingtoneManager.getDefaultUri(
                    android.media.RingtoneManager.TYPE_NOTIFICATION), null);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "✅ Notification channel created");
            }
        }
    }

    /**
     * Schedule all reminders for a habit with EXACT timing and DAILY repetition
     */
    public void scheduleHabitReminders(int habitId, String habitName, String reminderTimes) {
        if (reminderTimes == null || reminderTimes.trim().isEmpty()) {
            Log.d(TAG, "❌ No reminders to schedule for habit: " + habitName);
            return;
        }

        // Cancel existing reminders first to avoid duplicates
        cancelHabitReminders(habitId, reminderTimes);

        String[] times = reminderTimes.split(",");
        int successCount = 0;

        for (int i = 0; i < times.length; i++) {
            String time = times[i].trim();
            if (!time.isEmpty()) {
                boolean scheduled = scheduleDailyRepeatingReminder(habitId, habitName, time, i);
                if (scheduled) successCount++;
            }
        }

        Log.d(TAG, "✅ Scheduled " + successCount + "/" + times.length + " DAILY reminders for: " + habitName);
    }

    /**
     * CRITICAL FIX: Schedule DAILY REPEATING reminder with EXACT timing
     */
    private boolean scheduleDailyRepeatingReminder(int habitId, String habitName, String timeString, int reminderIndex) {
        try {
            Calendar calendar = parseTimeString(timeString);
            if (calendar == null) {
                Log.e(TAG, "❌ Invalid time format: " + timeString);
                return false;
            }

            Calendar now = Calendar.getInstance();

            // If time already passed today, schedule for tomorrow
            if (calendar.before(now)) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                Log.d(TAG, "⏩ Time passed today, first alarm tomorrow: " + timeString);
            }

            // Create UNIQUE request code for each reminder
            // Format: habitId * 1000 + reminderIndex (ensures uniqueness)
            int requestCode = (habitId * 1000) + reminderIndex;

            // Intent targets the correct BroadcastReceiver in the new package
            Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
            intent.putExtra("habit_id", habitId);
            intent.putExtra("habit_name", habitName);
            intent.putExtra("reminder_time", timeString);
            intent.putExtra("reminder_index", reminderIndex);
            intent.setAction("HABIT_REMINDER_" + requestCode); // Unique action

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (alarmManager == null) {
                Log.e(TAG, "❌ AlarmManager is null!");
                return false;
            }

            // USE setRepeating() for DAILY repetition with EXACT timing
            // We use setExactAndAllowWhileIdle/setExact if setRepeating is not available/recommended,
            // but setRepeating is the standard for daily. On recent Android, setExactAndAllowWhileIdle
            // might be better but let's stick to setRepeating as per the original intention and context.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // Android 4.4 to 11 and Android 12+ (if permission granted)
                // setRepeating is generally preferred for daily schedule where exactness is important.
                alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                );

                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                Log.d(TAG, "✅ DAILY REPEATING alarm set: " + habitName +
                        " at " + sdf.format(calendar.getTime()) +
                        " | Request Code: " + requestCode);
                return true;

            } else {
                // Android 4.3 and below
                alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                );
                return true;
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error scheduling reminder: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Cancel all reminders for a habit
     */
    public void cancelHabitReminders(int habitId, String reminderTimes) {
        if (reminderTimes == null || reminderTimes.trim().isEmpty()) {
            return;
        }

        String[] times = reminderTimes.split(",");
        for (int i = 0; i < times.length; i++) {
            int requestCode = (habitId * 1000) + i;
            cancelReminder(requestCode);
        }

        Log.d(TAG, "✅ Cancelled " + times.length + " reminders for habit ID: " + habitId);
    }

    /**
     * Cancel a single reminder
     */
    private void cancelReminder(int requestCode) {
        // Intent targets the correct BroadcastReceiver in the new package
        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.setAction("HABIT_REMINDER_" + requestCode);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "🗑️ Cancelled reminder with request code: " + requestCode);
        }
    }

    /**
     * Parse time string to Calendar object
     * Format: "08:00 AM" or "02:30 PM"
     */
    private Calendar parseTimeString(String timeString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date date = sdf.parse(timeString);
            if (date == null) return null;

            Calendar calendar = Calendar.getInstance();
            Calendar timeCalendar = Calendar.getInstance();
            timeCalendar.setTime(date);

            calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            return calendar;

        } catch (ParseException e) {
            Log.e(TAG, "Error parsing time: " + timeString);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Reschedule all reminders for all habits (after reboot)
     * // UPDATED: Argument type changed to HabitDatabaseHelper
     */
    public void rescheduleAllReminders(HabitDatabaseHelper databaseHelper) {
        try {
            // Assuming Habit is in the same package (com.example.lifesync)
            java.util.List<Habit> habits = databaseHelper.getAllHabits();
            int totalReminders = 0;

            for (Habit habit : habits) {
                String reminderTimes = habit.getProgress();
                if (reminderTimes != null && !reminderTimes.trim().isEmpty()) {
                    scheduleHabitReminders(habit.getId(), habit.getName(), reminderTimes);
                    String[] times = reminderTimes.split(",");
                    totalReminders += times.length;
                }
            }

            Log.d(TAG, "✅ Rescheduled " + totalReminders + " reminders for " + habits.size() + " habits");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error rescheduling reminders: " + e.getMessage());
        }
    }
}