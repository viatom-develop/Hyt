package com.viatom.lpble.ble;

import android.util.Log;


import java.util.Arrays;

public class DataController {
    public static int index = 0;

    public static int[] amp = {5, 10 ,20};
    public static int ampKey = 1;

    public static int maxIndex;
    public static float mm2px;

    public static BatteryInfo batteryInfo;

    public static int hr = 0;
    public static int hrGATT = 0;

    // for wave
    public static float[] dataSrc;
    // received from device
    public static float[] dataRec = new float[0];

    public static void feed(float[] fs) {
        if (fs == null || fs.length == 0) {
            fs = new float[5];
        }

        if (dataSrc == null) {
            dataSrc = new float[maxIndex];
        } else if(dataSrc.length == 0) {
            dataSrc = new float[maxIndex];
        }

        if(maxIndex == 0) {
            return;
        }

        for (int i = 0; i<fs.length; i++) {
            if(dataSrc.length != 0) {
                int tempIndex = (index + i) % dataSrc.length;
                dataSrc[tempIndex] = fs[i];
            }
        }

        if(dataSrc.length != 0) {
            index = (index + fs.length) % dataSrc.length;
        }
    }

    synchronized public static void receive(float[] fs) {
        if (fs == null || fs.length == 0) {
            return;
        }

        Log.d("dashboard", "DataController receive: " + fs.length);

        float[] temp = new float[dataRec.length + fs.length];
        System.arraycopy(dataRec, 0, temp, 0, dataRec.length);
        System.arraycopy(fs, 0, temp, dataRec.length, fs.length);

        dataRec = temp;

    }

    synchronized public static float[] draw(int n) {
        if (n == 0 || n > dataRec.length) {
            return null;
        }

//        Log.d("Dashboard", "draw: " + n);

//        float[] res = new float[n];
//        float[] temp = new float[dataRec.length - n];
//        System.arraycopy(dataRec, 0, res, 0, n);
//        System.arraycopy(dataRec, n, temp, 0, dataRec.length-n);

        float[] res = new float[n];
        float[] temp = new float[dataRec.length - n];
        System.arraycopy(dataRec, 0, res, 0, n);
        System.arraycopy(dataRec, n, temp, 0, dataRec.length-n);


//        float[] res = Arrays.copyOfRange(dataRec, 0, n-1);
//        float[] temp = Arrays.copyOfRange(dataRec, n, dataRec.length);

        dataRec = temp;

        return res;
    }

    synchronized public static void clear() {
//        float[] res = Arrays.copyOfRange(dataRec, 0, n-1);
        if(dataRec.length > 15) {
            float[] temp = Arrays.copyOfRange(dataRec, dataRec.length - 10, dataRec.length);
            dataRec = temp;
        }
    }
}
