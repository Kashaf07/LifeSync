package com.example.lifesync;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.util.ArrayList;

public class FragmentPrivateJournal extends Fragment implements JournalAdapter.OnJournalClickListener {

    private static final String PREFS_NAME = "JournalPrefs";
    private static final String KEY_PRIVATE_LAYOUT_MODE = "PrivateLayoutModeIsGrid";
    private static final String TAG = "FragmentPrivateJournal";

    private RecyclerView privateJournalsRV;
    private ArrayList<Journal> privateJournalArrayList;
    private JournalAdapter journalAdapter;
    private DBHelper dbHelper;
    private SharedPreferences sharedPreferences;
    private boolean isGridLayout = true;

    public FragmentPrivateJournal() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_private_journal, container, false);

        Toolbar topAppBar = view.findViewById(R.id.topAppBar);
        if (topAppBar != null) {
            topAppBar.setTitle("Hidden Folder");
            topAppBar.setNavigationOnClickListener(v -> {
                if (isAdded()) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }

        dbHelper = new DBHelper(getContext());
        privateJournalsRV = view.findViewById(R.id.privateJournalRecyclerView);

        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        isGridLayout = sharedPreferences.getBoolean(KEY_PRIVATE_LAYOUT_MODE, true);

        updateLayoutManager();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPrivateJournals();
    }

    private void loadPrivateJournals() {
        privateJournalArrayList = dbHelper.readJournals("private");

        // --- THIS IS THE UPDATED LINE ---
        // We pass "private" as the filter string
        journalAdapter = new JournalAdapter(privateJournalArrayList, getContext(), this, "private");

        privateJournalsRV.setAdapter(journalAdapter);
    }

    private void updateLayoutManager() {
        if (isGridLayout) {
            privateJournalsRV.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        } else {
            privateJournalsRV.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        loadPrivateJournals(); // This call will now use the new adapter
    }

    private void saveLayoutPreference(boolean isGrid) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_PRIVATE_LAYOUT_MODE, isGrid);
        editor.apply();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.private_journal_menu, menu);
        if (isGridLayout) {
            menu.findItem(R.id.action_grid_layout_private).setChecked(true);
        } else {
            menu.findItem(R.id.action_linear_layout_private).setChecked(true);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_grid_layout_private) {
            if (!isGridLayout) {
                isGridLayout = true;
                item.setChecked(true);
                saveLayoutPreference(true);
                updateLayoutManager();
            }
            return true;
        } else if (itemId == R.id.action_linear_layout_private) {
            if (isGridLayout) {
                isGridLayout = false;
                item.setChecked(true);
                saveLayoutPreference(false);
                updateLayoutManager();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onJournalClicked(int journalId) {
        Log.d(TAG, "Navigating to view journal: " + journalId);
        Bundle args = new Bundle();
        args.putInt("JOURNAL_ID", journalId);

        FragmentViewJournal viewFragment = new FragmentViewJournal();
        viewFragment.setArguments(args);

        navigateToFragment(viewFragment);
    }

    @Override
    public void onJournalEditClicked(int journalId) {
        Log.d(TAG, "Navigating to edit journal: " + journalId);
        Bundle args = new Bundle();
        args.putInt("JOURNAL_ID", journalId);

        FragmentAddJournal addFragment = new FragmentAddJournal();
        addFragment.setArguments(args);

        navigateToFragment(addFragment);
    }

    private void navigateToFragment(Fragment fragment) {
        if (isAdded() && getParentFragmentManager() != null) {
            try {
                int containerId = R.id.fragment_container;

                getParentFragmentManager().beginTransaction()
                        .replace(containerId, fragment)
                        .addToBackStack(null)
                        .commit();
            } catch (Exception e) {
                Log.e(TAG, "Error navigating to fragment. Container ID (R.id.fragment_container) not found?", e);
                Toast.makeText(getContext(), "Error: Could not navigate.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}