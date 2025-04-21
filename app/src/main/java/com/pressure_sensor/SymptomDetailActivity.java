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

        // Find all the detail views
        TextView painTextView = findViewById(R.id.painLevelValue);
        TextView otherSymptomsTextView = findViewById(R.id.otherSymptomsValue);
        TextView burningTextView = findViewById(R.id.burningValue);
        TextView numbnessTextView = findViewById(R.id.numbnessValue);
        TextView tinglingTextView = findViewById(R.id.tinglingValue);
        TextView dateTimeTextView = findViewById(R.id.dateTimeValue);

        // Retrieve the symptom log ID from the intent extras
        long symptomLogId = getIntent().getLongExtra("SYMPTOM_LOG_ID", -1L);
        if (symptomLogId == -1L) {
            finish();
            return;
        }

        // Query the database on a background thread
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
            SymptomsLog log = db.symptomsDao().getLogById(symptomLogId);
            if (log != null) {
                runOnUiThread(() -> {
                    painTextView.setText("" + log.painLevel + "/5");
                    otherSymptomsTextView.setText("" + log.otherSymptoms);
                    burningTextView.setText("Burning: " + (log.burning ? "Yes" : "No"));
                    numbnessTextView.setText("Numbness: " + (log.numbness ? "Yes" : "No"));
                    tinglingTextView.setText("Tingling (pins and needles): " + (log.tingling ? "Yes" : "No"));
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                    String formattedDate = sdf.format(new Date(log.timestamp));
                    dateTimeTextView.setText(formattedDate);
                });
            }
        }).start();
    }
}


