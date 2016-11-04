package com.mygdx.game.android.NeblinaClasses;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.mygdx.game.android.ControlPanel.BLEDeviceScanActivity;
import com.mygdx.game.android.notifactions.HapticService;
import com.mygdx.game.simulation.Simulation;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


/* Each instance of this class corresponds to a BLE device found in BLEDeviceScanActivity
   1. This class implements all of the GattCallbacks including:

   A. onServicesDiscovered() called when a gatt object is received -> connects to the gatt services
   B. onConnectionStateChanged() called when CONNECTED / DISCONECTED
   C. onCharacteristicWrite() called when a response is received from a command
   D. onCharacteristicChange() called when new data is received -> parses the responses

   2. Implements functions for sending commands to Neblina devices


 */
public class Neblina extends BluetoothGattCallback implements Parcelable {
    //NEBLINA CUSTOM UUIDs
    public static final UUID NEB_SERVICE_UUID = UUID.fromString("0df9f021-1532-11e5-8960-0002a5d5c51b");
    public static final UUID NEB_DATACHAR_UUID = UUID.fromString("0df9f022-1532-11e5-8960-0002a5d5c51b");
    public static final UUID NEB_CTRLCHAR_UUID = UUID.fromString("0df9f023-1532-11e5-8960-0002a5d5c51b");

    // Packet types
    public static final byte NEB_CTRL_PKTYPE_DATA		= 0;		// Data/Response
    public static final byte NEB_CTRL_PKTYPE_ACK		= 1;		// Ack
    public static final byte NEB_CTRL_PKTYPE_CMD		= 2;		// Command
    public static final byte NEB_CTRL_PKTYPE_RESERVE1	= 3;
    public static final byte NEB_CTRL_PKTYPE_ERR		= 4;		// Error response
    public static final byte NEB_CTRL_PKTYPE_RESERVE2	= 5;		//
    public static final byte NEB_CTRL_PKTYPE_RQSTLOG	= 6;		// Request status/error log
    public static final byte NEB_CTRL_PKTYPE_RESERVE3	= 7;

    //**
    // SUBSYSTEM VALUES
    public static final byte NEB_CTRL_SUBSYS_DEBUG		= 0;		// Status & logging
    public static final byte NEB_CTRL_SUBSYS_MOTION_ENG	= 1;		// Motion Engine
    public static final byte NEB_CTRL_SUBSYS_POWERMGMT	= 2;		// Power management
    public static final byte NEB_CTRL_SUBSYS_GPIO		= 3;		// GPIO control
    public static final byte NEB_CTRL_SUBSYS_LED		= 4;		// LED control
    public static final byte NEB_CTRL_SUBSYS_ADC		= 5;		// ADC control
    public static final byte NEB_CTRL_SUBSYS_DAC		= 6;		// DAC control
    public static final byte NEB_CTRL_SUBSYS_I2C		= 7;		// I2C control
    public static final byte NEB_CTRL_SUBSYS_SPI		= 8;		// SPI control
    public static final byte NEB_CTRL_SUBSYS_STORAGE    = 0x0B;		//NOR flash memory recorder
    public static final byte NEB_CTRL_SUBSYS_EEPROM		= 0x0C;		//small EEPROM storage

    // Power management subsystem command code
    public static final byte POWERMGMT_CMD_GET_BAT_LEVEL		= 0;	// Get battery level
    public static final byte POWERMGMT_CMD_GET_TEMPERATURE		= 1;	// Get temperature
    public static final byte POWERMGMT_CMD_SET_CHARGE_CURRENT	= 2;	// Set battery charge current

    // **
    // Debug subsystem command code
    public static final byte DEBUG_CMD_PRINTF					        = 0;	// The infamous printf thing.
    public static final byte DEBUG_CMD_SET_INTERFACE					= 1;	// sets the protocol interface - this command is now obsolete
    public static final byte DEBUG_CMD_MOTENGINE_RECORDER_STATUS		= 2;	// asks for the streaming status of the motion engine, as well as the flash recorder state
    public static final byte DEBUG_CMD_MOTION_ENG_UNIT_TEST_START_STOP	= 3;	// starts/stops the motion engine unit-test mode
    public static final byte DEBUG_CMD_MOTION_ENG_UNIT_TEST_DATA		= 4;	// data being transferred between the host and Neblina for motion engine's unit testing
    public static final byte DEBUG_CMD_GET_FW_VERSION					= 5;
    public static final byte DEBUG_CMD_DUMP_DATA						= 6; 	// dump and forward the data to the host (for printing on the screen, etc.)
    public static final byte DEBUG_CMD_STREAM_RSSI						= 7;	// get the BLE signal strength in db
    public static final byte DEBUG_CMD_GET_DATAPORT						= 8;	// Get streaming data interface port state.
    public static final byte DEBUG_CMD_SET_DATAPORT						= 9;	// Enable/Disable streaming data interface port

