package com.mygdx.game.android.Adapters;
import android.support.v4.app.FragmentActivity;
import android.view.ViewParent;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.content.Context;
import java.util.List;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.TableRow;
import android.app.Fragment;
import android.widget.CompoundButton;

import com.motsai.neblina.NebCmdItem;
import com.motsai.neblina.Neblina;

/**
 * Created by hoanmotsai on 2016-08-03.
 */
public class NebListAdapter extends ArrayAdapter<NebCmdItem> {
    public NebListAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public NebListAdapter(Context context, int resource, NebCmdItem[] items) {
        super(context, resource, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.nebcmd_item, null);
        }

        NebCmdItem p = getItem(position);

        if (p != null) {
            TextView label = (TextView) v.findViewById(R.id.textView);
            label.setText(p.mName);

            Switch c = (Switch)v.findViewById(R.id.switch1);
            c.setVisibility(View.INVISIBLE);
            c.setTag(-1);
            Button b = (Button) v.findViewById(R.id.button);
            b.setVisibility(View.INVISIBLE);
            b.setTag(-1);
            TextView t = (TextView) v.findViewById(R.id.textField);
            t.setVisibility(View.INVISIBLE);
            t.setTag(-1);
//            b.setId(300 + i);

            switch (p.mActuator) {
                case 1: // Switch
                    c.setVisibility(View.VISIBLE);
                    c.setTag(position);
                    c.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton vi, boolean isChecked)
                        {
                           // int idx = (int)vi.getTag();

                            ViewParent vp = vi.getParent().getParent().getParent();
                            if (vp instanceof ListView) {
                                ListView lv = (ListView) vp;
                                NebDeviceDetailFragment fr = (NebDeviceDetailFragment)lv.getTag();
                                if (fr != null) {
                                   fr.onSwitchButtonChanged(vi, isChecked);
                                }
                            }
                        }
                    });

                    break;
                case 2: // Button
                    b.setVisibility(View.VISIBLE);
                    b.setTag(position);
                    b.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View vi)
                        {
                           // int idx = (int)vi.getTag();
                            ViewParent vp = vi.getParent().getParent().getParent();
                            if (vp instanceof ListView) {
                                ListView lv = (ListView) vp;
                                NebDeviceDetailFragment fr = (NebDeviceDetailFragment)lv.getTag();
                                if (fr != null) {
                                    fr.onButtonClick(vi);
                                }
                            }
                        }
                    });
                    break;
                case 3: // Text field
                    t.setVisibility(View.VISIBLE);
                    t.setTag(position);

                    break;
            }
        }

        return v;
    }
}
