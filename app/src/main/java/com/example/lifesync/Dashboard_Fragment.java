package com.example.lifesync;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class Dashboard_Fragment extends Fragment {

    private CardView cardToDo, cardJournal, cardHabits, cardExpense;
    private CardView btnAddTask, btnAddExpense, btnLogHabit, btnWriteJournal;

    public Dashboard_Fragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Initialize navigation cards
        cardToDo = view.findViewById(R.id.cardToDo);
        cardJournal = view.findViewById(R.id.cardJournal);
        cardHabits = view.findViewById(R.id.cardHabits);
        cardExpense = view.findViewById(R.id.cardExpense);

        // Initialize quick action buttons
        btnAddTask = view.findViewById(R.id.btnAddTask);
        btnAddExpense = view.findViewById(R.id.btnAddExpense);
        btnLogHabit = view.findViewById(R.id.btnLogHabit);
        btnWriteJournal = view.findViewById(R.id.btnWriteJournal);

        // Set click listeners for navigation cards
        cardToDo.setOnClickListener(v -> navigateToFragment(new To_Do_Fragment()));
        cardJournal.setOnClickListener(v -> navigateToFragment(new Journal_Fragment()));
        cardHabits.setOnClickListener(v -> navigateToFragment(new Habit_Fragment()));
        cardExpense.setOnClickListener(v -> navigateToFragment(new Expense_Fragment()));

        // Set click listeners for quick action buttons
        btnAddTask.setOnClickListener(v -> navigateToFragment(new To_Do_Fragment()));

        btnAddExpense.setOnClickListener(v -> {
            // Navigate directly to add expense page
            navigateToFragment(new add_expense());
        });

        btnLogHabit.setOnClickListener(v -> navigateToFragment(new Habit_Fragment()));

        btnWriteJournal.setOnClickListener(v -> navigateToFragment(new Journal_Fragment()));

        return view;
    }

    private void navigateToFragment(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}