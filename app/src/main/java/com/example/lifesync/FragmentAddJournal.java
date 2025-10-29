package com.example.lifesync;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Spanned;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager; // --- ADDED IMPORT ---
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
// --- REMOVED: import androidx.navigation.fragment.NavHostFragment; ---

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FragmentAddJournal extends Fragment implements UndoRedoManager.UndoRedoStateListener, TextStyleBottomSheet.TextStyleListener {

    // --- PARAMETERS ---
    private static final String ARG_JOURNAL_ID = "JOURNAL_ID";
    private int journalId = -1;

    // --- CONSTANTS ---
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "FragmentAddJournal";

    // --- UI VIEWS ---
    private View rootView;
    private EditText titleEditText;
    private RichEditText contentEditText;
    private ImageView journalImageView;
    private LinearLayout playerLayout;
    private ImageButton buttonPlay;
    private ImageButton buttonDeleteAudio;
    private TextView textAudioStatus;
    private SeekBar audioSeekBar;
    private ImageButton addRecordingButton;

    // --- HELPERS & MANAGERS ---
    private DBHelper dbHelper;
    private UndoRedoManager undoRedoManager;
    private Uri imageUri;
    private long reminderTime = 0;
    private Handler seekBarHandler;
    private Runnable updateSeekBarRunnable;

    // --- STATE VARIABLES ---
    private boolean isFavorite, isDraft, isPrivate;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private int currentListType = -1;
    private boolean isInListMode = false;
    private int lastTextLength = 0;
    private String lastLineContent = "";
    private final Map<Class<?>, Object> composingStyles = new HashMap<>();
    private boolean isApplyingStyle = false;
    private boolean isUndoingOrRedoing = false;
    private int currentFontColor = Color.BLACK;
    private int currentHighlightColor = Color.TRANSPARENT;
    private boolean isExitingViaSaveButton = false;
    private int originalSoftInputMode = -1; // --- ADDED ---

    // --- AUDIO ---
    private String audioPath = null;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;

    // --- ACTIVITY LAUNCHERS ---
    private ActivityResultLauncher<String> audioPermissionLauncher;
    private ActivityResultLauncher<String> mGetContent;
    private ActivityResultLauncher<Uri> takePicture;
    private Uri photoUri;

    public FragmentAddJournal() {}

    // --- LIFECYCLE: onCreate ---
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            journalId = getArguments().getInt(ARG_JOURNAL_ID, -1);
        }

        setHasOptionsMenu(true);
        dbHelper = new DBHelper(requireContext());
        initializeAudioPermissionLauncher();

        mGetContent = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        imageUri = uri;
                        try {
                            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                            requireActivity().getContentResolver().takePersistableUriPermission(uri, takeFlags);
                            Log.d(TAG, "Successfully took persistable URI permission for: " + uri);
                        } catch (SecurityException e) {
                            Log.e(TAG, "Error taking persistable URI permission (SecurityException): ", e);
                            Toast.makeText(getContext(), "Could not get permanent access to image.", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Log.e(TAG, "Error taking persistable URI permission (General): ", e);
                        }
                        journalImageView.setImageURI(uri);
                        journalImageView.setVisibility(View.VISIBLE);
                    } else {
                        Log.w(TAG, "mGetContent returned null URI.");
                    }
                });

        takePicture = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result && photoUri != null) {
                        imageUri = photoUri;
                        Log.d(TAG, "Camera captured photo successfully. URI: " + imageUri);
                        try {
                            journalImageView.setImageURI(null);
                            journalImageView.setImageURI(imageUri);
                            journalImageView.setVisibility(View.VISIBLE);
                            Toast.makeText(getContext(), "HD Photo captured!", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "Error loading captured photo", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error setting image URI after camera capture: ", e);
                        }
                    } else {
                        Log.w(TAG, "Camera activity finished with result=" + result + " or photoUri is null.");
                        Toast.makeText(getContext(), "Failed to capture photo", Toast.LENGTH_SHORT).show();
                    }
                });

        seekBarHandler = new Handler(Looper.getMainLooper());
        setupBackButtonHandler();
    }

    // --- LIFECYCLE: onCreateView ---
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_add_journal, container, false);
        return rootView;
    }

    // --- LIFECYCLE: onViewCreated ---
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- THIS IS THE FIX (PART 1) ---
        // Save the original soft input mode and set it to ADJUST_RESIZE
        if (getActivity() != null && getActivity().getWindow() != null) {
            originalSoftInputMode = getActivity().getWindow().getAttributes().softInputMode;
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        // --- END OF FIX ---

        // --- Find All Views ---
        Toolbar topAppBar = view.findViewById(R.id.topAppBar);
        titleEditText = view.findViewById(R.id.titleEditText);
        contentEditText = view.findViewById(R.id.contentEditText);
        journalImageView = view.findViewById(R.id.journalImageView);
        playerLayout = view.findViewById(R.id.audioPlayerLayout);
        buttonPlay = playerLayout.findViewById(R.id.buttonPlay);
        buttonDeleteAudio = playerLayout.findViewById(R.id.buttonDeleteAudio);
        textAudioStatus = playerLayout.findViewById(R.id.textAudioStatus);
        audioSeekBar = playerLayout.findViewById(R.id.audioSeekBar);
        addRecordingButton = view.findViewById(R.id.addRecordingButton);

        // --- Setup Toolbar ---
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            activity.setSupportActionBar(topAppBar);
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        if (activity != null && activity.getSupportActionBar() != null) {
            if (journalId != -1) {
                activity.getSupportActionBar().setTitle("Edit Journal");
            } else {
                activity.getSupportActionBar().setTitle("New Journal");
            }
        }

        // --- UPDATED: Set toolbar navigation click ---
        topAppBar.setNavigationOnClickListener(v -> {
            // Save as draft on back button press
            saveJournal(false);
        });

        if (journalId != -1) {
            loadJournalData();
        }

        undoRedoManager = new UndoRedoManager(contentEditText);
        undoRedoManager.setUndoRedoStateListener(this);

        setupListeners();
        setupBottomToolbar();
        setupPlayerListeners();
        setupSeekBarListener();
    }

    private void initializeAudioPermissionLauncher() {
        audioPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Log.d(TAG, "RECORD_AUDIO permission granted.");
                startRecording();
            } else {
                Log.w(TAG, "RECORD_AUDIO permission denied.");
                Toast.makeText(getContext(), "Audio permission is required to record.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupListeners() {
        contentEditText.setOnSelectionChangedListener((start, end) -> {
            if (start == end) {
                updateComposingStylesFromCursor();
            }
        });

        contentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!isUndoingOrRedoing) {
                    lastTextLength = s.length();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isUndoingOrRedoing) return;

                if (count > before && s != null && s.length() > lastTextLength) {
                    String addedText = s.subSequence(start, start + count).toString();
                    if (addedText.contains("\n")) {
                        int newlinePos = start + addedText.indexOf("\n");
                        contentEditText.post(() -> {
                            if (isInListMode) {
                                handleListEnter(newlinePos);
                            }
                        });
                        return;
                    }
                }

                if (count > before && !isApplyingStyle) {
                    isApplyingStyle = true;
                    Editable editable = contentEditText.getText();
                    if (editable == null) {
                        isApplyingStyle = false;
                        return;
                    }
                    for (Object span : composingStyles.values()) {
                        Object newSpan = copySpan(span);
                        if (newSpan != null) {
                            editable.setSpan(newSpan, start, start + count, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                    isApplyingStyle = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
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

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopSeekBarUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    startSeekBarUpdate();
                }
            }
        });
    }

    private void setupPlayerListeners() {
        buttonPlay.setOnClickListener(v -> {
            if (isRecording) {
                Toast.makeText(getContext(), "Stop recording before playing.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isPlaying) {
                stopPlayback();
            } else {
                if (audioPath != null) {
                    startPlayback();
                } else {
                    Toast.makeText(getContext(), "No audio recording found.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonDeleteAudio.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Audio")
                    .setMessage("Are you sure you want to delete this audio recording?")
                    .setPositiveButton("Yes", (dialog, which) -> deleteAudio())
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void handleListEnter(int newlinePos) {
        isApplyingStyle = true;
        Editable editable = contentEditText.getText();
        if (editable == null) {
            isApplyingStyle = false;
            return;
        }

        int lineStart = findLineStart(editable, newlinePos);
        String currentLine = editable.subSequence(lineStart, newlinePos).toString();
        String content = getContentAfterListMarker(currentLine, currentListType);
        if (content.trim().isEmpty()) {
            if (lastLineContent.trim().isEmpty()) {
                Log.d(TAG, "Exiting list mode due to double enter on empty item.");
                editable.delete(lineStart, newlinePos);
                isInListMode = false;
                currentListType = -1;
                lastLineContent = "";
                isApplyingStyle = false;
                return;
            }
            lastLineContent = content;
        } else {
            lastLineContent = content;
        }
        int nextNumber = getNextNumber(currentLine, currentListType);
        String prefix = getListPrefix(currentListType, nextNumber);
        editable.insert(newlinePos + 1, prefix);
        Log.d(TAG, "Added list prefix '" + prefix + "' at position " + (newlinePos + 1));

        contentEditText.post(() -> contentEditText.setSelection(newlinePos + 1 + prefix.length()));
        isApplyingStyle = false;
    }

    private String getContentAfterListMarker(String line, int listType) {
        switch (listType) {
            case 0: return line.replaceFirst("^\\s*\\u2022 \\s*", "");
            case 1: return line.replaceFirst("^\\s*\\d+\\.\\s*", "");
            case 2: return line.replaceFirst("^\\s*[a-z]\\.\\s*", "");
            default: return line;
        }
    }

    private int getNextNumber(String currentLine, int listType) {
        switch (listType) {
            case 0: return 1;
            case 1:
                Pattern numberPattern = Pattern.compile("^\\s*(\\d+)\\.");
                Matcher numberMatcher = numberPattern.matcher(currentLine);
                if (numberMatcher.find()) {
                    try {
                        return Integer.parseInt(numberMatcher.group(1)) + 1;
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing list number", e);
                        return 1;
                    }
                }
                return 1;
            case 2:
                Pattern letterPattern = Pattern.compile("^\\s*([a-z])\\.");
                Matcher letterMatcher = letterPattern.matcher(currentLine);
                if (letterMatcher.find()) {
                    char currentLetter = letterMatcher.group(1).charAt(0);
                    if (currentLetter == 'z') return 1;
                    return (currentLetter - 'a' + 2);
                }
                return 1;
            default: return 1;
        }
    }

    private String getListPrefix(int listType, int number) {
        switch (listType) {
            case 0: return "\u2022 ";
            case 1: return number + ". ";
            case 2:
                if (number < 1) number = 1;
                char letter = (char) ('a' + (number - 1) % 26);
                return letter + ". ";
            default: return "";
        }
    }

    private int findLineStart(Editable editable, int position) {
        while (position > 0 && editable.charAt(position - 1) != '\n') {
            position--;
        }
        return position;
    }

    private void setupBottomToolbar() {
        rootView.findViewById(R.id.textStyleButton).setOnClickListener(v -> {
            TextStyleBottomSheet bottomSheet = new TextStyleBottomSheet();

            // --- THIS IS THE FIX ---
            // Tell the bottom sheet that *this* fragment is the listener
            bottomSheet.setTargetFragment(this, 0);
            // --- END OF FIX ---

            bottomSheet.show(getParentFragmentManager(), bottomSheet.getTag());
        });

        addRecordingButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                if (isPlaying) {
                    Toast.makeText(getContext(), "Stop playback before recording.", Toast.LENGTH_SHORT).show();
                    return;
                }
                checkAudioPermissionAndRecord();
            }
        });

        rootView.findViewById(R.id.addImageButton).setOnClickListener(v -> showImageSourceDialog());
        rootView.findViewById(R.id.overflowMenuButton).setOnClickListener(this::showOverflowMenu);
    }

    private void checkAudioPermissionAndRecord() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecording();
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startRecording() {
        if (audioPath != null) {
            Toast.makeText(getContext(), "Please delete the existing recording first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "AUDIO_" + timeStamp + ".m4a";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (storageDir == null || (!storageDir.exists() && !storageDir.mkdirs())) {
            Toast.makeText(getContext(), "Failed to access storage directory.", Toast.LENGTH_SHORT).show();
            return;
        }
        audioPath = new File(storageDir, fileName).getAbsolutePath();
        Log.d(TAG, "Starting recording. Saving to: " + audioPath);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mediaRecorder = new MediaRecorder(requireContext());
        } else {
            mediaRecorder = new MediaRecorder();
        }

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(audioPath);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            addRecordingButton.setImageResource(android.R.drawable.ic_media_pause);
            Toast.makeText(getContext(), "Recording started...", Toast.LENGTH_SHORT).show();
            playerLayout.setVisibility(View.GONE);
        } catch (IOException | IllegalStateException e) {
            Log.e(TAG, "MediaRecorder start/prepare failed", e);
            Toast.makeText(getContext(), "Recording failed to start.", Toast.LENGTH_SHORT).show();
            audioPath = null;
            if (mediaRecorder != null) {
                try { mediaRecorder.release(); } catch (Exception ignored) {}
                mediaRecorder = null;
            }
        }
    }

    private void stopRecording() {
        if (mediaRecorder == null) return;
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            Log.d(TAG,"Recording stopped successfully.");
        } catch (RuntimeException stopException) {
            Log.e(TAG, "Error stopping MediaRecorder (RuntimeException): " + stopException.getMessage());
            if (audioPath != null) {
                boolean deleted = new File(audioPath).delete();
                if (!deleted) {
                    Log.w(TAG, "Failed to delete corrupted recording file: " + audioPath);
                } else {
                    Log.d(TAG, "Deleted potentially corrupted recording file: " + audioPath);
                }
                audioPath = null;
            }
            Toast.makeText(getContext(), "Recording failed.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error releasing MediaRecorder: " + e.getMessage());
        } finally {
            mediaRecorder = null;
            isRecording = false;
            addRecordingButton.setImageResource(R.drawable.ic_mic);
        }

        if (audioPath != null) {
            Toast.makeText(getContext(), "Recording saved.", Toast.LENGTH_SHORT).show();
            playerLayout.setVisibility(View.VISIBLE);
            buttonDeleteAudio.setVisibility(View.VISIBLE);
            int duration = getAudioDuration(audioPath);
            audioSeekBar.setMax(Math.max(duration, 100));
            audioSeekBar.setProgress(0);
            updateAudioStatusText(0, duration);
            Log.d(TAG, "Audio player updated. Duration: " + duration);
        } else {
            playerLayout.setVisibility(View.GONE);
        }
    }

    private void startPlayback() {
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
                Log.d(TAG, "Playback started. Duration: " + mp.getDuration());
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer Error - what: " + what + " extra: " + extra);
                Toast.makeText(getContext(), "Error playing audio.", Toast.LENGTH_SHORT).show();
                stopPlayback();
                return true;
            });
            mediaPlayer.prepareAsync();

        } catch (IOException | IllegalStateException e) {
            Log.e(TAG, "MediaPlayer setup failed", e);
            Toast.makeText(getContext(), "Could not play audio.", Toast.LENGTH_SHORT).show();
            stopPlayback();
        }
    }

    private void stopPlayback() {
        stopSeekBarUpdate();
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping/releasing MediaPlayer", e);
            } finally {
                mediaPlayer = null;
            }
        }
        isPlaying = false;
        if (buttonPlay != null) {
            buttonPlay.setImageResource(android.R.drawable.ic_media_play);
        }
        if (audioPath != null && playerLayout != null && playerLayout.getVisibility() == View.VISIBLE) {
            if (audioSeekBar != null) audioSeekBar.setProgress(0);
            int duration = getAudioDuration(audioPath);
            updateAudioStatusText(0, duration);
        } else if (textAudioStatus != null && audioSeekBar != null) {
            textAudioStatus.setText("");
            audioSeekBar.setProgress(0);
            audioSeekBar.setMax(100);
        }
        Log.d(TAG, "Playback stopped.");
    }

    private void deleteAudio() {
        stopPlayback();
        if (audioPath != null) {
            File file = new File(audioPath);
            boolean deleted = file.delete();
            if (deleted) {
                Log.d(TAG, "Audio file deleted: " + audioPath);
                Toast.makeText(getContext(), "Audio deleted", Toast.LENGTH_SHORT).show();
            } else {
                Log.w(TAG, "Failed to delete audio file: " + audioPath);
                Toast.makeText(getContext(), "Failed to delete audio", Toast.LENGTH_SHORT).show();
            }
            audioPath = null;
            playerLayout.setVisibility(View.GONE);
        }
    }

    private void startSeekBarUpdate() {
        if (mediaPlayer == null || !mediaPlayer.isPlaying() || seekBarHandler == null) return;

        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying() && audioSeekBar != null) {
                    try {
                        int currentPosition = mediaPlayer.getCurrentPosition();
                        int max = audioSeekBar.getMax();
                        audioSeekBar.setProgress(Math.min(currentPosition, max));
                        updateAudioStatusText(currentPosition, max);
                        seekBarHandler.postDelayed(this, 300);
                    } catch (IllegalStateException e) {
                        Log.w(TAG, "IllegalStateException in seekBar update, stopping player.");
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
            mp.release();
            return Math.max(duration, 0);
        } catch (Exception e) {
            Log.e(TAG, "Error getting duration for path: " + path, e);
            if (mp != null) {
                try { mp.release(); } catch (Exception ignored) {}
            }
            return 0;
        }
    }

    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add Image");
        builder.setItems(new String[]{"Take Photo (HD)", "Choose from Gallery"}, (dialog, which) -> {
            if (which == 0) {
                takePhotoFromCamera();
            } else {
                try {
                    mGetContent.launch("image/*");
                } catch (Exception e) {
                    Log.e(TAG, "Error launching gallery picker", e);
                    Toast.makeText(getContext(), "Could not open gallery.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.show();
    }

    private void takePhotoFromCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Camera permission not granted. Requesting...");
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG, "Camera permission already granted. Launching camera...");
            launchCamera();
        }
    }

    private void launchCamera() {
        File photoFile;
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (storageDir == null || (!storageDir.exists() && !storageDir.mkdirs())) {
                throw new IOException("External storage directory not found or couldn't be created.");
            }
            photoFile = File.createTempFile(imageFileName, ".jpg", storageDir);
            Log.d(TAG, "Photo file created: " + photoFile.getAbsolutePath());

            photoUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile);
            Log.d(TAG, "Photo URI for camera: " + photoUri);
            takePicture.launch(photoUri);

        } catch (Exception ex) {
            Log.e(TAG, "Error creating photo file or getting URI", ex);
            Toast.makeText(getContext(), "Error preparing camera.", Toast.LENGTH_SHORT).show();
            photoUri = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Camera permission granted by user.");
                launchCamera();
            } else {
                Log.w(TAG, "Camera permission denied by user.");
                Toast.makeText(getContext(), "Camera permission is required to take photos", Toast.LENGTH_LONG).show();
            }
        }
    }

    // --- INTERFACE IMPLEMENTATIONS (TextStyleListener) ---

    @Override
    public void onStyleToggle(Class<?> styleClass, int styleType) {
        int start = contentEditText.getSelectionStart();
        int end = contentEditText.getSelectionEnd();
        Editable editable = contentEditText.getText();
        if (editable == null) return;
        Log.d(TAG, "Toggling style " + styleClass.getSimpleName() + " for range " + start + "-" + end);

        if (start != end) {
            boolean isApplied = isStyleActiveInSelection(styleClass, editable, start, end, styleType);
            Log.d(TAG, "Style currently applied to selection: " + isApplied);
            Object[] spans = editable.getSpans(start, end, styleClass);
            for (Object span : spans) {
                if (isCorrectStyleSpan(span, styleType)) {
                    editable.removeSpan(span);
                    Log.d(TAG, "Removed existing span: " + span);
                }
            }
            if (!isApplied) {
                Object newSpan = createSpan(styleClass, styleType);
                if (newSpan != null) {
                    editable.setSpan(newSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    Log.d(TAG, "Applied new span to selection: " + newSpan);
                }
            }
        } else {
            if (composingStyles.containsKey(styleClass)) {
                composingStyles.remove(styleClass);
                Log.d(TAG, "Removed composing style: " + styleClass.getSimpleName());
            } else {
                Object newSpan = createSpan(styleClass, styleType);
                if (newSpan != null) {
                    composingStyles.put(styleClass, newSpan);
                    Log.d(TAG, "Added composing style: " + styleClass.getSimpleName());
                }
            }
        }
    }

    @Override
    public void onHighlightToggled(boolean isActive) {
        Log.d(TAG, "Highlight toggled: " + isActive);
        int start = contentEditText.getSelectionStart();
        int end = contentEditText.getSelectionEnd();
        Editable editable = contentEditText.getText();
        if (editable == null) return;

        if (isActive) {
            currentHighlightColor = Color.YELLOW;
            if (start != end) {
                BackgroundColorSpan[] oldSpans = editable.getSpans(start, end, BackgroundColorSpan.class);
                for(BackgroundColorSpan oldSpan: oldSpans) editable.removeSpan(oldSpan);
                editable.setSpan(new BackgroundColorSpan(currentHighlightColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            composingStyles.put(BackgroundColorSpan.class, new BackgroundColorSpan(currentHighlightColor));
            Log.d(TAG, "Added composing highlight style (Yellow).");
        } else {
            currentHighlightColor = Color.TRANSPARENT;
            if (start != end) {
                BackgroundColorSpan[] spans = editable.getSpans(start, end, BackgroundColorSpan.class);
                for (BackgroundColorSpan span : spans) editable.removeSpan(span);
                Log.d(TAG, "Removed highlight from selection " + start + "-" + end);
            }
            composingStyles.remove(BackgroundColorSpan.class);
            Log.d(TAG, "Removed composing highlight style.");
        }
    }

    @Override
    public void onColorSelected(int color, boolean isHighlight) {
        int start = contentEditText.getSelectionStart();
        int end = contentEditText.getSelectionEnd();
        Class<?> spanClass = isHighlight ? BackgroundColorSpan.class : ForegroundColorSpan.class;
        Log.d(TAG, (isHighlight ? "Highlight" : "Font") + " color selected: " + String.format("#%06X", (0xFFFFFF & color)) + " for range " + start + "-" + end);

        if (isHighlight) {
            currentHighlightColor = color;
        } else {
            currentFontColor = color;
        }

        if (isHighlight && color == Color.TRANSPARENT) {
            onHighlightToggled(false);
            return;
        }

        Object span = isHighlight ? new BackgroundColorSpan(color) : new ForegroundColorSpan(color);
        if (start < end) {
            Editable editable = contentEditText.getText();
            if (editable == null) return;
            Object[] oldSpans = editable.getSpans(start, end, spanClass);
            for(Object oldSpan: oldSpans) editable.removeSpan(oldSpan);
            editable.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            composingStyles.put(spanClass, span);
        }
    }

    @Override
    public void onTextSizeSelected(int size) {
        int start = contentEditText.getSelectionStart();
        int end = contentEditText.getSelectionEnd();
        Object span = new AbsoluteSizeSpan(size, true); // size is in DP
        Log.d(TAG, "Text size selected: " + size + "dp for range " + start + "-" + end);

        if (start < end) {
            Editable editable = contentEditText.getText();
            if (editable == null) return;
            AbsoluteSizeSpan[] oldSpans = editable.getSpans(start, end, AbsoluteSizeSpan.class);
            for(AbsoluteSizeSpan oldSpan: oldSpans) editable.removeSpan(oldSpan);
            editable.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            composingStyles.put(AbsoluteSizeSpan.class, span);
        }
    }

    @Override
    public void onBulletListSelected(int listType) {
        Log.d(TAG, "Bullet list selected: Type " + listType);
        isInListMode = true;
        currentListType = listType;
        lastLineContent = "";

        int cursorPos = contentEditText.getSelectionStart();
        Editable editable = contentEditText.getText();
        if (editable == null) return;

        int lineStart = findLineStart(editable, cursorPos);
        String prefix = getListPrefix(listType, 1);

        String currentLineStartStr = editable.subSequence(lineStart, Math.min(lineStart + 5, editable.length())).toString();
        boolean alreadyHasPrefix = currentLineStartStr.matches("^\\s*(\\u2022 |\\d+\\. |[a-z]\\. ).*");

        if (!alreadyHasPrefix) {
            editable.insert(lineStart, prefix);
            Log.d(TAG, "Inserted list prefix '" + prefix + "' at " + lineStart);
            contentEditText.setSelection(lineStart + prefix.length());
        } else {
            Log.d(TAG, "Line already has a list prefix, not inserting new one.");
        }
    }

    @Override public int getCurrentListType() { return isInListMode ? currentListType : -1; }
    @Override public int getActiveFontColor() { return currentFontColor; }
    @Override public int getActiveHighlightColor() { return currentHighlightColor; }
    @Override public Map<Class<?>, Object> getActiveStyles() { return composingStyles; }

    // --- RICH TEXT HELPERS ---

    private void updateComposingStylesFromCursor() {
        composingStyles.clear();
        currentFontColor = Color.BLACK;
        currentHighlightColor = Color.TRANSPARENT;

        int pos = contentEditText.getSelectionStart();
        if (pos <= 0) return;

        Editable editable = contentEditText.getText();
        if (editable == null) return;
        Object[] spans = editable.getSpans(pos - 1, pos, Object.class);

        for (Object span : spans) {
            if (span instanceof StyleSpan) {
                if (((StyleSpan) span).getStyle() == Typeface.BOLD) composingStyles.put(StyleSpan.class, span);
                else if (((StyleSpan) span).getStyle() == Typeface.ITALIC) composingStyles.put(ItalicStyleSpan.class, span);
            } else if (span instanceof UnderlineSpan) composingStyles.put(UnderlineSpan.class, span);
            else if (span instanceof StrikethroughSpan) composingStyles.put(StrikethroughSpan.class, span);
            else if (span instanceof ForegroundColorSpan) {
                composingStyles.put(ForegroundColorSpan.class, span);
                currentFontColor = ((ForegroundColorSpan) span).getForegroundColor();
            } else if (span instanceof BackgroundColorSpan) {
                composingStyles.put(BackgroundColorSpan.class, span);
                currentHighlightColor = ((BackgroundColorSpan) span).getBackgroundColor();
            } else if (span instanceof AbsoluteSizeSpan) composingStyles.put(AbsoluteSizeSpan.class, span);
        }
    }

    private boolean isStyleActiveInSelection(Class<?> styleClass, Editable editable, int start, int end, int styleType) {
        Object[] spans = editable.getSpans(start, end, styleClass);
        for (Object span : spans) {
            if (isCorrectStyleSpan(span, styleType)) {
                int spanStart = editable.getSpanStart(span);
                int spanEnd = editable.getSpanEnd(span);
                if (Math.max(start, spanStart) < Math.min(end, spanEnd)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isCorrectStyleSpan(Object span, int styleType) {
        if (span instanceof StyleSpan) {
            return ((StyleSpan) span).getStyle() == styleType;
        }
        return true;
    }

    private Object createSpan(Class<?> styleClass, int styleType) {
        try {
            if (styleClass == StyleSpan.class) return new StyleSpan(styleType);
            if (styleClass == ItalicStyleSpan.class) return new ItalicStyleSpan();
            if (styleClass == UnderlineSpan.class) return new UnderlineSpan();
            if (styleClass == StrikethroughSpan.class) return new StrikethroughSpan();
        } catch (Exception e) {
            Log.e(TAG, "Error creating span instance for " + styleClass.getSimpleName(), e);
        }
        return null;
    }

    private Object copySpan(Object span) {
        if (span instanceof StyleSpan) return new StyleSpan(((StyleSpan) span).getStyle());
        if (span instanceof UnderlineSpan) return new UnderlineSpan();
        if (span instanceof StrikethroughSpan) return new StrikethroughSpan();
        if (span instanceof ForegroundColorSpan) return new ForegroundColorSpan(((ForegroundColorSpan) span).getForegroundColor());
        if (span instanceof BackgroundColorSpan) return new BackgroundColorSpan(((BackgroundColorSpan) span).getBackgroundColor());
        if (span instanceof AbsoluteSizeSpan) return new AbsoluteSizeSpan(((AbsoluteSizeSpan) span).getSize(), true);
        if (span instanceof BulletSpan) return new BulletSpan();
        Log.w(TAG, "Cannot copy span of type: " + span.getClass().getName());
        return null;
    }

    public static class ItalicStyleSpan extends StyleSpan {
        public ItalicStyleSpan() {
            super(Typeface.ITALIC);
        }
    }

    private void showOverflowMenu(View v) {
        PopupMenu popup = new PopupMenu(requireContext(), v);
        popup.getMenuInflater().inflate(R.menu.add_journal_overflow_menu, popup.getMenu());
        popup.getMenu().findItem(R.id.action_toggle_favorite).setChecked(isFavorite);
        popup.getMenu().findItem(R.id.action_toggle_draft).setChecked(isDraft);
        popup.getMenu().findItem(R.id.action_toggle_private).setChecked(isPrivate);

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_toggle_favorite) {
                isFavorite = !isFavorite;
                item.setChecked(isFavorite);
                Log.d(TAG, "Favorite toggled: " + isFavorite);
                return true;
            }
            if (itemId == R.id.action_toggle_draft) {
                isDraft = !isDraft;
                item.setChecked(isDraft);
                Log.d(TAG, "Draft toggled: " + isDraft);
                return true;
            }
            if (itemId == R.id.action_toggle_private) {
                isPrivate = !isPrivate;
                item.setChecked(isPrivate);
                Log.d(TAG, "Private toggled: " + isPrivate);
                return true;
            }
            return false;
        });
        popup.show();
    }

    // --- BACK BUTTON & SAVE ---

    private void setupBackButtonHandler() {
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                saveJournal(false); // Pass false for auto-save/draft
            }
        });
    }

    private void loadJournalData() {
        Log.d(TAG, "Loading data for journal ID: " + journalId);
        if (journalId == -1) {
            Log.w(TAG, "Cannot load journal, ID is -1.");
            return;
        }

        Journal journal = dbHelper.getJournal(journalId);
        if (journal != null) {
            titleEditText.setText(journal.getTitle());

            String htmlContent = journal.getContent();
            if (htmlContent != null && !htmlContent.isEmpty()) {
                Spanned spannedText = parseHtmlWithStyles(htmlContent);
                contentEditText.setText(spannedText);
                updateComposingStylesFromCursor();
            }

            isFavorite = journal.isFavorite();
            isDraft = journal.isDraft();
            isPrivate = journal.isPrivate();
            reminderTime = journal.getReminderTime();

            this.audioPath = journal.getAudioPath();
            if (this.audioPath != null && !this.audioPath.isEmpty() && new File(this.audioPath).exists()) {
                playerLayout.setVisibility(View.VISIBLE);
                buttonDeleteAudio.setVisibility(View.VISIBLE);
                int duration = getAudioDuration(this.audioPath);
                if(audioSeekBar != null) audioSeekBar.setMax(Math.max(duration, 100));
                if(audioSeekBar != null) audioSeekBar.setProgress(0);
                updateAudioStatusText(0, duration);
                Log.d(TAG, "Successfully loaded audio path: " + this.audioPath + " Duration: " + duration);
            } else {
                playerLayout.setVisibility(View.GONE);
                if (this.audioPath != null && !this.audioPath.isEmpty()) {
                    Log.w(TAG, "Audio path loaded (" + this.audioPath + ") but file does not exist.");
                }
                this.audioPath = null;
            }

            if (journal.getImageUri() != null && !journal.getImageUri().isEmpty()) {
                imageUri = Uri.parse(journal.getImageUri());
                try {
                    requireActivity().getContentResolver().openInputStream(imageUri).close();
                    journalImageView.setImageURI(imageUri);
                    journalImageView.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Successfully loaded image URI: " + imageUri);
                } catch (Exception e) {
                    Log.e(TAG, "Error loading image URI (permission lost or file deleted): " + imageUri, e);
                    Toast.makeText(getContext(), "Could not load saved image.", Toast.LENGTH_SHORT).show();
                    imageUri = null;
                    journalImageView.setVisibility(View.GONE);
                }
            } else {
                journalImageView.setVisibility(View.GONE);
            }
        } else {
            Log.w(TAG, "Could not find journal with ID: " + journalId + " in database.");
            Toast.makeText(getContext(), "Error loading journal.", Toast.LENGTH_SHORT).show();
            // --- UPDATED: Use FragmentManager ---
            if(isAdded()) {
                getParentFragmentManager().popBackStack();
            }
        }
    }

    private Spanned parseHtmlWithStyles(String html) {
        Spanned baseSpanned;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            baseSpanned = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            baseSpanned = Html.fromHtml(html);
        }
        SpannableStringBuilder builder = new SpannableStringBuilder(baseSpanned);
        parseInlineStyles(html, builder);
        return builder;
    }

    private void parseInlineStyles(String html, SpannableStringBuilder builder) {
        String plainText = builder.toString();
        Pattern spanPattern = Pattern.compile("<span\\s+style=\"([^\"]+)\">(.*?)</span>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher spanMatcher = spanPattern.matcher(html);

        while (spanMatcher.find()) {
            String styleAttr = spanMatcher.group(1);
            String content = spanMatcher.group(2);

            String plainContent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                plainContent = Html.fromHtml(content, Html.FROM_HTML_MODE_COMPACT).toString();
            } else {
                plainContent = Html.fromHtml(content).toString();
            }

            if (plainContent.isEmpty()) continue;

            if (styleAttr != null) {
                int startPos = plainText.indexOf(plainContent);
                if (startPos >= 0) {
                    int endPos = startPos + plainContent.length();

                    Pattern colorPattern = Pattern.compile("color:\\s*(#[0-9a-fA-F]{6}|rgb\\([^)]+\\))", Pattern.CASE_INSENSITIVE);
                    Matcher colorMatcher = colorPattern.matcher(styleAttr);
                    if (colorMatcher.find()) {
                        String colorValue = colorMatcher.group(1);
                        int color = parseColor(colorValue);
                        if (color != 0) {
                            builder.setSpan(new ForegroundColorSpan(color), startPos, endPos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }

                    Pattern sizePattern = Pattern.compile("font-size:\\s*(\\d+)(px)", Pattern.CASE_INSENSITIVE);
                    Matcher sizeMatcher = sizePattern.matcher(styleAttr);
                    if (sizeMatcher.find()) {
                        try {
                            int sizeInPx = Integer.parseInt(sizeMatcher.group(1));
                            int sizeInDp = (int) (sizeInPx / requireContext().getResources().getDisplayMetrics().density);
                            builder.setSpan(new AbsoluteSizeSpan(sizeInDp, true), startPos, endPos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            Log.d(TAG, "Loaded font size: " + sizeInPx + "px -> " + sizeInDp + "dp");
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing font-size from style: " + styleAttr, e);
                        }
                    }
                } else {
                    Log.w(TAG, "Could not find plain text '" + plainContent + "' in builder text to apply styles.");
                }
            }
        }
    }

    private int parseColor(String colorValue) {
        try {
            if (colorValue.startsWith("#")) {
                return Color.parseColor(colorValue);
            } else if (colorValue.startsWith("rgb")) {
                Pattern rgbPattern = Pattern.compile("rgb\\((\\d+),\\s*(\\d+),\\s*(\\d+)\\)");
                Matcher rgbMatcher = rgbPattern.matcher(colorValue);
                if (rgbMatcher.find()) {
                    int r = Integer.parseInt(rgbMatcher.group(1));
                    int g = Integer.parseInt(rgbMatcher.group(2));
                    int b = Integer.parseInt(rgbMatcher.group(3));
                    return Color.rgb(r, g, b);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing color: " + colorValue, e);
        }
        return 0;
    }

    private void saveJournal(boolean isManualSave) {
        String title = titleEditText.getText().toString().trim();
        Editable editableContent = contentEditText.getText();
        if (editableContent == null) editableContent = Editable.Factory.getInstance().newEditable("");

        String contentHtml = convertSpannedToHtml(editableContent);
        String plainContent = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            plainContent = Html.fromHtml(contentHtml, Html.FROM_HTML_MODE_COMPACT).toString().trim();
        } else {
            plainContent = Html.fromHtml(contentHtml).toString().trim();
        }

        boolean isEmpty = title.isEmpty() && plainContent.isEmpty() && imageUri == null && audioPath == null;

        if (isEmpty) {
            if (isManualSave) {
                Toast.makeText(getContext(), "Cannot save an empty journal.", Toast.LENGTH_SHORT).show();
            } else {
                if (journalId != -1) {
                    Log.d(TAG, "Journal is empty, deleting existing entry ID: " + journalId);
                    dbHelper.deleteJournal(journalId);
                    Toast.makeText(getContext(), "Empty journal deleted.", Toast.LENGTH_SHORT).show();
                }
                // --- UPDATED: Use FragmentManager ---
                if (isAdded()) {
                    getParentFragmentManager().popBackStack();
                }
            }
            return;
        }

        if (!isManualSave) {
            isDraft = true;
            Log.d(TAG, "Auto-saving as draft...");
        } else {
            isDraft = false;
        }

        if (title.isEmpty()) title = "Untitled";

        String date = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault()).format(new Date());
        String imageUriString = (imageUri != null) ? imageUri.toString() : null;

        long savedJournalId;
        Journal journalToSave = new Journal(title, contentHtml, date, isFavorite, isPrivate, isDraft, imageUriString, reminderTime, audioPath);
        Log.i(TAG, "Attempting to save journal. Title: '" + title + "', AudioPath: " + audioPath + ", isDraft: " + isDraft);

        if (journalId != -1) {
            journalToSave.setId(journalId);
            int rowsAffected = dbHelper.updateJournal(journalToSave);
            savedJournalId = journalId;
            if (rowsAffected > 0) {
                if (isManualSave) {
                    Toast.makeText(getContext(), "Journal Updated", Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG, "Successfully updated journal ID: " + savedJournalId);
            } else {
                Toast.makeText(getContext(), "Error updating journal", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error updating journal ID: " + savedJournalId + ". Rows affected: " + rowsAffected);
            }
        } else {
            savedJournalId = dbHelper.addJournal(journalToSave);
            if (savedJournalId > -1) {
                if (isManualSave) {
                    Toast.makeText(getContext(), "Journal Saved", Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG, "Successfully added new journal ID: " + savedJournalId);
                journalId = (int) savedJournalId;
            } else {
                Toast.makeText(getContext(), "Error saving new journal", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error saving new journal. DB returned ID: " + savedJournalId);
                // Don't pop back if save failed, user might want to retry
                return;
            }
        }

        // --- UPDATED: Use FragmentManager ---
        if (isAdded()) {
            getParentFragmentManager().popBackStack();
        }
    }

    private String convertSpannedToHtml(Editable editable) {
        if (editable == null) return "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.toHtml(editable, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
        } else {
            return Html.toHtml(editable);
        }
    }

    // --- MENU (Undo/Redo/Save) ---

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.add_journal_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        if (undoRedoManager != null) {
            menu.findItem(R.id.action_undo).setEnabled(undoRedoManager.canUndo());
            menu.findItem(R.id.action_redo).setEnabled(undoRedoManager.canRedo());
        } else {
            menu.findItem(R.id.action_undo).setEnabled(false);
            menu.findItem(R.id.action_redo).setEnabled(false);
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // This is handled by the toolbar's setNavigationOnClickListener
            return true;
        }
        if (itemId == R.id.action_save) {
            saveJournal(true); // Manual save
            return true;
        }
        if (undoRedoManager != null) {
            if (itemId == R.id.action_undo) {
                isUndoingOrRedoing = true;
                undoRedoManager.undo();
                isUndoingOrRedoing = false;
                return true;
            }
            if (itemId == R.id.action_redo) {
                isUndoingOrRedoing = true;
                undoRedoManager.redo();
                isUndoingOrRedoing = false;
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    // --- INTERFACE IMPLEMENTATION (UndoRedoManager.UndoRedoStateListener) ---
    @Override
    public void onStateChanged() {
        if (isAdded()) {
            requireActivity().invalidateOptionsMenu();
        }
    }

    // --- LIFECYCLE: onPause / onDestroy ---
    @Override
    public void onPause() {
        super.onPause();
        if (isPlaying) {
            stopPlayback();
        }
        if (isRecording) {
            stopRecording();
        }
    }

    // --- ADDED LIFECYCLE METHOD ---
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // --- THIS IS THE FIX (PART 2) ---
        // Restore original soft input mode when fragment is destroyed
        if (getActivity() != null && getActivity().getWindow() != null && originalSoftInputMode != -1) {
            getActivity().getWindow().setSoftInputMode(originalSoftInputMode);
        }
        // --- END OF FIX ---

        // Clean up toolbar
        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(null);
        }

        rootView = null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPlayback();
        stopSeekBarUpdate();
        if (mediaRecorder != null) {
            try { mediaRecorder.release(); } catch (Exception ignored) {}
            mediaRecorder = null;
        }
        // --- MOVED: rootView = null; was moved to onDestroyView ---
    }
}