package com.example.lifesync;

import android.content.Intent; // --- ADDED ---
import android.os.Bundle;
import androidx.annotation.Nullable; // --- ADDED ---
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

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView, (view, insets) -> {
            view.setPadding(0, 0, 0, 0);
            return insets;
        });

        if (savedInstanceState == null) {
            // --- FIX: Ensure container isn't null and load fragment ---
            if (findViewById(R.id.fragment_container) != null) {
                loadFragment(new Dashboard_Fragment());
            }
        }

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
        // --- FIX: Use getSupportFragmentManager() ---
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    // --- ADDED: Handle onActivityResult for FragmentViewJournal lock screen ---
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Find the current fragment in the container
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        // Check if it's the FragmentViewJournal
        if (currentFragment instanceof FragmentViewJournal) {
            // Forward the result to the fragment's custom method
            ((FragmentViewJournal) currentFragment).onActivityResultExternal(requestCode, resultCode, data);
        }

        // --- ADDED: Also forward to Journal_Fragment (for its lock screen) ---
        else if (currentFragment instanceof Journal_Fragment) {
            // Journal_Fragment uses the standard onActivityResult
            currentFragment.onActivityResult(requestCode, resultCode, data);
        }
    }
}