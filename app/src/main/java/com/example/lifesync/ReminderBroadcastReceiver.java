package com.example.lifesync;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ReminderBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "ReminderReceiver";
    // Must match the channel ID defined in NotificationHelper
    private static final String CHANNEL_ID = "habit_reminder_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "⏰ Reminder received at " + getCurrentTime());

        int habitId = intent.getIntExtra("habit_id", -1);
        String habitName = intent.getStringExtra("habit_name");
        String reminderTime = intent.getStringExtra("reminder_time");
        int reminderIndex = intent.getIntExtra("reminder_index", 0);

        if (habitId == -1 || habitName == null) {
            Log.e(TAG, "❌ Invalid reminder data");
            return;
        }

        Log.d(TAG, "📋 Habit: " + habitName + " | Time: " + reminderTime + " | Index: " + reminderIndex);

        // Show notification
        showNotification(context, habitId, habitName, reminderTime);
    }

    private void showNotification(Context context, int habitId, String habitName, String reminderTime) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) {
            Log.e(TAG, "❌ NotificationManager is null!");
            return;
        }

        // Intent to open the app (Assuming Habit_Fragment's host Activity is named MainActivity)
        // NOTE: If your main activity is named differently, update MainActivity.class below.
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                habitId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Notification sound
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Notification format:
        // Title: Habit Tracker
        // Content: [Name] - ⏰ [Time]
        String notificationTitle = "LifeSync Habit Reminder";
        String notificationContent = habitName + " - ⏰ " + reminderTime;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(notificationTitle)
                .setContentText(notificationContent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notificationContent))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        // Use unique notification ID for each reminder
        // A simple approach uses the habitId and current time component to minimize collisions
        int notificationId = habitId * 100 + reminderTime.hashCode();
        notificationManager.notify(notificationId, builder.build());

        Log.d(TAG, "✅ Notification shown: " + habitName + " | Scheduled: " + reminderTime + " | Actual: " + getCurrentTime());
    }

    private String getCurrentTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());
        return timeFormat.format(Calendar.getInstance().getTime());
    }
}
