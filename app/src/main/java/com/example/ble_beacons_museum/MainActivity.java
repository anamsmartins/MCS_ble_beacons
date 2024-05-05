package com.example.ble_beacons_museum;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Objects;

@RequiresApi(api = Build.VERSION_CODES.S)
public class MainActivity extends AppCompatActivity {

    // Constants
    private static final int REQUEST_PERMISSIONS = 123;
    private static final int BLUETOOTH_ENABLE = 4;
    private static final int REQUEST_BEACON1_ACTIVITY = 5;
    private static final int REQUEST_BEACON2_ACTIVITY = 6;
    private static final String BEACON1_ADDRESS = "CD:3B:21:29:C1:C3";
    private static final String BEACON2_ADDRESS = "FF:2F:31:2C:9B:9A";
    private static final long SCAN_PERIOD = 600000;

    // BLE Setup
    private final LeDeviceListAdapter leDeviceListAdapter = new LeDeviceListAdapter();
    private final String[] permissions = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning = false;
    private Handler handler = new Handler();

    // Beacons Setup
    private Thread threadBeacon1;
    private Thread threadBeacon2;
    private Thread threadScanInterval;
    private int beacon1seconds;
    private int beacon2seconds;
    private int scanIntervalSeconds;
    private TextView beacon1secondsText;
    private TextView beacon2secondsText;
    private TextView intervalTimerText;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        beacon1secondsText = (TextView) findViewById(R.id.time_beacon1_seconds);
        beacon2secondsText = (TextView) findViewById(R.id.time_beacon2_seconds);
        intervalTimerText = (TextView) findViewById(R.id.intervalTimerTextView);

        setupTimers();

        // Check if it has all permissions
        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            // Request all permissions
            ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_PERMISSIONS);
        } else {
            Toast.makeText(MainActivity.this, "All permissions already granted", Toast.LENGTH_SHORT).show();

            // Check if device supports bluetooth
            BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter == null) {
                Toast.makeText(MainActivity.this, "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!bluetoothAdapter.isEnabled()) {
                // Bluetooth is not enabled, request the user to enable it
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, BLUETOOTH_ENABLE);
            } else {
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

                scanLeDevice();
            }
        }
    }

    // After the answer to all the permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSIONS:
                // Check if all permissions were granted
                boolean allPermissionsGranted = true;
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false;
                        break;
                    }
                }
                if (allPermissionsGranted) {
                    // All permissions were granted, proceed with your logic
                    Toast.makeText(MainActivity.this, "All permissions granted", Toast.LENGTH_SHORT).show();

                    // Check if device supports bluetooth
                    BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
                    bluetoothAdapter = bluetoothManager.getAdapter();
                    if (bluetoothAdapter == null) {
                        Toast.makeText(MainActivity.this, "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!bluetoothAdapter.isEnabled()) {
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }

                        // Bluetooth is not enabled, request the user to enable it
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, BLUETOOTH_ENABLE);
                    }else {
                        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                        scanLeDevice();
                    }
                } else {
                    // At least one permission was denied, handle it appropriately
                    Toast.makeText(MainActivity.this, "Some permissions were denied", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    // Handle the result of the enable Bluetooth request or activity requests
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BLUETOOTH_ENABLE) {
            if (resultCode == RESULT_OK) {
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                scanLeDevice();
            } else {
                Toast.makeText(MainActivity.this, "The application needs the bluetooth enabled", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == REQUEST_BEACON1_ACTIVITY) {
            leDeviceListAdapter.removeDevice(BEACON1_ADDRESS);
        }

        if (requestCode == REQUEST_BEACON2_ACTIVITY) {
            leDeviceListAdapter.removeDevice(BEACON2_ADDRESS);
        }
    }

    private void setupTimers(){
        beacon1seconds = 0;
        beacon2seconds = 0;
        scanIntervalSeconds = 0;

        beacon1secondsText.setText("0 s");
        beacon2secondsText.setText("0 s");
        intervalTimerText.setText("0 s");


        threadBeacon1 = new Thread() {

            @Override
            public void run() {
                try {
                    while (!this.isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                beacon1seconds += 1;
                                String seconds = beacon1seconds + " s";
                                beacon1secondsText.setText(seconds);
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        threadBeacon2 = new Thread() {
            @Override
            public void run() {
                try {
                    while (!this.isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                beacon2seconds += 1;
                                String seconds = beacon2seconds + " s";
                                beacon2secondsText.setText(seconds);
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        threadScanInterval = new Thread() {
            @Override
            public void run() {
                try {
                    while (!this.isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                scanIntervalSeconds += 1;
                                String seconds = scanIntervalSeconds + " s";
                                intervalTimerText.setText(seconds);

                                if(scanIntervalSeconds == 30){
                                    String address = leDeviceListAdapter.getDeviceLowestRSSI();
                                    if (address == null){
                                        Toast.makeText(MainActivity.this, "No beacon detected!", Toast.LENGTH_SHORT).show();
                                    }else if(address.equals(BEACON1_ADDRESS)) {
                                        Intent beacon1Page = new Intent(MainActivity.this, BLEBeaconActivity.class);
                                        startActivityForResult(beacon1Page, REQUEST_BEACON1_ACTIVITY);
                                    }else {
                                        Intent beacon2Page = new Intent(MainActivity.this, BLEBeacon2Activity.class);
                                        startActivityForResult(beacon2Page, REQUEST_BEACON2_ACTIVITY);
                                    }
                                    scanIntervalSeconds = 0;

                                    if(address != null) {
                                        if (leDeviceListAdapter.hasDevice(BEACON1_ADDRESS)) {
                                            leDeviceListAdapter.removeDevice(BEACON1_ADDRESS);
                                        }
                                        if (leDeviceListAdapter.hasDevice(BEACON2_ADDRESS)) {
                                            leDeviceListAdapter.removeDevice(BEACON2_ADDRESS);
                                        }
                                    }
                                }

                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };
    }

    private void scanLeDevice() {
        threadScanInterval.start();
        threadBeacon1.start();
        threadBeacon2.start();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (!scanning) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }

                    scanning = false;
                    bluetoothLeScanner.stopScan(leScanCallback);
                }
            }, SCAN_PERIOD);

            scanning = true;
            bluetoothLeScanner.startScan(leScanCallback);
        } else {
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    private ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);

                    BluetoothDevice foundDevice = result.getDevice();

                    if (Objects.equals(foundDevice.getAddress(), BEACON1_ADDRESS)) {
                        if (leDeviceListAdapter.hasDevice(BEACON1_ADDRESS)){
                            leDeviceListAdapter.removeDevice(BEACON1_ADDRESS);
                        }
                        BluetoothDeviceWithRSSI bluetoothDeviceWithRSSI = new BluetoothDeviceWithRSSI(foundDevice, result.getRssi());
                        leDeviceListAdapter.addDevice(bluetoothDeviceWithRSSI);
                        beacon1seconds = 0;
                        beacon1secondsText.setText("0 s");
                    }

                    if (Objects.equals(foundDevice.getAddress(), BEACON2_ADDRESS)) {
                        if (leDeviceListAdapter.hasDevice(BEACON2_ADDRESS)){
                            leDeviceListAdapter.removeDevice(BEACON2_ADDRESS);
                        }
                        BluetoothDeviceWithRSSI bluetoothDeviceWithRSSI = new BluetoothDeviceWithRSSI(foundDevice, result.getRssi());
                        leDeviceListAdapter.addDevice(bluetoothDeviceWithRSSI);
                        beacon2seconds = 0;
                        beacon2secondsText.setText("0 s");
                    }
                }
            };


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}