package com.example.lifesync;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Expense_Fragment extends Fragment {
    private DatabaseHelper dbHelper;
    private TextView tvBalance, tvIncome, tvExpenses;
    private CardView cardIncome, cardExpense, cardTransactions, cardReports;
    private ImageButton btnProfile;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "ExpenseManagerPrefs";
    private static final String ACCOUNTS_KEY = "accounts";
    private static final String CURRENT_ACCOUNT_KEY = "current_account";

    public Expense_Fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_expense, container, false);

        dbHelper = new DatabaseHelper(getContext());
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Load current account and set it in database helper
        String currentAccount = getCurrentAccount();
        if (currentAccount == null || currentAccount.isEmpty()) {
            // First time launch - create and set default "Personal" account
            createAccount("Personal");
            currentAccount = "Personal";
            sharedPreferences.edit().putString(CURRENT_ACCOUNT_KEY, currentAccount).apply();
        }
        dbHelper.setCurrentAccount(currentAccount);

        tvBalance = view.findViewById(R.id.tvBalance);
        tvIncome = view.findViewById(R.id.tvIncome);
        tvExpenses = view.findViewById(R.id.tvExpenses);
        cardIncome = view.findViewById(R.id.cardIncome);
        cardExpense = view.findViewById(R.id.cardExpense);
        cardTransactions = view.findViewById(R.id.cardTransactions);
        cardReports = view.findViewById(R.id.cardReports);
        btnProfile = view.findViewById(R.id.btnProfile);

        updateStats();

        // Profile button click listener
        btnProfile.setOnClickListener(v -> showAccountManagementDialog());

        cardIncome.setOnClickListener(v -> {
            Fragment fragment = new add_income();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        cardExpense.setOnClickListener(v -> {
            Fragment fragment = new add_expense();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        cardTransactions.setOnClickListener(v -> {
            Fragment fragment = new transactions();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        cardReports.setOnClickListener(v -> {
            Fragment fragment = new reports();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStats();
    }

    private void updateStats() {
        if (dbHelper != null && tvBalance != null && tvIncome != null && tvExpenses != null) {
            Map<String, Double> stats = dbHelper.getStats();
            tvBalance.setText(String.format("₹%.2f", stats.get("balance")));
            tvIncome.setText(String.format("₹%.2f", stats.get("income")));
            tvExpenses.setText(String.format("₹%.2f", stats.get("expenses")));
        }
    }

    private void showAccountManagementDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_account_management, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        Button btnCreateAccount = dialogView.findViewById(R.id.btnCreateAccount);
        ListView lvAccounts = dialogView.findViewById(R.id.lvAccounts);
        TextView tvNoAccounts = dialogView.findViewById(R.id.tvNoAccounts);
        TextView tvCurrentAccount = dialogView.findViewById(R.id.tvCurrentAccount);

        // Load and display accounts
        List<String> accounts = getAccounts();
        String currentAccount = getCurrentAccount();

        if (currentAccount != null) {
            tvCurrentAccount.setText("Current Account: " + currentAccount);
            tvCurrentAccount.setVisibility(View.VISIBLE);
        } else {
            tvCurrentAccount.setVisibility(View.GONE);
        }

        if (accounts.isEmpty()) {
            tvNoAccounts.setVisibility(View.VISIBLE);
            lvAccounts.setVisibility(View.GONE);
        } else {
            tvNoAccounts.setVisibility(View.GONE);
            lvAccounts.setVisibility(View.VISIBLE);

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(),
                    android.R.layout.simple_list_item_1, accounts) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView textView = view.findViewById(android.R.id.text1);
                    textView.setPadding(30, 30, 30, 30);
                    textView.setTextSize(16);

                    String account = getItem(position);
                    if (account != null && account.equals(currentAccount)) {
                        textView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        textView.setText(account + " ✓");
                    }

                    return view;
                }
            };
            lvAccounts.setAdapter(adapter);

            lvAccounts.setOnItemClickListener((parent, view, position, id) -> {
                String selectedAccount = accounts.get(position);
                switchAccount(selectedAccount);
                dialog.dismiss();
            });

            lvAccounts.setOnItemLongClickListener((parent, view, position, id) -> {
                String selectedAccount = accounts.get(position);
                showDeleteAccountDialog(selectedAccount, dialog);
                return true;
            });
        }

        btnCreateAccount.setOnClickListener(v -> {
            dialog.dismiss();
            showCreateAccountDialog();
        });

        dialog.show();
    }

    private void showCreateAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Create New Account");

        final EditText input = new EditText(requireContext());
        input.setHint("Enter account name");
        input.setPadding(50, 30, 50, 30);
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String accountName = input.getText().toString().trim();
            if (!accountName.isEmpty()) {
                if (createAccount(accountName)) {
                    Toast.makeText(requireContext(), "Account created: " + accountName, Toast.LENGTH_SHORT).show();
                    switchAccount(accountName);
                } else {
                    Toast.makeText(requireContext(), "Account already exists", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "Please enter a valid name", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showDeleteAccountDialog(String accountName, AlertDialog parentDialog) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete '" + accountName + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (deleteAccount(accountName)) {
                        Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show();
                        parentDialog.dismiss();

                        // If deleted account was current, switch to another or clear
                        if (accountName.equals(getCurrentAccount())) {
                            List<String> remainingAccounts = getAccounts();
                            if (!remainingAccounts.isEmpty()) {
                                switchAccount(remainingAccounts.get(0));
                            } else {
                                clearCurrentAccount();
                                updateStats();
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean createAccount(String accountName) {
        Set<String> accounts = sharedPreferences.getStringSet(ACCOUNTS_KEY, new HashSet<>());
        Set<String> newAccounts = new HashSet<>(accounts);

        if (newAccounts.contains(accountName)) {
            return false;
        }

        newAccounts.add(accountName);
        sharedPreferences.edit().putStringSet(ACCOUNTS_KEY, newAccounts).apply();
        return true;
    }

    private boolean deleteAccount(String accountName) {
        Set<String> accounts = sharedPreferences.getStringSet(ACCOUNTS_KEY, new HashSet<>());
        Set<String> newAccounts = new HashSet<>(accounts);

        boolean removed = newAccounts.remove(accountName);
        if (removed) {
            sharedPreferences.edit().putStringSet(ACCOUNTS_KEY, newAccounts).apply();
        }
        return removed;
    }

    private List<String> getAccounts() {
        Set<String> accounts = sharedPreferences.getStringSet(ACCOUNTS_KEY, new HashSet<>());
        return new ArrayList<>(accounts);
    }

    private String getCurrentAccount() {
        return sharedPreferences.getString(CURRENT_ACCOUNT_KEY, null);
    }

    private void switchAccount(String accountName) {
        sharedPreferences.edit().putString(CURRENT_ACCOUNT_KEY, accountName).apply();
        Toast.makeText(requireContext(), "Switched to: " + accountName, Toast.LENGTH_SHORT).show();

        // Update the database helper to use this account
        if (dbHelper != null) {
            dbHelper.setCurrentAccount(accountName);
        }
        updateStats();
    }

    private void clearCurrentAccount() {
        sharedPreferences.edit().remove(CURRENT_ACCOUNT_KEY).apply();
    }
}