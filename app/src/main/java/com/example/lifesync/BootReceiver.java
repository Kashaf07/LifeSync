package com.example.lifesync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Device rebooted - rescheduling reminders");

            // ✅ Use correct helper class
            HabitDatabaseHelper databaseHelper = new HabitDatabaseHelper(context);

            // ✅ Use Notificationhelperhabit (the correct class name)
            Notificationhelperhabit notificationHelper = new Notificationhelperhabit(context);

            // ✅ This method now exists and matches the signature
            notificationHelper.rescheduleAllReminders(databaseHelper);

            Log.d(TAG, "All reminders rescheduled after boot");
        }
    }
}
