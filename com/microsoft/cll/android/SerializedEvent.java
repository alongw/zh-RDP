package com.microsoft.cll.android;

import com.microsoft.cll.android.EventEnums.Latency;
import com.microsoft.cll.android.EventEnums.Persistence;

/**
 * This class provides a layer that abstracts out which kind of Envelope we are using by only holding references
 * to the important pieces and serializing the data immediately.
 */
public class SerializedEvent
{
    private String serializedData;
    private Latency latency;
    private Persistence persistence;
    private double sampleRate;
    private String deviceId;

    public String getSerializedData() {
        return serializedData;
    }

    public void setSerializedData(String serializedData) {
        this.serializedData = serializedData;
    }

    public Latency getLatency() {
        return latency;
    }

    public void setLatency(Latency latency) {
        this.latency = latency;
    }

    public Persistence getPersistence() {
        return persistence;
    }

    public void setPersistence(Persistence persistence) {
        this.persistence = persistence;
    }

    public double getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(double sampleRate) {
        this.sampleRate = sampleRate;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