    // Data port control
    public static final byte DATAPORT_MAX	= 2;	// Max number of data port
    public static final byte DATAPORT_BLE	= 0; 	// streaming data port BLE
    public static final byte DATAPORT_UART	= 1;	//
    public static final byte DATAPORT_OPEN	= 1;	// Open streaming data port
    public static final byte DATAPORT_CLOSE	= 0;	// Close streaming data port

    //Flash Recorder subsystem commands
    public static final byte STORAGE_CMD_ERASE              = 0x01;     //erases the whole NOR flash
    public static final byte STORAGE_CMD_RECORD             = 0x02;     //start or stop recording in a new session
    public static final byte STORAGE_CMD_PLAY               = 0x03;     //playing back a pre-recorded session: either start or stop
    public static final byte STORAGE_CMD_GET_NB_SESSION     = 0x04;     //a command to get the total number of sessions in the NOR flash recorder
    public static final byte STORAGE_CMD_GET_SESSION_INFO   = 0x05;     //get the session length of a session ID. The session IDs start from 0 to n-1, where n is the total number of sessions in the NOR flash
    public static final byte STORAGE_CMD_READ_SESSION       = 0x06;

    // EEPROM subsystem commands and other defines
    public static final byte EEPROM_CMD_READ        = 0x01;     //reads 8-byte chunks of data
    public static final byte EEPROM_CMD_WRITE       = 0x02;     //write 8-bytes of data to the EEPROM

    // LED Commands
    public static final byte LED_CMD_SET_VALUE      = 1;
    public static final byte LED_CMD_GET_VALUE      = 2;

    //**
    // Motion engine commands
    public static final byte MOTION_CMD_DOWN_SAMPLE         = 0x01;
    public static final byte MOTION_CMD_MOTION_STATE        = 0x02;
    public static final byte MOTION_CMD_IMU_DATA 		    = 0x03;
    public static final byte MOTION_CMD_QUATERNION          = 0x04;
    public static final byte MOTION_CMD_EULER_ANGLE         = 0x05;
    public static final byte MOTION_CMD_EXTFORCE            = 0x06;
    public static final byte MOTION_CMD_SET_FUSION_TYPE     = 0x07;
    public static final byte MOTION_CMD_TRAJECTORY_RECORD   = 0x08;
    public static final byte MOTION_CMD_TRAJECTORY_INFO		= 0x09;
    public static final byte MOTION_CMD_PEDOMETER           = 0x0A;
    public static final byte MOTION_CMD_MAG_DATA            = 0x0B;
    public static final byte MOTION_CMD_SIT_STAND			= 0x0C;
    public static final byte MOTION_CMD_LOCK_HEADING_REF    = 0x0D;
    public static final byte MOTION_CMD_SET_ACC_RANGE   	= 0x0E;
    public static final byte MOTION_CMD_DISABLE_ALL_STREAM  = 0x0F;
    public static final byte MOTION_CMD_RESET_TIMESTAMP     = 0x10;
    public static final byte MOTION_CMD_FINGER_GESTURE      = 0x11;
    public static final byte MOTION_CMD_ROTATION_INFO		= 0x12;
    public static final byte MOTION_CMD_EXTRN_HEADING_CORR  = 0x13;

    private int deviceNum;
    private int connectedDevNum;

    //Periodic RSSI poll variables
    private boolean shouldPollRSSI = true;
    private long RETRY_TIME = 1000;
    private long START_TIME = 1000;

    int initializeState = 0;

    BluetoothDevice Nebdev;
    long DevId;
    BluetoothGatt mBleGatt;
    NeblinaDelegate mDelegate;
    BluetoothGattCharacteristic mCtrlChar;

    // Delay Calculation Variables //
    public static int size_max = 10000;

    // Packet Interval Variables //
    public static long[] delayTimeArray = new long[size_max];
    public int file_size1 = 0;
    private long currentTime = System.currentTimeMillis();
    private long delayTime = 0;
    private long lastTime = System.currentTimeMillis();
    private boolean isTimeinitializing = true;

    //Round Trip Variables
    public static long[] roundTripTimeArray = new long[size_max];
    public int file_size2 = 0;
    private long sentTime = System.currentTimeMillis();
    private long returnTime = System.currentTimeMillis();
    private long writeDescriptorTimestamp;
    private long writeDelay;
    private long onDescWriteTime;

/************************************* Hackathon Variables ************************************/
    private int rssiLowerThreshold = -90;
    private int rssiUpperThreshold = -75;
    private int isPresent = 1;

    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    //Paul's URLs
    private String alarmState = "0";
    private String alarmEventUrl = "http://10.92.0.85:3000/api/alarm";
    private String getMotionUrl = "http://requestb.in/1h67p571";
//    private String putPresenceUrl = "http://10.92.0.85:3000/api/presence"; //Paul's version
    private String putPresenceUrl = "http://requestb.in/19wezdi1"; //Test version
    //Igor's URLs
    private String triggerAlarmUrl= "http://hsj_dev/api/V1/movement/patient.php"; //Final Version
    private  boolean initializingService = true;


