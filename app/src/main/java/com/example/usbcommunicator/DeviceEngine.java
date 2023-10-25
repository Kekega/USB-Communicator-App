package com.example.usbcommunicator;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.util.Log;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public final class DeviceEngine {
    private final UsbManager mUsbManager;
    private final String componentName = "DeviceEngine";
    private volatile boolean mDeviceConnected;

    private final Context mContext;
    private final IUsbEngineCallback mCallback;
    private final BroadcastReceiver mPermissionReceiver;
    private UsbDeviceConnection connection;
    private UsbEndpoint endpointOut;
    private UsbSerialDevice serialDevice;


    public DeviceEngine(@NotNull Context mContext, @NotNull IUsbEngineCallback mCallback) {
        super();
        this.mContext = mContext;
        this.mCallback = mCallback;
        this.mUsbManager = (UsbManager) this.mContext.getSystemService(Context.USB_SERVICE);

        BroadcastReceiver mDetachedReceiver = new BroadcastReceiver() {
            public void onReceive(@NotNull Context context, @NotNull Intent intent) {
                if (Objects.requireNonNull(intent.getAction()).equals(UsbManager.ACTION_USB_ACCESSORY_DETACHED)) {
                    DeviceEngine.this.mCallback.onDeviceDisconnected();
                }
            }
        };
        this.mContext.registerReceiver(mDetachedReceiver, new IntentFilter("android.hardware.usb.action.USB_ACCESSORY_DETACHED"));

        this.mPermissionReceiver = new BroadcastReceiver() {
            public void onReceive(@NotNull Context context, @NotNull Intent intent) {
                DeviceEngine.this.mContext.unregisterReceiver((BroadcastReceiver) this);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    Log.d(DeviceEngine.this.componentName, "USB Permission granted! Let's try to connect.");
                    DeviceEngine.this.connectDevice();
                } else {
                    Log.d(DeviceEngine.this.componentName, "Permission denied!");
                    Toast.makeText(DeviceEngine.this.mContext, (CharSequence) "Permission denied! Please give permission!", Toast.LENGTH_SHORT).show();
                }

            }
        };
    }

    public final void onIntent(@Nullable Intent intent) {
        this.connectDevice();
    }

    public final void write(@NotNull byte[] data) {
        if (!this.mDeviceConnected) {
            Log.d(this.componentName, "Unable to write: not connected.");
            Toast.makeText(this.mContext, (CharSequence) "Not connected!", Toast.LENGTH_SHORT).show();
        } else {
            this.serialDevice.write(data);
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void connectDevice() {
        if (this.mDeviceConnected) {
            Log.d(this.componentName, "Already connected!");
            return;
        }

        Log.d(this.componentName, "Discovering devices...");
        HashMap<String, UsbDevice> deviceList = this.mUsbManager.getDeviceList();
        if (deviceList.isEmpty()) {
            Log.d(this.componentName, "No accessories found.");
            return;
        }

        UsbDevice device = deviceList.entrySet().iterator().next().getValue();
        if (!this.mUsbManager.hasPermission(device)) {
            Log.d(this.componentName, "Permission missing, requesting...");
            this.mContext.registerReceiver(this.mPermissionReceiver, new IntentFilter("com.example.usbcommunicator.USB_PERMISSION"));
            PendingIntent pi = PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.example.usbcommunicator.USB_PERMISSION"), 0);
            this.mUsbManager.requestPermission(device, pi);
            return;
        }

        Log.d(this.componentName, "Permission available, connecting...");
        this.connection = this.mUsbManager.openDevice(device);
        if (this.connection == null) {
            Log.e(this.componentName, "Unable to open device!");
            return;
        }

        this.serialDevice = UsbSerialDevice.createUsbSerialDevice(device, this.connection);
        this.serialDevice.open();
        this.serialDevice.setBaudRate(9600);
        this.serialDevice.setDataBits(UsbSerialInterface.DATA_BITS_8);
        this.serialDevice.setStopBits(UsbSerialInterface.STOP_BITS_1);
        this.serialDevice.setParity(UsbSerialInterface.PARITY_NONE);
        this.serialDevice.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);

        this.mCallback.onConnectionEstablished();
        this.mDeviceConnected = true;
        Log.d(this.componentName, "Connection established.");
    }
}
