package com.example.mywatchfinal;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class MySensor implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor sensor;
    private Context context;
    private OnSensorChangeListener listener;

    public MySensor(Context context, OnSensorChangeListener listener) {
        this.context = context;
        this.listener = listener;
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    public void start() {
        if (sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null) {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            listener.onSensorStarted();
        } else {
            listener.onSensorNotAvailable();
        }
    }

    public void stop() {
        sensorManager.unregisterListener(this);
        listener.onSensorStopped();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();
        if (sensorType == Sensor.TYPE_HEART_RATE) {
            float[] sensorValues = event.values;
            if (sensorValues != null && sensorValues.length > 0) {
                int heartRate = (int) sensorValues[0];
                listener.onSensorChanged(heartRate);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    public interface OnSensorChangeListener {
        void onSensorStarted();

        void onSensorStopped();

        void onSensorNotAvailable();

        void onSensorChanged(int heartRate);
    }
}
