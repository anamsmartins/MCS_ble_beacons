package com.example.ble_beacons_museum;

import android.bluetooth.BluetoothDevice;

public class BluetoothDeviceWithRSSI {
    private BluetoothDevice bluetoothDevice;
    private Integer rssi;

    public BluetoothDeviceWithRSSI(BluetoothDevice bluetoothDevice, Integer rssi) {
        this.bluetoothDevice = bluetoothDevice;
        this.rssi = rssi;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public Integer getRSSI() {
        return rssi;
    }

    public void setRSSI(Integer rssi) {
        this.rssi = rssi;
    }
}
