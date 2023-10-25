package com.example.usbcommunicator;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class UsbEngine {
    private final String componentName = "UsbEngine";
    private final Context context;
    private final IUsbCallback callback;

    SerialEngine serialEngine;
    AccessoryEngine accessoryEngine;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public UsbEngine(Context context, IUsbCallback callback) {
        this.context = context;
        this.callback = callback;

        this.serialEngine = new SerialEngine(context, callback);
        this.accessoryEngine = new AccessoryEngine(context, callback);

        IntentFilter detachedFilter = new IntentFilter();
        detachedFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        detachedFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (Objects.equals(intent.getAction(), UsbManager.ACTION_USB_ACCESSORY_DETACHED)) {
                    Log.d(componentName, "Accessory detached.");
                    accessoryEngine.disconnect();
                    callback.onDeviceDisconnected();
                } else if (Objects.equals(intent.getAction(), UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                    Log.d(componentName, "Device detached.");
                    serialEngine.disconnect();
                    callback.onDeviceDisconnected();
                }
            }
        }, detachedFilter);

        context.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    Log.d(componentName, "USB Permission granted! Let's try to connect.");
                    serialEngine.maybeConnect();
                    accessoryEngine.maybeConnect();
                } else {
                    Log.d(componentName, "Permission denied!");
                    Toast.makeText(context, "Permission denied! Please give permission!", Toast.LENGTH_SHORT).show();
                }

            }
        }, new IntentFilter("com.example.usbcommunicator.USB_PERMISSION"));
    }


    public void onNewIntent(@Nullable Intent intent) {
        Log.d(componentName, "Processing intent...");
        this.serialEngine.maybeConnect();
        this.accessoryEngine.maybeConnect();
    }

    public boolean isConnected() {
        return serialEngine.isConnected || accessoryEngine.isConnected;
    }

    public void write(byte[] data) {
        if (this.serialEngine.isConnected) {
            this.serialEngine.write(data);
        } else if (this.accessoryEngine.isConnected) {
            this.accessoryEngine.write(data);
        } else {
            Log.d(componentName, "Unable to write: not connected!");
            Toast.makeText(context, "Not connected!", Toast.LENGTH_SHORT).show();
        }
    }

    public String connectionStatus() {
        if (this.serialEngine.isConnected) {
            return "Connected to Serial";
        } else if (this.accessoryEngine.isConnected) {
            return "Connected to Accessory";
        } else {
            return "Disconnected";
        }
    }
}
