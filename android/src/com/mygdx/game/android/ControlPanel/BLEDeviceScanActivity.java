package com.mygdx.game.android.ControlPanel;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.mobileconnectors.cognito.DefaultSyncCallback;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.badlogic.gdx.backends.android.AndroidFragmentApplication;
import com.mygdx.game.Invaders;
import com.mygdx.game.android.NeblinaClasses.AndroidGetQ;
import com.mygdx.game.android.Adapters.CustomListAdapter;
import com.mygdx.game.android.NeblinaClasses.NebDeviceDetailFragment;
import com.mygdx.game.android.NeblinaClasses.Neblina;
import com.mygdx.game.android.NeblinaClasses.Quaternions;
import com.mygdx.game.android.R;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.ButterKnife;

public class BLEDeviceScanActivity extends FragmentActivity implements AndroidFragmentApplication.Callbacks{

    //List Variables
    private static final int REQUEST_ENABLE_BT = 0;
    private List<String> mDeviceNameList;
    private CustomListAdapter mLeDeviceListAdapter;
    private static final long SCAN_PERIOD = 60000;
    private static Map<String,Neblina> mDeviceList = new HashMap<String,Neblina>();
    private static Neblina activeDevice;
    private static NebDeviceDetailFragment activeDeviceDelegate;
    private static boolean mBluetoothGatt;
    public static boolean debug_mode = true;
    public String identityID = "";

    //Launch Visualization in a fragment
    public static AndroidGetQ invaderInterface = new AndroidGetQ();

    //GATT CALLBACK VARIABLES
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    public final static String ACTION_GATT_CONNECTED = "com.inspirationindustry.motsaibluetooth.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.inspirationindustry.motsaibluetooth.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.inspirationindustry.motsaibluetooth.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.inspirationindustry.motsaibluetooth.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.inspirationindustry.motsaibluetooth.EXTRA_DATA";
    private final static String TAG = BLEDeviceScanActivity.class.getSimpleName();
    private int mConnectionState = STATE_DISCONNECTED;
    public static final String ACTION_DATA_WRITE = "android.ble.common.ACTION_DATA_WRITE";
    public static final MediaType MEDIA_TYPE_MARKDOWN
            = MediaType.parse("text/x-markdown; charset=utf-8");
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    //Button state variables
    public static boolean is_BLE_BUTTON_on           = false;
    public static boolean is_UART_BUTTON_on          = false;
    public static boolean is_QUATERNION_BUTTON_on    = false;
    public static boolean is_MAG_BUTTON_on           = false;
    public static boolean is_LOCK_BUTTON_on          = false;
    public static boolean is_ERASE_BUTTON_on         = false;
    public static boolean is_RECORD_BUTTON_on        = false;
    public static boolean is_PLAYBACK_BUTTON_on      = false;
    public static boolean is_LED0_BUTTON_on          = false;
    public static boolean is_LED1_BUTTON_on          = false;
    public static boolean is_EEPROM_BUTTON_on        = false;
    public static boolean is_CHARGE_INPUT_on         = false;
    int eepromCounter = 0;
    public static int playbackNumber = 0;

