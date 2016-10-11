package com.mygdx.game.android.NeblinaClasses;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;


import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

import com.motsai.neblina.Neblina;

/**
 * An activity representing a list of NebDevices. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link NebDeviceDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class NebDeviceListActivity extends AppCompatActivity {
    //NEBLINA CUSTOM UUIDs
    public static final UUID[] SCAN_UUID = {Neblina.NEB_SERVICE_UUID,};// UUID.fromString("0df9f021-1532-11e5-8960-0002a5d5c51b"), };

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
                    //if (device.getUuids())
                    int i = 0;
                    long deviceID = 0;

                    // Try to find our device ID
                    while (i < scanRecord.length && scanRecord[i] > 0) {
                        if (scanRecord[i] > 0) {
                            if (scanRecord[i + 1] == -1)
                            {
                                ByteBuffer x =  ByteBuffer.wrap(scanRecord, i+4, 8);
                                x.order(ByteOrder.LITTLE_ENDIAN);
                                deviceID = x.getLong();
                                break;
                            }
                            i += scanRecord[i] + 1;
                        }
                    }

                    RecyclerView recyclerView = (RecyclerView)findViewById(R.id.nebdevice_list);
                    SimpleItemRecyclerViewAdapter r = (SimpleItemRecyclerViewAdapter)recyclerView.getAdapter();
                    r.addItem(new Neblina(deviceID, device));
                       //     mLeDeviceListAdapter.addDevice(device);
                        //    mLeDeviceListAdapter.notifyDataSetChanged();
  //                      }
      //              });
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nebdevice_list);
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        mBluetoothAdapter = bluetoothManager.getAdapter();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        View recyclerView = findViewById(R.id.nebdevice_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);

        scanLeDevice(true);

        if (findViewById(R.id.nebdevice_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
 /*           mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, 100);//SCAN_PERIOD);*/

            //mScanning = true;
            //mBluetoothAdapter.
            mBluetoothAdapter.startLeScan(SCAN_UUID, mLeScanCallback);
        } else {
            //mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter());
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final Map<String, Neblina> mNebDevices = new HashMap<String, Neblina>();

        //public SimpleItemRecyclerViewAdapter() {
           // mNebDevices.put(null, null);
        //}


        public void addItem(Neblina dev) {
            if (mNebDevices.containsKey(dev.toString()) == false) {
                mNebDevices.put(dev.toString(), dev);
                Log.w("BLUETOOTH DEBUG", "Item added " + dev.toString());
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.nebdevice_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            Set<Map.Entry<String, Neblina>> mset = mNebDevices.entrySet();
            Object[] mobj = mset.toArray();
            holder.mItem = ((Map.Entry<String, Neblina>)mobj[position]).getValue();

            if (holder.mItem == null)
                return;

            holder.mIdView.setText(holder.mItem.toString());

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scanLeDevice(false);
                    //mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    if (mTwoPane) {
                        Bundle arguments = new Bundle();

                        arguments.putParcelable(NebDeviceDetailFragment.ARG_ITEM_ID, holder.mItem);
                        NebDeviceDetailFragment fragment = new NebDeviceDetailFragment();
                        fragment.setArguments(arguments);
                        //fragment.SetItem(holder.mItem);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.nebdevice_detail_container, fragment)
                                .commit();
                    } else {
                        Context context = v.getContext();
                        //Bundle arguments = new Bundle();

                       // arguments.putSerializable(NebDeviceDetailFragment.ARG_ITEM_ID, holder.mItem);

                        Intent intent = new Intent(context, NebDeviceDetailActivity.class);
                        intent.putExtra(NebDeviceDetailFragment.ARG_ITEM_ID, holder.mItem);

                        context.startActivity(intent);
                    }
                    //holder.mItem.Connect(getBaseContext());
                }
            });
        }

        @Override
        public int getItemCount() {
            return mNebDevices.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mIdView;
            public Neblina mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mIdView = (TextView) view.findViewById(R.id.id);
            }

            @Override
            public String toString() {
                return super.toString();
            }
        }
    }
}
