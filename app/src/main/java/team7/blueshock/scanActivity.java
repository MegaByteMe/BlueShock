package team7.blueshock;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import java.util.ArrayList;

public class scanActivity extends AppCompatActivity {

    private BluetoothAdapter mBLEAdapter;
    private bleReceiver mRx;
    private final String ble_not_supported = "Bluetooth Low Energy capability could not be located";
    private int REQUEST_ENABLE_BT = 1;

    private ListView scanList;
    public ArrayAdapter<BluetoothDevice> adap;
    public ArrayList<BluetoothDevice> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        //Rescan has some duplication bugs - feature may not be need disabling for now
        findViewById(R.id.rescanBtn).setEnabled(false);

        scanList = (ListView) findViewById(R.id.scanView);
        adap = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
        scanList.setAdapter(adap);

        scanList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int itemPosition = position;
                String itemValue = scanList.getItemAtPosition(position).toString();
                //String itemValue = ((BluetoothDevice)scanList.getItemAtPosition(position)).getAddress();
            }
        });

        BluetoothManager bleManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBLEAdapter = bleManager.getAdapter();

        scanBLE();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Init - Bluetooth
        Log.d("Blue", "Start Init");
        // If BLE is not enabled, Request Enable
        if (mBLEAdapter == null || !mBLEAdapter.isEnabled()) {
            Intent enableBleI = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBleI, REQUEST_ENABLE_BT);
        }
        // Check for LE Support
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, ble_not_supported, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Log.d("Blue", "BLE Init Complete.");
    }

    @Override
    protected void onPause(){
        super.onPause();
        //Kill open handlers
        if (mBLEAdapter.isDiscovering()) mBLEAdapter.cancelDiscovery();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Tear down open connections
        if (mBLEAdapter.isDiscovering()) mBLEAdapter.cancelDiscovery();
        unregisterReceiver(mRx);
    }

    public void cancelBtnClick(View V) {
        restoreMain(false, null);
    }

    public void selBtnClick(View V) {
        // TODO setting pairing in this method is undesirable - this is for UI testing only
        if(scanList.getCheckedItemPosition() >= 0) {
            int j = scanList.getCheckedItemPosition();
            String s;

            BluetoothDevice bDev = ((BluetoothDevice)(scanList.getItemAtPosition(j)));

            //Guard against device null name
            if(bDev.getName() != null) s = bDev.getName();
            else s = "Device is nameless.";

            Log.d("Blue", s + " <-- HERE!");
            Toast.makeText(this, "You selected: " + s, Toast.LENGTH_LONG).show();

            restoreMain(true, bDev);
        }
        else Toast.makeText(this, "Please select a device to connect to!", Toast.LENGTH_LONG).show();
    }

    public void rescanBtnClick( View V ) {
        list.clear();
        adap.clear();
        adap.notifyDataSetChanged();
        scanBLE();
    }

    public void scanBLE() {
        if (mBLEAdapter.isDiscovering()) mBLEAdapter.cancelDiscovery();
        mBLEAdapter.startDiscovery();
        Log.d("Blue", "Starting Discovery...");
        mRx = new bleReceiver();
        IntentFilter ifilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mRx, ifilter);
        Log.d("Blue", "Discovery Complete.");
        // TODO : kill this receiver, it is causing memory leaks
    }

    public void restoreMain(boolean p, BluetoothDevice fDev) {
        Bundle xtra = getIntent().getExtras();
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("BLUE", fDev);
        i.putExtra("PAIRED", p);

        if(mBLEAdapter.isDiscovering()) mBLEAdapter.cancelDiscovery();

        if(fDev != null) setResult(RESULT_OK, i);
        else setResult(RESULT_CANCELED);
        finish();
    }

    private class bleReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                list.add(device);
                adap.notifyDataSetChanged();
                Log.d("Blue", "Found: " + device.getName() + "@" + device.getAddress());
            }
        }
    }

}