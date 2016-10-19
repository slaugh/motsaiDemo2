package com.mygdx.game.android.NeblinaClasses;

import com.mygdx.game.android.NeblinaClasses.NebDeviceDetailFragment;

/**
 * Created by scott on 2016-05-11.
 */
public class AndroidGetQ implements com.mygdx.game.Invaders.InvaderInterface {

    public int shipNumber;

    public AndroidGetQ(int shipNumber) {
        this.shipNumber = shipNumber;
    }

    @Override
    public double getQ0() {

        if(shipNumber ==1)        return NebDeviceDetailFragment.latest_Q0;
        else return NebDeviceDetailFragment.latest_Q0_2;
    }

    @Override
    public double getQ1() {
        if(shipNumber ==1)        return NebDeviceDetailFragment.latest_Q1;
        else return NebDeviceDetailFragment.latest_Q1_2;
    }

    @Override
    public double getQ2() {
        if(shipNumber ==1)        return NebDeviceDetailFragment.latest_Q2;
        else return NebDeviceDetailFragment.latest_Q2_2;
    }

    @Override
    public double getQ3() {
        if(shipNumber ==1)        return NebDeviceDetailFragment.latest_Q3;
        else return NebDeviceDetailFragment.latest_Q3_2;
    }

    @Override
    public long getTimestamp() {
        return NebDeviceDetailFragment.timestamp_N;
    }
}
