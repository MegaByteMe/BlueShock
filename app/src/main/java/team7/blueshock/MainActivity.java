/*
Low Power Wireless Shock Detection System
Developed by Team7

Codename: BlueShock
Revision:0
Change:5

Changelog:
    1. Built generic UI with SeekBar, Text, and Single Button
    2. Integrated listener for SeekBar and tied to updated text feedback in UI
    3. Added OS version and BLE capability checks, tied buttons to methods, and added bluetooth enable

Needs:
Oversights:
Pitfalls:
*/

package team7.blueshock;

        import android.bluetooth.BluetoothAdapter;
        import android.bluetooth.BluetoothDevice;
        import android.bluetooth.BluetoothManager;
        import android.content.Context;
        import android.content.Intent;
        import android.content.pm.PackageManager;
        import android.os.Build;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.util.Log;
        import android.view.View;
        import android.widget.Button;
        import android.widget.TextView;
        import android.widget.Toast;
        import java.util.Set;

public class MainActivity extends AppCompatActivity {
    public boolean PAIR = false;
    public int SHKVAL = 0;

    private final String ble_not_supported = "Bluetooth Low Energy capability could not be located";

    private static int REQUEST_ENABLE_BT = 1, REQUEST_SCAN_BT = 2, REQUEST_CONFIG_BT = 3;

    private BluetoothAdapter mBleAdap;   // <-adjustment
    private BluetoothManager btManager;

    private TextView shkSetTxtView, devTxtView, axisXTxtView, axisYTxtView, axisZTxtView;

    Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // OS Catch - Ensure minimum OS version that supports BLE
        if (Build.VERSION.SDK_INT < 18) {
            // Detect if OS is lower then SDK 18 - first release supporting BLE
            // If so terminate application - BLE does not exist on this platform
            // TODO need graceful application termination and notification dialog
            Toast.makeText(this, "Android Version Not Supported, Requires Kitkat or higher.", Toast.LENGTH_LONG ).show();
            finish();
        }

        btn = (Button) findViewById(R.id.setBtn);
        shkSetTxtView = (TextView) findViewById(R.id.shkSetTxtView);
        devTxtView = (TextView) findViewById(R.id.devTxtView);
        axisXTxtView = (TextView) findViewById(R.id.axisXTxtView);
        axisYTxtView = (TextView) findViewById(R.id.axisYTxtView);
        axisZTxtView = (TextView) findViewById(R.id.axisZTxtView);

        axisXTxtView.setVisibility(View.INVISIBLE);
        axisYTxtView.setVisibility(View.INVISIBLE);
        axisZTxtView.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("Blue", "onResume...");

        Bundle xtra = getIntent().getExtras();

        if (getIntent().hasExtra("PAIRED")) PAIR = xtra.getBoolean("PAIRED");
        else PAIR = false;
        progBtnCntl(PAIR);

        if (getIntent().hasExtra("SVAL")) {
            SHKVAL = xtra.getInt("SVAL");
            shkSetTxtView.setText(Integer.toString(SHKVAL));
        }

        if(getIntent().hasExtra("AXIS")) {
            boolean b[] = new boolean[3];
            b = xtra.getBooleanArray("AXIS");
            if (b[0]) axisXTxtView.setVisibility(View.VISIBLE);
            if (b[1]) axisYTxtView.setVisibility(View.VISIBLE);
            if (b[2]) axisZTxtView.setVisibility(View.VISIBLE);
        }

        //Protect against emulator crashes, checking for hardware that doesnt exist
        //if(!DEBUG) {
            // Hardware Catch - Determine if hardware has BLE capability
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(this, ble_not_supported, Toast.LENGTH_LONG).show();
                finish();
            }

            // Init - Bluetooth
            // TODO need better error returns for wrong OS / hardware
            //final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBleAdap = btManager.getAdapter();

            // If BLE is not enabled, Request Enable
            if (mBleAdap == null || !mBleAdap.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        //}

        Set<BluetoothDevice> paired = mBleAdap.getBondedDevices();
        if (paired.size() > 0) {
            for (BluetoothDevice device : paired) {
                String BTDevName = device.getName();
                String BTDevAddr = device.getAddress();
                Log.d("Blue", BTDevName + " - " + BTDevAddr);
                }
            }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("Blue", "Entered Result Section");
        if(requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == RESULT_OK) {
                Log.d("BLUE", "REQUEST_ENABLE_BT returned");
            }
        }
        else if(requestCode == REQUEST_SCAN_BT) {
            if(resultCode == RESULT_OK){
                Log.d("Blue", "scan returned good");
                BluetoothDevice foundDev = data.getParcelableExtra("BLUE");
                PAIR = true;
                progBtnCntl(PAIR);
                Log.d("BLUE", "Entered code return");
                if(foundDev != null) {
                    Log.d("BLUE", foundDev.getName() + " - " + foundDev.getAddress());
                    devTxtView.setText(foundDev.getName());
                    //foundDev.createBond();
                }
            }
        }
        else if(requestCode == REQUEST_CONFIG_BT) {
            if(resultCode == RESULT_OK) {
                Log.d("Blue", "REQUEST_CONFIG_BT returned");
                Bundle xtra = data.getExtras();
                this.getIntent().putExtras(xtra);
            }
        }
    }

    public void scanBtnClick( View V ) {
        Intent i = new Intent(this, scanActivity.class);
        startActivityForResult(i, REQUEST_SCAN_BT);
    }

    public void conBtnClick( View V ) {
        Intent i = new Intent(this, ConActivity.class);
        startActivityForResult(i, REQUEST_CONFIG_BT);
    }

    public void prgBtnClick( View V ) {

    }

    private void progBtnCntl( boolean e ) {
        btn.setEnabled(e);
        btn.refreshDrawableState();
    }

    // DEBUG ROUTINES - NOT FOR FINAL PRODUCTION
    public void DBGkill( View V ) {
        // TODO Remove for final version
        Intent i = new Intent(this, Developer.class);
        startActivity(i);
        finish();
    }
}
