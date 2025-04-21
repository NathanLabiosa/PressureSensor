package com.pressure_sensor;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class LogSymptomsBottomSheet extends BottomSheetDialogFragment {

    private int selectedPainLevel = 0; // 0 = none selected
    private EditText otherSymptomsEditText;

    // New boolean fields for additional symptoms
    private boolean burning = false;
    private boolean numbness = false;
    private boolean tingling = false;

    public interface OnSymptomsLoggedListener {
        void onSymptomsLogged(int painLevel, String otherSymptoms, boolean burning, boolean numbness, boolean tingling);
    }

    private OnSymptomsLoggedListener callback;

    public LogSymptomsBottomSheet(OnSymptomsLoggedListener callback) {
        this.callback = callback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_bottom_sheet_symptoms, container, false);

        // Close button
        TextView closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());

        // Pain buttons (as before)
        Button painButton1 = view.findViewById(R.id.painButton1);
        Button painButton2 = view.findViewById(R.id.painButton2);
        Button painButton3 = view.findViewById(R.id.painButton3);
        Button painButton4 = view.findViewById(R.id.painButton4);
        Button painButton5 = view.findViewById(R.id.painButton5);
        Button[] painButtons = new Button[] { painButton1, painButton2, painButton3, painButton4, painButton5 };

        View.OnClickListener painButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View buttonView) {
                for (Button btn : painButtons) {
                    btn.setSelected(false);
                }
                buttonView.setSelected(true);
                int viewId = buttonView.getId();
                if (viewId == R.id.painButton1) {
                    selectedPainLevel = 1;
                } else if (viewId == R.id.painButton2) {
                    selectedPainLevel = 2;
                } else if (viewId == R.id.painButton3) {
                    selectedPainLevel = 3;
                } else if (viewId == R.id.painButton4) {
                    selectedPainLevel = 4;
                } else if (viewId == R.id.painButton5) {
                    selectedPainLevel = 5;
                }
            }
        };

        painButton1.setOnClickListener(painButtonListener);
        painButton2.setOnClickListener(painButtonListener);
        painButton3.setOnClickListener(painButtonListener);
        painButton4.setOnClickListener(painButtonListener);
        painButton5.setOnClickListener(painButtonListener);

        // "Other Symptoms" button + EditText
        Button otherSymptomsButton = view.findViewById(R.id.otherSymptomsButton);
        otherSymptomsEditText = view.findViewById(R.id.otherSymptomsEditText);
        otherSymptomsButton.setOnClickListener(v -> {
            if (otherSymptomsEditText.getVisibility() == View.GONE) {
                otherSymptomsEditText.setVisibility(View.VISIBLE);
            } else {
                otherSymptomsEditText.setVisibility(View.GONE);
            }
        });

        // Set up new symptom fields:
        // Burning
        Button burningYesButton = view.findViewById(R.id.burningYesButton);
        Button burningNoButton = view.findViewById(R.id.burningNoButton);
        burningYesButton.setOnClickListener(v -> {
            burning = true;
            burningYesButton.setSelected(true);
            burningNoButton.setSelected(false);
        });
        burningNoButton.setOnClickListener(v -> {
            burning = false;
            burningYesButton.setSelected(false);
            burningNoButton.setSelected(true);
        });

        // Numbness
        Button numbnessYesButton = view.findViewById(R.id.numbnessYesButton);
        Button numbnessNoButton = view.findViewById(R.id.numbnessNoButton);
        numbnessYesButton.setOnClickListener(v -> {
            numbness = true;
            numbnessYesButton.setSelected(true);
            numbnessNoButton.setSelected(false);
        });
        numbnessNoButton.setOnClickListener(v -> {
            numbness = false;
            numbnessYesButton.setSelected(false);
            numbnessNoButton.setSelected(true);
        });

        // Tingling
        Button tinglingYesButton = view.findViewById(R.id.tinglingYesButton);
        Button tinglingNoButton = view.findViewById(R.id.tinglingNoButton);
        tinglingYesButton.setOnClickListener(v -> {
            tingling = true;
            tinglingYesButton.setSelected(true);
            tinglingNoButton.setSelected(false);
        });
        tinglingNoButton.setOnClickListener(v -> {
            tingling = false;
            tinglingYesButton.setSelected(false);
            tinglingNoButton.setSelected(true);
        });

        // Create the submit button programmatically (or add it in XML)
        Button submitButton = new Button(getContext());
        submitButton.setText("Submit");
        submitButton.setBackgroundResource(R.drawable.rounded_button);
        submitButton.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        Typeface redHatFont = ResourcesCompat.getFont(getContext(), R.font.red_hat_regular);
        submitButton.setTypeface(redHatFont);
        submitButton.setBackgroundTintList(null);
        ((ViewGroup) view).addView(submitButton);

        // Submit button listener
        submitButton.setOnClickListener(v -> {
            String otherSymptoms = otherSymptomsEditText.getVisibility() == View.VISIBLE
                    ? otherSymptomsEditText.getText().toString()
                    : "";
            if (callback != null) {
                callback.onSymptomsLogged(selectedPainLevel, otherSymptoms, burning, numbness, tingling);
            }
            dismiss();
        });

        return view;
    }
}

