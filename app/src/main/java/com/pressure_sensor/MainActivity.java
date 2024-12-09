package com.pressure_sensor;

import android.Manifest;
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
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.bluetooth.le.ScanResult;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private TextView textViewValue;
    private BluetoothAdapter bluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_ALL_PERMISSION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewValue = findViewById(R.id.textViewValue);

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Request necessary permissions.
        checkPermissions();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                        },
                        REQUEST_ALL_PERMISSION);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        REQUEST_ALL_PERMISSION);
            }
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ALL_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show();
            startBleScan();  // Start scanning after permissions are granted
        } else {
            Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show();
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
                Log.d("BLE", "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b"));
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8"));
                gatt.setCharacteristicNotification(characteristic, true);
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                if (descriptor != null) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    boolean stat = gatt.writeDescriptor(descriptor);
                    if (!stat) {
                        Log.e("BLE", "Failed to write descriptor");
                    }
                } else {
                    Log.e("BLE", "Descriptor not found");
                }

                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            final String voltageStr = characteristic.getStringValue(0); // "X.XX V"
            try {
                // Extract the numeric part from the voltage string
                String numericPart = voltageStr.replace(" V", ""); // Remove the ' V' part
                float voltage = Float.parseFloat(numericPart); // Convert string to float

                // Calculate the pressure using the provided equation
                double pressure = calculatePressure(voltage); // Method to calculate pressure

                // Prepare the display text for the TextView
                String displayText = String.format("%.2f kPa of pressure at the sensor %.2f Voltage", pressure, voltage);

                // Update UI elements: TextView and SemicircleGaugeView
                runOnUiThread(() -> {
                    textViewValue.setText(displayText);

                    // Find the gauge view and update its pressure value
                    SemicircleGaugeView gaugeView = findViewById(R.id.semicircleGaugeView);
                    gaugeView.setPressure((float) pressure); // Cast to float if necessary
                });
            } catch (NumberFormatException e) {
                Log.e("BLE", "Failed to parse voltage: " + voltageStr, e);
            }
        }

        private double calculatePressure(double voltage) {
            // Assuming the quadratic form: Ax^2 + Bx + C = voltage
            // Here A, B, and C need to be derived from the equation by rearranging it to the standard quadratic form.
            float A = (float) -0.000000003;
            float B = (float) 0.0002;
            float C = (float)(0.3131 - voltage);

            // Calculate the discriminant
            float discriminant = B * B - 4 * A * C;

            // Check if discriminant is positive
            if (discriminant >= 0) {
                // Two possible solutions for pressure (x)
                float x1 = (float) ((-B + Math.sqrt(discriminant)) / (2 * A));
                float x2 = (float) ((-B - Math.sqrt(discriminant)) / (2 * A));

                // Assuming physical constraints mean pressure must be positive and within sensor range
                return Math.min(x1, x2);

            } else {
                // No real roots; handle error or use default value
                return 0; // Default or error value
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
        BluetoothGatt bluetoothGatt = device.connectGatt(this, false, gattCallback);
    }



}