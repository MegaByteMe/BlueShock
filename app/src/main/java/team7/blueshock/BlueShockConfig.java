/*
Low Power Wireless Shock Detection System
Developed by Team7

Codename: BlueShock
Revision:1
Change:1

Notes:

*/

package team7.blueshock;

import android.os.Parcel;
import android.os.Parcelable;

public class BlueShockConfig implements Parcelable{
    private int shockThreshold, AxisBox, getShockID;
    private boolean SETUP, PAIRED, xBoxSet, yBoxSet, zBoxSet;

    public boolean isPAIRED() { return PAIRED; }

    public boolean isSETUP() { return SETUP; }

    public int getAxisBox() { return AxisBox; }

    public int getShockThreshold() { return shockThreshold; }

    public void setAxisBox(int axisBox) {
        AxisBox = axisBox;
        axisTranslation();
    }

    public void setPAIRED(boolean PAIRED) { this.PAIRED = PAIRED; }

    public void setSETUP(boolean SETUP) { this.SETUP = SETUP; }

    public void setShockThreshold(int shockThreshold) { this.shockThreshold = shockThreshold; }

    public int getGetShockID() { return getShockID; }

    public int generateShockID() { return ++getShockID; }

    public boolean isxBoxSet() { return xBoxSet; }

    public boolean isyBoxSet() { return yBoxSet; }

    public boolean iszBoxSet() { return zBoxSet; }

    public void setxBoxSet(boolean xBoxSet) { this.xBoxSet = xBoxSet; AxisBox = AxisBox ^ 0b0100; }

    public void setyBoxSet(boolean yBoxSet) { this.yBoxSet = yBoxSet; AxisBox = AxisBox ^ 0b0010; }

    public void setzBoxSet(boolean zBoxSet) { this.zBoxSet = zBoxSet; AxisBox = AxisBox ^ 0b0001; }

    public BlueShockConfig() {
        this.shockThreshold = 0;
        this.AxisBox = 0;
        this.SETUP = false;
        this.PAIRED = false;
        this.getShockID = 0;
        this.xBoxSet = false;
        this.yBoxSet = false;
        this.zBoxSet = false;
    }

    public BlueShockConfig(BlueShockConfig bConfig) {
        this.shockThreshold = bConfig.getShockThreshold();
        this.AxisBox = bConfig.getAxisBox();
        this.SETUP = bConfig.isSETUP();
        this.PAIRED = bConfig.isPAIRED();
        this.getShockID = bConfig.getGetShockID();
        this.xBoxSet = bConfig.isxBoxSet();
        this.yBoxSet = bConfig.isyBoxSet();
        this.zBoxSet = bConfig.iszBoxSet();
    }

    private void axisTranslation() {
        switch(AxisBox) {
            case 7:
                // X, Y, and Z place selected
                setxBoxSet(true);
                setyBoxSet(true);
                setzBoxSet(true);
                break;
            case 6:
                // X and Y plane selected
                setxBoxSet(true);
                setyBoxSet(true);
                setzBoxSet(false);
                break;
            case 5:
                // X and Z plane selected
                setxBoxSet(true);
                setyBoxSet(false);
                setzBoxSet(true);
                break;
            case 4:
                // X plane selected
                setxBoxSet(true);
                setyBoxSet(false);
                setzBoxSet(false);
                break;
            case 3:
                // Y and Z plane selected
                setxBoxSet(false);
                setyBoxSet(true);
                setzBoxSet(true);
                break;
            case 2:
                // Y plane selected
                setxBoxSet(false);
                setyBoxSet(true);
                setzBoxSet(false);
                break;
            case 1:
                // Z plane selected
                setxBoxSet(false);
                setyBoxSet(false);
                setzBoxSet(true);
                break;
        }
    }

    protected BlueShockConfig(Parcel in) {
        shockThreshold = in.readInt();
        AxisBox = in.readInt();
        getShockID = in.readInt();
        SETUP = in.readByte() != 0x00;
        PAIRED = in.readByte() != 0x00;
        xBoxSet = in.readByte() != 0x00;
        yBoxSet = in.readByte() != 0x00;
        zBoxSet = in.readByte() != 0x00;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(shockThreshold);
        dest.writeInt(AxisBox);
        dest.writeInt(getShockID);
        dest.writeByte((byte) (SETUP ? 0x01 : 0x00));
        dest.writeByte((byte) (PAIRED ? 0x01 : 0x00));
        dest.writeByte((byte) (xBoxSet ? 0x01 : 0x00));
        dest.writeByte((byte) (yBoxSet ? 0x01 : 0x00));
        dest.writeByte((byte) (zBoxSet ? 0x01 : 0x00));
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<BlueShockConfig> CREATOR = new Parcelable.Creator<BlueShockConfig>() {
        @Override
        public BlueShockConfig createFromParcel(Parcel in) {
            return new BlueShockConfig(in);
        }

        @Override
        public BlueShockConfig[] newArray(int size) {
            return new BlueShockConfig[size];
        }
    };
}
