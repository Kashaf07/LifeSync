package com.example.lifesync;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // ✅ Make layout fully use the screen area safely (handles gesture navigation)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // 🧩 FIX: Remove extra space below bottom navigation
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

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
