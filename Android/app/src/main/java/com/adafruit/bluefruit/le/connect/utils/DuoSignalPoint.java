package com.adafruit.bluefruit.le.connect.utils;

import com.jjoe64.graphview.series.DataPoint;

import java.util.ArrayList;
import java.util.List;

/*
 * Utility class for structuring data from two accelerometers
 */
public class DuoSignalPoint {
    private int timestamp;
    private int x1;
    private int y1;
    private int z1;

    private int x2;
    private int y2;
    private int z2;

    public DuoSignalPoint(int timestamp_, int x_1, int y_1, int z_1, int x_2, int y_2, int z_2) {
        timestamp = timestamp_;
        x1 = x_1;
        y1 = y_1;
        z1 = z_1;

        x2 = x_2;
        y2 = y_2;
        z2 = z_2;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public List<DataPoint> getDataPoints() {
        List<DataPoint> dataPoints = new ArrayList<DataPoint>();
        dataPoints.add(new DataPoint((double) timestamp, (double) x1));
        dataPoints.add(new DataPoint((double) timestamp, (double) y1));
        dataPoints.add(new DataPoint((double) timestamp, (double) z1));
        dataPoints.add(new DataPoint((double) timestamp, (double) x2));
        dataPoints.add(new DataPoint((double) timestamp, (double) y2));
        dataPoints.add(new DataPoint((double) timestamp, (double) z2));
        return dataPoints;
    }

    public String toString() {
        return timestamp + "," + x1 + "," + y1 + "," + z1 + "," + x2 + "," + y2 + "," + z2;
    }

    public double[] getFeatures() {
        double[] features = new double[6];
        features[0] = x1;
        features[1] = y1;
        features[2] = z1;

        features[3] = x2;
        features[4] = y2;
        features[5] = z2;
        return features;
    }
}

