package com.mygdx.game.android.Adapters;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class Haptics extends Service {
    public Haptics() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
