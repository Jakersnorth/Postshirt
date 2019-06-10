package com.adafruit.bluefruit.le.connect.utils;

import com.jjoe64.graphview.series.DataPoint;

/*
 * Utility class for structuring data from single accelerometer
 */

public class SignalPoint {
    private int timestamp;
    private int x;
    private int y;
    private int z;

    public SignalPoint(int timestamp_, int x_, int y_, int z_) {
        timestamp = timestamp_;
        x = x_;
        y = y_;
        z = z_;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public int getX() {
        return x;
    }

    public DataPoint getXDP() {
        return new DataPoint((double)timestamp, (double)x);
    }

    public int getY() {
        return y;
    }

    public DataPoint getYDP() {
        return new DataPoint((double)timestamp, (double)y);
    }

    public int getZ() {
        return z;
    }

    public DataPoint getZDP() {
        return new DataPoint((double)timestamp, (double)z);
    }

    public String toString() {
        return timestamp + "," + x + "," + y + "," + z;
    }
}
