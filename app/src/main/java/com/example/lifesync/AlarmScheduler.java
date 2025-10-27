package com.example.lifesync;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.example.lifesync.receiver.TaskNotificationReceiver;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AlarmScheduler {

    public static void scheduleTaskReminder(Context context, int taskId, String taskTitle, String taskDate) {
        try {
            // Parse the task date (format: d/M/yyyy)
            SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
            Date date = sdf.parse(taskDate);

            if (date == null) return;

            // Set reminder for 9 AM one day before the deadline
            Calendar reminderTime = Calendar.getInstance();
            reminderTime.setTime(date);
            reminderTime.add(Calendar.DAY_OF_MONTH, -1); // One day before
            reminderTime.set(Calendar.HOUR_OF_DAY, 9);
            reminderTime.set(Calendar.MINUTE, 0);
            reminderTime.set(Calendar.SECOND, 0);

            // FOR TESTING - Notification after 1 minute
            //reminderTime.setTimeInMillis(System.currentTimeMillis() + 10000); // 10 seconds

            // Don't schedule if time has passed
            if (reminderTime.getTimeInMillis() <= System.currentTimeMillis()) {
                return;
            }

            Intent intent = new Intent(context, TaskNotificationReceiver.class);
            intent.putExtra("task_id", taskId);
            intent.putExtra("task_title", taskTitle);
            intent.putExtra("task_date", taskDate);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    taskId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime.getTimeInMillis(),
                            pendingIntent
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cancelTaskReminder(Context context, int taskId) {
        Intent intent = new Intent(context, TaskNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}