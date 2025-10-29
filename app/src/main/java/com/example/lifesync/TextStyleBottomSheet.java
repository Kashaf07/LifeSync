package com.example.lifesync;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Map;

public class TextStyleBottomSheet extends BottomSheetDialogFragment {

    private TextStyleListener mListener;
    private View selectedListView = null;
    private View selectedFontSizeView = null;
    private View selectedFontColorView = null;
    private View selectedHighlightColorView = null;

    public interface TextStyleListener {
        void onStyleToggle(Class<?> styleClass, int styleType);
        void onColorSelected(int color, boolean isHighlight);
        void onTextSizeSelected(int size);
        void onHighlightToggled(boolean isActive);
        void onBulletListSelected(int listType);
        int getCurrentListType();
        Map<Class<?>, Object> getActiveStyles();
        int getActiveFontColor();
        int getActiveHighlightColor();
    }

    /**
     * --- THIS IS THE CRITICAL FIX ---
     *
     * This method is called when the fragment attaches to its host.
     * We check in a specific order to find the listener.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // 1. Check if the Target Fragment was set (which we now do in FragmentAddJournal)
        if (getTargetFragment() instanceof TextStyleListener) {
            mListener = (TextStyleListener) getTargetFragment();
        }
        // 2. Fallback to check the Parent Fragment
        else if (getParentFragment() instanceof TextStyleListener) {
            mListener = (TextStyleListener) getParentFragment();
        }
        // 3. Fallback to check the Activity (Context)
        else if (context instanceof TextStyleListener) {
            mListener = (TextStyleListener) context;
        }
        // 4. If none of the above work, throw the error.
        else {
            throw new ClassCastException(context.toString()
                    + " or parent/target fragment must implement TextStyleListener");
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // NOTE: Make sure you have a layout file named 'bottom_sheet_text_styles.xml'
        // in your res/layout folder.
        return inflater.inflate(R.layout.bottom_sheet_text_styles, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check for mListener null in case onAttach failed, though it should throw.
        if (mListener == null) {
            dismiss();
            return;
        }

        view.findViewById(R.id.closeButton).setOnClickListener(v -> dismiss());
        setupStyleButtons(view);
        setupParagraphStyles(view);
        setupColorGrids(view);
    }

    private void setupStyleButtons(View view) {
        ImageButton boldButton = view.findViewById(R.id.boldButton);
        ImageButton italicButton = view.findViewById(R.id.italicButton);
        ImageButton underlineButton = view.findViewById(R.id.underlineButton);
        ImageButton strikethroughButton = view.findViewById(R.id.strikethroughButton);
        ImageButton highlightButton = view.findViewById(R.id.highlightButton);

        Map<Class<?>, Object> activeStyles = mListener.getActiveStyles();

        // Check for BOLD specifically
        boolean isBoldActive = false;
        if (activeStyles.containsKey(android.text.style.StyleSpan.class)) {
            Object span = activeStyles.get(android.text.style.StyleSpan.class);
            if (span instanceof android.text.style.StyleSpan && ((android.text.style.StyleSpan) span).getStyle() == Typeface.BOLD) {
                isBoldActive = true;
            }
        }
        boldButton.setSelected(isBoldActive);

        italicButton.setSelected(activeStyles.containsKey(FragmentAddJournal.ItalicStyleSpan.class));
        underlineButton.setSelected(activeStyles.containsKey(android.text.style.UnderlineSpan.class));
        strikethroughButton.setSelected(activeStyles.containsKey(android.text.style.StrikethroughSpan.class));
        highlightButton.setSelected(activeStyles.containsKey(android.text.style.BackgroundColorSpan.class));

        boldButton.setOnClickListener(v -> {
            mListener.onStyleToggle(android.text.style.StyleSpan.class, Typeface.BOLD);
            v.setSelected(!v.isSelected());
        });

        italicButton.setOnClickListener(v -> {
            mListener.onStyleToggle(FragmentAddJournal.ItalicStyleSpan.class, Typeface.ITALIC);
            v.setSelected(!v.isSelected());
        });

        underlineButton.setOnClickListener(v -> {
            mListener.onStyleToggle(android.text.style.UnderlineSpan.class, 0);
            v.setSelected(!v.isSelected());
        });

        strikethroughButton.setOnClickListener(v -> {
            mListener.onStyleToggle(android.text.style.StrikethroughSpan.class, 0);
            v.setSelected(!v.isSelected());
        });

        highlightButton.setOnClickListener(v -> {
            boolean isNowActive = !v.isSelected();
            mListener.onHighlightToggled(isNowActive);
            v.setSelected(isNowActive);
        });
    }

    private void setupParagraphStyles(View view) {
        View bulletDot = view.findViewById(R.id.bulletDotButton);
        View bulletDigit = view.findViewById(R.id.bulletDigitButton);
        View bulletLetter = view.findViewById(R.id.bulletLetterButton);

        // Check current list type
        int currentListType = mListener.getCurrentListType();
        if (currentListType == 0 && bulletDot != null) {
            selectListView(bulletDot);
        } else if (currentListType == 1 && bulletDigit != null) {
            selectListView(bulletDigit);
        } else if (currentListType == 2 && bulletLetter != null) {
            selectListView(bulletLetter);
        }

        if (bulletDot != null) {
            bulletDot.setOnClickListener(v -> {
                selectListView(v);
                mListener.onBulletListSelected(0);
            });
        }

        if (bulletDigit != null) {
            bulletDigit.setOnClickListener(v -> {
                selectListView(v);
                mListener.onBulletListSelected(1);
            });
        }

        if (bulletLetter != null) {
            bulletLetter.setOnClickListener(v -> {
                selectListView(v);
                mListener.onBulletListSelected(2);
            });
        }
    }

    private void selectListView(View view) {
        if (selectedListView != null) {
            selectedListView.setSelected(false);
        }
        view.setSelected(true);
        selectedListView = view;
    }

    private void setupColorGrids(View view) {
        GridLayout fontColorGrid = view.findViewById(R.id.fontColorPickerContainer);
        int[] fontColors = {
                Color.BLACK, Color.parseColor("#424242"),
                Color.parseColor("#757575"), Color.parseColor("#BDBDBD"),
                Color.RED, Color.parseColor("#FF5722"),
                Color.parseColor("#FF9800"), Color.parseColor("#FFC107"),
                Color.parseColor("#FFEB3B"), Color.parseColor("#CDDC39"),
                Color.parseColor("#8BC34A"), Color.parseColor("#4CAF50"),
                Color.parseColor("#009688"), Color.parseColor("#00BCD4"),
                Color.parseColor("#03A9F4"), Color.parseColor("#2196F3"),
                Color.parseColor("#3F51B5"), Color.parseColor("#673AB7"),
                Color.parseColor("#9C27B0"), Color.parseColor("#E91E63"),
                Color.parseColor("#F44336"), Color.WHITE
        };
        populateColorGrid(fontColorGrid, fontColors, false, mListener.getActiveFontColor());

        GridLayout highlightColorGrid = view.findViewById(R.id.highlightColorPickerContainer);
        int[] highlightColors = {
                Color.TRANSPARENT, Color.parseColor("#FFEB3B"),
                Color.parseColor("#FFC107"), Color.parseColor("#FF9800"),
                Color.parseColor("#8BC34A"), Color.parseColor("#4CAF50"),
                Color.parseColor("#00BCD4"), Color.parseColor("#03A9F4"),
                Color.parseColor("#2196F3"), Color.parseColor("#9C27B0"),
                Color.parseColor("#E91E63"), Color.parseColor("#FFCDD2")
        };
        populateColorGrid(highlightColorGrid, highlightColors, true, mListener.getActiveHighlightColor());
    }

    private void populateColorGrid(GridLayout grid, int[] colors, boolean isHighlight, int activeColor) {
        grid.removeAllViews();
        View viewToSelect = null;

        for (int color : colors) {
            View colorView = new View(getContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            int size = (int) (40 * getResources().getDisplayMetrics().density);
            params.width = size;
            params.height = size;
            params.setMargins(6, 6, 6, 6);
            colorView.setLayoutParams(params);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(color);
            drawable.setStroke(3, Color.parseColor("#E0E0E0"));

            if (color == Color.TRANSPARENT) {
                drawable.setColor(Color.WHITE);
                drawable.setStroke(3, Color.parseColor("#F44336"));
            }

            colorView.setBackground(drawable);
            colorView.setTag(color);

            colorView.setOnClickListener(v -> {
                selectColorView(v, isHighlight);
                mListener.onColorSelected(color, isHighlight);
            });

            grid.addView(colorView);

            if (color == activeColor) {
                viewToSelect = colorView;
            }
        }

        if (viewToSelect != null) {
            selectColorView(viewToSelect, isHighlight);
        }
    }

    private void selectColorView(View view, boolean isHighlight) {
        View previousSelection = isHighlight ? selectedHighlightColorView : selectedFontColorView;

        if (previousSelection != null) {
            GradientDrawable oldDrawable = new GradientDrawable();
            oldDrawable.setShape(GradientDrawable.OVAL);
            int oldColor = (int) previousSelection.getTag();
            oldDrawable.setColor(oldColor);
            oldDrawable.setStroke(3, Color.parseColor("#E0E0E0"));
            if (oldColor == Color.TRANSPARENT) {
                oldDrawable.setColor(Color.WHITE);
                oldDrawable.setStroke(3, Color.parseColor("#F44336"));
            }
            previousSelection.setBackground(oldDrawable);
        }

        // Highlight selected color
        GradientDrawable selectedDrawable = new GradientDrawable();
        selectedDrawable.setShape(GradientDrawable.OVAL);
        int selectedColor = (int) view.getTag();
        selectedDrawable.setColor(selectedColor);
        selectedDrawable.setStroke(6, Color.parseColor("#000000"));
        if (selectedColor == Color.TRANSPARENT) {
            selectedDrawable.setColor(Color.WHITE);
            selectedDrawable.setStroke(6, Color.parseColor("#F44336"));
        }
        view.setBackground(selectedDrawable);

        if (isHighlight) {
            selectedHighlightColorView = view;
        } else {
            selectedFontColorView = view;
        }
    }

    private void selectFontSizeView(View view) {
        if (selectedFontSizeView != null) {
            selectedFontSizeView.setSelected(false);
        }
        view.setSelected(true);
        selectedFontSizeView = view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Clear the listener to avoid memory leaks
        mListener = null;
    }
}