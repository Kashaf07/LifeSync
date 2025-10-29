package com.example.lifesync;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;
import android.view.MenuItem;

// The class now correctly implements the required abstract method from the interface
public class MainActivity extends AppCompatActivity implements Dashboard_Fragment.NavigationListener {
    private BottomNavigationView bottomNavigationView;

    // --- Implementation of the abstract method from Dashboard_Fragment.NavigationListener ---
    @Override
    public void navigateTo(int navId) {
        // We call the existing centralized method that handles the logic
        loadFragmentAndSelectNav(navId);
    }
    // ------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Make layout fully use the screen area safely
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // FIX: Remove extra space below bottom navigation
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView, (view, insets) -> {
            view.setPadding(0, 0, 0, 0);
            return insets;
        });

        // Load the Dashboard fragment by default
        if (savedInstanceState == null) {
            loadFragmentAndSelectNav(R.id.nav_dashboard);
        }

        // Handle navigation item clicks
        bottomNavigationView.setOnItemSelectedListener(item -> {
            // Use the centralized navigation method
            return loadFragmentAndSelectNav(item.getItemId());
        });
    }

    // Renamed the method for clarity and centralized navigation logic
    private boolean loadFragmentAndSelectNav(int navId) {
        Fragment selectedFragment = null;

        if (navId == R.id.nav_dashboard) {
            selectedFragment = new Dashboard_Fragment();
        } else if (navId == R.id.nav_journal) {
            selectedFragment = new Journal_Fragment();
        } else if (navId == R.id.nav_habits) {
            selectedFragment = new Habit_Fragment();
        } else if (navId == R.id.nav_expense) {
            selectedFragment = new Expense_Fragment();
        } else if (navId == R.id.nav_todo) {
            selectedFragment = new To_Do_Fragment();
        }

        if (selectedFragment != null) {
            // Update the fragment container
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .addToBackStack(null)
                    .commit();

            // Manually set the selected item in the BottomNavigationView
            MenuItem item = bottomNavigationView.getMenu().findItem(navId);
            if (item != null) {
                item.setChecked(true);
            }
            return true;
        }
        return false;
    }
}