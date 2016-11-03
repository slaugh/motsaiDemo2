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

import com.mygdx.game.android.NeblinaClasses.NebDeviceDetailFragment;
import com.mygdx.game.android.R;


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

    //Create the button on the screen when it comes into view on the UI
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;

        if (view == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            view = inflater.inflate(R.layout.nebcmd_item, null);
        }

        NebCmdItem item = getItem(position);

        //Setup all all the types but INVISIBLE
        if (item != null) {
            TextView label = (TextView) view.findViewById(R.id.textView);
            label.setText(item.mName);

            Switch aSwitch = (Switch)view.findViewById(R.id.switch1);
            aSwitch.setVisibility(View.INVISIBLE);
            aSwitch.setTag(-1);
//            aSwitch.isActivated();
//            aSwitch.setActivated();

            Button button = (Button) view.findViewById(R.id.button);
            button.setVisibility(View.INVISIBLE);
            button.setTag(-1);
            button.setText(item.mText);
//            button.isActivated();
//            button.setActivated();

            TextView textView = (TextView) view.findViewById(R.id.textField);
            textView.setVisibility(View.INVISIBLE);
            textView.setTag(-1);

            //Depending on the type, set it and make it VISIBLE
            switch (item.mActuator) {
                case 1: // Switch
                    aSwitch.setVisibility(View.VISIBLE);
                    aSwitch.setTag(position);
                    aSwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked)
                        {
                            ViewParent viewParent = compoundButton.getParent().getParent().getParent();
                            if (viewParent instanceof ListView) {
                                ListView lv = (ListView) viewParent;
                                NebDeviceDetailFragment fragment = (NebDeviceDetailFragment)lv.getTag();
                                if (fragment != null) {
                                   fragment.onSwitchButtonChanged(compoundButton, isChecked);
                                }
                            }
                        }
                    });

                    break;
                case 2: // Button
                    button.setVisibility(View.VISIBLE);
                    button.setTag(position);
                    button.setOnClickListener(new OnClickListener() {
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
                    textView.setVisibility(View.VISIBLE);
                    textView.setTag(position);
                    break;
            }
        }

        return view;
    }
}
