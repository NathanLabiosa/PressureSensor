package com.pressure_sensor;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SymptomDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom_detail);

        // Set up the close button
        TextView closeButton = findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> finish());

        TextView painTextView = findViewById(R.id.painLevelValue);
        TextView otherSymptomsTextView = findViewById(R.id.otherSymptomsValue);
        TextView dateTimeTextView = findViewById(R.id.dateTimeValue);

        long symptomLogId = getIntent().getLongExtra("SYMPTOM_LOG_ID", -1L);
        if (symptomLogId == -1) {
            // Invalid ID; close the activity.
            finish();
            return;
        }

        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
            SymptomsLog log = db.symptomsDao().getLogById(symptomLogId);
            if (log != null) {
                runOnUiThread(() -> {
                    painTextView.setText(log.painLevel + "/5");
                    otherSymptomsTextView.setText(log.otherSymptoms);
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                    String formattedDate = sdf.format(new Date(log.timestamp));
                    dateTimeTextView.setText(formattedDate);

                });
            }
        }).start();
    }
}


