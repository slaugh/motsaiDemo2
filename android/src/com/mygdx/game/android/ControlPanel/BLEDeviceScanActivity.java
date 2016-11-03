package com.mygdx.game.android.ControlPanel;

//Android Tools
import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
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
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

//AWS
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.mobileconnectors.cognito.DefaultSyncCallback;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

//LibGDX
import com.badlogic.gdx.backends.android.AndroidFragmentApplication;
import com.mygdx.game.Invaders;
import com.mygdx.game.android.NeblinaClasses.AndroidGetQ;
import com.mygdx.game.android.Adapters.CustomListAdapter;
import com.mygdx.game.android.NeblinaClasses.NebDeviceDetailFragment;
import com.mygdx.game.android.NeblinaClasses.Neblina;
import com.mygdx.game.android.NeblinaClasses.Quaternions;
import com.mygdx.game.android.R;
import com.mygdx.game.android.notifactions.HapticService;
import com.mygdx.game.simulation.Simulation;

//Java
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Butterknife
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/*This is the MAIN display class holding:
    1. The BLE Device Scan List
    2. A Device detail fragment
    3. A game fragment

  The process goes through the following steps:
    A. OnCreate() is called which initializes the UI and starts the BLE Scan
    B. Each scanned BLE device will be stored as a new Neblina object
    C. The BLE Scan will return a list of BLE Devices that the user can select
    D. When the user selects a BLE Device then onListItemClick is called. This connects to the
        device and activtes a NebDeviceDetailFragment that can be used to send commands to it.
 */
public class BLEDeviceScanActivity extends FragmentActivity implements AndroidFragmentApplication.Callbacks{

    //Butterknife get views
    @InjectView(R.id.refreshButton) Button refreshButton;
    @InjectView(R.id.cloudStreamToggle) Button toggleButton;

    private boolean mTwoPane;

    //List Variables
    private List<String> mDeviceNameList;
    private CustomListAdapter mLeDeviceListAdapter;
    private static Map<String,Neblina> mDeviceList = new HashMap<String,Neblina>();
    private static Neblina activeDevice;
    private static NebDeviceDetailFragment activeDeviceDelegate;
    private static final int REQUEST_ENABLE_BT = 0;
    private static final int MAX_BLE_DEVICES = Simulation.MAX_SHIPS;
    private static final long SCAN_PERIOD = 60000;

    //AWS cognito identity
    public String identityID = "";

    //Setup the Quaternion interface
    public static AndroidGetQ[] invaderInterfaces = new AndroidGetQ[MAX_BLE_DEVICES];
    public static int numberOfConnectedDevices = 0;

    //BLUETOOTH CONSTANTS
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private final static String TAG = BLEDeviceScanActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 8;
    public static final String ACTION_GATT_CONNECTED = "com.inspirationindustry.motsaibluetooth.ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED = "com.inspirationindustry.motsaibluetooth.ACTION_GATT_DISCONNECTED";
    public static final  String ACTION_GATT_SERVICES_DISCOVERED = "com.inspirationindustry.motsaibluetooth.ACTION_GATT_SERVICES_DISCOVERED";
    public static final String ACTION_DATA_AVAILABLE = "com.inspirationindustry.motsaibluetooth.ACTION_DATA_AVAILABLE";
    public static final String EXTRA_DATA = "com.inspirationindustry.motsaibluetooth.EXTRA_DATA";
    public static final String ACTION_DATA_WRITE = "android.ble.common.ACTION_DATA_WRITE";

    //INITIALIZATION VARIABLES
    public boolean firstDevice = true;

    //BLUETOOTH CODE VARIABLES
    private int mConnectionState = STATE_DISCONNECTED;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;

    //HACKATHON VARIABLES
    public static boolean enableHackathon = false;
    public static boolean isStreaming = false;


    /************************************MAIN INITIALIZATION CODE********************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Main initialization code
        initializeSpaceshipInterface();
        initializeBLE();

        //Setup UI
        setContentView(R.layout.ble_scan_activity);
        ButterKnife.inject(this);
        setupFragmentAdapters();

        //Start the BLE Scan
        scanLeDevice(true);

        //Hackathon Background Service for acknowledging messages
        if(enableHackathon==true) {
            Intent intent = new Intent(this, HapticService.class);
            this.startService(intent);
        }

        if (findViewById(R.id.nebdevice_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }
    }


    //This function sets up the lists needed for the Space Invaders Interface
    private void initializeSpaceshipInterface() {
        for(int i = 0; i < MAX_BLE_DEVICES; i++){
            invaderInterfaces[i] = new AndroidGetQ(i);
        }
    }


    //This function verifies that BLE is available, active, and that the app has necessary permissions
    public void initializeBLE() {

        //Check that this device supports BLE
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish(); //optional kill switch
        }

        //For newer versions you need to get permission from the user to access location information
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access to access bluetooth");
                builder.setMessage("Please grant location access");
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


    //This sets up the adapters that will bridge the UI to the code
    private void setupFragmentAdapters() {

        //1. NeblinaDeviceList: Setup the BLE Devices list fragment and its adapter
        ListView yourListView = (ListView) findViewById(android.R.id.list);
        mDeviceNameList = new ArrayList<String>(); //Data Source
        mLeDeviceListAdapter = new CustomListAdapter(this, getApplicationContext(),mDeviceNameList);
        yourListView.setAdapter(mLeDeviceListAdapter);

        //2. DetailList: Setup the automatically generated buttons fragment
        activeDeviceDelegate = (NebDeviceDetailFragment) getFragmentManager().findFragmentById(R.id.button_list_fragment);
    }


    //This triggers the BLE scan
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            //stops scanning after a pre-defined period
            mHandler = new Handler();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                        Log.w("BLUETOOTH DEBUG", "ending the scan!");
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);
                Log.w("BLUETOOTH DEBUG", "starting the scan!");
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

/************************************END OF MAIN INITIALIZATION CODE********************************/


/*********************************** CALLBACK FUNCTIONS *****************************************/

//Callback function for when a user chooses a BLE device
public void onListItemClick(String deviceKey) {

    //Get the NEBLINA device and setup the NEBLINA interface
    activeDevice = mDeviceList.get(deviceKey);
    activeDevice.Connect(getBaseContext());

    Bundle arguments = new Bundle();
    arguments.putParcelable(NebDeviceDetailFragment.ARG_ITEM_ID, activeDevice);
    activeDeviceDelegate.SetItem(activeDevice);

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


    //This restarts the BLE scan. Useful when the app has been idle for some time.
    @OnClick(R.id.refreshButton)void setRefreshButton(){
        scanLeDevice(true);
    }



    @OnClick(R.id.cloudStreamToggle)void streamToCloud(View view) {

        if(view.isActivated()){
            isStreaming = false;
            view.setActivated(false);
        }else {
            isStreaming = true;
            view.setActivated(true);
        }
    }

    //This starts the data visualization tools
    @OnClick(R.id.dataVisualisation) void dataVisualization(){
        Log.w("DEBUG", "Starting Visualization");
        Intent intent = new Intent(this, DynamicData.class);
        startActivity(intent);
    }

    //This is used when the user is granting permissions for BLE localization
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

    @Override
    public void onRestart(){
        super.onRestart();
    }


/************************************* HELPER FUNCTIONS *****************************************/

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

    @Override
    public void exit() {

    }

    public static class GameFragment extends AndroidFragmentApplication
    {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            return initializeForView(new Invaders(invaderInterfaces)); }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this,HapticService.class);
        this.stopService(intent);
    }

}

