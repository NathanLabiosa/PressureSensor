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

    public interface OnSymptomsLoggedListener {
        void onSymptomsLogged(int painLevel, String otherSymptoms);
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

        // Find the pain buttons
        Button painButton1 = view.findViewById(R.id.painButton1);
        Button painButton2 = view.findViewById(R.id.painButton2);
        Button painButton3 = view.findViewById(R.id.painButton3);
        Button painButton4 = view.findViewById(R.id.painButton4);
        Button painButton5 = view.findViewById(R.id.painButton5);

// Put them into an array for easy iteration.
        Button[] painButtons = new Button[] { painButton1, painButton2, painButton3, painButton4, painButton5 };

        View.OnClickListener painButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View buttonView) {
//                 Reset the background of all pain buttons to a default (e.g., light gray)
                for (Button btn : painButtons) {
                    btn.setSelected(false);
                }
                // Mark the clicked button as selected
                buttonView.setSelected(true);

                // Determine which button was pressed to update selectedPainLevel
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
            // Toggle visibility of the EditText
            if (otherSymptomsEditText.getVisibility() == View.GONE) {
                otherSymptomsEditText.setVisibility(View.VISIBLE);
            } else {
                otherSymptomsEditText.setVisibility(View.GONE);
            }
        });


        Button submitButton = new Button(getContext());
        submitButton.setText("Submit");

// Set the background to your custom drawable that uses defaultButtonColor
        submitButton.setBackgroundResource(R.drawable.rounded_button);

// Set the text color to white
        submitButton.setTextColor(ContextCompat.getColor(getContext(), R.color.white));

// Set the font to Red Hat (make sure red_hat_regular.ttf is in res/font)
        Typeface redHatFont = ResourcesCompat.getFont(getContext(), R.font.red_hat_regular);
        submitButton.setTypeface(redHatFont);

// Optionally, remove any tint (if needed)
        submitButton.setBackgroundTintList(null);

// Add the button to the parent view
        ((ViewGroup) view).addView(submitButton);

        submitButton.setOnClickListener(v -> {
            String otherSymptoms = otherSymptomsEditText.getVisibility() == View.VISIBLE
                    ? otherSymptomsEditText.getText().toString()
                    : "";
            if (callback != null) {
                callback.onSymptomsLogged(selectedPainLevel, otherSymptoms);
            }
            dismiss();
        });

        return view;
    }
}
