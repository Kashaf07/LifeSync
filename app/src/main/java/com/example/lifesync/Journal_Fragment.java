package com.example.lifesync;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

public class Journal_Fragment extends Fragment implements JournalAdapter.OnJournalClickListener {

    private static final int LOCK_REQUEST_CODE_PRIVATE_FOLDER = 121;
    private static final String PREFS_NAME = "JournalPrefs";
    private static final String KEY_LAYOUT_MODE = "LayoutModeIsGrid";
    private static final String TAG = "Journal_Fragment";

    private RecyclerView journalsRV;
    private ArrayList<Journal> journalArrayList;
    private JournalAdapter journalAdapter;
    private DBHelper dbHelper;
    private String currentFilter = "all"; // Default filter

    private SharedPreferences sharedPreferences;
    private boolean isGridLayout = false;

    public Journal_Fragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_journal, container, false);

        MaterialToolbar topAppBar = view.findViewById(R.id.topAppBar);

        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(topAppBar);
        }

        dbHelper = new DBHelper(getContext());
        journalsRV = view.findViewById(R.id.journalRecyclerView);

        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        isGridLayout = sharedPreferences.getBoolean(KEY_LAYOUT_MODE, false);

        FloatingActionButton newEntryFab = view.findViewById(R.id.newEntryFab);

        newEntryFab.setOnClickListener(fabView -> {
            Bundle args = new Bundle();
            args.putInt("JOURNAL_ID", -1);

            FragmentAddJournal addFragment = new FragmentAddJournal();
            addFragment.setArguments(args);

            navigateToFragment(addFragment);
        });

        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: loadJournals("all"); break;
                    case 1: loadJournals("drafts"); break;
                    case 2: loadJournals("favorites"); break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        updateLayoutManager();
        return view;
    }

    private void updateLayoutManager() {
        if (isGridLayout) {
            journalsRV.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        } else {
            journalsRV.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        loadJournals(currentFilter); // This call will now pass the filter
    }

    private void saveLayoutPreference(boolean isGrid) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_LAYOUT_MODE, isGrid);
        editor.apply();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getView() != null && getActivity() instanceof AppCompatActivity) {
            MaterialToolbar topAppBar = getView().findViewById(R.id.topAppBar);
            ((AppCompatActivity) getActivity()).setSupportActionBar(topAppBar);
        }

        loadJournals(currentFilter); // Reloads the list with the correct filter
    }

    private void loadJournals(String filter) {
        currentFilter = filter;
        journalArrayList = dbHelper.readJournals(filter);

        // --- THIS IS THE UPDATED LINE ---
        // We now pass the 'currentFilter' string to the adapter's constructor
        journalAdapter = new JournalAdapter(journalArrayList, getContext(), this, currentFilter);

        journalsRV.setAdapter(journalAdapter);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
        if (isGridLayout) {
            menu.findItem(R.id.action_grid_layout).setChecked(true);
        } else {
            menu.findItem(R.id.action_linear_layout).setChecked(true);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_hidden_folder) {
            requestAuthentication(LOCK_REQUEST_CODE_PRIVATE_FOLDER, "Unlock to view hidden folder");
            return true;
        } else if (itemId == R.id.action_grid_layout) {
            if (!isGridLayout) {
                isGridLayout = true; item.setChecked(true);
                saveLayoutPreference(true); updateLayoutManager();
            }
            return true;
        } else if (itemId == R.id.action_linear_layout) {
            if (isGridLayout) {
                isGridLayout = false; item.setChecked(true);
                saveLayoutPreference(false); updateLayoutManager();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void requestAuthentication(int requestCode, String description) {
        KeyguardManager keyguardManager = (KeyguardManager) requireContext().getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager.isKeyguardSecure()) {
            Intent intent = keyguardManager.createConfirmDeviceCredentialIntent("Authentication Required", description);
            if (intent != null) {
                startActivityForResult(intent, requestCode);
            } else {
                Toast.makeText(getContext(), "Could not launch lock screen.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "Please set a screen lock to use this feature.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == getActivity().RESULT_OK) {
            if (requestCode == LOCK_REQUEST_CODE_PRIVATE_FOLDER) {
                navigateToFragment(new FragmentPrivateJournal());
            }
        }
    }

    // --- JournalAdapter.OnJournalClickListener Implementation ---

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