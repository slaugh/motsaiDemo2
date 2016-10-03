package com.mygdx.game.android;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.mobileconnectors.cognito.DefaultSyncCallback;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.mygdx.game.Invaders;

import java.util.LinkedList;
import java.util.List;

public class AndroidLauncher extends AndroidApplication {
	AndroidGetQ invaderInterface = new AndroidGetQ();
    private final static int REQUEST_ENABLE_BT = 1;

    //AWS Experiment variables
    public String identityID = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        //TODO: Play around with AWS drivers here

        //Problem: Send a constant stream of quaternions up to the cloud for machine learning processing

        //Solution: 1. Temporary -> Generate data every couple milisecconds until we have accesss to the hardware

        //          2. Collect quaternion data into a buffer
        //          2.A. THERE IS A THING CALLED "BatchWriteItem" WOOHOO! THE DAY IS SAVED!!! Also BatchSave seems to work well
        //          2.B. The idea would be to replace the auto-generated Quaternions with a Queue that flushes over time
        //          2.C. Ideally the code checks to see if the data went through before deleting it


            new runAWS().execute("GOGOGO!"); //Tests showed that about 960/1000 Quaternions showed up in Dynamo

        //Questions: 1. What type of JAVA buffer to use? How do we queue and dequeue?
        //              1.a. We could a simple Java Queue<float> or more likely Queue<Quaternions>
        //              1.a.i We could use the add(), size() and poll() -> note: poll() returns null when empty (check for empty case)
        //           2. What is the maximum size of the packets going to AWS... let's test this
        //              2.a looks like 400 Bytes
        //              2.b If we know the header size and the size per Quaternions set then we can estimate the buffer size.
        //              2.c Since we are sending JSON we have 1byte per character... how many characters are we sending?
        //              2.d
        //           3. Could the data arrive out of order (i.e. Q1 and Q2 upadated without Q3, Q5 and timstamp, etc.), how to manage this?


		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		Invaders invaders = new Invaders(invaderInterface);
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

    public class runAWS extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                    getApplicationContext(),
                    "us-east-1:6e702b0c-80ab-4461-9ec3-239f1d163cd5", // Identity Pool ID
                    Regions.US_EAST_1 // Region
            );

            identityID = credentialsProvider.getIdentityId();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.w("LogTag", "my ID is " + identityID);
                }
            });

            // Initialize the Cognito Sync client
            CognitoSyncManager syncClient = new CognitoSyncManager(
                    getApplicationContext(),
                    Regions.US_EAST_1, // Region
                    credentialsProvider);

// Create a record in a dataset and synchronize with the server
            com.amazonaws.mobileconnectors.cognito.Dataset dataset = syncClient.openOrCreateDataset("myDataset");
            dataset.put("myKey", "myValue");
            dataset.synchronize(new DefaultSyncCallback() {
                @Override
                public void onSuccess(com.amazonaws.mobileconnectors.cognito.Dataset dataset, List newRecords) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.w("LogTag", "Creating a Record was successful!" + identityID);
                        }
                    });
                }
            });

            AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(credentialsProvider);
            DynamoDBMapper mapper = new DynamoDBMapper(ddbClient);

        int buffer_iterations = 1000;
        List<Quaternions> upload_buffer = new LinkedList<Quaternions>();

        //          3. Fill a buffer with the maximum amount of data that can be transmitted in an AWS packet
        for(int i = 0; i< buffer_iterations; i++){
            double double_i = (double) i;
            Quaternions q_i = new Quaternions("Refined 1k #" + i , double_i, double_i, double_i, double_i);
            upload_buffer.add(q_i);
        }

            mapper.batchSave(upload_buffer);

            //When the upload is confirmed then you can flush the buffer

            return null;
        }
    }


}