package team7.blueshock;

import android.app.Activity;
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
    private final String ble_not_supported = "Bluetooth Low Energy capability could not be located";
    private static int REQUEST_ENABLE_BT = 1;

    private ListView scanList;
    //public ArrayAdapter<String> adap;
    //public ArrayList<String> list = new ArrayList<>();
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
                //String itemValue = (String) scanList.getItemAtPosition(position);
                String itemValue = scanList.getItemAtPosition(position).toString();
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
    }

    public void cancelBtnClick(View V) {
        restoreMain(false);
    }

    public void selBtnClick(View V) {
        // TODO setting pairing in this method is undesirable - this is for UI testing only
        Log.d("Blue", "Select pushed.");
        if(scanList.getCheckedItemPosition() >= 0) {
            int j = scanList.getCheckedItemPosition();
            String s = new String(((BluetoothDevice) scanList.getItemAtPosition(j)).getName());
            //String s = new String((BluetoothDevice)(scanList.getItemAtPosition(j)));
            Log.d("Blue", s + " <-- HERE!");
            Toast.makeText(this, "You selected: " + s, Toast.LENGTH_LONG).show();
        }
        else Toast.makeText(this, "Please select a device to connect to!", Toast.LENGTH_LONG).show();
        //restoreMain(true);
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
        bleReceiver mRx = new bleReceiver();
        IntentFilter ifilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mRx, ifilter);
        Log.d("Blue", "Discovery Complete.");
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

        if(mBLEAdapter.isDiscovering()) mBLEAdapter.cancelDiscovery();

        startActivity(i);
        finish();
    }

    public void initBLE() {
        Log.d("Blue", "Start Init");
        // Init - Bluetooth
        // If BLE is not enabled, Request Enable
        if (mBLEAdapter == null || !mBLEAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, ble_not_supported, Toast.LENGTH_LONG).show();
            finish();
        }
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        Log.d("Blue", "BLE Init Complete.");
    }

    private class bleReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //list.add(device.getName() + " - " + device.getAddress());
                list.add(device);
                adap.notifyDataSetChanged();
                Log.d("Blue", "Found: " + device.getName() + " - " + device.getAddress());
            }
        }
    }

}