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

    // Allows UI emulation and testing in android studio
    // true - disables ble/bt checks
    // false - allows ble/bt checks - used for device testing / production release
    static boolean DEBUG = true;                    // Allows emulation for android studio

    private TextView barText;                           // gShock threshold value feedback
    //private static String ble_not_supported = "Bluetooth Low Energy capability could not be located";
    private static int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final String ble_not_supported = "Bluetooth Low Energy capability could not be located";
        barText = (TextView) findViewById(R.id.threshText);
        SeekBar sBar = (SeekBar) findViewById(R.id.shockBar); // gShock threshold user input element
        Button sButt = (Button) findViewById(R.id.button);

        if(!DEBUG) {
            // OS Catch - Ensure minimum OS version that supports BLE
            if (Build.VERSION.SDK_INT < 18) {
                // Detect if OS is lower then SDK 18 - first release supporting BLE
                // If so terminate application - BLE does not exist on this platform
                // TODO need graceful application termination and notification dialog
                finish();
            }

            // Hardware Catch - Determine if hardware has BLE capability
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(this, ble_not_supported, Toast.LENGTH_SHORT).show();
                finish();
            }

            // Init - Bluetooth
            // TODO need better error returns for wrong OS / hardware
            final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter myBTAdapter = bluetoothManager.getAdapter();

            // Hardware Catch - Determine if Bluetooth is enabled, if not request enable
            if (myBTAdapter == null || !myBTAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
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

    public void prgBtnClick( View V) {
        Toast.makeText(this, "you pushed my button " + barText.getText().toString(), Toast.LENGTH_SHORT).show();
    }

    public void scanBtnClick( View V ) {
        //startActivity(new Intent(MainActivity.this, scanActivity.class));

        Toast.makeText(this, "you pushed the scan button ", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, scanActivity.class);
        startActivity(intent);
    }

    public void DBGkill( View V ) {
        // Implement button to kill the application to assist in app testing
        // TODO Remove for final code version
        finish();
    }

}
