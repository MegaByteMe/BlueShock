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
    private float[] xData, yData, zData;

    public int getId() {
        return id;
    }

    public int getsThreshold() { return sThreshold; }

    public float[] getxData() { return xData; }

    public float[] getyData() { return yData; }

    public float[] getzData() { return zData; }

    public void setxData(float[] xData) { this.xData = xData; }

    public void setxData(int[] xData) {
        for(int i = 0; i < xData.length; i++) this.xData[i] = xData[i];
    }

    public void setyData(float[] yData) { this.yData = yData; }

    public void setyData(int[] yData) {
        for(int i = 0; i < yData.length; i++) this.yData[i] = yData[i];
    }

    public void setzData(float[] zData) { this.zData = zData; }

    public void setzData(int[] zData) {
        for(int i = 0; i < zData.length; i++) this.zData[i] = zData[i];
    }

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

    public float[] pullData() {
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
        return new float[]{0};
    }

    public int getAxisBox() { return axisBox; }

    private ShockEvent(Parcel parcel) {
        id = parcel.readInt();
        sThreshold = parcel.readInt();
        axisBox = parcel.readInt();
        dateTime = parcel.readString();
        xData = parcel.createFloatArray();
        yData = parcel.createFloatArray();
        zData = parcel.createFloatArray();
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
        dest.writeFloatArray(xData);
        dest.writeFloatArray(yData);
        dest.writeFloatArray(zData);
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
