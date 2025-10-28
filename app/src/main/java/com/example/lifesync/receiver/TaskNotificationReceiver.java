package com.example.lifesync.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.lifesync.NotificationHelper;

public class TaskNotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String taskTitle = intent.getStringExtra("task_title");
        String taskDate = intent.getStringExtra("task_date");
        int taskId = intent.getIntExtra("task_id", 0);

        String message = "Task \"" + taskTitle + "\" is due tomorrow (" + taskDate + ")";

        NotificationHelper.showNotification(
                context,
                "Task Reminder 🔔",
                message,
                taskId
        );
    }
}