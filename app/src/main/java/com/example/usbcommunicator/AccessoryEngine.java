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
import java.util.Objects;

public final class AccessoryEngine {
    private final UsbManager mUsbManager;
    private final String componentName = "AccessoryEngine";
    private volatile boolean mAccessoryConnected;
    private ParcelFileDescriptor mParcelFileDescriptor;
    private FileInputStream mInputStream;
    private FileOutputStream mOutputStream;
    private final BroadcastReceiver mPermissionReceiver;
    private final Runnable mAccessoryReader = () -> {
        byte[] buf = new byte[1024];

        while (true) {
            try {
                int read = AccessoryEngine.this.mInputStream.read(buf);
                AccessoryEngine.this.mCallback.onDataReceived(buf, read);
            } catch (Exception exc) {
                Log.d(AccessoryEngine.this.componentName, "run:" + exc.getMessage());
                Log.d(AccessoryEngine.this.componentName, "run: exiting reader thread");
                break;
            }
        }

        if (AccessoryEngine.this.mParcelFileDescriptor != null) {
            try {
                AccessoryEngine.this.mParcelFileDescriptor.close();
            } catch (IOException exc) {
                Log.d(AccessoryEngine.this.componentName, "run: Unable to close ParcelFD");
            }
        }

        if (AccessoryEngine.this.mInputStream != null) {
            try {
                AccessoryEngine.this.mInputStream.close();
            } catch (IOException exc) {
                Log.d(AccessoryEngine.this.componentName, "run: Unable to close InputStream");
            }
        }

        if (AccessoryEngine.this.mOutputStream != null) {
            try {
                AccessoryEngine.this.mOutputStream.close();
            } catch (IOException var3) {
                Log.d(AccessoryEngine.this.componentName, "run: Unable to close OutputStream");
            }
        }

        AccessoryEngine.this.mAccessoryConnected = false;
    };
    private final Context mContext;
    private final IUsbEngineCallback mCallback;

    public AccessoryEngine(@NotNull Context mContext, @NotNull IUsbEngineCallback mCallback) {
        super();
        this.mContext = mContext;
        this.mCallback = mCallback;
        this.mUsbManager = (UsbManager) this.mContext.getSystemService(Context.USB_SERVICE);

        BroadcastReceiver mDetachedReceiver = new BroadcastReceiver() {
            public void onReceive(@NotNull Context context, @NotNull Intent intent) {
                if (Objects.requireNonNull(intent.getAction()).equals(UsbManager.ACTION_USB_ACCESSORY_DETACHED)) {
                    AccessoryEngine.this.mCallback.onDeviceDisconnected();
                }
            }
        };
        this.mContext.registerReceiver(mDetachedReceiver, new IntentFilter("android.hardware.usb.action.USB_ACCESSORY_DETACHED"));

        this.mPermissionReceiver = new BroadcastReceiver() {
            public void onReceive(@NotNull Context context, @NotNull Intent intent) {
                AccessoryEngine.this.mContext.unregisterReceiver((BroadcastReceiver) this);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    Log.d(AccessoryEngine.this.componentName, "USB Permission granted! Let's try to connect.");
                    AccessoryEngine.this.connectAccessory();
                } else {
                    Log.d(AccessoryEngine.this.componentName, "Permission denied!");
                    Toast.makeText(AccessoryEngine.this.mContext, (CharSequence) "Permission denied! Please give permission!", Toast.LENGTH_SHORT).show();
                }

            }
        };
    }

    public final void onIntent(@Nullable Intent intent) {
        this.connectAccessory();
    }

    public final void write(@NotNull byte[] data) {
        if (!this.mAccessoryConnected || this.mOutputStream == null) {
            Log.d(this.componentName, "Unable to write: not connected.");
            Toast.makeText(this.mContext, (CharSequence) "Not connected!", Toast.LENGTH_SHORT).show();
            return;
        } else {
            try {
                this.mOutputStream.write(data);
                Log.d(this.componentName, "write: Data send: " + Arrays.toString(data));
            } catch (IOException exc) {
                Log.d(this.componentName, "write: could not send data");
            }

        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private final void connectAccessory() {
        if (this.mAccessoryConnected && this.mOutputStream != null && this.mInputStream != null) {
            Log.d(this.componentName, "Already connected!");
            return;
        }

        Log.d(this.componentName, "Discovering accessories...");
        UsbAccessory[] accessoryList = this.mUsbManager.getAccessoryList();
        if (accessoryList == null || accessoryList.length == 0) {
            Log.d(this.componentName, "No accessories found.");
            return;
        }

        UsbAccessory mAccessory = accessoryList[0];
        if (!this.mUsbManager.hasPermission(mAccessory)) {
            Log.d(this.componentName, "Permission missing, requesting...");
            this.mContext.registerReceiver(this.mPermissionReceiver, new IntentFilter("com.example.usbcommunicator.USB_PERMISSION"));
            PendingIntent pi = PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.example.usbcommunicator.USB_PERMISSION"), 0);
            this.mUsbManager.requestPermission(mAccessory, pi);
            return;
        }

        Log.d(this.componentName, "Permission available, connecting...");
        this.mParcelFileDescriptor = this.mUsbManager.openAccessory(mAccessory);
        if (this.mParcelFileDescriptor == null) {
            Log.e(this.componentName, "Unable to open accessory!");
            return;
        }

        ParcelFileDescriptor pd = this.mParcelFileDescriptor;
        FileDescriptor mFileDescriptor = pd.getFileDescriptor();
        this.mInputStream = new FileInputStream(mFileDescriptor);
        this.mOutputStream = new FileOutputStream(mFileDescriptor);
        this.mAccessoryConnected = true;
        this.mCallback.onConnectionEstablished();
        Thread sAccessoryThread = new Thread(this.mAccessoryReader, "Reader Thread");
        sAccessoryThread.start();
        Log.d(this.componentName, "Connection established.");
    }
}