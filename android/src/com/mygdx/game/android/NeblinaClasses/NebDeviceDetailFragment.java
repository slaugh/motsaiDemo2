package com.mygdx.game.android.NeblinaClasses;


/**
 * Created by scott on 2016-06-30.
 */

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.mygdx.game.android.Adapters.NebCmdItem;
import com.mygdx.game.android.Adapters.NebListAdapter;
import com.mygdx.game.android.ControlPanel.BLEDeviceScanActivity;
import com.mygdx.game.android.R;

import java.util.Arrays;

import static com.mygdx.game.android.NeblinaClasses.Neblina.DEBUG_CMD_DUMP_DATA;
import static com.mygdx.game.android.NeblinaClasses.Neblina.DEBUG_CMD_GET_DATAPORT;
import static com.mygdx.game.android.NeblinaClasses.Neblina.DEBUG_CMD_GET_FW_VERSION;
import static com.mygdx.game.android.NeblinaClasses.Neblina.DEBUG_CMD_MOTENGINE_RECORDER_STATUS;
import static com.mygdx.game.android.NeblinaClasses.Neblina.DEBUG_CMD_SET_DATAPORT;
import static com.mygdx.game.android.NeblinaClasses.Neblina.DEBUG_CMD_SET_INTERFACE;
import static com.mygdx.game.android.NeblinaClasses.Neblina.EEPROM_CMD_READ;
import static com.mygdx.game.android.NeblinaClasses.Neblina.EEPROM_CMD_WRITE;
import static com.mygdx.game.android.NeblinaClasses.Neblina.MOTION_CMD_MAG_DATA;
import static com.mygdx.game.android.NeblinaClasses.Neblina.MOTION_CMD_QUATERNION;
import static com.mygdx.game.android.NeblinaClasses.Neblina.NEB_CTRL_SUBSYS_DEBUG;
import static com.mygdx.game.android.NeblinaClasses.Neblina.NEB_CTRL_SUBSYS_EEPROM;
import static com.mygdx.game.android.NeblinaClasses.Neblina.NEB_CTRL_SUBSYS_MOTION_ENG;
import static com.mygdx.game.android.NeblinaClasses.Neblina.NEB_CTRL_SUBSYS_STORAGE;
import static com.mygdx.game.android.NeblinaClasses.Neblina.STORAGE_CMD_PLAY;
import static com.mygdx.game.android.NeblinaClasses.Neblina.STORAGE_CMD_RECORD;

public class NebDeviceDetailFragment extends Fragment implements NeblinaDelegate {

    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";
    public static final NebCmdItem[] cmdList = new NebCmdItem[] {
            new NebCmdItem(NEB_CTRL_SUBSYS_DEBUG, DEBUG_CMD_SET_DATAPORT, "BLE Data Port", 1, ""),
            new NebCmdItem(NEB_CTRL_SUBSYS_DEBUG, DEBUG_CMD_SET_DATAPORT, "UART Data Port", 1, ""),
            new NebCmdItem(NEB_CTRL_SUBSYS_MOTION_ENG, Neblina.MOTION_CMD_SET_FUSION_TYPE, "Fusion 9 axis", 1, ""),
            new NebCmdItem(NEB_CTRL_SUBSYS_MOTION_ENG, MOTION_CMD_QUATERNION, "Quaternion Stream", 1, ""),
            new NebCmdItem(NEB_CTRL_SUBSYS_MOTION_ENG, MOTION_CMD_MAG_DATA, "Mag Stream", 1, ""),
            new NebCmdItem(NEB_CTRL_SUBSYS_MOTION_ENG, Neblina.MOTION_CMD_LOCK_HEADING_REF, "Lock Heading Ref.", 1, ""),
            new NebCmdItem(NEB_CTRL_SUBSYS_STORAGE, Neblina.STORAGE_CMD_ERASE, "Flash Erase All", 1, ""),
            new NebCmdItem(NEB_CTRL_SUBSYS_STORAGE, STORAGE_CMD_RECORD, "Flash Record", 1, ""),
            new NebCmdItem(NEB_CTRL_SUBSYS_STORAGE, STORAGE_CMD_PLAY, "Flash Playback", 1, ""),
            new NebCmdItem(Neblina.NEB_CTRL_SUBSYS_LED, Neblina.LED_CMD_SET_VALUE, "Set LED0 level", 3, ""),
            new NebCmdItem(Neblina.NEB_CTRL_SUBSYS_LED, Neblina.LED_CMD_SET_VALUE, "Set LED1 level", 3, ""),
            new NebCmdItem(Neblina.NEB_CTRL_SUBSYS_LED, Neblina.LED_CMD_SET_VALUE, "Set LED2", 1, ""),
            new NebCmdItem(NEB_CTRL_SUBSYS_EEPROM, EEPROM_CMD_READ, "EEPROM Read", 2, "Read"),
            new NebCmdItem(Neblina.NEB_CTRL_SUBSYS_POWERMGMT, Neblina.POWERMGMT_CMD_SET_CHARGE_CURRENT, "Charge Current in mA", 3, ""),
            new NebCmdItem((byte)0xf, (byte)0, "Motion data stream", 1, ""),
            new NebCmdItem((byte)0xf, (byte)1, "Heading", 1, "")
    };

