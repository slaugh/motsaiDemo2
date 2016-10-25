package com.mygdx.game.android.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.mygdx.game.android.ControlPanel.BLEDeviceScanActivity;
import com.mygdx.game.android.R;

import java.util.List;

/**
 * Created by scott on 2016-10-07.
 */

//This function maps the Neblina items discovered to the UI using a View Holder Pattern
public class CustomListAdapter extends BaseAdapter {

    List<String> neblinas;
    private Context context;
    BLEDeviceScanActivity p;
    private LayoutInflater inflater;

    //Constructor
    public CustomListAdapter(BLEDeviceScanActivity parent, Context mainActivity, List<String> neblinaDevices) {
        p = parent;
        context=mainActivity;
        neblinas=neblinaDevices;
        inflater = ( LayoutInflater )context.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount() {
        return neblinas.size();
    }

    public void add(String neblinaString){
        neblinas.add(neblinaString);
    }

    @Override
    public Object getItem(int position) {
        return neblinas.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }


    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        ViewHolder holder;

        //Build a new View if needed
        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.custom_neblina_select_device_row,null);
            holder = new ViewHolder();
            holder.tv = (TextView) convertView.findViewById(R.id.custom_neblina_list_text);
            convertView.setTag(holder);
        } else {
            //Get the existing view
            holder = (ViewHolder) convertView.getTag();
        }

        holder.tv.setText(neblinas.get(position));

        //Set the click listener calling the BLE Activity's onListItemClick function
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewHolder temp = (ViewHolder) v.getTag();
                p.onListItemClick(temp.tv.getText().toString());
            }
        });
        return convertView;
    }

    //This will use the View Holder pattern
    public class ViewHolder
    {
        TextView tv;
    }
}
