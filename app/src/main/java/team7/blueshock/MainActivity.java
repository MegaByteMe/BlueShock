/*
Low Power Wireless Shock Detection System
Developed by Team7

Codename: BlueShock
Revision:1
Change:6

Pitfalls:
Notes:
    Base UUID: 4c1e0000-4c64-41ab-b51f-9797339e4ab7
    Config: 0x0788
    Shock: 0xA53C
    DATA: 0xF6C2
*/

package team7.blueshock;

        import android.app.AlertDialog;
        import android.bluetooth.BluetoothAdapter;
        import android.bluetooth.BluetoothDevice;
        import android.bluetooth.BluetoothGatt;
        import android.bluetooth.BluetoothGattCallback;
        import android.bluetooth.BluetoothGattCharacteristic;
        import android.bluetooth.BluetoothGattDescriptor;
        import android.bluetooth.BluetoothGattService;
        import android.bluetooth.BluetoothManager;
        import android.bluetooth.BluetoothProfile;
        import android.content.Context;
        import android.content.DialogInterface;
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
        import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    // Service UUIDs
    private static final UUID CONFIG_SERV = UUID.fromString("4c1e0788-4c64-41ab-b51f-9797339e4ab7");
    private static final UUID ALERT_SERV = UUID.fromString("4c1ea53c-4c64-41ab-b51f-9797339e4ab7");
    private static final UUID DATA_SERV = UUID.fromString("4c1ef6c2-4c64-41ab-b51f-9797339e4ab7");

    // Configuration Characteristic UUIDs
    private static final UUID CONFIG_THRESH = UUID.fromString("4c1ecfc0-4c64-41ab-b51f-9797339e4ab7");
    private static final UUID CONFIG_AXIS = UUID.fromString("4c1e45a1-4c64-41ab-b51f-9797339e4ab7");

    // Alert Event Characteristic UUID
    private static final UUID ALERT_EVENT = UUID.fromString("4c1e5705-4c64-41ab-b51f-9797339e4ab7");

    // Data Characteristic UUIDs
    private static final UUID DATA_XDATA = UUID.fromString("4c1eabea-4c64-41ab-b51f-9797339e4ab7");
    private static final UUID DATA_YDATA = UUID.fromString("4c1e94d6-4c64-41ab-b51f-9797339e4ab7");
    private static final UUID DATA_ZDATA = UUID.fromString("4c1e9960-4c64-41ab-b51f-9797339e4ab7");
    private static final UUID DATA_TXTOTAL = UUID.fromString("4c1e8393-4c64-41ab-b51f-9797339e4ab7");

    // Generic BLE UUIDs
    private static final UUID GEN_ATTR = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");
    private static final UUID GEN_ACC = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    private static final UUID GEN_CHAR_DNAME = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");
    private static final UUID GEN_CHAR_APP = UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb");
    private static final UUID GEN_CHAR_PPC = UUID.fromString("00002a04-0000-1000-8000-00805f9b34fb");

    private static final UUID CHAR_UPDATE_NOT_DESC = UUID.fromString("4c1e2902-4c64-41ab-b51f-9797339e4ab7");
    private static final UUID CHAR_UPDATE_NOT_DESC2 = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final String ble_not_supported = "Bluetooth Low Energy capability could not be located";

    private static int REQUEST_ENABLE_BT = 1, REQUEST_SCAN_BT = 2, REQUEST_CONFIG_BT = 3, REQUEST_DEV = 4;

    private static final int UINT8_TYPE = 1, UINT16_TYPE = 2;

    public boolean PAIR = false, SETUP = false, ALERT = false;
    public int SHKVAL = 0;
    public static int txTotal = 0;

    private BluetoothAdapter mBleAdap;
    private BluetoothManager btManager;
    private BluetoothGatt myConnectedGatt;

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
            Toast.makeText(this, "Android Version Not Supported, Requires Kitkat or higher.", Toast.LENGTH_LONG).show();
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
        restore_settings(xtra);

        progBtnCntl(PAIR && SETUP);

        // Hardware Catch - Determine if hardware has BLE capability
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, ble_not_supported, Toast.LENGTH_LONG).show();
            finish();
        }

        // Init - Bluetooth
        // TODO need better error returns for wrong OS / hardware
        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBleAdap = btManager.getAdapter();

        // If BLE is not enabled, Request Enable
        if (mBleAdap == null || !mBleAdap.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if(myConnectedGatt != null ) devTxtView.setText(myConnectedGatt.getDevice().getName());

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
    protected void onPause() {
        super.onPause();
        Log.d("Blue", "onPause occurred");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("Blue", "onStop occurred");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.d("BLUE", "REQUEST_ENABLE_BT returned");
            }
        }
        else if (requestCode == REQUEST_SCAN_BT) {
            if (resultCode == RESULT_OK) {
                BluetoothDevice foundDev = data.getParcelableExtra("BLUE");
                PAIR = true;
                progBtnCntl(PAIR && SETUP);

                if (foundDev != null) {
                    Log.d("BLUE", foundDev.getName() + " - " + foundDev.getAddress());
                    devTxtView.setText(foundDev.getName());
                    //foundDev.createBond();
                    myConnectedGatt = foundDev.connectGatt(this, true, myGattCallb);
                }
            }
        }
        else if (requestCode == REQUEST_CONFIG_BT) {
            if (resultCode == RESULT_OK) {
                Log.d("Blue", "REQUEST_CONFIG_BT returned");

                Bundle xtra = data.getExtras();
                getIntent().putExtras(xtra);
                restore_settings(xtra);

                SETUP = true;
                progBtnCntl(PAIR && SETUP);
            }
        }
    }

    private void restore_settings(Bundle xtra) {
        if (getIntent().hasExtra("SVAL")) {
            SHKVAL = xtra.getInt("SVAL");
            shkSetTxtView.setText(Integer.toString(SHKVAL));
        }

        if (getIntent().hasExtra("AXIS")) {
            boolean b[] = new boolean[3];
            b = xtra.getBooleanArray("AXIS");
            if (b[0]) axisXTxtView.setVisibility(View.VISIBLE);
            if (b[1]) axisYTxtView.setVisibility(View.VISIBLE);
            if (b[2]) axisZTxtView.setVisibility(View.VISIBLE);
        }
    }

    public void scanBtnClick(View V) {
        Intent i = new Intent(this, scanActivity.class);
        startActivityForResult(i, REQUEST_SCAN_BT);
    }

    public void conBtnClick(View V) {
        Intent i = new Intent(this, ConActivity.class);
        startActivityForResult(i, REQUEST_CONFIG_BT, getIntent().getExtras());
    }

    public void prgBtnClick(View V) {
        int shock_val = Integer.parseInt(shkSetTxtView.getText().toString());
        int axis_val = 0;

        Log.d("Blue", "Program button pushed");

        if(axisXTxtView.getVisibility() == View.VISIBLE) axis_val |= 0b00000100;
        if(axisYTxtView.getVisibility() == View.VISIBLE) axis_val |= 0b00000010;
        if(axisZTxtView.getVisibility() == View.VISIBLE) axis_val |= 0b00000001;

        wr_char(shock_val, ALERT_SERV, CONFIG_THRESH);
        //wr_char(axis_val, ALERT_SERV, CONFIG_AXIS);
    }

    public void wr_char(int value, UUID serv_UUID, UUID char_UUID) {
        //TODO: need a queueing state machine to control writes and increment on the call backs

        BluetoothGattCharacteristic character = myConnectedGatt.getService(serv_UUID).getCharacteristic(char_UUID);
        int dType = character.getWriteType();
        byte me = (byte) value;
        character.setValue(me, dType, 0);
        myConnectedGatt.writeCharacteristic(character);
        
        Log.d("Blue", "BLE writing " + char_UUID.toString());
    }

    public void progBtnCntl(boolean e) {
        btn.setEnabled(e);
        btn.refreshDrawableState();
    }

    // DEBUG ROUTINES - NOT FOR FINAL PRODUCTION
    public void DBGkill(View V) {
        // TODO Remove for final version
        Intent i = new Intent(this, Developer.class);
        startActivityForResult(i, REQUEST_DEV);
    }

    public void notifyOnAlert() {
        Log.d("Blue", "notify has been called");
        new AlertDialog.Builder(this)
                .setTitle("Shock Event!")
                .setMessage("Shock Event Threshold has been met.")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing for now
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing for now
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .create()
                .show();

        ALERT = false;
    }

    public void write_notification(BluetoothGatt gatt, BluetoothGattCharacteristic character) {
        gatt.setCharacteristicNotification(character, true);
        BluetoothGattDescriptor desc = character.getDescriptor(CHAR_UPDATE_NOT_DESC2);
        desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(desc);
    }

    private BluetoothGattCallback myGattCallb = new BluetoothGattCallback() {
    //private class BLEGattCallback extends BluetoothGattCallback {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);

                if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("Blue", "STATE CONNECTED");
                    gatt.discoverServices();
                }
                else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("Blue", "STATE DISCONNECTED");
                    gatt.disconnect();
                }
                else if (status != BluetoothGatt.GATT_SUCCESS) gatt.disconnect();
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);

                for (BluetoothGattService bluetoothGattService : gatt.getServices()) {
                    Log.d("Blue", "Service is: " + bluetoothGattService.getUuid().toString());
                    if (bluetoothGattService.getUuid().toString().contains("4c1e")) {
                        for (BluetoothGattCharacteristic charact : bluetoothGattService.getCharacteristics()) {
                            UUID checky = charact.getUuid();
                            Log.d("Blue", "Characteristic is: " + checky.toString());

                            //TODO: issue with writing notification descriptor that is causing characteristic writes to fail and connection closed.

                            if(checky.compareTo(DATA_TXTOTAL) == 0) {
                                Log.d("Blue", "Found TXTOTAL");
                                write_notification(gatt, charact);
                            }
                            else if (checky.compareTo(DATA_XDATA) == 0) {
                                Log.d("Blue", "Found XDATA");
                                write_notification(gatt, charact);
                            }
                            else if (checky.compareTo(DATA_YDATA) == 0) {
                                Log.d("Blue", "Found YDATA");
                                write_notification(gatt, charact);
                            }
                            else if (checky.compareTo(DATA_ZDATA) == 0) {
                                Log.d("Blue", "Found ZDATA");
                                write_notification(gatt, charact);
                            }
                            else if (checky.compareTo(ALERT_EVENT) == 0) {
                                Log.d("Blue", "Found ALERT");
                                write_notification(gatt, charact);
                            }
                        }
                    }
                }
                Log.d("Blue", "Leaving Service Discovery");
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                Log.d("Blue", "onCharacteristicRead: " + characteristic.toString());
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                Log.d("Blue", "onCharacteristicWrite: " + characteristic.toString());
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);

                UUID checkUUID = characteristic.getUuid();

                if(checkUUID.compareTo(ALERT_EVENT) == 0) {
                    Log.d("Blue", "ALERT characteristic changed");
                    ALERT = true;
                }
                else if(checkUUID.compareTo(DATA_TXTOTAL) == 0) {
                    Log.d("Blue", "TXTOTAL characteristic changed");
                    txTotal = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    txTotal--;
                    characteristic.setValue(txTotal, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    gatt.writeCharacteristic(characteristic);
                }
                else if(checkUUID.compareTo(DATA_XDATA) == 0) Log.d("Blue", "XDATA characteristic changed");
                else if(checkUUID.compareTo(DATA_YDATA) == 0) Log.d("Blue", "YDATA characteristic changed");
                else if(checkUUID.compareTo(DATA_ZDATA) == 0) Log.d("Blue", "ZDATA characteristic changed");
                else Log.d("Blue", "Characteristic changed but was not identified by UUID" + characteristic.getValue().toString());
            }

            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                super.onReliableWriteCompleted(gatt, status);
            }
        };
}
