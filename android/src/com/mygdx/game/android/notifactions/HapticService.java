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
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import static com.mygdx.game.android.NeblinaClasses.Neblina.JSON;

public class HapticService extends Service implements SensorEventListener {

    public static Neblina mNeblina;
    private SensorManager sensorManager;
    private long RETRY_TIME = 1000;
    private long START_TIME = 1000;

//    private String getNotificationUrl = "http://10.92.0.85:3000/api/notification";
    private static String getNotificationUrl = "http://requestb.in/19wezdi1";
//    private String postSnoozeUrl = "http://10.92.0.85:3000/api/snooze";
    private String postSnoozeUrl = "http://requestb.in/10nj3cu1";

    private static boolean alarmRinging = true;
    private static boolean alarmAcknowledged = true;

    public HapticService() {
        Log.w("HAPTIC SERVICE","HAPTIC SERVICE CONSTRUCTOR!");
    }

    @Override
    public void onCreate() {
        Log.w("HAPTIC SERVICE","HAPTIC SERVICE ONCREATE()!");
        Timer myTimer = new Timer();
        myTimer.scheduleAtFixedRate(new pollNotification(),START_TIME,RETRY_TIME);
    }

    public class pollNotification extends TimerTask {

        @Override
        public void run() {
            Log.w("HAPTIC SERVICE","HAPTIC SERVICE IS UP AND RUNNING!");
            if(alarmRinging == true){
                triggerAlarm();
            }

            getFromNotificationUrl();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

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

        if (accelationSquareRoot >= 2) //
        {
            Log.w("HAPTIC DEBUG", "Calling the haptics!");
            if(alarmRinging == true){
                alarmRinging = false;
                alarmAcknowledged = true;
                postToSnoozeUrl();
            }

        }
    }

    public void triggerAlarm(){
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(500);

        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        r.play();
    }

    public void getFromNotificationUrl(){
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(getNotificationUrl) //TODO: Phase this out because we are getting this through the notification
                    .build();
            Response response = client.newCall(request).execute();
            Log.w("BLUETOOTH_DEBUG", "RECEIVED THE NOTIFICATION STATUS: " + response.body().toString());
            //TODO: Once API for getMotionUrl is finished -> Set motionStatus based on the result to either 0 or 1
            if(response.body().toString()=="0"){
            }else if(response.body().toString()=="1"){
                triggerAlarm();
                alarmRinging = true;
            }else if(response.body().toString()=="2"){
            }else {
            }
        } catch (IOException e){
            Log.w("BLUETOOTH_DEBUG","DANGER! WILL ROBINSON!");
        }
    }

    public void postToSnoozeUrl(){
        try {
            OkHttpClient client2 = new OkHttpClient();
            String json = "";
            RequestBody body = RequestBody.create(JSON, json);
            Request request = new Request.Builder()
                    .url(postSnoozeUrl)
                    .post(body)
                    .build();
            Response response2 = client2.newCall(request).execute();
        }catch (IOException e){}
    }

    @Override
    public void onDestroy() {
        sensorManager.unregisterListener(this);
        Log.w("HAPTIC SERVICE","HAPTIC SERVICE ENDING :( BYE FOR NOW!");
    }
}