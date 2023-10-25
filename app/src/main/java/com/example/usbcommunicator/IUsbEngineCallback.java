package com.example.usbcommunicator;

import org.jetbrains.annotations.Nullable;

public interface IUsbEngineCallback {
    void onConnectionEstablished();

    void onDeviceDisconnected();

    void onDataReceived(@Nullable byte[] var1, int var2);
}
