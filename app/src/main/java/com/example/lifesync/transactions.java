package com.example.lifesync;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class transactions extends Fragment {
    private DatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private SharedPreferences sharedPreferences;
    private TextView tvTransactionCount;
    private LinearLayout emptyState;
    private Button btnDaily, btnMonthly, btnYearly;
    private String currentFilter = "daily"; // default filter
    private static final String PREFS_NAME = "ExpenseManagerPrefs";
    private static final String CURRENT_ACCOUNT_KEY = "current_account";

    public transactions() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);

        dbHelper = new DatabaseHelper(getContext());
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Load and set current account in database helper
        String currentAccount = getCurrentAccount();
        android.util.Log.d("Transactions", "Current Account: " + currentAccount);

        if (currentAccount != null && !currentAccount.isEmpty()) {
            dbHelper.setCurrentAccount(currentAccount);
        } else {
            dbHelper.setCurrentAccount(null);
            android.util.Log.d("Transactions", "No account selected - showing all transactions");
        }

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerView);
        tvTransactionCount = view.findViewById(R.id.tvTransactionCount);
        emptyState = view.findViewById(R.id.emptyState);
        btnDaily = view.findViewById(R.id.btnDaily);
        btnMonthly = view.findViewById(R.id.btnMonthly);
        btnYearly = view.findViewById(R.id.btnYearly);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        updateFilterButtons();

        // Set up filter buttons
        btnDaily.setOnClickListener(v -> {
            currentFilter = "daily";
            updateFilterButtons();
            loadTransactions();
        });

        btnMonthly.setOnClickListener(v -> {
            currentFilter = "monthly";
            updateFilterButtons();
            loadTransactions();
        });

        btnYearly.setOnClickListener(v -> {
            currentFilter = "yearly";
            updateFilterButtons();
            loadTransactions();
        });

        loadTransactions();

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().onBackPressed());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        String currentAccount = getCurrentAccount();
        if (currentAccount != null && dbHelper != null) {
            dbHelper.setCurrentAccount(currentAccount);
            loadTransactions();
        }
    }

    private String getCurrentAccount() {
        return sharedPreferences.getString(CURRENT_ACCOUNT_KEY, null);
    }

    private void updateFilterButtons() {
        // Reset all buttons to inactive state
        btnDaily.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                getResources().getColor(android.R.color.white)));
        btnDaily.setTextColor(getResources().getColor(android.R.color.darker_gray));

        btnMonthly.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                getResources().getColor(android.R.color.white)));
        btnMonthly.setTextColor(getResources().getColor(android.R.color.darker_gray));

        btnYearly.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                getResources().getColor(android.R.color.white)));
        btnYearly.setTextColor(getResources().getColor(android.R.color.darker_gray));

        // Set active button
        int activeColor = Color.parseColor("#4CAF50");
        int activeTextColor = Color.WHITE;

        switch (currentFilter) {
            case "daily":
                btnDaily.setBackgroundTintList(android.content.res.ColorStateList.valueOf(activeColor));
                btnDaily.setTextColor(activeTextColor);
                break;
            case "monthly":
                btnMonthly.setBackgroundTintList(android.content.res.ColorStateList.valueOf(activeColor));
                btnMonthly.setTextColor(activeTextColor);
                break;
            case "yearly":
                btnYearly.setBackgroundTintList(android.content.res.ColorStateList.valueOf(activeColor));
                btnYearly.setTextColor(activeTextColor);
                break;
        }
    }

    private void loadTransactions() {
        List<Transaction> allTransactions = dbHelper.getAllTransactions();
        List<Transaction> filteredTransactions = filterTransactionsByPeriod(allTransactions, currentFilter);

        android.util.Log.d("Transactions", "Filtered transactions count: " + filteredTransactions.size());

        // Update transaction count
        if (tvTransactionCount != null) {
            String countText = filteredTransactions.size() + (filteredTransactions.size() == 1 ? " item" : " items");
            tvTransactionCount.setText(countText);
        }

        // Show/hide empty state
        if (filteredTransactions.isEmpty()) {
            if (emptyState != null) {
                emptyState.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        } else {
            if (emptyState != null) {
                emptyState.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }

        adapter = new TransactionAdapter(filteredTransactions);
        recyclerView.setAdapter(adapter);
    }

    private List<Transaction> filterTransactionsByPeriod(List<Transaction> transactions, String period) {
        List<Transaction> filtered = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentYear = calendar.get(Calendar.YEAR);

        for (Transaction transaction : transactions) {
            try {
                Date transactionDate = sdf.parse(transaction.getDate());
                if (transactionDate == null) continue;

                calendar.setTime(transactionDate);
                int transDay = calendar.get(Calendar.DAY_OF_MONTH);
                int transMonth = calendar.get(Calendar.MONTH);
                int transYear = calendar.get(Calendar.YEAR);

                switch (period) {
                    case "daily":
                        // Today's transactions
                        Calendar today = Calendar.getInstance();
                        if (transDay == today.get(Calendar.DAY_OF_MONTH) &&
                                transMonth == today.get(Calendar.MONTH) &&
                                transYear == today.get(Calendar.YEAR)) {
                            filtered.add(transaction);
                        }
                        break;

                    case "monthly":
                        // Current month's transactions
                        Calendar now = Calendar.getInstance();
                        if (transMonth == now.get(Calendar.MONTH) &&
                                transYear == now.get(Calendar.YEAR)) {
                            filtered.add(transaction);
                        }
                        break;

                    case "yearly":
                        // Current year's transactions
                        if (transYear == Calendar.getInstance().get(Calendar.YEAR)) {
                            filtered.add(transaction);
                        }
                        break;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return filtered;
    }

    private String convertDateFormat(String dateStr) {
        try {
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return displayFormat.format(dbFormat.parse(dateStr));
        } catch (Exception e) {
            e.printStackTrace();
            return dateStr;
        }
    }

    class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
        private List<Transaction> transactions;

        TransactionAdapter(List<Transaction> transactions) {
            this.transactions = transactions;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transaction, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Transaction t = transactions.get(position);

            holder.tvTitle.setText(t.getCategory());
            holder.tvDate.setText(convertDateFormat(t.getDate()));

            if (t.getDescription() != null && !t.getDescription().trim().isEmpty()) {
                holder.tvDescription.setVisibility(View.VISIBLE);
                holder.tvDescription.setText(t.getDescription());
            } else {
                holder.tvDescription.setVisibility(View.GONE);
            }

            holder.tvAmount.setText(String.format("₹ %,.0f", t.getAmount()));

            if (t.getType().equals("income")) {
                holder.tvIcon.setText("💰");
                holder.typeIndicator.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                getResources().getColor(android.R.color.holo_green_light)
                        )
                );
                holder.tvAmount.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                holder.tvIcon.setText("💸");
                holder.typeIndicator.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                getResources().getColor(android.R.color.holo_red_light)
                        )
                );
                holder.tvAmount.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }

            holder.itemView.setOnLongClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Delete Transaction")
                        .setMessage("Are you sure you want to delete this transaction?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            dbHelper.deleteTransaction(t.getId());
                            transactions.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, transactions.size());

                            if (tvTransactionCount != null) {
                                String countText = transactions.size() + (transactions.size() == 1 ? " item" : " items");
                                tvTransactionCount.setText(countText);
                            }

                            if (transactions.isEmpty() && emptyState != null) {
                                emptyState.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return transactions.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDescription, tvDate, tvAmount, tvIcon;
            LinearLayout typeIndicator;

            ViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvDescription = itemView.findViewById(R.id.tvDescription);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                tvIcon = itemView.findViewById(R.id.tvIcon);
                typeIndicator = itemView.findViewById(R.id.typeIndicator);
            }
        }
    }
}
