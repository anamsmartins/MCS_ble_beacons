package com.example.ble_beacons_museum;

import java.util.ArrayList;
import java.util.Objects;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class LeDeviceListAdapter {

    // Adapter for holding devices found through scanning.

    private ArrayList<BluetoothDeviceWithRSSI> mLeDevices;

    public LeDeviceListAdapter() {
        super();
        mLeDevices = new ArrayList<BluetoothDeviceWithRSSI>();
    }

    public void addDevice(BluetoothDeviceWithRSSI device) {
        if (!mLeDevices.contains(device)) {
            mLeDevices.add(device);
        }
    }

    public BluetoothDeviceWithRSSI getDevice(int position) {
        return mLeDevices.get(position);
    }

    public void clear() {
        mLeDevices.clear();
    }

    public int getCount() {
        return mLeDevices.size();
    }

    public Object getItem(int i) {
        return mLeDevices.get(i);
    }

    public long getItemId(int i) {
        return i;
    }

    public boolean hasDevice(String address){
        return mLeDevices.stream().anyMatch(element -> Objects.equals(element.getBluetoothDevice().getAddress(), address));
    }

    public void removeDevice(String address) {
        mLeDevices.stream().filter(device -> address.equals(device.getBluetoothDevice().getAddress())).findFirst().ifPresent(foundDevice -> mLeDevices.remove(foundDevice));
    }

    public String getDeviceLowestRSSI(){
        if (mLeDevices.isEmpty()){
            return null;
        }

        BluetoothDeviceWithRSSI minRSSIObj = mLeDevices.get(0); // Assume the first object has the least rssi
        for (BluetoothDeviceWithRSSI device : mLeDevices) {
            if (device.getRSSI() > minRSSIObj.getRSSI()) {
                minRSSIObj = device;
            }
        }
        return minRSSIObj.getBluetoothDevice().getAddress();
    }
}