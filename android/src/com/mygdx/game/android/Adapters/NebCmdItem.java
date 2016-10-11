package com.mygdx.game.android.Adapters;

/**
 * Created by scott on 2016-10-11.
 */

public class NebCmdItem {
    public byte mSubSysId;
    public byte mCmdId;
    public String mName;
    public int mActuator;
    public String mText;

    public NebCmdItem(byte SubSys, byte CmdId, String Name, int Actuator, String Text) {
        mSubSysId = SubSys;
        mCmdId = CmdId;
        mName = Name;
        mActuator = Actuator;
        mText = Text;
    }
}
