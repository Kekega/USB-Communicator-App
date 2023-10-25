package com.example.usbcommunicator;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public final class AccessoryEngine {
    private final UsbManager usbManager;
    private final String componentName = "AccessoryEngine";
    public volatile boolean isConnected;
    private ParcelFileDescriptor pd;
    private FileInputStream inputStream;
    private FileOutputStream outputStream;
    private final Runnable accessoryReader = () -> {
        byte[] buf = new byte[1024];

        while (true) {
            try {
                int read = AccessoryEngine.this.inputStream.read(buf);
                AccessoryEngine.this.mCallback.onDataReceived(buf, read);
            } catch (Exception exc) {
                Log.d(AccessoryEngine.this.componentName, "run:" + exc.getMessage());
                Log.d(AccessoryEngine.this.componentName, "run: exiting reader thread");
                break;
            }
        }

        disconnect();
    };
    private final Context mContext;
    private final IUsbCallback mCallback;

    public AccessoryEngine(@NotNull Context mContext, @NotNull IUsbCallback mCallback) {
        super();
        this.mContext = mContext;
        this.mCallback = mCallback;
        this.usbManager = (UsbManager) this.mContext.getSystemService(Context.USB_SERVICE);
    }

    public void disconnect() {
        isConnected = false;

        if (pd != null) {
            try {
                pd.close();
            } catch (IOException exc) {
                Log.d(componentName, "Disconnect: unable to close ParcelFD");
            }
        }

        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException exc) {
                Log.d(componentName, "Disconnect: unable to close InputStream");
            }
        }

        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException var3) {
                Log.d(componentName, "Disconnect: unable to close OutputStream");
            }
        }
    }

    public void write(@NotNull byte[] data) {
        try {
            this.outputStream.write(data);
            Log.d(this.componentName, "Data written" + Arrays.toString(data));
        } catch (IOException exc) {
            Log.d(this.componentName, "Could not write: " + exc.getMessage());
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public void maybeConnect() {
        if (this.isConnected && this.outputStream != null && this.inputStream != null) {
            Log.d(this.componentName, "Already connected!");
            return;
        }

        Log.d(this.componentName, "Discovering accessories...");
        UsbAccessory[] accessoryList = this.usbManager.getAccessoryList();
        if (accessoryList == null || accessoryList.length == 0) {
            Log.d(this.componentName, "No accessories found.");
            return;
        }

        UsbAccessory mAccessory = accessoryList[0];
        if (!this.usbManager.hasPermission(mAccessory)) {
            Log.d(this.componentName, "Permission missing, requesting...");
            PendingIntent pi = PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.example.usbcommunicator.USB_PERMISSION"), 0);
            this.usbManager.requestPermission(mAccessory, pi);
            return;
        }

        Log.d(this.componentName, "Permission available, connecting...");
        this.pd = this.usbManager.openAccessory(mAccessory);
        if (this.pd == null) {
            Log.e(this.componentName, "Unable to open accessory!");
            return;
        }

        ParcelFileDescriptor pd = this.pd;
        FileDescriptor mFileDescriptor = pd.getFileDescriptor();
        this.inputStream = new FileInputStream(mFileDescriptor);
        this.outputStream = new FileOutputStream(mFileDescriptor);
        this.isConnected = true;
        this.mCallback.onConnectionEstablished();
        Thread sAccessoryThread = new Thread(this.accessoryReader, "Reader Thread");
        sAccessoryThread.start();
        Log.d(this.componentName, "Connection established.");
    }
}