    private int alarmStatus = 0; //0 is off, 1 is notification, 2 is master alarm



    /************************************* Constructor Function ****************************************/

    //Constructor
    public Neblina(long id, BluetoothDevice dev) {
        Nebdev = dev;
        DevId = id;
        mDelegate = null;
        mBleGatt = null;
        mCtrlChar = null;
    }


/********************************** Timing Tests and RSSI Functions ******************************/

    public class pollRSSI extends TimerTask {

        @Override
        public void run() {
            mBleGatt.readRemoteRssi();
        }
    }

    //Called when the results of readRemoteRssi() get back
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status){
        Log.w("BLUETOOTH_DEBUG","RSSI is:" +  rssi);


        //TODO: YOU CAN TEST COOPERATHON API CALLS HERE
        if(initializingService==true){
            initializingService = false;
            HapticService.mNeblina = this;
        }

        if(BLEDeviceScanActivity.enableHackathon == true) {
            putToPresenceUrl(rssi); //Main call to PUT presence
        }
    }

    public class JitterTest extends  TimerTask {
        @Override
        public void run() {
            getLed();
        }
    }

/****************************************HACKATHON API CALLS *************************************/

//    public void getFromMotionUrl(){
//        try {
//            OkHttpClient client = new OkHttpClient();
//            Request request = new Request.Builder()
//                    .url(getMotionUrl)
//                    .build();
//            Response response = client.newCall(request).execute();
//            Log.w("BLUETOOTH_DEBUG", "RECEIVED THE MOTION STATUS: " + response.body().toString());

//        } catch (IOException e){
//            Log.w("BLUETOOTH_DEBUG","DANGER! WILL ROBINSON!");
//        }
//    }



    public void putToPresenceUrl(int rssi){


        if (rssi >= rssiUpperThreshold) {
            isPresent = 1; //TODO: add filtering and/or hysterisis
        } else if (rssi >= rssiLowerThreshold){
            //is Present stays where it is
        } else isPresent = 0;

        if(BLEDeviceScanActivity.enableHackathon==true){
                try {

                    OkHttpClient client = new OkHttpClient();

                    String json = "{\"presence\":\"" + isPresent + "\"}";
                    RequestBody body = RequestBody.create(JSON, json);
                    Request request = new Request.Builder()
                            .url(putPresenceUrl)
                            .put(body)
                            .build();
                    Response response = client.newCall(request).execute();
                    Log.w("BLUETOOTH_DEBUG", "Put to presence url: " + response.body().toString());


        //            new java.util.Timer().schedule(
        //                    new java.util.TimerTask() {
        //                        @Override
        //                        public void run() {
        //                            getFromNotificationUrl();
        //                        }
        //                    },
        //                    10
        //            );

                    Log.w("BLUETOOTH_DEBUG", "PULLED THE TRIGGER: " + response.body().toString());
                } catch (IOException e){
                    Log.w("BLUETOOTH_DEBUG","DANGER! WILL ROBINSON2!"+e.toString());
                }
            }
    }

    public void putToMasterAlarmUrl(){
        try {
            OkHttpClient client = new OkHttpClient();
            String json = "{\"mac_addr\":\"MAC101010101010\", \"movement_alert\":\"1\"}";
            RequestBody body = RequestBody.create(JSON, json);
            Request request = new Request.Builder()
                    .url(triggerAlarmUrl)
                    .put(body)
                    .build();
            Response response = client.newCall(request).execute();
            Log.w("BLUETOOTH_DEBUG", "PULLED THE TRIGGER: " + response.body().toString());


            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            putToAlarmUrl();
                        }
                    },
                    5000
            );

            Log.w("BLUETOOTH_DEBUG", "PULLED THE TRIGGER: " + response.body().toString());
        } catch (IOException e){
            Log.w("BLUETOOTH_DEBUG","DANGER! WILL ROBINSON3!"+e.toString());
        }
    }

    public void putToSnoozeUrl(){
        try {
            OkHttpClient client = new OkHttpClient();
            String json = "{\"mac_addr\":\"MAC101010101010\", \"movement_alert\":\"1\"}";
            RequestBody body = RequestBody.create(JSON, json);
            Request request = new Request.Builder()
                    .url(triggerAlarmUrl)
                    .put(body)
                    .build();
            Response response = client.newCall(request).execute();
            Log.w("BLUETOOTH_DEBUG", "PULLED THE TRIGGER: " + response.body().toString());


            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            putToAlarmUrl();
                        }
                    },
                    5000
            );

            Log.w("BLUETOOTH_DEBUG", "PULLED THE TRIGGER: " + response.body().toString());
        } catch (IOException e){
            Log.w("BLUETOOTH_DEBUG","DANGER! WILL ROBINSON4!"+e.toString());
        }
    }



    public void putToAlarmUrl(){
        if(BLEDeviceScanActivity.enableHackathon==true){
            try {
                OkHttpClient client2 = new OkHttpClient();
                String json2 = "{\"Alarm\":\"" + alarmState + "\"}";
                RequestBody body2 = RequestBody.create(JSON, json2);
                Request request2 = new Request.Builder()
                        .url(alarmEventUrl)
                        .put(body2)
                        .build();
                Response response2 = client2.newCall(request2).execute();
            }catch (IOException e){}
        }
    }
