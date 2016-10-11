package com.mygdx.game.android;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Created by scott on 2016-10-07.
 */

public class CustomListAdapter extends BaseAdapter {

    List<String> neblinas;
    private Context context;
    int [] imageId;
    BLEDeviceScanActivity p;
    private static LayoutInflater inflater=null;

    //Constructor
    public CustomListAdapter(BLEDeviceScanActivity parent, Context mainActivity, List<String> neblinaDevices) {
        // TODO Auto-generated constructor stub
        p = parent;
        context=mainActivity;
        neblinas=neblinaDevices;
        inflater = ( LayoutInflater )context.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return neblinas.size();
    }

    public void add(String neblinaString){
        Log.w("CUSTOM LIST ADAPTOR", "RANDOM ADD FUNCTION CALLED");
        neblinas.add(neblinaString);
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return neblinas.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
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

        //TODO: Implement on click listener
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//
                ViewHolder temp = (ViewHolder) v.getTag();
                p.onListItemClick(temp.tv.getText().toString());

//                ListView l, View v, int position, long id
                Toast.makeText(context, "You Clicked "+neblinas.get(position), Toast.LENGTH_LONG).show();
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
