package com.example.lifesync;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment; // Added Fragment import
import android.util.Log; // <-- FIX: Added missing Log import
import java.util.Arrays;
import java.util.List;

public class FilterDialogFragment extends DialogFragment {

    private static final String TAG = "FilterDialogFragment"; // FIX: Added missing TAG definition

    public interface FilterDialogListener {
        void onFilterSelected(String status, String time);
    }

    private FilterDialogListener listener;
    private String selectedStatus = "All";
    private String selectedTime = "All";

    private List<TextView> statusChips;
    private List<TextView> timeChips;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Assuming R.style.CustomDialogTheme exists in your project
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialogTheme);

        if (getArguments() != null) {
            selectedStatus = getArguments().getString("status", "All");
            selectedTime = getArguments().getString("time", "All");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().setGravity(Gravity.CENTER);
        }

        // Assuming R.layout.dialog_filter_options exists in your project
        View view = inflater.inflate(R.layout.dialog_filter_options, container, false);

        initializeChips(view);
        setupChipClickListeners();

        TextView initialStatusChip = findChipByText(statusChips, selectedStatus);
        if (initialStatusChip != null) updateChipUI(statusChips, initialStatusChip);

        TextView initialTimeChip = findChipByText(timeChips, selectedTime);
        if (initialTimeChip != null) updateChipUI(timeChips, initialTimeChip);

        return view;
    }

    private TextView findChipByText(List<TextView> chips, String text) {
        for (TextView chip : chips) {
            if (chip.getText().toString().equalsIgnoreCase(text)) {
                return chip;
            }
        }
        return null;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;

            // Note: R.dimen.dialog_horizontal_margin is assumed to exist for calculating width
            try {
                int margin = getResources().getDimensionPixelSize(R.dimen.dialog_horizontal_margin);
                width = width - (2 * margin);
            } catch (Exception e) {
                // Fallback width if dimension resource is not found
                width = (int) (width * 0.80);
            }

            dialog.getWindow().setLayout(width, height);
        }
    }

    private void initializeChips(View view) {
        // Assuming the following IDs exist in R.layout.dialog_filter_options
        statusChips = Arrays.asList(
                view.findViewById(R.id.tvStatusAll),
                view.findViewById(R.id.tvStatusPending),
                view.findViewById(R.id.tvStatusComplete)
        );

        timeChips = Arrays.asList(
                view.findViewById(R.id.tvTimeAll),
                view.findViewById(R.id.tvTimeMorning),
                view.findViewById(R.id.tvTimeAfternoon),
                view.findViewById(R.id.tvTimeEvening),
                // Assuming R.id.tvTimeAnytime corresponds to the "Night" chip based on context
                view.findViewById(R.id.tvTimeAnytime)
        );
    }

    private void setupChipClickListeners() {
        setupChipGroup(statusChips, "status");
        setupChipGroup(timeChips, "time");
    }

    private void setupChipGroup(List<TextView> chips, final String category) {
        for (TextView chip : chips) {
            chip.setOnClickListener(v -> {
                String selectedText = ((TextView) v).getText().toString();
                switch (category) {
                    case "status":
                        selectedStatus = selectedText;
                        break;
                    case "time":
                        selectedTime = selectedText;
                        break;
                }

                updateChipUI(chips, v);

                // FIX: Check if listener is set before calling the method
                if (listener != null) {
                    listener.onFilterSelected(selectedStatus, selectedTime);
                }
                dismiss();
            });
        }
    }

    private void updateChipUI(List<TextView> chips, View selectedView) {
        for (TextView chip : chips) {
            // Assuming R.drawable.filter_chip_selected_bg and R.drawable.filter_chip_unselected_bg exist
            if (chip == selectedView) {
                chip.setBackgroundResource(R.drawable.filter_chip_selected_bg);
                chip.setTextColor(Color.WHITE);
            } else {
                chip.setBackgroundResource(R.drawable.filter_chip_unselected_bg);
                chip.setTextColor(Color.BLACK);
            }
        }
    }

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);

        // FIX: Prioritize Target Fragment listener (Habit_Fragment)
        Fragment targetFragment = getTargetFragment();
        if (targetFragment instanceof FilterDialogListener) {
            listener = (FilterDialogListener) targetFragment;
            return;
        }

        // Fallback: If no target fragment, check if the hosting Activity is the listener
        try {
            listener = (FilterDialogListener) context;
        } catch (ClassCastException e) {
            // This is acceptable, as Habit_Fragment is now the primary listener
            Log.w(TAG, context.toString() + " does not implement FilterDialogListener. Relying on Target Fragment.");
        }
    }
}