/******************************** GATT CALLBACKS *************************************************/

    //Called when Gatt changed to CONNECTED or DISCONNECTED. This adds a Spaceship to the visualization
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        String intentAction;
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            connectedDevNum = BLEDeviceScanActivity.numberOfConnectedDevices;
            Simulation.ships[connectedDevNum].isActive = true;
            BLEDeviceScanActivity.numberOfConnectedDevices++;
            Log.w("BLUETOOTH DEBUG", "CONNECTION ESTABLISHED. " + connectedDevNum + " devices connected");
            gatt.discoverServices();
        }
        else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.w("BLUETOOTH DEBUG", "DISCONNECTED... BYE BYE!");
            connectedDevNum = -1;
            BLEDeviceScanActivity.numberOfConnectedDevices--;
        }

    }


    //Called when Gatt services are discovered
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            //Get the characteristic from the discovered gatt server
            Log.w("BLUETOOTH DEBUG", "SERVICES DISCOVERED!");
            BluetoothGattService service = gatt.getService(NEB_SERVICE_UUID);

            //Get the DATA characteristic and set notifications on
            BluetoothGattCharacteristic data_characteristic = service.getCharacteristic(NEB_DATACHAR_UUID);
            mCtrlChar = service.getCharacteristic(NEB_CTRLCHAR_UUID);
            gatt.setCharacteristicNotification(data_characteristic, true);
            List<BluetoothGattDescriptor> descriptors = data_characteristic.getDescriptors();
            BluetoothGattDescriptor descriptor = descriptors.get(0);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBleGatt.writeDescriptor(descriptor);
            writeDescriptorTimestamp = System.currentTimeMillis();

            if(shouldPollRSSI){
                //Poll RSSI
                    Timer myTimer = new Timer();
                    myTimer.scheduleAtFixedRate(new pollRSSI(),START_TIME,RETRY_TIME);

                //Used for Jitter Tests
//                myTimer.scheduleAtFixedRate(new JitterTest(),START_TIME+250,20);
            }
        }
    }


    //This is called as a confirmation of setting CharacteristicNotifications
    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
        onDescWriteTime = System.currentTimeMillis();
        writeDelay = onDescWriteTime - writeDescriptorTimestamp;
        Log.w("BLUETOOTH DEBUG", "Write Delay is: " + writeDelay);
        switch (initializeState){
            case 0:
                streamQuaternion(true);

//                setDataPort((byte)0,(byte)1);
                initializeState++;
                break;
            default:
                break;
        }
    }


    //Called when the writing of a characteristic returns an acknowledgement
    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
        switch (initializeState){
            case 0:
//                setDataPort((byte) 0, (byte) 1); //BLE port is 0, Activate is 1
                initializeState++;
                break;
            case 1:
                getMotionStatus();
                initializeState++;
                break;
            case 2:
                getLed();
                initializeState++;
                break;
            case 3:
                getDataPortState();
                initializeState++;
                break;
            case 4:
                getFirmwareVersion(); //TODO: Troubleshoot why this no longer return a value -> V2 issue???
                initializeState++;
                break;
            case 5:
                streamIMU(true);
                initializeState++;
                break;
            default:
                break;
        }
    }

    //Called when a new characteristic value arrives such as Quaternions or IMU stream values
    @Override
    public void onCharacteristicChanged (BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic) {

        //Check to see if delegate is assigned
        if (mDelegate == null) {
            return;
        }

        //PARSE HEADERS FROM RECEIVED PACKETS
        final byte[] pkt =  characteristic.getValue();
        int subsys = pkt[0] & 0x1f;
        final int pktype = pkt[0] >> 5;
        byte[] data = new byte[16];
        boolean errFlag = false;

        if (pktype == NEB_CTRL_PKTYPE_ACK)
            return;

        if ((subsys & 0x80) == 0x80)
        {
            subsys &= 0x7F;
            errFlag = true;
        }

        if (subsys == NEB_CTRL_PKTYPE_CMD){
            Log.w("BLUETOOTH_DEBUG","CMD ACK received");
        }

        int datalen = pkt.length - 4;

        for (int i = 0; i < datalen; i++)
            data[i] = pkt[i+4];


        //Now process the packet depending on the header values
        switch (subsys) {
            case NEB_CTRL_SUBSYS_DEBUG:		// Status & logging
                mDelegate.didReceiveDebugData(pkt[3], data, datalen, errFlag);
                break;

            case NEB_CTRL_SUBSYS_MOTION_ENG:// Motion Engine

                //JITTER TEST CODE
//                currentTime = System.nanoTime();  //Alternative Time() -> Requires about 100 clock cycles
                currentTime = System.currentTimeMillis(); //Requires about 5 clock cycles
                delayTime = currentTime - lastTime;
                if(isTimeinitializing!=true) {
                    if(file_size1 < size_max){
                        delayTimeArray[file_size1]=delayTime;
                        file_size1++;
                    }
                } else {
                    isTimeinitializing = false;
                }
                lastTime = currentTime;

                //Let the delegate handle the call
                mDelegate.didReceiveFusionData(pkt[3], data, errFlag, connectedDevNum);
                break;

            case NEB_CTRL_SUBSYS_POWERMGMT:	// Power management
                mDelegate.didReceivePmgntData(pkt[3], data,  datalen, errFlag);
                break;

            case NEB_CTRL_SUBSYS_LED:		// LED control

                //ROUND TRIP TIME TEST CODE
                returnTime = System.currentTimeMillis();
                if(file_size2 < size_max){
                    roundTripTimeArray[file_size2] = returnTime - sentTime;
                    file_size2++;
                }

                //Normal parsing
                mDelegate.didReceiveLedData(pkt[3], data,  datalen, errFlag);
                break;
            case NEB_CTRL_SUBSYS_STORAGE:	//NOR flash memory recorder
                mDelegate.didReceiveStorageData(pkt[3], data,  datalen, errFlag);
                break;

            case NEB_CTRL_SUBSYS_EEPROM:	//small EEPROM storage
                mDelegate.didReceiveEepromData(pkt[3], data,  datalen, errFlag);
                break;

        }
    }