    //The dummy content this fragment is presenting.
    public Neblina mNebDev;
    private TextView mTextLabel1;
    private TextView mTextLabel2;
    private ListView mCmdListView;

    //Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon screen orientation changes)
    public static float latest_Q0 = 0.0f;
    public static float latest_Q1 = 0.0f;
    public static float latest_Q2 = 0.0f;
    public static float latest_Q3 = 0.0f;
    public static String Q0_string = "";
    public static String Q1_string = "";
    public static String Q2_string = "";
    public static String Q3_string = "";
    public static long timestamp_N =0;


    public NebDeviceDetailFragment() {

    }

    public void SetItem(Neblina item) {

        mNebDev = item;
        mNebDev.SetDelegate(this);
//        mNebDev.Connect(getActivity());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //        if (getArguments().containsKey(ARG_ITEM_ID)) {
//            // Load the dummy content specified by the fragment
//            // arguments. In a real-world scenario, use a Loader
//            // to load content from a content provider.
//            mNebDev = (Neblina) getArguments().getParcelable(ARG_ITEM_ID);
//            mNebDev.SetDelegate(this);
//        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.nebdevice_detail, container, false);


        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextLabel1 = (TextView) rootView.findViewById(R.id.textView1);
                mTextLabel2 = (TextView) rootView.findViewById(R.id.textView2);
                mCmdListView = (ListView) rootView.findViewById(R.id.listView);
            }
        });

        NebListAdapter adapter = new NebListAdapter(getActivity().getApplicationContext(),
                R.layout.nebcmd_item, cmdList);

        mCmdListView.setAdapter(adapter);
        mCmdListView.setTag(this);
        mCmdListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {

                    if (mNebDev != null) {
                        mNebDev.getMotionStatus();
                        mNebDev.getDataPortState();
                        mNebDev.getLed();
                        mNebDev.getFirmwareVersion();
                    }
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {

            }
        });


        return rootView;
    }

    public void onSwitchButtonChanged(CompoundButton button, boolean isChecked) {
        int idx = (int) button.getTag();
        if (idx < 0 && idx > cmdList.length)
            return;

        switch (cmdList[idx].mSubSysId) {
            case NEB_CTRL_SUBSYS_DEBUG:
                switch (cmdList[idx].mCmdId)
                {
                    case DEBUG_CMD_SET_INTERFACE:
                        //mNedDev.setInterface(isChecked == true ? 1);
                        break;
                    case DEBUG_CMD_DUMP_DATA:
                        break;
                    case DEBUG_CMD_SET_DATAPORT:
                        if (isChecked)
                            mNebDev.setDataPort(idx, (byte) 1);
                        else
                            mNebDev.setDataPort(idx, (byte) 0);
                        break;
                    default:
                        break;
                }
                break;

            case NEB_CTRL_SUBSYS_MOTION_ENG:
                switch (cmdList[idx].mCmdId) {
                    case MOTION_CMD_QUATERNION:
                        mNebDev.streamQuaternion(isChecked);
                }
                break;
        }
    }

    public void onButtonClick(View button) {
        int idx = (int) button.getTag();
        if (idx < 0 && idx > cmdList.length)
            return;
        switch (cmdList[idx].mSubSysId) {
            case NEB_CTRL_SUBSYS_EEPROM:
                switch (cmdList[idx].mCmdId) {
                    case EEPROM_CMD_READ:
                        mNebDev.eepromRead(0);
                        break;
                    case EEPROM_CMD_WRITE:
                        break;
                }
                break;
        }
    }

    public int getCmdIdx(int subsysId, int cmdId) {
        for (int i = 0; i < cmdList.length; i++) {
            if (cmdList[i].mSubSysId == subsysId && cmdList[i].mCmdId == cmdId) {
                return i;
            }
        }
        return -1;
    }


    public void initializeNeblina() {
        //By default start streaming quaternions
        mNebDev.streamQuaternion(true);
        BLEDeviceScanActivity.is_QUATERNION_BUTTON_on = true;
    }

    public void didReceiveRSSI(int rssi) {
    }

    public void didReceiveFusionData(int type , byte[] data, boolean errFlag) {
        switch (type) {
            case MOTION_CMD_QUATERNION:

                //Merge Note A. Neblina Code
                final String s = String.format("%d, %d, %d", data[4], data[6], data[8]);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTextLabel1.setText(s);
                        mTextLabel1.getRootView().postInvalidate();
                    }
                });


                //Merge Note B. Original Code
                //Puts the characteristic values into the intent
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));
                }

                //TODO: Fix timestamping
