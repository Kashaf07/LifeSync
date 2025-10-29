package com.example.lifesync;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

public class JournalAdapter extends RecyclerView.Adapter<JournalAdapter.ViewHolder> {
    private final ArrayList<Journal> journalArrayList;
    private final Context context;
    private final DBHelper dbHelper;
    // --- ADDED ---
    private final OnJournalClickListener clickListener;

    // --- Interface for navigation callbacks ---
    public interface OnJournalClickListener {
        void onJournalClicked(int journalId);
        void onJournalEditClicked(int journalId);
    }

    // --- UPDATED CONSTRUCTOR ---
    public JournalAdapter(ArrayList<Journal> journalArrayList, Context context, OnJournalClickListener listener) {
        this.journalArrayList = journalArrayList;
        this.context = context;
        this.dbHelper = new DBHelper(context);
        this.clickListener = listener; // Assign listener
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.journal_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Journal journal = journalArrayList.get(position);
        holder.titleTV.setText(journal.getTitle());
        holder.dateTV.setText(journal.getDate());


        // --- UPDATED: Use listener for navigation ---
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onJournalClicked(journal.getId());
            }
        });

        holder.optionsIV.setOnClickListener(v -> showPopupMenu(holder.optionsIV, journal, position));
    }

    private void showPopupMenu(View view, Journal journal, int position) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.inflate(R.menu.journal_item_menu);

        MenuItem favoriteItem = popup.getMenu().findItem(R.id.action_toggle_favorite);
        MenuItem privateItem = popup.getMenu().findItem(R.id.action_toggle_private);

        if (journal.isFavorite()) {
            favoriteItem.setTitle("Remove from Favorites");
        } else {
            favoriteItem.setTitle("Add to Favorites");
        }

        if (journal.isPrivate()) {
            privateItem.setTitle("Make Public");
        } else {
            privateItem.setTitle("Make Private");
        }


        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            // --- UPDATED: Use listener for edit navigation ---
            if (itemId == R.id.action_edit) {
                if (clickListener != null) {
                    clickListener.onJournalEditClicked(journal.getId());
                }
                return true;
            } else if (itemId == R.id.action_delete) {
                new AlertDialog.Builder(context)
                        .setTitle("Delete Journal")
                        .setMessage("Are you sure you want to delete this journal entry?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            String audioPath = journal.getAudioPath();
                            if (audioPath != null && !audioPath.isEmpty()) {
                                try {
                                    new File(audioPath).delete();
                                } catch (Exception e) {
                                    // Log error, but proceed with DB delete
                                }
                            }

                            dbHelper.deleteJournal(journal.getId());
                            journalArrayList.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, journalArrayList.size());
                            Toast.makeText(context, "Journal Deleted", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("No", null)
                        .show();
                return true;
            } else if (itemId == R.id.action_toggle_favorite) {
                journal.setFavorite(!journal.isFavorite());
                dbHelper.updateJournal(journal);
                notifyItemChanged(position);
                return true;
            } else if (itemId == R.id.action_toggle_private) {
                journal.setPrivate(!journal.isPrivate());
                dbHelper.updateJournal(journal);
                journalArrayList.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, journalArrayList.size());
                Toast.makeText(context, journal.isPrivate() ? "Journal made private" : "Journal made public", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                return false;
            }
        });
        popup.show();
    }


    @Override
    public int getItemCount() {
        return journalArrayList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTV;
        private final TextView dateTV;
        private final ImageView optionsIV;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTV = itemView.findViewById(R.id.journalTitle);
            dateTV = itemView.findViewById(R.id.journalDate);
            optionsIV = itemView.findViewById(R.id.optionsMenu);
        }
    }
}