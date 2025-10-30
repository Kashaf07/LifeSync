package com.example.lifesync;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity; // --- ADDED IMPORT ---
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction; // --- ADDED ---
// --- REMOVED: import androidx.navigation.fragment.NavHostFragment; ---

// --- FIXED IMPORTS: Ensure these point to your lifecync package ---
// import com.example.lifesync.DBHelper;
// import com.example.lifesync.Journal;
// --- END FIXED IMPORTS ---

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class FragmentViewJournal extends Fragment {

    private static final int INITIAL_LOCK_REQUEST_CODE = 221;
    private static final int ACTION_LOCK_REQUEST_CODE = 222;
    private static final String TAG = "FragmentViewJournal";

    private TextView titleTextView, dateTextView;
    private WebView contentWebView;
    private DBHelper dbHelper;
    private int journalId;
    private Journal currentJournal;
    private boolean isUnlocked = false;
    private Runnable pendingAction;

    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private LinearLayout playerLayout;
    private ImageButton buttonPlay;
    private TextView textAudioStatus;
    private SeekBar audioSeekBar;
    private Handler seekBarHandler;
    private Runnable updateSeekBarRunnable;
    private String currentAudioPath = null;

    public FragmentViewJournal() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null && getArguments().containsKey("JOURNAL_ID")) {
            journalId = getArguments().getInt("JOURNAL_ID");
        } else {
            Log.e(TAG, "FragmentViewJournal created without a JOURNAL_ID argument.");
            Toast.makeText(getContext(), "Error: No journal ID provided.", Toast.LENGTH_SHORT).show();
            // --- UPDATED: Use FragmentManager ---
            if (isAdded()) {
                getParentFragmentManager().popBackStack();
            }
        }
    }

    @SuppressLint({"SetJavaScriptEnabled"})
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_journal, container, false);

        titleTextView = view.findViewById(R.id.titleTextView);
        dateTextView = view.findViewById(R.id.dateTextView);
        contentWebView = view.findViewById(R.id.contentWebView);

        playerLayout = view.findViewById(R.id.audioPlayerLayout);
        buttonPlay = playerLayout.findViewById(R.id.buttonPlay);
        textAudioStatus = playerLayout.findViewById(R.id.textAudioStatus);
        audioSeekBar = playerLayout.findViewById(R.id.audioSeekBar);

        seekBarHandler = new Handler(Looper.getMainLooper());
        contentWebView.setBackgroundColor(0);
        contentWebView.getSettings().setJavaScriptEnabled(false);
        contentWebView.getSettings().setDefaultTextEncodingName("utf-8");

        dbHelper = new DBHelper(requireContext());

        setupSeekBarListener();

        // --- UPDATED: Handle back navigation from toolbar ---
        androidx.appcompat.widget.Toolbar topAppBar = view.findViewById(R.id.topAppBar);

        // You must tell the Activity to use your fragment's toolbar
        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(topAppBar);
            // Optional: Set title
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("View Journal");
        }

        if (topAppBar != null) {
            topAppBar.setNavigationOnClickListener(v -> {
                if (isAdded()) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        displayJournalContentOrRequestInitialUnlock();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isPlaying) {
            stopPlayback();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPlayback();
        stopSeekBarUpdate();

        // --- THIS IS CRITICAL ---
        // When the view is destroyed, clear the Activity's app bar
        // to avoid this fragment's menu showing on other fragments.
        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(null);
        }
        // --- END CRITICAL ---
    }

    private void setupSeekBarListener() {
        audioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    try {
                        if (mediaPlayer.getDuration() > 0) {
                            mediaPlayer.seekTo(progress);
                            updateAudioStatusText(progress, mediaPlayer.getDuration());
                        }
                    } catch (IllegalStateException e) {
                        Log.w(TAG, "IllegalStateException during SeekBar seekTo.");
                        stopPlayback();
                    }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { stopSeekBarUpdate(); }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    startSeekBarUpdate();
                }
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void displayJournalContentOrRequestInitialUnlock() {
        currentJournal = dbHelper.getJournal(journalId);
        if (currentJournal == null) {
            Log.e(TAG, "Failed to load journal with ID: " + journalId);
            Toast.makeText(getContext(), "Error loading journal.", Toast.LENGTH_SHORT).show();
            // --- UPDATED: Use FragmentManager ---
            if (isAdded()) {
                getParentFragmentManager().popBackStack();
            }
            return;
        }

        if (currentJournal.isPrivate() && !isUnlocked) {
            titleTextView.setText("Private Journal");
            dateTextView.setText("");
            contentWebView.loadData("<html><body style='padding:16px; color:#757575;'>Unlock to view this journal.</body></html>", "text/html", "UTF-8");
            playerLayout.setVisibility(View.GONE);
            requestAuthentication(INITIAL_LOCK_REQUEST_CODE, "Unlock to view this journal.");
        } else {
            // This 'else' block will now run correctly after you unlock
            titleTextView.setText(currentJournal.getTitle());
            dateTextView.setText(currentJournal.getDate());
            currentAudioPath = currentJournal.getAudioPath();
            if (currentAudioPath != null && !currentAudioPath.isEmpty() && new File(currentAudioPath).exists()) {
                playerLayout.setVisibility(View.VISIBLE);
                setupPlayerListener();
                int duration = getAudioDuration(currentAudioPath);
                audioSeekBar.setMax(duration > 0 ? duration : 100);
                audioSeekBar.setProgress(0);
                updateAudioStatusText(0, duration);
            } else {
                playerLayout.setVisibility(View.GONE);
                currentAudioPath = null;
            }

            String htmlContent = currentJournal.getContent();
            StringBuilder fullHtml = new StringBuilder();
            fullHtml.append("<!DOCTYPE html>")
                    .append("<html><head>")
                    .append("<meta charset=\"UTF-8\">")
                    .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                    .append("<style>")
                    .append("body { margin: 0; padding: 16px; font-family: sans-serif; color: #424242; word-wrap: break-word; }")
                    .append("p { margin-bottom: 0.5em; }")
                    .append("img { max-width: 100%; height: auto; display: block; margin: 16px 0; border-radius: 8px; }")
                    .append("ul, ol { padding-left: 20px; margin-top: 0.5em; margin-bottom: 0.5em; }")
                    .append("li { margin-bottom: 0.25em; }")
                    .append("</style>")
                    .append("</head><body>");

            if (currentJournal.getImageUri() != null && !currentJournal.getImageUri().isEmpty()) {
                String imageBase64 = uriToBase64(currentJournal.getImageUri());
                if (imageBase64 != null) {
                    fullHtml.append("<img src=\"data:image/jpeg;base64,")
                            .append(imageBase64)
                            .append("\" alt=\"Journal Image\"/>");
                }
            }
            if (htmlContent != null && !htmlContent.isEmpty()) {
                fullHtml.append(htmlContent);
            }
            fullHtml.append("</body></html>");
            contentWebView.loadDataWithBaseURL(null, fullHtml.toString(), "text/html", "UTF-8", null);
        }
        if (isAdded()) {
            requireActivity().invalidateOptionsMenu();
        }
    }

    private void setupPlayerListener() {
        buttonPlay.setOnClickListener(v -> {
            if (isPlaying) {
                stopPlayback();
            } else if (currentAudioPath != null) {
                startPlayback(currentAudioPath);
            } else {
                Toast.makeText(getContext(), "Audio file not found.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startPlayback(String audioPath) {
        if (audioPath == null || !(new File(audioPath).exists())) {
            Toast.makeText(getContext(), "Audio file not found.", Toast.LENGTH_SHORT).show();
            return;
        }
        stopPlayback();
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioPath);
            mediaPlayer.setOnCompletionListener(mp -> stopPlayback());
            mediaPlayer.setOnPreparedListener(mp -> {
                audioSeekBar.setMax(mp.getDuration());
                updateAudioStatusText(0, mp.getDuration());
                mp.start();
                isPlaying = true;
                buttonPlay.setImageResource(android.R.drawable.ic_media_pause);
                startSeekBarUpdate();
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                stopPlayback();
                return true;
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            stopPlayback();
        }
    }

    private void stopPlayback() {
        stopSeekBarUpdate();
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) { }
            mediaPlayer = null;
        }
        isPlaying = false;
        if (buttonPlay != null) buttonPlay.setImageResource(android.R.drawable.ic_media_play);
        if (currentAudioPath != null && playerLayout.getVisibility() == View.VISIBLE) {
            if (audioSeekBar != null) audioSeekBar.setProgress(0);
            updateAudioStatusText(0, getAudioDuration(currentAudioPath));
        }
    }

    private void startSeekBarUpdate() {
        if (mediaPlayer == null || !mediaPlayer.isPlaying() || seekBarHandler == null) return;
        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    try {
                        int currentPosition = mediaPlayer.getCurrentPosition();
                        int max = audioSeekBar.getMax();
                        audioSeekBar.setProgress(Math.min(currentPosition, max));
                        updateAudioStatusText(currentPosition, max);
                        seekBarHandler.postDelayed(this, 300);
                    } catch (IllegalStateException e) {
                        stopPlayback();
                    }
                } else {
                    stopSeekBarUpdate();
                }
            }
        };
        seekBarHandler.post(updateSeekBarRunnable);
    }

    private void stopSeekBarUpdate() {
        if (seekBarHandler != null && updateSeekBarRunnable != null) {
            seekBarHandler.removeCallbacks(updateSeekBarRunnable);
        }
    }

    private void updateAudioStatusText(int currentPosition, int duration) {
        if (textAudioStatus == null) return;
        if (duration <= 0) {
            textAudioStatus.setText("...");
            return;
        }
        String currentTimeStr = formatTime(currentPosition);
        String totalTimeStr = formatTime(duration);
        textAudioStatus.setText(String.format("%s / %s", currentTimeStr, totalTimeStr));
    }

    private String formatTime(long millis) {
        if (millis < 0) millis = 0;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1);
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private int getAudioDuration(String path) {
        if (path == null || path.isEmpty() || !(new File(path).exists())) return 0;
        MediaPlayer mp = null;
        try {
            mp = new MediaPlayer();
            mp.setDataSource(path);
            mp.prepare();
            int duration = mp.getDuration();
            return Math.max(duration, 0);
        } catch (Exception e) {
            return 0;
        } finally {
            if (mp != null) {
                try { mp.release(); } catch (Exception ignored) {}
            }
        }
    }

    private String uriToBase64(String uriString) {
        if (uriString == null || uriString.isEmpty()) return null;
        InputStream inputStream = null;
        try {
            Uri uri = Uri.parse(uriString);
            inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            options.inSampleSize = calculateInSampleSize(options, 1024, 1024);
            options.inJustDecodeBounds = false;

            inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            if (bitmap == null) return null;

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            bitmap.recycle();
            byte[] imageBytes = outputStream.toByteArray();
            return Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (Exception e) {
            return null;
        } finally {
            if (inputStream != null) {
                try { inputStream.close(); } catch (IOException ignored) {}
            }
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void requestAuthentication(int requestCode, String description) {
        if (!isAdded()) return; // Check if fragment is added
        KeyguardManager keyguardManager = (KeyguardManager) requireContext().getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager != null && keyguardManager.isKeyguardSecure()) {
            Intent intent = keyguardManager.createConfirmDeviceCredentialIntent("Authentication Required", description);
            if (intent != null) {
                try {
                    // This result is caught by MainActivity and forwarded
                    requireActivity().startActivityForResult(intent, requestCode);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Could not start authentication.", Toast.LENGTH_SHORT).show();
                    if (isAdded()) getParentFragmentManager().popBackStack(); // UPDATED
                }
            } else {
                Toast.makeText(getContext(), "Unable to start device authentication.", Toast.LENGTH_SHORT).show();
                if (isAdded()) getParentFragmentManager().popBackStack(); // UPDATED
            }
        } else {
            Toast.makeText(getContext(), "Please set a screen lock to protect private journals.", Toast.LENGTH_LONG).show();
            if (isAdded()) getParentFragmentManager().popBackStack(); // UPDATED
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.view_journal_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    // You can add onPrepareOptionsMenu here if needed to update menu items dynamically

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (currentJournal == null)
            return super.onOptionsItemSelected(item);

        if (itemId == R.id.action_edit) {
            executeProtectedAction(() -> {
                // --- UPDATED: Use FragmentManager ---
                Bundle args = new Bundle();
                args.putInt("JOURNAL_ID", journalId);
                FragmentAddJournal addFragment = new FragmentAddJournal();
                addFragment.setArguments(args);
                navigateToFragment(addFragment);
            });
            return true;
        } else if (itemId == R.id.action_delete) {
            executeProtectedAction(() -> new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Journal")
                    .setMessage("Are you sure you want to delete this journal entry?")
                    .setPositiveButton("Yes", (dialog, which) -> deleteCurrentJournal())
                    .setNegativeButton("No", null)
                    .show());
            return true;
        } else if (itemId == R.id.action_toggle_favorite) {
            currentJournal.setFavorite(!currentJournal.isFavorite());
            dbHelper.updateJournal(currentJournal);
            Toast.makeText(getContext(), currentJournal.isFavorite() ? "Added to Favorites" : "Removed from Favorites", Toast.LENGTH_SHORT).show();
            if (isAdded()) requireActivity().invalidateOptionsMenu();
            return true;
        } else if (itemId == R.id.action_toggle_private) {
            executeProtectedAction(() -> {
                currentJournal.setPrivate(!currentJournal.isPrivate());
                dbHelper.updateJournal(currentJournal);
                Toast.makeText(getContext(), currentJournal.isPrivate() ? "Journal made private" : "Journal made public", Toast.LENGTH_SHORT).show();
                isUnlocked = !currentJournal.isPrivate();
                if (isAdded()) requireActivity().invalidateOptionsMenu();
            });
            return true;
        } else if (itemId == R.id.action_share_text) {
            shareJournalAll();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteCurrentJournal() {
        stopPlayback();
        if (currentJournal.getAudioPath() != null && !currentJournal.getAudioPath().isEmpty()) {
            try {
                new File(currentJournal.getAudioPath()).delete();
            } catch (Exception e) { }
        }
        dbHelper.deleteJournal(journalId);
        Toast.makeText(getContext(), "Journal Deleted", Toast.LENGTH_SHORT).show();
        // --- UPDATED: Use FragmentManager ---
        if (isAdded()) {
            getParentFragmentManager().popBackStack();
        }
    }

    private void executeProtectedAction(Runnable action) {
        if (currentJournal != null && currentJournal.isPrivate()) {
            this.pendingAction = action;
            requestAuthentication(ACTION_LOCK_REQUEST_CODE, "Unlock to perform this action.");
        } else {
            action.run();
        }
    }

    // This method is called by MainActivity's onActivityResult
    public void onActivityResultExternal(int requestCode, int resultCode, Intent data) {
        if (requestCode == INITIAL_LOCK_REQUEST_CODE) {
            if (resultCode == getActivity().RESULT_OK) {
                isUnlocked = true;
                displayJournalContentOrRequestInitialUnlock();
            } else {
                Toast.makeText(getContext(), "Authentication required to view.", Toast.LENGTH_SHORT).show();
                // --- UPDATED: Use FragmentManager ---
                if (isAdded()) {
                    getParentFragmentManager().popBackStack();
                }
            }
        } else if (requestCode == ACTION_LOCK_REQUEST_CODE) {
            if (resultCode == getActivity().RESULT_OK && pendingAction != null) {
                pendingAction.run();
            } else if (resultCode != getActivity().RESULT_OK) {
                Toast.makeText(getContext(), "Authentication failed. Action cancelled.", Toast.LENGTH_SHORT).show();
            }
            pendingAction = null;
        }
    }

    // --- SHARE METHODS ---

    private void shareJournalAll() {
        if (currentJournal == null || !isAdded()) return;

        String title = currentJournal.getTitle();
        String htmlContent = currentJournal.getContent();
        String plainTextContent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            plainTextContent = Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_COMPACT).toString();
        } else {
            plainTextContent = Html.fromHtml(htmlContent).toString();
        }
        String shareBody = title + "\n\n" + plainTextContent;

        Intent shareIntent = new Intent();
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);

        ArrayList<Uri> attachmentUris = new ArrayList<>();

        String imageUriString = currentJournal.getImageUri();
        if (imageUriString != null && !imageUriString.isEmpty()) {
            try {
                Uri imageUri = Uri.parse(imageUriString);
                requireContext().getContentResolver().openInputStream(imageUri).close();
                attachmentUris.add(imageUri);
            } catch (Exception e) { }
        }

        String audioPath = currentJournal.getAudioPath();
        if (audioPath != null && !audioPath.isEmpty()) {
            try {
                File audioFile = new File(audioPath);
                if (audioFile.exists()) {
                    Uri audioUri = FileProvider.getUriForFile(
                            requireContext(),
                            requireContext().getPackageName() + ".fileprovider",
                            audioFile
                    );
                    attachmentUris.add(audioUri);
                }
            } catch (Exception e) { }
        }

        if (attachmentUris.isEmpty()) {
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
        } else if (attachmentUris.size() == 1) {
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, attachmentUris.get(0));
            shareIntent.setType(getMimeType(attachmentUris.get(0)));
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachmentUris);
            shareIntent.setType("*/*");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        try {
            startActivity(Intent.createChooser(shareIntent, "Share Journal"));
        } catch (Exception e) {
            Toast.makeText(getContext(), "Could not find an app to share with.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getMimeType(Uri uri) {
        if (!isAdded()) return "*/*";
        String mimeType;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            ContentResolver cr = requireContext().getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
        }
        return mimeType != null ? mimeType : "*/*";
    }

    // --- ADDED: Helper method for navigation ---
    private void navigateToFragment(Fragment fragment) {
        if (isAdded() && getParentFragmentManager() != null) {
            try {
                // This ID comes from your activity_main.xml file
                int containerId = R.id.fragment_container;

                getParentFragmentManager().beginTransaction()
                        .replace(containerId, fragment)
                        .addToBackStack(null) // This lets the user press the back button
                        .commit();
            } catch (Exception e) {
                Log.e(TAG, "Error navigating to fragment. Container ID (R.id.fragment_container) not found?", e);
                Toast.makeText(getContext(), "Error: Could not navigate.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}