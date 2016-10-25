package com.mygdx.game.android.Adapters;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class Haptics extends Service {
    public Haptics() {
    }

    //Called when service is first created
    @Override
    public void onCreate(){
    }


    //Used for starting the service
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        return super.onStartCommand(intent,flags,startId);
    }

    //Used for binding the service
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    //Call stopSelf() or stopService() to kill the service
    //Clean up any threads, registered listeners, receivers, etc.
    @Override
    public void onDestroy(){

    }
}
