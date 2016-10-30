package com.mygdx.game.android.notifactions;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

import com.mygdx.game.android.NeblinaClasses.Neblina;

import java.util.Timer;
import java.util.TimerTask;

public class HapticService extends Service implements SensorEventListener {

    boolean awayFromBaby;
    boolean babyMoving;
    public static Neblina mNeblina;

    private long RETRY_TIME = 1000;
    private long START_TIME = 1000;

    private long lastUpdate;
    private SensorManager sensorManager;

    public HapticService() {
    }

    @Override
    public void onCreate() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lastUpdate = System.currentTimeMillis();

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
//        Message msg = mServiceHandler.obtainMessage();
//        msg.arg1 = startId;
//        mServiceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private void getAccelerometer(SensorEvent event) {
        float[] values = event.values;
        // Movement
        float x = values[0];
        float y = values[1];
        float z = values[2];

        float accelationSquareRoot = (x * x + y * y + z * z)
                / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
        long actualTime = event.timestamp;
        if (accelationSquareRoot >= 2) //
        {
            if (actualTime - lastUpdate < 200) {
                return;
            }
            lastUpdate = actualTime;
            Log.w("HAPTIC DEBUG", "Calling the haptics!");
            triggerAlarm();
        }
    }

    //TODO: Implement the callback -> When GET REQUEST returns motion status
    public void getMotionStatusCallback() {
        if( babyMoving ){
            triggerAlarm();
        }
        //TODO: Trigger a timed callback to probe for when the Alarm is acknowledged and a timer in case it is not acknowledged
    }

    public void triggerAlarm(){
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(500);

        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        r.play();
    }

    @Override
    public void onDestroy() {
        sensorManager.unregisterListener(this);
        Log.w("HAPTIC SERVICE","HAPTIC SERVICE ENDING :( BYE FOR NOW!");
    }
}