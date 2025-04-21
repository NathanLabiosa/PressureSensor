package com.pressure_sensor;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ZoneDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zone_detail);

        TextView instructionTextView = findViewById(R.id.instructionTextView);
        ImageView instructionImageView = findViewById(R.id.instructionImageView);
        TextView closeButton = findViewById(R.id.closeButton);
        TextView zoneHeadingTextView = findViewById(R.id.zoneHeadingTextView);

        // Set up the close button so users can return to the main page.
        closeButton.setOnClickListener(v -> finish());

        // Get the zone type from the intent extras.
        String zoneType = getIntent().getStringExtra("ZONE_TYPE");
        String instructions = "";
        if (zoneType != null) {
            if (zoneType.equals("yellow")) {
                zoneHeadingTextView.setText("You are at risk pressure");
                instructions = "Elevate your leg and float the heel (as shown in graphic). " +
                        "Keep your leg in this position for five minutes and see what the pressure reading says. " +
                        "This allows bloodflow to return to the part of your foot that is feeling too much pressure.";
            } else if (zoneType.equals("red")) {
                zoneHeadingTextView.setText("You are dangerous pressure");
                instructions = "Take pressure immediately off your leg and elevate it. " +
                        "Call your doctor’s office right away to let them know you’ve hit the red zone.";
            }
        }
        instructionTextView.setText(instructions);

        // Load the common PNG image regardless of zone.
        instructionImageView.setImageResource(R.drawable.common_instruction);
    }
}