package com.pressure_sensor;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.bluetooth.le.ScanResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.UUID;

public class MainActivity extends AppCompatActivity implements LogSymptomsBottomSheet.OnSymptomsLoggedListener {

    private TextView textViewValue;
    private BluetoothAdapter bluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_ALL_PERMISSION = 2;

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;


    public enum Zone {
        NORMAL,
        YELLOW,
        RED
    }

    private Zone currentZone = Zone.NORMAL;
    private long yellowZoneStart = 0; // When we first entered yellow
    private ListView eventListView;
    private List<Event> eventList = new ArrayList<>();
    private EventAdapter eventAdapter;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable analysisRunnable = new Runnable() {
        @Override
        public void run() {
            analyzePressureData();
            // Schedule next run after 1 minute (60000 ms)
            handler.postDelayed(this, 60000);
        }
    };

    private BluetoothDevice s3Device;
    private Handler bleHandler = new Handler(Looper.getMainLooper());
    private static final long RECONNECT_DELAY_MS = 2000;

    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic measurementChar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //startPeriodicDatabaseCheck();

        textViewValue = findViewById(R.id.textViewValue);
        eventListView = findViewById(R.id.eventListView);
        eventAdapter = new EventAdapter(this, eventList);
        eventListView.setAdapter(eventAdapter);

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        Button logSymptomsButton = findViewById(R.id.logSymptomsButton);
        logSymptomsButton.setOnClickListener(v -> {
            LogSymptomsBottomSheet sheet = new LogSymptomsBottomSheet(this);
            sheet.show(getSupportFragmentManager(), "LogSymptomsBottomSheet");
        });

