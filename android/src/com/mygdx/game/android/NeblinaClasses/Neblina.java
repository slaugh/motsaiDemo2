package com.mygdx.game.android.NeblinaClasses;


/**
 * Created by scott on 2016-06-27.
 */
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
import com.mygdx.game.simulation.Simulation;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


/**
 * Created by hoanmotsai on 2016-06-10.
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

    // Subsystem values
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

    // ***
    // Power management subsystem command code
    public static final byte POWERMGMT_CMD_GET_BAT_LEVEL		= 0;	// Get battery level
    public static final byte POWERMGMT_CMD_GET_TEMPERATURE		= 1;	// Get temperature
    public static final byte POWERMGMT_CMD_SET_CHARGE_CURRENT	= 2;	// Set battery charge current

    // ***
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
    // ***

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

    // Motion engine commands
    public static final byte MOTION_CMD_DOWN_SAMPLE         = 0x01;
    public static final byte MOTION_CMD_MOTION_STATE        = 0x02;
    public static final byte MOTION_CMD_IMU_DATA 		    = 0x03;
    public static final byte MOTION_CMD_QUATERNION          = 0x04;
    public static final byte MOTION_CMD_EULER_ANGLE         = 0x05;
    public static final byte MOTION_CMD_EXTFORCE            = 0x06;
    public static final byte MOTION_CMD_SET_FUSION_TYPE     = 0x07;
    public static final byte MOTION_CMD_TRAJECTORY_RECORD   = 0x08;
    //#define TrajectoryRecStop 0x09
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
    private int motionStatus = 0;


    public void SetDelegate(NeblinaDelegate neblinaDelegate) {
        mDelegate = neblinaDelegate;
    }

    @Override
    public String toString() {
        return Nebdev.getName() + "_" + Long.toHexString(DevId).toUpperCase();
    }

    public Neblina(long id, BluetoothDevice dev) {
        Nebdev = dev;
        DevId = id;
        mDelegate = null;
        mBleGatt = null;
        mCtrlChar = null;
    }

    @Override
    public boolean equals(Object obj) {
        if (DevId == ((Neblina)obj).DevId)
            return true;
        return false;
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

    // Here are where the BluetoothGattCallbacks are
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
                Timer myTimer = new Timer();
                myTimer.scheduleAtFixedRate(new Task(),START_TIME,RETRY_TIME);
//                myTimer.scheduleAtFixedRate(new JitterTest(),START_TIME+250,20);
            }
        }
    }

    public class Task extends TimerTask {

        @Override
        public void run() {
            mBleGatt.readRemoteRssi();
        Log.w("BLUETOOTH_DEBUG","Requesting RSSI AT:" + System.currentTimeMillis());
            // DO WHAT YOU NEED TO DO HERE
        }
    }

    public class JitterTest extends  TimerTask {
        @Override
        public void run() {
            getLed();
        }
    }


    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status){
        Log.w("BLUETOOTH_DEBUG","RSSI is:" +  rssi);


        if(rssi > 80){
            mBleGatt.readRemoteRssi();
            if(motionStatus==1){
                //TODO: OH NO! THE BABY NEEDS SAVING!!! Trigger a PUT request to notify the server
                HttpClient client = new HttpClient();
                GetMethod method = new GetMethod("http://requestb.in/1h67p571");
                try {
                    int statusCode = client.executeMethod(method);
                    byte[] responseBody = method.getResponseBody();
                    System.out.println(new String(responseBody));
                } catch (Exception e) {
                    System.err.println("Fatal error: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    method.releaseConnection();
                }
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
                initializeState++;
                break;
            default:
                break;
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
        switch (initializeState){
            case 0:
                streamQuaternion(true);
                initializeState++;
                break;
            case 1:
                getFirmwareVersion();
                initializeState++;
                break;
            case 2:
                getMotionStatus();
                initializeState++;
                break;
            case 3:
                getDataPortState();
                initializeState++;
                break;
            case 4:
                getLed();
                initializeState++;
                break;
            case 5:
                setMotionCmdDownSample(true);
                initializeState++;
                break;
            default:
                break;
        }
    }

    @Override
    public void onCharacteristicRead (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
        Log.w("BLUETOOTH_DEBUG","OnCharacteristicRead! :D");

    }


    //PROCESS RECEIVED PACKETS
    @Override
    public void onCharacteristicChanged (BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic) {

        if (mDelegate == null) {
            return;
        }

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

        int datalen = pkt.length - 4;

        for (int i = 0; i < datalen; i++)
            data[i] = pkt[i+4];


        switch (subsys) {
            case NEB_CTRL_SUBSYS_DEBUG:		// Status & logging
                mDelegate.didReceiveDebugData(pkt[3], data, datalen, errFlag);
                break;
            case NEB_CTRL_SUBSYS_MOTION_ENG:// Motion Engine

//                currentTime = System.nanoTime();  //Requires about 100 clock cycles
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


                mDelegate.didReceiveFusionData(pkt[3], data, errFlag, connectedDevNum);
                break;
            case NEB_CTRL_SUBSYS_POWERMGMT:	// Power management
                mDelegate.didReceivePmgntData(pkt[3], data,  datalen, errFlag);
                break;
            case NEB_CTRL_SUBSYS_LED:		// LED control

                returnTime = System.currentTimeMillis();
                Log.w("BLUETOOTH DEBUG", "LED STATE RETURNED");
                if(file_size2 < size_max){
                    roundTripTimeArray[file_size2] = returnTime - sentTime;
                    file_size2++;
                }

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


    // PACKET CREATION CALLS
    public void getDataPortState() {
        if (isDeviceReady() == false) {
            return;
        }

        byte[] pkbuf = new byte[4];

        pkbuf[0] = ((NEB_CTRL_PKTYPE_CMD << 5) | NEB_CTRL_SUBSYS_DEBUG); // 0x40
        pkbuf[1] = 0;	// Data len
        pkbuf[2] = 0;
        pkbuf[3] = DEBUG_CMD_GET_DATAPORT;	// Cmd

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
        pkbuf[2] = 0;
        pkbuf[3] = DEBUG_CMD_GET_FW_VERSION;	// Cmd

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
        pkbuf[2] = 0;
        pkbuf[3] = DEBUG_CMD_MOTENGINE_RECORDER_STATUS;	// Cmd

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
        pkbuf[2] = 0;
        pkbuf[3] = DEBUG_CMD_MOTENGINE_RECORDER_STATUS;	// Cmd

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
        pkbuf[2] = 0;
        pkbuf[3] = DEBUG_CMD_SET_DATAPORT;	// Cmd

        // Port = 0 : BLE
        // Port = 1 : UART
        pkbuf[4] = (byte)PortIdx;
        pkbuf[5] = Ctrl;		// 1 - Open, 0 - Close

        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
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
        pkbuf[2] = 0;
        pkbuf[3] = EEPROM_CMD_WRITE; // Cmd

        pkbuf[4] = (byte)(pageNo & 0xff);
        pkbuf[5] = (byte)((pageNo >> 8) & 0xff);

        for (int i = 0; i < 8; i += 1) {
            pkbuf[i + 6] = data[i];
        }

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
        pkbuf[2] = 0;
        pkbuf[3] = LED_CMD_GET_VALUE;	// Cmd

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
        pkbuf[2] = 0;
        pkbuf[3] = LED_CMD_SET_VALUE;	// Cmd

        // Nb of LED to set
        pkbuf[4] = 1;
        pkbuf[5] = LedNo;
        pkbuf[6] = Value;

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
        pkbuf[2] = 0;
        pkbuf[3] = POWERMGMT_CMD_GET_TEMPERATURE;	// Cmd

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
        pkbuf[2] = 0;
        pkbuf[3] = POWERMGMT_CMD_SET_CHARGE_CURRENT;	// Cmd

        // Data
        pkbuf[4] = (byte)(Current & 0xFF);
        pkbuf[5] = (byte)((Current >> 8) & 0xFF);

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
        pkbuf[2] = 0;
        pkbuf[3] = MOTION_CMD_SET_ACC_RANGE;	// Cmd
        pkbuf[8] = Mode;

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
        pkbuf[2] = 0;
        pkbuf[3] = MOTION_CMD_SET_FUSION_TYPE;	// Cmd

        // Data
        pkbuf[8] = Mode;

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
        pkbuf[2] = 0;
        pkbuf[3] = MOTION_CMD_LOCK_HEADING_REF;	// Cmd

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
        pkbuf[2] = 0;
        pkbuf[3] = MOTION_CMD_DISABLE_ALL_STREAM;	// Cmd

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
        pkbuf[2] = 0;
        pkbuf[3] = MOTION_CMD_EULER_ANGLE; // Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }

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
        pkbuf[2] = 0;
        pkbuf[3] = MOTION_CMD_EXTFORCE;	// Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }
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
        pkbuf[2] = 0;
        pkbuf[3] = MOTION_CMD_IMU_DATA;	// Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }
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
        pkbuf[2] = 0;
        pkbuf[3] = MOTION_CMD_MAG_DATA;	// Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }
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
        pkbuf[2] = 0;
        pkbuf[3] = MOTION_CMD_MOTION_STATE;	// Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }
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
        pkbuf[2] = 0;
        pkbuf[3] = MOTION_CMD_PEDOMETER; // Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }
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
        pkbuf[2] = 0;
        pkbuf[3] = MOTION_CMD_QUATERNION;	// Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }
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
        pkbuf[2] = 0;
        pkbuf[3] = MOTION_CMD_DOWN_SAMPLE;	// Cmd

        if (Enable == true)
        {
            pkbuf[8] = 10;
        }
        else
        {
            pkbuf[8] = 0;
        }
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
        pkbuf[2] = 0;
        pkbuf[3] = MOTION_CMD_ROTATION_INFO;	// Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }
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
        pkbuf[2] = 0;
        pkbuf[3] = MOTION_CMD_SIT_STAND;	// Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }
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
        pkbuf[2] = 0;
        pkbuf[3] = MOTION_CMD_TRAJECTORY_INFO;	// Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }
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
        pkbuf[2] = 0;
        pkbuf[3] = MOTION_CMD_RESET_TIMESTAMP;	// Cmd

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
        pkbuf[2] = 0;
        pkbuf[3] = MOTION_CMD_TRAJECTORY_RECORD;	// Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }
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
        pkbuf[2] = 0;
        pkbuf[3] = STORAGE_CMD_GET_NB_SESSION; // Cmd

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
        pkbuf[2] = 0;
        pkbuf[3] = STORAGE_CMD_GET_SESSION_INFO; // Cmd

        pkbuf[8] = (byte)(sessionId & 0xff);
        pkbuf[9] = (byte)((sessionId >> 8) & 0xff);

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
        pkbuf[2] = 0;
        pkbuf[3] = STORAGE_CMD_ERASE; // Cmd

        if (Enable == true)
        {
            pkbuf[8] = 0;
        }
        else
        {
            return;
        }
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
        pkbuf[2] = 0;
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
        pkbuf[2] = 0;
        pkbuf[3] = STORAGE_CMD_RECORD;	// Cmd

        if (Enable == true)
        {
            pkbuf[8] = 1;
        }
        else
        {
            pkbuf[8] = 0;
        }
        mCtrlChar.setValue(pkbuf);
        mBleGatt.writeCharacteristic(mCtrlChar);
    }

    @Override
    public int describeContents() {
        return 0;
    }


    //PARCELABLE INTERFACE FUNCTIONS
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

}