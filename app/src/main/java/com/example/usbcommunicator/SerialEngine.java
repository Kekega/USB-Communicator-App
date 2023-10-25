package com.example.usbcommunicator;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;

public final class SerialEngine {
    private final UsbManager mUsbManager;
    private final String componentName = "DeviceEngine";
    public volatile boolean isConnected;

    private final Context mContext;
    private final IUsbCallback mCallback;
    private UsbSerialDevice serialDevice;


    public SerialEngine(@NotNull Context mContext, @NotNull IUsbCallback mCallback) {
        super();
        this.mContext = mContext;
        this.mCallback = mCallback;
        this.mUsbManager = (UsbManager) this.mContext.getSystemService(Context.USB_SERVICE);
    }

    public void disconnect() {
        isConnected = false;
        serialDevice.close();
    }

    public void write(byte[] data) {
        this.serialDevice.write(data);
        Log.d(componentName, "Data written: " + Arrays.toString(data));
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public void maybeConnect() {
        if (this.isConnected) {
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
            PendingIntent pi = PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.example.usbcommunicator.USB_PERMISSION"), 0);
            this.mUsbManager.requestPermission(device, pi);
            return;
        }

        Log.d(this.componentName, "Permission available, connecting...");
        UsbDeviceConnection connection = this.mUsbManager.openDevice(device);
        if (connection == null) {
            Log.e(this.componentName, "Unable to open device!");
            return;
        }

        this.serialDevice = UsbSerialDevice.createUsbSerialDevice(device, connection);
        this.serialDevice.open();
        this.serialDevice.setBaudRate(9600);
        this.serialDevice.setDataBits(UsbSerialInterface.DATA_BITS_8);
        this.serialDevice.setStopBits(UsbSerialInterface.STOP_BITS_1);
        this.serialDevice.setParity(UsbSerialInterface.PARITY_NONE);
        this.serialDevice.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);

        this.isConnected = true;
        this.mCallback.onConnectionEstablished();
        Log.d(this.componentName, "Connection established.");
    }
}