        eventListView.setOnItemClickListener((parent, view, position, id) -> {
            Event event = eventAdapter.getItem(position);
            if (event != null) {
                // If it's a symptom log, check for a valid symptomLogId.
                if (event.symptomLogId != -1) {
                    Intent intent = new Intent(MainActivity.this, SymptomDetailActivity.class);
                    intent.putExtra("SYMPTOM_LOG_ID", event.symptomLogId);
                    startActivity(intent);
                } else if (event.description.equals("Yellow Zone - Click for Instructions") ||
                        event.description.equals("Red Zone - Click for Instructions")) {
                    // Launch the zone instruction activity
                    Intent intent = new Intent(MainActivity.this, ZoneDetailActivity.class);
                    // Pass along the zone type as an extra (e.g., "red" or "yellow")
                    if (event.description.contains("Yellow")) {
                        intent.putExtra("ZONE_TYPE", "yellow");
                    } else {
                        intent.putExtra("ZONE_TYPE", "red");
                    }
                    startActivity(intent);
                }
            }
        });
        ImageButton doctorViewButton = findViewById(R.id.doctorViewButton);
        doctorViewButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DoctorViewActivity.class);
            startActivity(intent);
        });

        ImageButton batteryButton = findViewById(R.id.batteryButton);
        batteryButton.setOnClickListener(v -> {
            if (bluetoothGatt != null && measurementChar != null) {
                // writing to the char will trigger the ESP32 to send back its battery voltage
                measurementChar.setValue(new byte[]{0x00});  // payload ignored by ESP32
                boolean ok = bluetoothGatt.writeCharacteristic(measurementChar);
                if (!ok) {
                    Toast.makeText(this, "Failed to request battery level", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Not connected yet", Toast.LENGTH_SHORT).show();
            }
        });


        createNotificationChannel();

        // Request necessary permissions.
        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(analysisRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(analysisRunnable);
    }

    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // For Android 13+ (API level 33 and above), add the POST_NOTIFICATIONS permission.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // For Android 12 (API level 31/S) and above: check location and Bluetooth permissions.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For Android M through Android 11: only location permissions are needed.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        }

        // If there are any permissions that are not granted, request them.
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    REQUEST_ALL_PERMISSION);
        } else {
            // All permissions are already granted; start BLE scan.
            startBleScan();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ALL_PERMISSION) {
            boolean allGranted = true;
            if (grantResults.length > 0) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
            } else {
                allGranted = false;
            }

            if (allGranted) {
                Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show();
                startBleScan();  // Start scanning after permissions are granted
            } else {
                Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            startBleScan();  // Start scanning after Bluetooth is enabled
        }
    }


    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BLE", "Connected to GATT server. Attempting to start service discovery: " + gatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w("BLE", "S3 disconnected—will retry in " + RECONNECT_DELAY_MS + "ms");
                gatt.close();                      // clean up old GATT
                bleHandler.postDelayed(() -> {
                    if (s3Device != null) {
                        Log.d("BLE", "Re‑connecting to S3");
                        connectToDevice(s3Device);
                    }
                }, RECONNECT_DELAY_MS);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // 1) save for later writes
                bluetoothGatt = gatt;

                // 2) look up the characteristic and save it
                BluetoothGattService service =
                        gatt.getService(UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b"));
                measurementChar =
                        service.getCharacteristic(UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8"));

                // 3) enable notifications exactly as you already do
                gatt.setCharacteristicNotification(measurementChar, true);
                BluetoothGattDescriptor descriptor = measurementChar.getDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                if (descriptor != null) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            final String voltageStr = characteristic.getStringValue(0); // "X.XX V"
            if (voltageStr.startsWith("BAT:")) {
                // battery reply path
                String batt = voltageStr.substring(4);  // remove the "BAT:" prefix
                sendBatteryNotification(batt);
            } else {
                // Extract the numeric part from the voltage string
                String numericPart = voltageStr.replace(" V", ""); // Remove the ' V' part
                float voltage = Float.parseFloat(numericPart); // Convert string to float

                // Calculate the pressure using the provided equation
                double pressure = calculatePressure(voltage); // Method to calculate pressure

                // Update UI elements: TextView and SemicircleGaugeView
                runOnUiThread(() -> {
                    // 1) Update the zone-status label
                    updatePressureZoneUI(pressure);
                    String status;
                    if (pressure < 666.612) {
                        status = "Acceptable Pressure ";          // green
                    } else if (pressure < 6666.12) {
                        status = "At Risk Pressure ";             // yellow
                    } else {
                        status = "Dangerous Pressure ";           // red
                    }
                    textViewValue.setText(status);

                    // Find the gauge view and update its pressure value
                    SemicircleGaugeView gaugeView = findViewById(R.id.semicircleGaugeView);
                    gaugeView.setPressure((float) pressure); // Cast to float if necessary
                });

                // Create a PressureMeasurement record
                PressureMeasurement measurement = new PressureMeasurement();
                measurement.timestamp = System.currentTimeMillis();
                measurement.pressure = pressure;

                // Insert the record in a background thread
                new Thread(() -> {
                    AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
                    db.pressureMeasurementDao().insertMeasurement(measurement);
                }).start();


            }
            runOnUiThread(() -> analyzePressureData());
        }
        private static final String TAG = "CalcPressure";
        private double calculatePressure(double voltage) {
            // grab the current setting
            MitigatorSettings.Type type = MitigatorSettings.getCurrent();
            // log it so you’ll see it in logcat whenever this method runs
            Log.d(TAG, "calculatePressure() using mitigator: " + type);

            switch (type) {
                case SMALL:
                    Log.d(TAG, String.format("SMALL formula: voltage=%.2f", voltage));
                    //Voltage = -0.0502*Pressure + 2.226
                    return Math.max((voltage - 2.226) / -0.0502 * 1000,0);
                case LARGE:
                    Log.d(TAG, String.format("LARGE formula: voltage=%.2f", voltage));
                    return Math.max((voltage - 2.0682) / -0.0494 * 1000,0);
                case MEDIUM:
                default:
                    Log.d(TAG, String.format("MEDIUM formula: voltage=%.2f", voltage));
                    return Math.max((voltage - 2.1265) / -0.0506 * 1000,0);
            }
        }

    };

    private void startBleScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
            if (scanner != null) {
                scanner.startScan(new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        super.onScanResult(callbackType, result);
                        BluetoothDevice device = result.getDevice();
                        if (device.getName() != null && device.getName().equals("ESP32 Voltage Meter")) {
                            scanner.stopScan(this);
                            connectToDevice(device);
                            s3Device = device;
                        }
                    }

                    @Override
                    public void onBatchScanResults(List<ScanResult> results) {
                        super.onBatchScanResults(results);
                    }

                    @Override
                    public void onScanFailed(int errorCode) {
                        super.onScanFailed(errorCode);
                    }
                });
            }
        } else {
            // Permission not granted
            Toast.makeText(this, "BLE scan permission not granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        device.connectGatt(this, false, gattCallback);
    }

    private void startPeriodicDatabaseCheck() {
        final Handler handler = new Handler(Looper.getMainLooper());
        final int delay = 60 * 1000; // 60 seconds

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new Thread(() -> {
                    AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
                    List<PressureMeasurement> highPressureMeasurements = db.pressureMeasurementDao().getMeasurementsAbove(6000);
                    if (!highPressureMeasurements.isEmpty()) {
                        runOnUiThread(() -> sendNotification(highPressureMeasurements.get(0).pressure));
                    }
                }).start();

                // Re-run this check after the delay
                handler.postDelayed(this, delay);
            }
        }, delay);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Pressure Alerts";
            String description = "Notifications when pressure exceeds safe levels";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("pressure_channel", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    private void sendNotification(double pressure) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "pressure_channel")
                .setSmallIcon(R.drawable.baseline_priority_high_24) // replace with your icon resource
                .setContentTitle("High Pressure Alert")
                .setContentText("Pressure reached " + pressure + " kPa")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // Use a unique ID if you want multiple notifications, or a constant ID to update the same one.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify(1, builder.build());
    }

    private void sendYellowZoneNotification() {
        // Build and send a yellow zone notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "pressure_channel")
                .setSmallIcon(R.drawable.baseline_priority_high_24)
                .setContentTitle("Yellow Zone")
                .setContentText("Unsafe pressure: recovery period not achieved!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(this).notify(2, builder.build());
    }

    private void sendRedZoneNotification() {
        // Build and send a red zone notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "pressure_channel")
                .setSmallIcon(R.drawable.baseline_priority_high_24)
                .setContentTitle("Red Zone")
                .setContentText("Critical pressure condition: prolonged unsafe readings!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(this).notify(3, builder.build());
    }

    private static final String TAG = "ZoneAnalysis";
    private void analyzePressureData() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
            long currentTime = System.currentTimeMillis();
            long tenMinutesAgo = currentTime - (10 * 60 * 1000L);

            // 1) First, pull *all* measurements so we can test for a full 10-min span:
            List<PressureMeasurement> allMeasurements =
                    db.pressureMeasurementDao().getAllMeasurements();  // assumes you’ve added this DAO method
            if (allMeasurements.isEmpty()) {
                // no data at all yet → nothing to do
                return;
            }
            // sort oldest→newest
            Collections.sort(allMeasurements, (a, b) -> Long.compare(a.timestamp, b.timestamp));

            long earliestTs = allMeasurements.get(0).timestamp;
            // if the earliest measurement is *newer* than (now - 10min), we don't yet
            // have a full 10 minutes of samples, so bail out
            if (earliestTs > tenMinutesAgo) {
                Log.d(TAG, "Not yet 10 minutes of history; skipping analysis");
                currentZone = Zone.NORMAL;
                return;
            }

            // 2) Now fetch just the last 10 minutes’ worth of data
            List<PressureMeasurement> measurements =
                    db.pressureMeasurementDao().getMeasurementsSince(tenMinutesAgo);
            Collections.sort(measurements, (m1, m2) ->
                    Long.compare(m1.timestamp, m2.timestamp));
            Log.d(TAG, "Analysis starting");

            // 1) Look for a high spike (>6000)
            boolean highSpikeDetected = false;
            long spikeTime = 0;
            for (PressureMeasurement m : measurements) {
                if (m.pressure > 6000) {
                    highSpikeDetected = true;
                    spikeTime = m.timestamp;
                    break;
                }
            }

            // 2) No spike → back to NORMAL
            if (!highSpikeDetected) {
                currentZone = Zone.NORMAL;
                return;
            }

            // 3) Check for a 5-minute safe stretch (<600) after the spike
            Long safeStart = null;
            boolean safeAchieved = false;
            for (PressureMeasurement m : measurements) {
                if (m.timestamp < spikeTime) continue;
                if (m.pressure < 600) {
                    if (safeStart == null) safeStart = m.timestamp;
                    if (m.timestamp - safeStart >= 5 * 60_000L) {
                        safeAchieved = true;
                        break;
                    }
                } else {
                    safeStart = null;
                }
            }

            if (safeAchieved) {
                // recovery → back to NORMAL if needed
                if (currentZone != Zone.NORMAL) {
                    currentZone = Zone.NORMAL;
                }
            } else {
                // still unsafe
                if (currentZone == Zone.NORMAL) {
                    // first time → YELLOW
                    currentZone = Zone.YELLOW;
                    yellowZoneStart = currentTime;
                    runOnUiThread(() -> {
                        sendYellowZoneNotification();
                        onYellowZoneReached();
                    });
                } else if (currentZone == Zone.YELLOW
                        && currentTime - yellowZoneStart >= 5 * 60_000L) {
                    // 5 more minutes in YELLOW → RED
                    currentZone = Zone.RED;
                    runOnUiThread(() -> {
                        sendRedZoneNotification();
                        onRedZoneReached();
                    });
                }
            }
        }).start();
    }

    // Helper method to add an event.
    private void addEvent(String description) {
        Event newEvent = new Event(description, System.currentTimeMillis());
        // Insert at the beginning so the most recent event is at the top
        eventList.add(0, newEvent);
        runOnUiThread(() -> eventAdapter.notifyDataSetChanged());
    }

    // For example, when yellow zone is reached:
    private void onYellowZoneReached() {
        addEvent("Yellow Zone - Click for Instructions");
    }

    // And when red zone is reached:
    private void onRedZoneReached() {
        addEvent("Red Zone - Click for Instructions");
    }

    // And for patient logging symptoms:
    private void onSymptomsLogged() {
        addEvent("Symptoms logged");
    }
    private void updatePressureZoneUI(double pressure) {
        // pick the correct color
        @ColorRes int colorRes;
        if (pressure < 600) {
            colorRes = R.color.zone_green;     // safe
        } else if (pressure < 6000) {
            colorRes = R.color.zone_yellow;    // at risk
        } else {
            colorRes = R.color.zone_red;       // dangerous
        }

        // find your views
        TextView label = findViewById(R.id.currentPressureLabel);
        CardView card  = findViewById(R.id.currentPressureCard);

        // apply the tint
        label.setBackgroundTintList(
                ContextCompat.getColorStateList(this, colorRes));
        card.setBackgroundTintList(
                ContextCompat.getColorStateList(this, colorRes));
    }



    // This is called when the user hits "Submit" in the bottom sheet
    @Override
    public void onSymptomsLogged(int painLevel, String otherSymptoms, boolean burning, boolean numbness, boolean tingling) {
        new Thread(() -> {
            // Get the database instance.
            AppDatabase db = AppDatabase.getDatabase(getApplicationContext());

            // Create a new SymptomsLog record and populate its fields.
            SymptomsLog log = new SymptomsLog();
            log.timestamp = System.currentTimeMillis();
            log.painLevel = painLevel;
            log.otherSymptoms = otherSymptoms;
            log.burning = burning;
            log.numbness = numbness;
            log.tingling = tingling;

            // Insert the record into the database.
            long newId = db.symptomsDao().insertSymptomsLog(log);

            // Update your UI on the main thread.
            runOnUiThread(() -> {
                // Create a new event that refers to this symptom log.
                Event symptomEvent = new Event("Symptoms Logged", System.currentTimeMillis(), newId);
                eventList.add(0, symptomEvent); // Insert at the top of the list.
                eventAdapter.notifyDataSetChanged();
            });
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ensure we don’t leak GATT resources when your activity goes away
        bleHandler.removeCallbacksAndMessages(null);
        // if you kept a BluetoothGatt field, call gatt.disconnect() & gatt.close() here
    }


    private static final int BATTERY_NOTIFICATION_ID = 927; // any unique ID

    private static final float BATTERY_MIN_VOLTAGE = 3.0f;
    private static final float BATTERY_MAX_VOLTAGE = 4.28f;

    private void sendBatteryNotification(String batteryVoltage) {
        // Try to parse the voltage and map to 0–100%
        String percentText;
        try {
            // batteryVoltage comes in like "3.75 V"
            float voltage = Float.parseFloat(batteryVoltage.replace(" V", ""));
            // normalize between 3.0 and 4.28
            float pct = (voltage - BATTERY_MIN_VOLTAGE)
                    / (BATTERY_MAX_VOLTAGE - BATTERY_MIN_VOLTAGE)
                    * 100f;
            // clamp to [0,100]
            pct = Math.max(0f, Math.min(100f, pct));
            int rounded = Math.round(pct);
            percentText = rounded + "%";
        } catch (NumberFormatException e) {
            // fallback on parse error
            percentText = batteryVoltage;
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "pressure_channel")
                .setSmallIcon(R.drawable.battery)
                .setContentTitle("Battery Level")
                .setContentText(percentText)         // now shows "75%" instead of "3.75 V"
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Android 13+ permission guard
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Dispatch
        NotificationManagerCompat.from(this)
                .notify(BATTERY_NOTIFICATION_ID, builder.build());
    }



}