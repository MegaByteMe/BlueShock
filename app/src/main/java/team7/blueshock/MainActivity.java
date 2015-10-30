/*
Low Power Wireless Shock Detection System
Developed by Team7

Codename: BlueShock
Revision:0
Change:3

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
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private TextView barText;                           // Text threshold bar value feedback
    private SeekBar sBar;                               // SeekBar which sets Threshold value
    private Button sButt;
    private BluetoothAdapter mBluetoothAdapter;
    private static String ble_not_supported = "Bluetooth Low Energy capability could not be located";
    private static int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        barText = (TextView) findViewById(R.id.threshText);
        sBar = (SeekBar) findViewById(R.id.shockBar);
        sButt = (Button) findViewById(R.id.button);

        // OS Catch - Ensure minimum OS version that supports BLE
        if(Build.VERSION.SDK_INT < 18) {
            // Detect if OS is lower then SDK 18 - first release supporting BLE
            // If so terminate application - BLE does not exist on this platform
            // TODO need graceful application termination and notification dialog
            finish();
        }

        // Init - Bluetooth
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Hardware Catch - Determine if hardware has BLE capability
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            //Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "error", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Hardware Catch - Determine if Bluetooth is enabled, if not request enable
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // UI Operation - Setup listener for user modifying the seek bar
        sBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Auto Generated Stub
                barText.setText(Integer.toString(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //Auto Generated Stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //Auto Generated Stub
            }
        });

    }

    public void buttonClick( View V ) {
        Toast.makeText(this, new StringBuilder().append("you pushed my button ").append(barText.getText()).toString(), Toast.LENGTH_LONG).show();
    }

    public void DBGkill( View V ) {
        // Implement button to kill the application to assist in app testing
        // TODO Remove for final code version
        finish();
    }

}