/*********************************** SEND COMMAND CALLS **********************************/
    public void getDataPortState() {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[4];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_DEBUG); // 0x40
        pkbuf[1] = 0;	// Data len
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = DEBUG_CMD_GET_DATAPORT;	// Cmd

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);

        mCtrlChar.setValue(pkbuf);
        boolean success = mBleGatt.writeCharacteristic(mCtrlChar);
        Log.w("BLUETOOTH DEBUG", "Writing Motion Status: " + success);
    }

    public void getFirmwareVersion() {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[4];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_DEBUG);
        pkbuf[1] = 16;
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = DEBUG_CMD_GET_FW_VERSION;	// Cmd

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);

        mCtrlChar.setValue(pkbuf);
        boolean success = mBleGatt.writeCharacteristic(mCtrlChar);
        Log.w("BLUETOOTH DEBUG", "Writing Version Number: " + success);
    }

    public void getMotionStatus() {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_DEBUG);
        pkbuf[1] = 16;
        pkbuf[2] = (byte)0xff;
//        pkbuf[2] = 104;
        pkbuf[3] = DEBUG_CMD_MOTENGINE_RECORDER_STATUS;	// Cmd

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);

        mCtrlChar.setValue(pkbuf);
        boolean success = mBleGatt.writeCharacteristic(mCtrlChar);
        Log.w("BLUETOOTH DEBUG", "Writing Motion Status: " + success);
    }

    public void getRecorderStatus() {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_DEBUG);
        pkbuf[1] = 16;
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = DEBUG_CMD_MOTENGINE_RECORDER_STATUS;	// Cmd

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);

        mCtrlChar.setValue(pkbuf);
        boolean isWriteSuccessful = mBleGatt.writeCharacteristic(mCtrlChar);
        Log.w("BLUETOOTH DEBUG", "Writing Recorder Status: " + isWriteSuccessful);
    }

    public void setDataPort(int PortIdx, byte Ctrl) {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[6];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_DEBUG); // 0x40
        pkbuf[1] = 2;
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = DEBUG_CMD_SET_DATAPORT;	// Cmd

        // Port = 0 : BLE
        // Port = 1 : UART
        pkbuf[4] = (byte)PortIdx;
        pkbuf[5] = Ctrl;		// 1 - Open, 0 - Close

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);

        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
        Log.w("BLUETOOTH_DEBUG", "Sensing the BLE Data Port Command");
    }

    public void setInterface(byte Interf) {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_DEBUG); // 0x40
        pkbuf[1] = 16;
        pkbuf[2] = 0;
        pkbuf[3] = DEBUG_CMD_SET_INTERFACE;	// Cmd

        // Interf = 0 : BLE
        // Interf = 1 : UART
        pkbuf[4] = Interf;
        pkbuf[8] = 0;

        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    // *** EEPROM
    public void eepromRead(int pageNo) {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_EEPROM);
        pkbuf[1] = 16;
        pkbuf[2] = 0;
        pkbuf[3] = EEPROM_CMD_READ; // Cmd

        pkbuf[4] = (byte)(pageNo & 0xff);
        pkbuf[5] = (byte)((pageNo >> 8) & 0xff);

        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    public void eepromWrite(int pageNo, byte[] data) {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_EEPROM);
        pkbuf[1] = 16;
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = EEPROM_CMD_WRITE; // Cmd

        pkbuf[4] = (byte)(pageNo & 0xff);
        pkbuf[5] = (byte)((pageNo >> 8) & 0xff);

        for (int i = 0; i < 8; i += 1) {
            pkbuf[i + 6] = data[i];
        }

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);

        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    // *** LED subsystem commands
    public void getLed() {
        if (isDeviceReady() == false) {
            Log.w("BLUETOOTH DEBUG", "DEVICE NOT READY! NO LED READINGS :(");
            return;
        }

        byte[] pkbuf = new byte[4];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_LED);
        pkbuf[1] = 0;	// Data length
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = LED_CMD_GET_VALUE;	// Cmd

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);

        mCtrlChar.setValue(pkbuf);
        sentTime = System.currentTimeMillis();
        boolean success = mBleGatt.writeCharacteristic(mCtrlChar);
        Log.w("BLUETOOTH DEBUG", "Writing GET LED state: " + success);
    }

    public void setLed(byte LedNo, byte Value) {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_LED);
        pkbuf[1] = 16;
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = LED_CMD_SET_VALUE;	// Cmd

        // Nb of LED to set
        pkbuf[4] = 1;
        pkbuf[5] = LedNo;
        pkbuf[6] = Value;

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    // *** Power management sybsystem commands
    public void getTemperature() {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[4];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_POWERMGMT);
        pkbuf[1] = 0;	// Data length
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = POWERMGMT_CMD_GET_TEMPERATURE;	// Cmd

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);

        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    public void setBatteryChargeCurrent(int Current) {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[6];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_POWERMGMT);
        pkbuf[1] = 2;	// Data length
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = POWERMGMT_CMD_SET_CHARGE_CURRENT;	// Cmd

        // Data
        pkbuf[4] = (byte)(Current & 0xFF);
        pkbuf[5] = (byte)((Current >> 8) & 0xFF);

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    // *** Motion Settings
    public void setAccelerometerRange(byte Mode) {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_MOTION_ENG); //0x41
        pkbuf[1] = 16;//UInt8(sizeof(Fusion_DataPacket_t))
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = MOTION_CMD_SET_ACC_RANGE;	// Cmd
        pkbuf[8] = Mode;

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    public void setFusionType(byte Mode) {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_MOTION_ENG); //0x41
        pkbuf[1] = 16;//UInt8(sizeof(Fusion_DataPacket_t))
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = MOTION_CMD_SET_FUSION_TYPE;	// Cmd

        // Data
        pkbuf[8] = Mode;

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    public void setLockHeadingReference(boolean Enable) {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_MOTION_ENG); //0x41
        pkbuf[1] = 16;//UInt8(sizeof(Fusion_DataPacket_t))
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = MOTION_CMD_LOCK_HEADING_REF;	// Cmd

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    // *** Motion Streaming Send
    public void streamDisableAll()
    {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_MOTION_ENG);
        pkbuf[1] = 16;//UInt8(sizeof(Fusion_DataPacket_t))
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = MOTION_CMD_DISABLE_ALL_STREAM;	// Cmd

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);

        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    public void streamEulerAngle(boolean Enable)
    {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_MOTION_ENG); //0x41
        pkbuf[1] = 16;//UInt8(sizeof(Fusion_DataPacket_t))
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = MOTION_CMD_EULER_ANGLE; // Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    public void streamExternalForce(boolean Enable)
    {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_MOTION_ENG); //0x41
        pkbuf[1] = 16;//UInt8(sizeof(Fusion_DataPacket_t))
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = MOTION_CMD_EXTFORCE;	// Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    public void streamIMU(boolean Enable)
    {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_MOTION_ENG); //0x41
        pkbuf[1] = 16;//(sizeof(Fusion_DataPacket_t))
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = MOTION_CMD_IMU_DATA;	// Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    public void streamMAG(boolean Enable)
    {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_MOTION_ENG); //0x41
        pkbuf[1] = 16;//UInt8(sizeof(Fusion_DataPacket_t))
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = MOTION_CMD_MAG_DATA;	// Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    public void streamMotionState(boolean Enable)
    {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_MOTION_ENG); //0x41
        pkbuf[1] = 16; //UInt8(sizeof(Fusion_DataPacket_t))
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = MOTION_CMD_MOTION_STATE;	// Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    public void streamPedometer(boolean Enable)
    {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_MOTION_ENG); //0x41
        pkbuf[1] = 16;//UInt8(sizeof(Fusion_DataPacket_t))
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = MOTION_CMD_PEDOMETER; // Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }


    public void streamQuaternion(boolean Enable)
    {
        if (isDeviceReady() == false) {
            Log.w("BLUETOOTH DEBUG", "Device is not ready!" + Enable );
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_MOTION_ENG); //0x41
        pkbuf[1] = 16;//UInt8(sizeof(Fusion_DataPacket_t))
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = MOTION_CMD_QUATERNION;	// Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        boolean success = mBleGatt.writeCharacteristic(mCtrlChar);
        Log.w("BLUETOOTH_DEBUG","Write Stream Quaternions: " + success);
    }

    public void setMotionCmdDownSample(boolean Enable)
    {
        if (isDeviceReady() == false) {
            Log.w("BLUETOOTH DEBUG", "Device is not ready!" + Enable );
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_MOTION_ENG); //0x41
        pkbuf[1] = 16;//UInt8(sizeof(Fusion_DataPacket_t))
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = MOTION_CMD_DOWN_SAMPLE;	// Cmd

        if (Enable == true)
        {
            pkbuf[8] = 10;
        }
        else
        {
            pkbuf[8] = 0;
        }

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        boolean success = mBleGatt.writeCharacteristic(mCtrlChar);
        Log.w("BLUETOOTH_DEBUG","Write CMD Down Sample: " + success);
    }

    public void streamRotationInfo(boolean Enable) {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_MOTION_ENG); //0x41
        pkbuf[1] = 16;//UInt8(sizeof(Fusion_DataPacket_t))
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = MOTION_CMD_ROTATION_INFO;	// Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    public void  streamSittingStanding(boolean Enable) {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_MOTION_ENG); //0x41
        pkbuf[1] = 16;//UInt8(sizeof(Fusion_DataPacket_t));
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = MOTION_CMD_SIT_STAND;	// Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    public void streamTrajectoryInfo(boolean Enable)
    {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_MOTION_ENG); //0x41
        pkbuf[1] = 16;//UInt8(sizeof(Fusion_DataPacket_t))
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = MOTION_CMD_TRAJECTORY_INFO;	// Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    // *** Motion utilities
    public void resetTimeStamp() {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_MOTION_ENG);
        pkbuf[1] = 16; //UInt8(sizeof(Fusion_DataPacket_t))
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = MOTION_CMD_RESET_TIMESTAMP;	// Cmd

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    public void recordTrajectory(boolean Enable)
    {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_MOTION_ENG); //0x41
        pkbuf[1] = 16;//UInt8(sizeof(Fusion_DataPacket_t))
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = MOTION_CMD_TRAJECTORY_RECORD;	// Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    // *** Storage subsystem commands
    public void getSessionCount() {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_STORAGE);
        pkbuf[1] = 16;
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = STORAGE_CMD_GET_NB_SESSION; // Cmd

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    public void getSessionInfo(int sessionId) {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_STORAGE);
        pkbuf[1] = 16;
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = STORAGE_CMD_GET_SESSION_INFO; // Cmd

        pkbuf[8] = (byte)(sessionId & 0xff);
        pkbuf[9] = (byte)((sessionId >> 8) & 0xff);

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    public void eraseStorage(boolean Enable) {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_STORAGE); //0x41
        pkbuf[1] = 16;
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = STORAGE_CMD_ERASE; // Cmd

        if (Enable == true)
        {
            pkbuf[8] = 0;
        }
        else
        {
            return;
        }

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    public void sessionPlayback(boolean Enable, int sessionId) {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_STORAGE);
        pkbuf[1] = 16;
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = STORAGE_CMD_PLAY; // Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }

        pkbuf[9] = (byte)(sessionId & 0xff);
        pkbuf[10] = (byte)((sessionId >> 8) & 0xff);


        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    public void sessionRecord(boolean Enable) {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[20];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_STORAGE); //0x41
        pkbuf[1] = 16;
        pkbuf[2] = (byte)0xff;
        pkbuf[3] = STORAGE_CMD_RECORD;	// Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }

        pkbuf[2] = calculateCRC8Java(pkbuf,pkbuf.length);
        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    @Override
    public int describeContents() {
        return 0;
    }


    /************************************* Helper Functions ****************************************/

    @Override
    public boolean equals(Object obj) {
        if (DevId == ((Neblina)obj).DevId)
            return true;
        return false;
    }

    @Override
    public String toString() {
        return Nebdev.getName() + "_" + Long.toHexString(DevId).toUpperCase();
    }


    public void SetDelegate(NeblinaDelegate neblinaDelegate) {
        mDelegate = neblinaDelegate;
    }


    public boolean isDeviceReady() {
        if (Nebdev == null)
            return false;
        return true;
    }

    public boolean Connect(Context ctext) {
        mBleGatt = Nebdev.connectGatt(ctext, false, this);
        return mBleGatt != null;
    }

    public void Disconnect() {
        mBleGatt.disconnect();
        mBleGatt = null;
    }

    //Get initial state information and turn on Quaternions
    public void initializeNeblina() {
        //Get device states
        getFirmwareVersion();
        getMotionStatus();
        getDataPortState();
        getLed();
        //By default start streaming quaternions
        streamQuaternion(true);
    }

    //This function takes calculateCRC() function and expands each function in the order of operations and formats the bytes and ints properly to work in java
    //TODO: Make this code a bit cleaner for sanity's sake
    private byte calculateCRC8Java(byte[] pData, int len ) {

        int i, e, f, crc;

        crc = 0;
        for (i = 0; i < len; i++)
        {
            int b = pData[i] & 0xFF;
            e = crc ^ b;

            f = e ^ (e >> 4) ^ (e >> 7); //Breaking up this line is the split test

            byte byteA = (byte) e;

            int int1 = byteA & 0xFF; //e ->fixed int
            int int2 = 4; //4 ->fixed int
            int int3 = 7; //7 ->fixed int

            int temp1 = int1 >> int2; //replaces (e >> 4) -> int
            byte result1 = (byte) temp1; //replaces (e >> 4) -> byte
            int int4 = (byte) result1; //replaces (e >> 4) -> fixed int

            int temp2 = int1 >> int3; //replaces (e >> 7) -> int
            byte result2 = (byte) temp2; //replaces (e >> 7) -> byte
            int int5 = (byte) result2; //replaces (e >> 7) -> fixed int

            int temp3 = int1 ^ int4; //replaces e ^ (e >> 4)  -> int
            byte result3 = (byte)temp3; //replaces e ^ (e >> 4)  -> byte
            int int6 = result3 & 0xFF; //replaces e ^ (e >> 4)  -> fixed int

            int temp4 = int6 ^ int5;  //replaces e ^ (e >> 4) ^ (e >> 7)
            byte final_split_result = (byte)temp4;

            f = final_split_result & 0xFF;

            //Second line to split
//            crc = ((f << 1) ^ (f << 4));
            byte number1 = (byte) 1;
            byte number4 = (byte) 4;

            int intNumber1 = number1 & 0xFF;
            int intNumber4 = number4 & 0xFF;

            int intIntermediary1 = f << intNumber1;
            int intIntermediary4 = f << intNumber4;

            byte byteIntermediary1 = (byte) intIntermediary1;
            byte byteIntermediary4 = (byte) intIntermediary4;

            int intFinalEquation1 = byteIntermediary1 &0xFF;
            int intFinalEquation4 = byteIntermediary4 &0xFF;

            crc = intFinalEquation1 ^ intFinalEquation4;
        }
        return (byte)crc; //Doesn't matter if we add & 0xFF
    }

    //This is close to the original code copied from iOS -> it doesn't work properly
    private byte calculateCRC(byte[] pData, int len ) {

        int i;
        byte e, f, crc;

        crc = 0;
        for (i = 0; i < len; i++)
        {
            e = (byte)(crc ^ pData[i]);
            f = (byte)(e ^ (e >> 4) ^ (e >> 7));
            crc = (byte) ((f << 1) ^ (f << 4));
        }
        return crc;
    }


/****************************PARCELABLE INTERFACE FUNCTIONS***************************************/
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeValue(Nebdev);
        out.writeLong(DevId);
        out.writeValue(mBleGatt);
        out.writeValue(mDelegate);
        out.writeValue(mCtrlChar);
    }

    public static final Parcelable.Creator<Neblina> CREATOR
            = new Parcelable.Creator<Neblina>() {
        public Neblina createFromParcel(Parcel in) {
            return new Neblina(in);
        }
        public Neblina[] newArray(int size) {
            return new Neblina[size];
        }
    };

    private Neblina(Parcel in) {
        Nebdev = (BluetoothDevice) in.readValue(null);
        DevId = in.readLong();
        mBleGatt = (BluetoothGatt) in.readValue(null);
        mDelegate = (NeblinaDelegate) in.readValue(null);
        mCtrlChar = (BluetoothGattCharacteristic) in.readValue(null);
    }
}