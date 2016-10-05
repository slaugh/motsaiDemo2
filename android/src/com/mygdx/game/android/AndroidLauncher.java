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
    Thread simulateQuaternionGeneration = new Thread(new UpdateThread());
    public String identityID = "";
    final static int buffer_iterations = 5;
    final static int generate_every_millis = 10;
    final static int max_buffer_size = 500;
    List<Quaternions> upload_buffer = new LinkedList<Quaternions>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        //TODO: Play around with AWS drivers here
//        simulateQuaternionGeneration.start();
        
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

            //Here is where we flush the buffer

            if(upload_buffer.size() >= max_buffer_size) {
                List<Quaternions> to_send = upload_buffer.subList(0, max_buffer_size);

            //Send data to dynamoDB and collect a list of the failures
            final List<DynamoDBMapper.FailedBatch> sent_buffer;
            sent_buffer = mapper.batchSave(to_send); //This seems like it might block the thread for future outputs

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.w("AWS PACKET DROP", "There were " + sent_buffer.size() + " lost Quaternions");
                    }
                });

                //flush the elements of the upload_buffer that were sent
                for(int i = max_buffer_size ; i >= 0; i--){
                    upload_buffer.remove(i);
                }
            }
            return null;
        }
    }

    public class UpdateThread implements Runnable {

        @Override
        public void run() {
            while(true)
            {
                try {
                                        //          3. Fill a buffer with the maximum amount of data that can be transmitted in an AWS packet
                    for(int i = 0; i< buffer_iterations; i++){
                        Long tsLong = System.currentTimeMillis()/1000;
                        String ts = tsLong.toString();
                        double double_i = (double) i;
                        Quaternions q_i = new Quaternions("Timestamp:" + ts + "#" + i , double_i, double_i, double_i, double_i);
                        upload_buffer.add(q_i);
                    }

                    new runAWS().execute("GOGOGO!");

                    simulateQuaternionGeneration.sleep(generate_every_millis);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}

