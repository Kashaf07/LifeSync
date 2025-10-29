package com.example.lifesync;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.util.Map;
import java.util.Stack;

public class UndoRedoManager {

    // Interface to notify the Activity when the undo or redo stack's state changes.
    public interface UndoRedoStateListener {
        // --- THIS WAS REMOVED as it's not used by the manager ---
        // Map<Class<?>, Object> getActiveStyles();

        void onStateChanged();
    }

    private final EditText editText;
    private final Stack<String> undoStack = new Stack<>();
    private final Stack<String> redoStack = new Stack<>();
    private boolean isUndoingOrRedoing = false;
    private UndoRedoStateListener stateListener;

    // --- New additions for improved functionality ---
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable endTypingBurstRunnable;
    private boolean isTypingBurst = false;
    private static final long TYPING_BURST_DELAY_MS = 800; // 0.8-second pause signals the end of a typing burst

    public UndoRedoManager(EditText editText) {
        this.editText = editText;

        // This runnable will be posted with a delay to mark the end of a typing burst.
        endTypingBurstRunnable = () -> isTypingBurst = false;

        this.editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!isUndoingOrRedoing) {
                    // Always remove any pending "end of burst" runnable.
                    handler.removeCallbacks(endTypingBurstRunnable);

                    // If we are not in the middle of a burst, this new change
                    // marks the beginning of one.
                    if (!isTypingBurst) {
                        isTypingBurst = true;
                        // Save the state *before* the burst of edits begins.
                        undoStack.push(s.toString());
                        redoStack.clear(); // A new user edit clears the redo history.
                        notifyStateChanged();
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed for this implementation.
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!isUndoingOrRedoing) {
                    // After every character change, reset the timer that marks the end of the burst.
                    handler.postDelayed(endTypingBurstRunnable, TYPING_BURST_DELAY_MS);
                }
            }
        });
    }

    /**
     * Sets the listener that will be notified of state changes.
     * @param listener The listener to notify.
     */
    public void setUndoRedoStateListener(UndoRedoStateListener listener) {
        this.stateListener = listener;
    }

    /**
     * Notifies the listener, if one is set, that the state has changed.
     */
    private void notifyStateChanged() {
        if (stateListener != null) {
            stateListener.onStateChanged();
        }
    }

    /**
     * Performs an undo operation.
     */
    public void undo() {
        if (canUndo()) {
            isUndoingOrRedoing = true;
            String lastState = undoStack.pop();
            redoStack.push(editText.getText().toString());
            editText.setText(lastState);
            editText.setSelection(lastState.length());
            isUndoingOrRedoing = false;
            notifyStateChanged();
        }
    }

    /**
     * Performs a redo operation.
     */
    public void redo() {
        if (canRedo()) {
            isUndoingOrRedoing = true;
            String nextState = redoStack.pop();
            undoStack.push(editText.getText().toString());
            editText.setText(nextState);
            editText.setSelection(nextState.length());
            isUndoingOrRedoing = false;
            notifyStateChanged();
        }
    }

    /**
     * @return True if there are actions to undo, false otherwise.
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * @return True if there are actions to redo, false otherwise.
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
}