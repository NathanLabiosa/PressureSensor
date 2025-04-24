package com.pressure_sensor;

import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DoctorViewActivity extends AppCompatActivity {

    private TextView maxPressureTextView, avg1MinTextView, avg3MinTextView, avg5MinTextView;
    private Button resetButton, smallBtn, mediumBtn, largeBtn;
    private TextView closeButton;
    private long baselineTime;  // This will be our starting point for queries

    private final Handler handler = new Handler();
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            updateDoctorViewData();
            handler.postDelayed(this, 1000); // Update every second
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.doctor_view);

        maxPressureTextView = findViewById(R.id.maxPressureTextView);
        avg1MinTextView = findViewById(R.id.avg1MinTextView);
        avg3MinTextView = findViewById(R.id.avg3MinTextView);
        avg5MinTextView = findViewById(R.id.avg5MinTextView);
        resetButton = findViewById(R.id.resetButton);
        closeButton = findViewById(R.id.closeButton);

        // Set the initial baseline. For example, show the last 10 minutes by default.
        baselineTime = System.currentTimeMillis() - (10 * 60 * 1000L);

        // Set up the reset button to update the baseline time to "now"
        resetButton.setOnClickListener(v -> {
            baselineTime = System.currentTimeMillis();
        });

        // Set up the close button to finish the activity and return to the main screen
        closeButton.setOnClickListener(v -> finish());

        smallBtn  = findViewById(R.id.smallMitBtn);
        mediumBtn = findViewById(R.id.mediumMitBtn);
        largeBtn  = findViewById(R.id.largeMitBtn);

// initialize highlighting
        updateMitigatorButtons();

        smallBtn.setOnClickListener(v -> {
            MitigatorSettings.setCurrent(MitigatorSettings.Type.SMALL);
            updateMitigatorButtons();
        });
        mediumBtn.setOnClickListener(v -> {
            MitigatorSettings.setCurrent(MitigatorSettings.Type.MEDIUM);
            updateMitigatorButtons();
        });
        largeBtn.setOnClickListener(v -> {
            MitigatorSettings.setCurrent(MitigatorSettings.Type.LARGE);
            updateMitigatorButtons();
        });

        // Start periodic updates
        handler.post(updateRunnable);
    }

    private void updateMitigatorButtons() {
        MitigatorSettings.Type sel = MitigatorSettings.getCurrent();
        smallBtn.setEnabled(sel != MitigatorSettings.Type.SMALL);
        mediumBtn.setEnabled(sel != MitigatorSettings.Type.MEDIUM);
        largeBtn.setEnabled(sel != MitigatorSettings.Type.LARGE);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
    }

    private void updateDoctorViewData() {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
            long currentTime = System.currentTimeMillis();

            // Use baselineTime as the lower bound for your query instead of a fixed "10 minutes ago"
            List<PressureMeasurement> measurements = db.pressureMeasurementDao().getMeasurementsSince(baselineTime);

            // Calculate averages
            double sum1 = 0, sum3 = 0, sum5 = 0;
            int count1 = 0, count3 = 0, count5 = 0;
            for (PressureMeasurement m : measurements) {
                if (m.timestamp >= currentTime - (1 * 60 * 1000L)) {
                    sum1 += m.pressure;
                    count1++;
                }
                if (m.timestamp >= currentTime - (3 * 60 * 1000L)) {
                    sum3 += m.pressure;
                    count3++;
                }
                if (m.timestamp >= currentTime - (5 * 60 * 1000L)) {
                    sum5 += m.pressure;
                    count5++;
                }
            }
            double avg1 = (count1 > 0) ? sum1 / count1 : 0;
            double avg3 = (count3 > 0) ? sum3 / count3 : 0;
            double avg5 = (count5 > 0) ? sum5 / count5 : 0;

            // Retrieve max pressure using the new DAO method (note: this method should be defined in your DAO)
            Double maxPressureValue = db.pressureMeasurementDao().getMaxPressureSince(baselineTime);
            double maxPressure = (maxPressureValue == null) ? 0.0 : maxPressureValue;

            runOnUiThread(() -> {
                // "%.2f" means “floating‑point with 2 digits after the decimal”
                String maxStr  = String.format(Locale.getDefault(), "Max Pressure: %.2f", maxPressure);
                String avg1Str = String.format(Locale.getDefault(), "Average Pressure (1 min): %.2f", avg1);
                String avg3Str = String.format(Locale.getDefault(), "Average Pressure (3 mins): %.2f", avg3);
                String avg5Str = String.format(Locale.getDefault(), "Average Pressure (5 mins): %.2f", avg5);

                maxPressureTextView.setText(maxStr);
                avg1MinTextView .setText(avg1Str);
                avg3MinTextView .setText(avg3Str);
                avg5MinTextView .setText(avg5Str);
            });
        });
    }
}
