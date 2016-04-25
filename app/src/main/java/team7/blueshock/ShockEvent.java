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

public class ShockEvent implements Parcelable{
    private final int id, sThreshold, axisBox;
    private final String dateTime;
    private int[] xData, yData, zData;

    public int getId() {
        return id;
    }

    public int getsThreshold() { return sThreshold; }

    public int[] getxData() { return xData; }

    public int[] getyData() { return yData; }

    public int[] getzData() { return zData; }

    public void setxData(int[] xData) { this.xData = xData; }

    public void setyData(int[] yData) { this.yData = yData; }

    public void setzData(int[] zData) { this.zData = zData; }

    public String getDateTime() { return dateTime; }

    public ShockEvent(int shockID, int shockThreshold, String dateAndTime, int axisBox) {
        this.id = shockID;
        this.sThreshold = shockThreshold;
        this.dateTime = dateAndTime;
        this.axisBox = axisBox;
    }

    public ShockEvent(ShockEvent shockEvent) {
        this.id = shockEvent.getId();
        this.sThreshold = shockEvent.getsThreshold();
        this.dateTime = shockEvent.getDateTime();
        this.axisBox = shockEvent.getAxisBox();
    }

    public int[] pullData() {
        //TODO finish handling multiple planes
        switch(axisBox) {
            case 7:
                // X, Y, and Z place selected
                break;
            case 6:
                // X and Y plane selected
                break;
            case 5:
                // X and Z plane selected
                break;
            case 4:
                // X plane selected
                return xData;
            case 3:
                // Y and Z plane selected
                break;
            case 2:
                // Y plane selected
                return yData;
            case 1:
                // Z plane selected
                return zData;
        }
        return new int[]{0};
    }

    public int getAxisBox() { return axisBox; }

    protected ShockEvent(Parcel parcel) {
        id = parcel.readInt();
        sThreshold = parcel.readInt();
        axisBox = parcel.readInt();
        dateTime = parcel.readString();
        xData = parcel.createIntArray();
        yData = parcel.createIntArray();
        zData = parcel.createIntArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(sThreshold);
        dest.writeInt(axisBox);
        dest.writeString(dateTime);
        dest.writeIntArray(xData);
        dest.writeIntArray(yData);
        dest.writeIntArray(zData);
    }

    public static final Parcelable.Creator<ShockEvent> CREATOR = new Parcelable.Creator<ShockEvent>() {
        @Override
        public ShockEvent createFromParcel(Parcel parcel) {
            return new ShockEvent(parcel);
        }

        @Override
        public ShockEvent[] newArray(int size) {
            return new ShockEvent[size];
        }
    };
}
