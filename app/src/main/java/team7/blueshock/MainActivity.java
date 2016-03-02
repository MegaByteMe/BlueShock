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
        import android.support.annotation.Nullable;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.util.Log;
        import android.view.View;
        import android.widget.Button;
        import android.widget.CheckBox;
        import android.widget.SeekBar;
        import android.widget.TextView;
        import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    // Allows UI emulation and testing in android studio
    // true - disables ble/bt checks
    // false - allows ble/bt checks - used for device testing / production release
    private static boolean DEBUG = true;

    public boolean PAIR = false;
    public int SHKVAL = 0;
    public int CHKBOXS = 0;

    private final String ble_not_supported = "Bluetooth Low Energy capability could not be located";
    private static int REQUEST_ENABLE_BT = 1;
    private TextView barText;
    private SeekBar sBar;
    private CheckBox xBox;
    private CheckBox yBox;
    private CheckBox zBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        barText = (TextView) findViewById(R.id.threshText);
        sBar = (SeekBar) findViewById(R.id.shockBar); // gShock threshold user input element
        xBox = (CheckBox) findViewById(R.id.xchkBox);
        yBox = (CheckBox) findViewById(R.id.ychkBox);
        zBox = (CheckBox) findViewById(R.id.zchkBox);

        // Disable Program Device button since no BLE devices are paired yet
        findViewById(R.id.prgBtn).setEnabled(false);

        // OS Catch - Ensure minimum OS version that supports BLE
        if (Build.VERSION.SDK_INT < 18) {
            // Detect if OS is lower then SDK 18 - first release supporting BLE
            // If so terminate application - BLE does not exist on this platform
            // TODO need graceful application termination and notification dialog
            Toast.makeText(this, "Android Version Not Supported, Requires Kitkat or higher.", Toast.LENGTH_LONG ).show();
            finish();
        }

        //Protect against emulator crashes, checking for hardware that doesnt exist
        if(!DEBUG) {
            // Hardware Catch - Determine if hardware has BLE capability
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(this, ble_not_supported, Toast.LENGTH_LONG).show();
                finish();
            }

            // Init - Bluetooth
            // TODO need better error returns for wrong OS / hardware
            final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter myBTAdapter = bluetoothManager.getAdapter();

            // If BLE is not enabled, Request Enable
            if (myBTAdapter == null || !myBTAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Init Checkboxes
        xBox.setChecked(false);
        yBox.setChecked(false);
        zBox.setChecked(false);

        // UI Operation - Setup listener for user modifying the seek bar
        sBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Auto Generated Stub
                barText.setText(Integer.toString(progress));
                SHKVAL = progress;
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

    protected void onResume() {
        super.onResume();
        Log.d("Blue", "onResume...");

        Bundle xtra = getIntent().getExtras();

        if( getIntent().hasExtra("PAIRED") ) PAIR = xtra.getBoolean("PAIRED");
        progBtnCntl(PAIR);

        if(getIntent().hasExtra("SVAL")) {
            SHKVAL = xtra.getInt("SVAL");
            sBar.setProgress(xtra.getInt("SVAL"));
        }

        if(getIntent().hasExtra("AXIS")) fixBoxs(xtra.getBooleanArray("AXIS"));
    }

    public void prgBtnClick( View V) {
        Toast.makeText(this, "you pushed my button " + barText.getText().toString(), Toast.LENGTH_SHORT).show();
    }

    public void scanBtnClick( View V ) {
        Intent i = new Intent(this, scanActivity.class);
        i.putExtra("SVAL", SHKVAL);
        i.putExtra("AXIS", chkBoxs());
        startActivity(i);
        finish();
    }

    private void progBtnCntl( boolean e ) {
        Button btn = (Button) findViewById(R.id.prgBtn);
        btn.setEnabled(e);
        btn.refreshDrawableState();
    }

    private boolean[] chkBoxs() {
        return ( new boolean[]{ xBox.isChecked(), yBox.isChecked(), zBox.isChecked() } );
    }

    private void fixBoxs( boolean[] b) {
        xBox.setChecked(b[0]);
        yBox.setChecked(b[1]);
        zBox.setChecked(b[2]);
        xBox.refreshDrawableState();
        yBox.refreshDrawableState();
        zBox.refreshDrawableState();
        Log.d("Blue", "fixBoxs: " + Boolean.toString(b[0]) + Boolean.toString(b[1]) + Boolean.toString(b[2]));
    }

    // DEBUG ROUTINES - NOT FOR FINAL PRODUCTION
    public void DBGkill( View V ) {
        // TODO Remove for final version
        Intent i = new Intent(this, Developer.class);
        i.putExtra("SVAL", SHKVAL);
        i.putExtra("AXIS", chkBoxs());
        startActivity(i);
        finish();
    }
}
