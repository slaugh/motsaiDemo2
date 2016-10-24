package com.mygdx.game.android.ControlPanel;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.mobileconnectors.cognito.DefaultSyncCallback;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.badlogic.gdx.backends.android.AndroidFragmentApplication;
import com.mygdx.game.Invaders;
import com.mygdx.game.android.NeblinaClasses.AndroidGetQ;
import com.mygdx.game.android.NeblinaClasses.Quaternions;

import java.util.LinkedList;
import java.util.List;

public class AndroidLauncher extends FragmentActivity implements AndroidFragmentApplication.Callbacks {
    public static AndroidGetQ invaderInterface = new AndroidGetQ(1);
    public static AndroidGetQ invaderInterface2 = new AndroidGetQ(2);
    public static AndroidGetQ[] invaderInterfaces = new AndroidGetQ[8];
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
        Log.w("PROGRAM FLOW", "IN AndroidLauncher onCreate!");
        GameFragment fragment = new GameFragment();
        FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
        trans.replace(android.R.id.content, fragment);
        trans.commit();
    }

    public static class GameFragment extends AndroidFragmentApplication {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            Log.w("PROGRAM FLOW", "IN GAME FRAGMENT onCreateView()!");
            return initializeForView(new Invaders(invaderInterfaces));
        }
    }

    @Override
    public void exit() {
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