//            timestamp_N = (timestamp[3]&0xff)<<24 | (timestamp[2]&0xff)<<16 | (timestamp[1]&0xff)<<8 | (timestamp[0]&0xff)<<0;
                //TODO: Fix and Test Sending Data To The Cloud
//          sendQuaternionsToCloudRESTfully(Q0_string, Q1_string, Q2_string, Q3_string); //The pitcher works, the catcher fails
//            new getAWSID().execute("gogogo!"); //Uses the AWS Android SDK -> Seems to work

                //Unwrap Data Based on Motsai's Neblina Protocol
                if (data.length == 16) {
                    //Plus 1 is to remind me that the end of the range is non-inclusive
                    //Minus 4 since the header and timestamp are chopped off
//                    final byte[] header = Arrays.copyOfRange(data, 0, 3 + 1); //Bytes 0-3 are the header
//                    final byte[] timestamp = Arrays.copyOfRange(data, 4, 7 + 1); //Bytes 4-7 are the timestamp
                    final byte[] q0 = Arrays.copyOfRange(data, 8-4, 9-4 + 1); // Bytes 8-9 are Q0 value
                    final byte[] q1 = Arrays.copyOfRange(data, 10-4, 11-4 + 1); // Bytes 10-11 are Q1 value
                    final byte[] q2 = Arrays.copyOfRange(data, 12-4, 13-4 + 1); // Bytes 12-13 are Q2 value
                    final byte[] q3 = Arrays.copyOfRange(data, 14-4, 15-4 + 1); // Bytes 14-15 are Q3 value
                    final byte[] reserved = Arrays.copyOfRange(data, 16-4, 19-4 + 1); // Bytes 16-19 are reserved

                    //Convert to big endian
                    latest_Q0 = normalizedQ(q0);
                    latest_Q1 = normalizedQ(q1);
                    latest_Q2 = normalizedQ(q2);
                    latest_Q3 = normalizedQ(q3);

                    //Create a string version
                    Q0_string = String.valueOf(latest_Q0);
                    Q1_string = String.valueOf(latest_Q1);
                    Q2_string = String.valueOf(latest_Q2);
                    Q3_string = String.valueOf(latest_Q3);
                break;
        }
    }
    }

    public void didReceiveDebugData(int type, byte[] data, boolean errFlag) {

    }
    public void didReceivePmgntData(int type, byte[] data, boolean errFlag) {

    }
    public void didReceiveStorageData(int type, byte[] data, boolean errFlag) {
        BLEDeviceScanActivity.playbackNumber = 0;

    }
    public void didReceiveEepromData(int type, byte[] data, boolean errFlag) {

    }
    public void didReceiveLedData(int type, byte[] data, boolean errFlag) {

    }

    private float normalizedQ(byte[] q) {
        if(q.length==2){
            int val = ((q[1]&0xff)<<8)|(q[0]&0xff); //concatenate the byte array into an int
            float normalized = (float) val / 32768; //normalize by dividing by 2^15
            if (normalized > 1.0) normalized = normalized-2;
            return normalized;
        }else return -1;
    }


    public void didConnectNeblina() {
        mNebDev.getMotionStatus();
        mNebDev.getDataPortState();
        mNebDev.getLed();
        mNebDev.getFirmwareVersion();
    }


    public void didReceiveDebugData(int type, byte[] data, int dataLen, boolean errFlag) {
        NebListAdapter adapter = (NebListAdapter) mCmdListView.getAdapter();

        switch (type) {
            case DEBUG_CMD_MOTENGINE_RECORDER_STATUS:
            {
                switch (data[8]) {
                    case 1:    // Playback
                    {
                        int i = getCmdIdx(NEB_CTRL_SUBSYS_STORAGE, STORAGE_CMD_RECORD);
                        Switch v = (Switch) mCmdListView.findViewWithTag(i);
                        if (v != null) {
                            v.setChecked(false);
                            v.getRootView().postInvalidate();
                        }
                        i = getCmdIdx(NEB_CTRL_SUBSYS_STORAGE, STORAGE_CMD_PLAY);
                        v = (Switch) mCmdListView.findViewWithTag(i);
                        if (v != null) {
                            v.setChecked(true);
                            v.getRootView().postInvalidate();
                        }
                    }
                    break;
                    case 2:    // Recording
                    {
                        int i = getCmdIdx(NEB_CTRL_SUBSYS_STORAGE, STORAGE_CMD_PLAY);
                        Switch v = (Switch) mCmdListView.findViewWithTag(i);
                        if (v != null) {
                            v.setChecked(false);
                            v.getRootView().postInvalidate();
                        }
                        i = getCmdIdx(NEB_CTRL_SUBSYS_STORAGE, STORAGE_CMD_RECORD);
                        v = (Switch) mCmdListView.findViewWithTag(i);
                        if (v != null) {
                            v.setChecked(true);
                            v.getRootView().postInvalidate();
                        }
                    }
                    break;
                    default: {
                        int i = getCmdIdx(NEB_CTRL_SUBSYS_STORAGE, STORAGE_CMD_RECORD);
                        Switch v = (Switch) mCmdListView.findViewWithTag(i);
                        if (v != null) {
                            v.setChecked(false);
                            v.getRootView().postInvalidate();
                        }
                        i = getCmdIdx(NEB_CTRL_SUBSYS_STORAGE, STORAGE_CMD_PLAY);
                        v = (Switch) mCmdListView.findViewWithTag(i);
                        if (v != null) {
                            v.setChecked(false);
                            v.getRootView().postInvalidate();
                        }
                    }
                    break;
                }
                int i = getCmdIdx(NEB_CTRL_SUBSYS_MOTION_ENG, MOTION_CMD_QUATERNION);
                Switch v = (Switch) mCmdListView.findViewWithTag(i);

                if (v != null) {
                    v.setChecked(((data[4] & 8) >> 3) != 0);
                    v.getRootView().postInvalidate();
                }
                i = getCmdIdx(NEB_CTRL_SUBSYS_MOTION_ENG, MOTION_CMD_MAG_DATA);
                v = (Switch) mCmdListView.findViewWithTag(i);
                if (v != null) {
                    v.setChecked(((data[4] & 0x80) >> 7) != 0);
                    v.getRootView().postInvalidate();
                }
            }
            break;
            case DEBUG_CMD_GET_FW_VERSION:
            {
                String s = String.format("API:%d, FEN:%d.%d.%d, BLE:%d.%d.%d", data[0], data[1], data[2], data[3], data[4], data[5], data[6]);

                mTextLabel2.setText(s);
                mTextLabel2.getRootView().postInvalidate();
            }
            break;
            case DEBUG_CMD_DUMP_DATA:
            {
                String s = String.format("%02x %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x",
                        data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7], data[8], data[9],
                        data[10], data[11], data[12], data[13], data[14], data[15]);
                mTextLabel1.setText(s);
                mTextLabel1.getRootView().postInvalidate();
            }
            break;
            case DEBUG_CMD_GET_DATAPORT:
                int i = getCmdIdx(NEB_CTRL_SUBSYS_DEBUG, DEBUG_CMD_SET_DATAPORT);
                Switch v = (Switch) mCmdListView.findViewWithTag(i);
                if (v != null) {
                    v.setChecked(data[0] != 0);
                    v.getRootView().postInvalidate();
                }
                v = (Switch) mCmdListView.findViewWithTag(i + 1);
                if (v != null) {
                    v.setChecked(data[1] != 0);
                    v.getRootView().postInvalidate();
                }
                break;
        }

    }
    public void didReceivePmgntData(int type, byte[] data, int dataLen, boolean errFlag) {

    }
    public void didReceiveStorageData(int type, byte[] data, int dataLen, boolean errFlag) {

    }
    public void didReceiveEepromData(int type, byte[] data, int dataLen, boolean errFlag) {

    }
    public void didReceiveLedData(int type, byte[] data, int dataLen, boolean errFlag) {

    }

}
