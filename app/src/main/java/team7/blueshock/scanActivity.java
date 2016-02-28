package team7.blueshock;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.*;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.util.Log;
import java.util.List;

public class scanActivity extends AppCompatActivity {

    //final BluetoothManager bleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    private BluetoothAdapter mBLEAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothLeScanner mBLEScanner;
    private ScanCallback mScanCallb;
    private boolean mScanning;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    //mBLEAdapter.stopLeScan(mLeScanCallback);
                    mBLEAdapter.startDiscovery();
                }
            }, SCAN_PERIOD);
        }
    }

    public void cancelBtnClick(View V) {
        restoreMain(false);
    }

    public void selBtnClick(View V) {
        // TODO setting pairing in this method is undesirable - this is for UI testing only
        Log.d("Blue", "Select pushed, returning to Main.");
        restoreMain(true);
    }

    public void scanBLE() {
        if (mBLEAdapter.isDiscovering()) {
            mBLEAdapter.cancelDiscovery();
        }

        mBLEAdapter.startDiscovery();
    }

    public void restoreMain(boolean p) {
        Bundle xtra = getIntent().getExtras();
        int val = 0;
        boolean[] b = new boolean[]{ false, false, false };
        Intent i = new Intent(this, MainActivity.class);

        if(xtra.containsKey("SVAL")) val = xtra.getInt("SVAL");
        if(xtra.containsKey("AXIS")) b = xtra.getBooleanArray("AXIS");
        i.putExtra("PAIRED", p);
        i.putExtra("SVAL", val);
        i.putExtra("AXIS", b);

        startActivity(i);
        finish();
    }
}