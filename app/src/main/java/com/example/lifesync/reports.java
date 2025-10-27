package com.example.lifesync;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class reports extends Fragment {
    private DatabaseHelper dbHelper;
    private TextView tvSummaryIncome, tvSummaryExpenses, tvSummaryBalance;
    private LineChart lineChartExpenses, lineChartIncome;
    private Button btnDaily, btnMonthly, btnYearly;
    private CardView monthSelectorCard;
    private LinearLayout chartsContainer;
    private Spinner spinnerMonth;
    private SharedPreferences sharedPreferences;
    private String currentFilter = "daily"; // default to daily
    private int selectedMonth = Calendar.getInstance().get(Calendar.MONTH);
    private int selectedYear = Calendar.getInstance().get(Calendar.YEAR);
    private static final String PREFS_NAME = "ExpenseManagerPrefs";
    private static final String CURRENT_ACCOUNT_KEY = "current_account";

    public reports() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports, container, false);

        dbHelper = new DatabaseHelper(getContext());
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String currentAccount = sharedPreferences.getString(CURRENT_ACCOUNT_KEY, null);
        if (currentAccount != null && !currentAccount.isEmpty()) {
            dbHelper.setCurrentAccount(currentAccount);
        }

        // Initialize views
        tvSummaryIncome = view.findViewById(R.id.tvSummaryIncome);
        tvSummaryExpenses = view.findViewById(R.id.tvSummaryExpenses);
        tvSummaryBalance = view.findViewById(R.id.tvSummaryBalance);
        lineChartExpenses = view.findViewById(R.id.lineChartExpenses);
        lineChartIncome = view.findViewById(R.id.lineChartIncome);
        btnDaily = view.findViewById(R.id.btnDaily);
        btnMonthly = view.findViewById(R.id.btnMonthly);
        btnYearly = view.findViewById(R.id.btnYearly);
        monthSelectorCard = view.findViewById(R.id.monthSelectorCard);
        chartsContainer = view.findViewById(R.id.chartsContainer);
        spinnerMonth = view.findViewById(R.id.spinnerMonth);

        // Setup month spinner
        setupMonthSpinner();

        // Update button colors immediately to avoid purple default colors
        updateFilterButtons();

        // Set up filter buttons
        btnDaily.setOnClickListener(v -> {
            currentFilter = "daily";
            updateFilterButtons();
            monthSelectorCard.setVisibility(View.GONE);
            chartsContainer.setVisibility(View.GONE); // Hide charts for daily
            loadAllData();
        });

        btnMonthly.setOnClickListener(v -> {
            currentFilter = "monthly";
            updateFilterButtons();
            monthSelectorCard.setVisibility(View.VISIBLE);
            chartsContainer.setVisibility(View.VISIBLE); // Show charts for monthly
            loadAllData();
        });

        btnYearly.setOnClickListener(v -> {
            currentFilter = "yearly";
            updateFilterButtons();
            monthSelectorCard.setVisibility(View.GONE);
            chartsContainer.setVisibility(View.VISIBLE); // Show charts for yearly
            loadAllData();
        });

        loadAllData();

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().onBackPressed());

        return view;
    }


    private void setupMonthSpinner() {
        List<String> months = new ArrayList<>();
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};

        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH);

        for (int i = 0; i < 12; i++) {
            months.add(monthNames[i] + " " + currentYear);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, months);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(adapter);

        spinnerMonth.setSelection(currentMonth);

        spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMonth = position;
                selectedYear = currentYear;

                if (currentFilter.equals("monthly")) {
                    loadAllData();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        String currentAccount = sharedPreferences.getString(CURRENT_ACCOUNT_KEY, null);
        if (currentAccount != null && !currentAccount.isEmpty() && dbHelper != null) {
            dbHelper.setCurrentAccount(currentAccount);
            loadAllData();
        }
    }

    private void updateFilterButtons() {
        btnDaily.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.parseColor("#F5F5F5")));
        btnDaily.setTextColor(Color.parseColor("#757575"));

        btnMonthly.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.parseColor("#F5F5F5")));
        btnMonthly.setTextColor(Color.parseColor("#757575"));

        btnYearly.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.parseColor("#F5F5F5")));
        btnYearly.setTextColor(Color.parseColor("#757575"));

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

    private void loadAllData() {
        loadSummary();

        // Only load charts if not in daily mode
        if (!currentFilter.equals("daily")) {
            loadExpenseLineChart();
            loadIncomeLineChart();
        }
    }

    private void loadSummary() {
        Map<String, Double> stats = getFilteredStats();
        tvSummaryIncome.setText(String.format("₹ %.2f", stats.getOrDefault("income", 0.0)));
        tvSummaryExpenses.setText(String.format("₹ %.2f", stats.getOrDefault("expenses", 0.0)));
        tvSummaryBalance.setText(String.format("₹ %.2f", stats.getOrDefault("balance", 0.0)));
    }

    private Map<String, Double> getFilteredStats() {
        Map<String, Double> filteredStats = new LinkedHashMap<>();

        Map<String, Double> incomeStats = getFilteredDateWiseStats("income");
        Map<String, Double> expenseStats = getFilteredDateWiseStats("expense");

        double totalIncome = 0.0;
        double totalExpenses = 0.0;

        for (Double value : incomeStats.values()) {
            totalIncome += value;
        }

        for (Double value : expenseStats.values()) {
            totalExpenses += value;
        }

        filteredStats.put("income", totalIncome);
        filteredStats.put("expenses", totalExpenses);
        filteredStats.put("balance", totalIncome - totalExpenses);

        return filteredStats;
    }

    private Map<String, Double> getFilteredDateWiseStats(String type) {
        Map<String, Double> allDateStats = dbHelper.getDateWiseStats(type);
        Map<String, Double> filtered = new LinkedHashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        for (Map.Entry<String, Double> entry : allDateStats.entrySet()) {
            try {
                Date transactionDate = sdf.parse(entry.getKey());
                if (transactionDate == null) continue;

                calendar.setTime(transactionDate);
                int transMonth = calendar.get(Calendar.MONTH);
                int transYear = calendar.get(Calendar.YEAR);
                int transDay = calendar.get(Calendar.DAY_OF_MONTH);

                Calendar now = Calendar.getInstance();
                boolean include = false;

                switch (currentFilter) {
                    case "daily":
                        include = (transDay == now.get(Calendar.DAY_OF_MONTH) &&
                                transMonth == now.get(Calendar.MONTH) &&
                                transYear == now.get(Calendar.YEAR));
                        break;

                    case "monthly":
                        include = (transMonth == selectedMonth && transYear == selectedYear);
                        break;

                    case "yearly":
                        include = (transYear == now.get(Calendar.YEAR));
                        break;
                }

                if (include) {
                    filtered.put(entry.getKey(), entry.getValue());
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return filtered;
    }

    private void loadExpenseLineChart() {
        Map<String, Double> dateStats = getFilteredDateWiseStats("expense");

        List<Entry> entries = new ArrayList<>();
        List<String> dates = new ArrayList<>();

        int index = 0;
        for (Map.Entry<String, Double> entry : dateStats.entrySet()) {
            dates.add(formatDateForDisplay(entry.getKey()));
            entries.add(new Entry(index++, entry.getValue().floatValue()));
        }

        if (entries.isEmpty()) {
            lineChartExpenses.clear();
            lineChartExpenses.setNoDataText("No expense data available");
            lineChartExpenses.setNoDataTextColor(Color.GRAY);
            lineChartExpenses.invalidate();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Expenses");
        dataSet.setColor(Color.parseColor("#FF5252"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleColor(Color.parseColor("#FF5252"));
        dataSet.setCircleRadius(5f);
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setCircleHoleRadius(3f);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.parseColor("#1A1A1A"));
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#FF5252"));
        dataSet.setFillAlpha(30);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);

        LineData lineData = new LineData(dataSet);
        lineChartExpenses.setData(lineData);

        configureChart(lineChartExpenses, dates);
    }

    private void loadIncomeLineChart() {
        Map<String, Double> dateStats = getFilteredDateWiseStats("income");

        List<Entry> entries = new ArrayList<>();
        List<String> dates = new ArrayList<>();

        int index = 0;
        for (Map.Entry<String, Double> entry : dateStats.entrySet()) {
            dates.add(formatDateForDisplay(entry.getKey()));
            entries.add(new Entry(index++, entry.getValue().floatValue()));
        }

        if (entries.isEmpty()) {
            lineChartIncome.clear();
            lineChartIncome.setNoDataText("No income data available");
            lineChartIncome.setNoDataTextColor(Color.GRAY);
            lineChartIncome.invalidate();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Income");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleColor(Color.parseColor("#4CAF50"));
        dataSet.setCircleRadius(5f);
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setCircleHoleRadius(3f);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.parseColor("#1A1A1A"));
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#4CAF50"));
        dataSet.setFillAlpha(30);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);

        LineData lineData = new LineData(dataSet);
        lineChartIncome.setData(lineData);

        configureChart(lineChartIncome, dates);
    }

    private String formatDateForDisplay(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateStr;
        }
    }

    private void configureChart(LineChart chart, List<String> dates) {
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDoubleTapToZoomEnabled(true);
        chart.setDragDecelerationEnabled(true);
        chart.setDragDecelerationFrictionCoef(0.9f);

        if (dates.size() > 7) {
            chart.setVisibleXRangeMaximum(7f);
            chart.setVisibleXRangeMinimum(3f);
        }
        chart.moveViewToX(0);

        Description description = new Description();
        description.setText("");
        chart.setDescription(description);

        chart.getLegend().setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.parseColor("#757575"));
        xAxis.setTextSize(10f);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dates));
        xAxis.setLabelRotationAngle(-45f);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.parseColor("#757575"));
        leftAxis.setTextSize(10f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#E0E0E0"));
        leftAxis.setAxisMinimum(0f);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        chart.animateY(1000);
        chart.invalidate();
    }
}