    //Code Variables
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 8;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Main initialization code
        checkBluetoothPermissions();
        setContentView(R.layout.ble_scan_activity);
        ButterKnife.inject(this);
        activateBLE();
        setupFragmentAdapters();
        scanLeDevice(true);
    }

    private void checkBluetoothPermissions(){

        //        //Check to see if Bluetooth Adapters are enabled and available
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //Case 1: No Bluetooth on this device
        if(mBluetoothAdapter==null)
        {
            Context context = getApplicationContext();
            CharSequence text = "Bluetooth is not available, use a device that has bluetooth";
            int duration = Toast.LENGTH_LONG;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        } else{
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

            } else {
                //Case 3: Bluetooth is enabled so start the program
            }
        }

        //For newer versions you need to get permission from the user to access location information
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app neds location access");
                builder.setMessage("Please gran location access");
                builder.setPositiveButton(android.R.string.ok,null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener(){

                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
            return;
        }
    }


    private void setupFragmentAdapters() {

        //Setup the BLE Devices list fragment and its adapter
        //When a BLE item is clicked, it goes to onListItemClick()
        ListView yourListView = (ListView) findViewById(android.R.id.list);
        mDeviceNameList = new ArrayList<String>(); //Data Source
        mLeDeviceListAdapter = new CustomListAdapter(this, getApplicationContext(),mDeviceNameList);
        yourListView.setAdapter(mLeDeviceListAdapter);

        //Setup the automatically generated buttons fragment
//            activeDeviceDelegate = (NebDeviceDetailFragment) getFragmentManager().findFragmentById(R.id.button_list_fragment); //This is returning null
        activeDeviceDelegate = (NebDeviceDetailFragment) getFragmentManager().findFragmentById(R.id.button_list_fragment);

        //TODO: Add an adapter here somewhere???
    }

    public void activateBLE() {
        //This should pass
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish(); //optional kill switch
        }

        //Get the Bluetooth Adapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();


        //Enable Bluetooth if required
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            //stops scanning after a pre-defined period
            mHandler = new Handler();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(debug_mode ==true) {
                        Log.w("BLUETOOTH DEBUG", "ending the scan!");
                    }
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);
            if(debug_mode ==true) {
                Log.w("BLUETOOTH DEBUG", "starting the scan!");
            }
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            if(debug_mode ==true) {
                Log.w("BLUETOOTH DEBUG", "ending the scan!");
            }
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }


    @Override
    public void onRestart(){
        super.onRestart();
        if(debug_mode ==true) {
            Log.w("BLUETOOTH_DEBUG", "onRestart!");
        }
        if(mConnectionState==STATE_CONNECTED) {
        }
    }

    //Callback function for when a user chooses a BLE device
    public void onListItemClick(String deviceKey) {

        //Get the NEBLINA device and setup the NEBLINA interface
        activeDevice = mDeviceList.get(deviceKey);
        mBluetoothGatt = activeDevice.Connect(getBaseContext());

        Bundle arguments = new Bundle();
        arguments.putParcelable(NebDeviceDetailFragment.ARG_ITEM_ID, activeDevice);
        activeDeviceDelegate.SetItem(activeDevice);
//        activeDeviceDelegate.setArguments(arguments);
//        activeDevice.Connect(getBaseContext());

//        this.getFragmentManager().beginTransaction()
//                    .add(activeDeviceDelegate, "Fun")
//                    .commit();

        //Tell the user he's connected
        Toast.makeText(this, "Connecting to " + deviceKey, Toast.LENGTH_LONG).show();
    }

    //Callback for when a BLE device is found
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback(){
                @Override
            public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            //Get the ID of the discovered device
                            int i = 0;
                            long deviceID = 0;
                            while (i < scanRecord.length && scanRecord[i] > 0) {
                                if (scanRecord[i] > 0) {
                                    if (scanRecord[i + 1] == -1) {
                                        ByteBuffer x = ByteBuffer.wrap(scanRecord, i + 4, 8);
                                        x.order(ByteOrder.LITTLE_ENDIAN);
                                        deviceID = x.getLong();
                                        break;
                                    }
                                    i += scanRecord[i] + 1;
                                }
                            }

                            //Add the device to the list if it isn't there already
                            if(device.getName() != null){
                                Neblina neblina = new Neblina(deviceID,device);
                                if (mDeviceList.containsKey(neblina.toString()) == false) {
                                    mLeDeviceListAdapter.add(neblina.toString());
                                    mLeDeviceListAdapter.notifyDataSetChanged();
                                    mDeviceList.put(neblina.toString(), neblina);
                                }
                            }
                        }
                    });
                }
            };


    //BROADCAST WITHOUT CHARACTERISTIC
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        if(debug_mode ==true) {
            Log.w("BLUETOOTH DEBUG", "You are broadcasting: " + action);
        }
        sendBroadcast(intent);
    }

    //BROADCAST WITH CHARACTERISTIC
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
            Log.w("BLUETOOTH DEBUG", "You are in LONG form of onBroadcastUpdate");

            sendBroadcast(intent);
        }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();
            if (BLEDeviceScanActivity.ACTION_GATT_CONNECTED.equals(action)) {
                mConnectionState = STATE_CONNECTED;
                if(debug_mode ==true) {
                    Log.w("BLUETOOTH DEBUG", "The intent action is ACTION_GATT_CONNECTED");
                }
                invalidateOptionsMenu();
            } else if (BLEDeviceScanActivity.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnectionState = STATE_DISCONNECTED;
                if(debug_mode ==true) {
                    Log.w("BLUETOOTH DEBUG", "The intent action is ACTION_GATT_DISCONNECTED");
                }
                invalidateOptionsMenu();
            } else if (BLEDeviceScanActivity.
                    ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                if(debug_mode ==true) {
                    Log.w("BLUETOOTH DEBUG", "The intent action is ACTION_GATT_SERVICES_DISCOVERED");
                }
            } else if (BLEDeviceScanActivity.ACTION_DATA_AVAILABLE.equals(action)) {
                if(debug_mode ==true) {
                    Log.w("BLUETOOTH DEBUG", "The intent action is ACTION_DATA_AVAILABLE");
                }
            }
        }
    };

    public static boolean isInteger(String s, int radix) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i),radix) < 0) return false;
        }
        return true;
    }


    //TODO: Add a stream to cloud button -> maybe integrate with an

    //************************************ HTTP NETWORKING CODE *****************************************************//
    private void sendQuaternionsToCloudRESTfully(String q0_string, String q1_string, String q2_string, String q3_string) {
        //Example GET URL - This worked!
//        String databaseURL = "https://api.thingspeak.com/update?api_key=E3VK2KDK3IBGK8HT&field1=1";

        //Example POST URL - This worked!
//        String databaseURL ="https://api.thingspeak.com/update.json";

        //Example AWS IoT URL - Returns "Missing Authentication Token" error
//        String databaseURL = "https://A13X9WUMZAX5RM.iot.us-east-1.amazonaws.com/things/Neblina_Test1/shadow";

        String databaseURL = "https://j4pguaz22a.execute-api.us-east-1.amazonaws.com/prod/dynamodump";

        //Send Quaternions
//        String apiKey = "b7721b89f28c6045846cfbc72c2c545c";
//        String databaseURL = "https://api.forecast.io/forecast/" + apiKey +
//                "/" + q0_string + "," + q1_string + "," + q2_string + "," + q3_string;

        if (isNetworkAvailable()) {
            OkHttpClient client = new OkHttpClient();

            String postBody = "{" +
                    " \"timestamp\":\"00000069\"," +
                    " \"q1\":\"1\"," +
                    " \"q2\":\"2\"," +
                    " \"q3\":\"3\"," +
                    " \"q4\":\"4\"" +
                    "}";

            Log.w("HTTP_DEBUG", "sending: " + postBody);

            Request request = new Request.Builder()
                    .url(databaseURL)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(JSON, postBody))
                    .build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.w("HTTP_DEBUG", "onFailure's runOnUiThread was called");
                        }
                    });
                    Log.w("HTTP_DEBUG", "onFailure was called");
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        }
                    });
                    try {
                        String jsonData = response.body().string();
                        Log.w("HTTP_DEBUG", jsonData);
//                        Log.i(TAG, response.body().string()); //This was the offending clause
                        if (response.isSuccessful()) {
                            int i = parseJSONResponse(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.w("HTTP_DEBUG", "Makes it to the second run()");
                                }
                            });

                        } else {
                           Log.w("HTTP_DEBUG", "HMMMMM Something bad happened here :(");
                        }

                    } catch (IOException e) {
                        Log.e(TAG, "IOException caught: ", e);
                    } catch (JSONException e) {
                        Log.e(TAG, "JSONException caught: ", e);
                    }
                }
            });
        }
        else {
            Toast.makeText(this, "Network Is Unavailable!!!", Toast.LENGTH_SHORT).show();
        }
    }


    private boolean isNetworkAvailable() {

        ConnectivityManager manager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if(networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;

        }
        else {
            Toast.makeText(this,getString(R.string.network_unavailable_message),Toast.LENGTH_LONG).show();
        }
        return isAvailable;
    }

    //Not being used at the moment
    private int parseJSONResponse(String jsonData)throws JSONException{

//        Example JSON Parsing Code
//        JSONObject response = new JSONObject(jsonData);
//        String timezone = response.getString("timezone");
//        JSONObject daily = response.getJSONObject("daily");
//        JSONArray data = daily.getJSONArray("data");
//
//        String[] days = new String[data.length()];
//
//        for (int i = 0; i < data.length(); i++){
//            JSONObject jsonDay = data.getJSONObject(i);
//            String value = new String();
//
//            value = jsonDay.getString("summary");
//            value = jsonDay.getString("icon");
//            value = jsonDay.getDouble("temperatureMax");
//            value = (jsonDay.getLong("time");
//            value = (timezone);
//
//            days[i] = value;
        return 0;
        }

    public static class GameFragment extends AndroidFragmentApplication
    {
        // 5. Add the initializeForView() code in the Fragment's onCreateView method.
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            Log.w("PROGRAM FLOW", "IN GAME FRAGMENT onCreateView()!");
            return initializeForView(new Invaders(invaderInterface));   }
    }

    @Override
    public void exit() {}

    public class getAWSID extends AsyncTask<String, Void, Void> {

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

            Quaternions quaternions = new Quaternions();
            quaternions.setQ1(NebDeviceDetailFragment.latest_Q0);
            quaternions.setQ2(NebDeviceDetailFragment.latest_Q1);
            quaternions.setQ3(NebDeviceDetailFragment.latest_Q2);
            quaternions.setQ4(NebDeviceDetailFragment.latest_Q3);
            quaternions.setTimestamp(Long.toString(NebDeviceDetailFragment.timestamp_N));

            mapper.save(quaternions);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.w("LogTag", "so basically the problem is here");
                }
            });

            //READ an object from DynamoDB
//            final Book selectedBook = mapper.load(Book.class, 001);
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Log.w("LogTag", "Book Value: " + selectedBook.getAuthor());
//                }
//            });
            return null;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    }




