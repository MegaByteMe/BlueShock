package team7.blueshock;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
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

    private BluetoothAdapter mBLEAdapter = BluetoothAdapter.getDefaultAdapter();
    private final String ble_not_supported = "Bluetooth Low Energy capability could not be located";
    private static int REQUEST_ENABLE_BT = 1;

    private ListView scanList;
    public ArrayAdapter<String> adap;
    public ArrayList<String> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        scanList = (ListView) findViewById(R.id.scanView);
        adap = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
        scanList.setAdapter(adap);

        scanList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int itemPosition = position;
                String itemValue = (String) scanList.getItemAtPosition(position);
            }
        });

        initBLE();
        scanBLE();
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
        Log.d("Blue", "Starting Discovery");

        bleReceiver mRx = new bleReceiver();
        IntentFilter ifilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mRx, ifilter);

        Log.d("Blue", "BLE Scan Complete.");
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

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, ble_not_supported, Toast.LENGTH_LONG).show();
            finish();
        }

        // Init - Bluetooth
        // TODO need better error returns for wrong OS / hardware
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        // If BLE is not enabled, Request Enable
        if (mBLEAdapter == null || !mBLEAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        Log.d("Blue", "BLE Init Complete.");
    }

    private class bleReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                list.add(device.getName() + " - " + device.getAddress());
                adap.notifyDataSetChanged();
                Log.d("Blue", "Found: " + device.getName() + " - " + device.getAddress());
            }
        }
    }
}