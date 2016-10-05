package com.mygdx.game.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w("PROGRAM FLOW", "IN MainActivity!");
        Intent intent = new Intent(this, BLEDeviceScanActivity.class);
        startActivity(intent);
        finish(); //Close once we end the splash screen
    }

}
