package com.mygdx.game.android;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.mygdx.game.Invaders;


public class AndroidLauncher extends AndroidApplication {
	AndroidGetQ invaderInterface = new AndroidGetQ();
    private final static int REQUEST_ENABLE_BT = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        //TODO: Play around with AWS drivers here

        //Problem: Send a constant stream of quaternions up to the cloud for machine learning processing

        //Solution: 1. Temporary -> Generate data every couple milisecconds until we have accesss to the hardware
        //          2. Collect quaternion data into a buffer
        //          3. Fill a buffer with the maximum amount of data that can be transmitted in an AWS packet
        //          4. When the buffer is greater than or equal to the packet capacity then flush the buffer to AWS

        //Questions: 1. What type of JAVA buffer to use
        //           2. How do we queue and dequeue
        //           3. What is the maximum size of the packets going to AWS... let's test this


		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		Invaders invaders = new Invaders(invaderInterface); //Why can't I call an invaderInterface here...
		initialize(invaders, config);

        //Check to see if Bluetooth Adapters are enabled and available
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //Case 1: No Bluetooth on this device
        if (mBluetoothAdapter == null) {
            Context context = getApplicationContext();
            CharSequence text = "Bluetooth is not available, use a device that has bluetooth";
            int duration = Toast.LENGTH_LONG;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            // Device does not support Bluetooth
            this.finishAffinity();
        } else {
            //Case 2: Bluetooth exists but is not enabled
            if (!mBluetoothAdapter.isEnabled()) {
                // Bluetooth is not enable :)
                //Explain to the user that he needs to enable his bluetooth
                Context context = getApplicationContext();
                CharSequence text = "Bluetooth is not enabled, please activate your Bluetooth";
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();

                //Send the user to go enable bluetooth and come back when finished
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else  {
                //Case 3: Bluetooth is enabled so start the program
                Intent intent = new Intent(this, BLEDeviceScanActivity.class);
                startActivity(intent);
            }
        }
	}
}