package com.example.lifesync;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Load the Dashboard fragment by default
        if (savedInstanceState == null) {
            loadFragment(new Dashboard_Fragment());
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                loadFragment(new Dashboard_Fragment());
                return true;
            } else if (id == R.id.nav_journal) {
                loadFragment(new Journal_Fragment());
                return true;
            } else if (id == R.id.nav_habits) {
                loadFragment(new Habit_Fragment());
                return true;
            } else if (id == R.id.nav_expense) {
                loadFragment(new Expense_Fragment());
                return true;
            } else if (id == R.id.nav_todo) {
                loadFragment(new To_Do_Fragment());
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

