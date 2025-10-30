package com.example.lifesync;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;
import android.graphics.Color;
import android.os.Build;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;

    // CONSOLIDATED PRIMARY COLOR (Dark Purple/Violet) for Dashboard, Journal, and To Do
    private static final int COLOR_PRIMARY_DARK = Color.parseColor("#5B5F97");

    // Theme-specific colors from the layouts
    private static final int COLOR_HABITS = Color.parseColor("#2D6071");    // Dark Teal/Cyan (Habit FABs/Header Accent)
    private static final int COLOR_EXPENSE = Color.parseColor("#2E4E3F");   // Dark Green/Teal (Expense Header Text/Accents)


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Make layout fully use the screen area safely (handles gesture navigation)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Remove extra space below bottom navigation
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView, (view, insets) -> {
            // Disable Android's automatic bottom inset padding
            view.setPadding(0, 0, 0, 0);
            return insets;
        });

        // Load the Dashboard fragment by default
        if (savedInstanceState == null) {
            loadFragment(new Dashboard_Fragment());
        }

        // Handle navigation item clicks
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment selectedFragment = null;

            if (id == R.id.nav_dashboard) {
                selectedFragment = new Dashboard_Fragment();
            } else if (id == R.id.nav_journal) {
                selectedFragment = new Journal_Fragment();
            } else if (id == R.id.nav_habits) {
                selectedFragment = new Habit_Fragment();
            } else if (id == R.id.nav_expense) {
                selectedFragment = new Expense_Fragment();
            } else if (id == R.id.nav_todo) {
                selectedFragment = new To_Do_Fragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    private void setStatusBarColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(color);
        }
    }


    private void loadFragment(Fragment fragment) {
        // Check if the fragment is the one currently visible to avoid redundant replace calls.
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment != null && fragment.getClass().equals(currentFragment.getClass())) {
            return;
        }

        // Determine color based on the fragment being loaded
        int statusBarColor;
        if (fragment instanceof Dashboard_Fragment) {
            statusBarColor = COLOR_PRIMARY_DARK; // Consolidated primary color
        } else if (fragment instanceof Journal_Fragment) {
            statusBarColor = COLOR_PRIMARY_DARK; // Consolidated primary color
        } else if (fragment instanceof To_Do_Fragment) {
            statusBarColor = COLOR_PRIMARY_DARK; // Consolidated primary color
        } else if (fragment instanceof Habit_Fragment) {
            statusBarColor = COLOR_HABITS;
        } else if (fragment instanceof Expense_Fragment) {
            statusBarColor = COLOR_EXPENSE;
        } else {
            // Default fallback color
            statusBarColor = Color.BLACK;
        }

        // Apply status bar color before starting the transaction
        setStatusBarColor(statusBarColor);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitAllowingStateLoss();
    }
}