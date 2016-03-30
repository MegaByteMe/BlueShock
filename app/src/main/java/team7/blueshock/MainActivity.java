/*
Low Power Wireless Shock Detection System
Developed by Team7

Codename: BlueShock
Revision:1
Change:6

Pitfalls:
Notes:
    Base UUID: 4c1e0000-4c64-41ab-b51f-9797339e4ab7
               00000000-fa11-ca11-10ad-c0ffeefa5727
    Config: 0x0788
    Shock: 0xA53C
    DATA: 0xF6C2
    DEMO_SERVICE: 0x1523

*/

package team7.blueshock;

        import android.app.AlertDialog;
        import android.app.Service;
        import android.bluetooth.BluetoothAdapter;
        import android.bluetooth.BluetoothClass;
        import android.bluetooth.BluetoothDevice;
        import android.bluetooth.BluetoothGatt;
        import android.bluetooth.BluetoothGattCallback;
        import android.bluetooth.BluetoothGattCharacteristic;
        import android.bluetooth.BluetoothGattDescriptor;
        import android.bluetooth.BluetoothGattService;
        import android.bluetooth.BluetoothManager;
        import android.bluetooth.BluetoothProfile;
        import android.content.BroadcastReceiver;
        import android.content.Context;
        import android.content.DialogInterface;
        import android.content.Intent;
        import android.content.pm.PackageManager;
        import android.os.Build;
        import android.os.IBinder;
        import android.support.annotation.MainThread;
        import android.support.annotation.Nullable;
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

    private static final UUID CONFIG_SERV = UUID.fromString("4c1e0788-4c64-41ab-b51f-9797339e4ab7");
    private static final UUID CONFIG_THRESH = UUID.fromString("4c1e0000-4c64-41ab-b51f-9797339e4ab7");
    private static final UUID CONFIG_AXIS = UUID.fromString("4c1e0000-4c64-41ab-b51f-9797339e4ab7");

    private static final UUID SHOCK_SERV = UUID.fromString("4c1ea53c-4c64-41ab-b51f-9797339e4ab7");
    private static final UUID SHOCK_EVENT = UUID.fromString("4c1e0000-4c64-41ab-b51f-9797339e4ab7");

    private static final UUID DATA_SERV = UUID.fromString("4c1ef6c2-4c64-41ab-b51f-9797339e4ab7");
    private static final UUID DATA_XDATA = UUID.fromString("4c1e0000-4c64-41ab-b51f-9797339e4ab7");
    private static final UUID DATA_YDATA = UUID.fromString("4c1e0000-4c64-41ab-b51f-9797339e4ab7");
    private static final UUID DATA_ZDATA = UUID.fromString("4c1e0000-4c64-41ab-b51f-9797339e4ab7");

    private static final UUID DEMO_SERVICE = UUID.fromString("4c1ebadd-4c64-41ab-b51f-9797339e4ab7");
    private static final UUID OUR_CHAR = UUID.fromString("4c1e0000-4c64-41ab-b51f-9797339e4ab7");

    private static final UUID GEN_ATTR = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");
    private static final UUID GEN_ACC = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    private static final UUID GEN_CHAR_DNAME = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");
    private static final UUID GEN_CHAR_APP = UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb");
    private static final UUID GEN_CHAR_PPC = UUID.fromString("00002a04-0000-1000-8000-00805f9b34fb");

    private static final UUID BTN = UUID.fromString("4c1e1524-4c64-41ab-b51f-9797339e4ab7");
    private static final UUID LED = UUID.fromString("4c1e1525-4c64-41ab-b51f-9797339e4ab7");
    private static final UUID LED1 = UUID.fromString("4c1e1526-4c64-41ab-b51f-9797339e4ab7");
    private static final UUID INTEG = UUID.fromString("4c1e1527-4c64-41ab-b51f-9797339e4ab7");

    private static final UUID CHAR_UPDATE_NOT_DESC = UUID.fromString("4c1e2902-4c64-41ab-b51f-9797339e4ab7");
    private static final UUID CHAR_UPDATE_NOT_DESC2 = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final String ble_not_supported = "Bluetooth Low Energy capability could not be located";

    private static int REQUEST_ENABLE_BT = 1, REQUEST_SCAN_BT = 2, REQUEST_CONFIG_BT = 3, REQUEST_DEV = 4;

    public boolean PAIR = false;
    public int SHKVAL = 0;

    private BluetoothAdapter mBleAdap;
    private BluetoothManager btManager;
    private BluetoothGatt myConnectedGatt;
    private BLEGattCallback myGattCallb;

    private AlertDialog.Builder alertDialogSEV;

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

        myGattCallb = new BLEGattCallback();

        alertDialogSEV = new AlertDialog.Builder(this);
        alertDialogSEV.setTitle("Shock Event!");
        alertDialogSEV.setMessage("A Shock Event meeting the set threshold has occurred.");
        alertDialogSEV.setIcon(android.R.drawable.ic_dialog_alert);
        alertDialogSEV.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        alertDialogSEV.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialogSEV.create();
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

        if (getIntent().hasExtra("AXIS")) {
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
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.d("BLUE", "REQUEST_ENABLE_BT returned");
            }
        } else if (requestCode == REQUEST_SCAN_BT) {
            if (resultCode == RESULT_OK) {
                Log.d("Blue", "scan returned good");
                BluetoothDevice foundDev = data.getParcelableExtra("BLUE");
                PAIR = true;
                progBtnCntl(PAIR);
                Log.d("BLUE", "Entered code return");
                if (foundDev != null) {
                    Log.d("BLUE", foundDev.getName() + " - " + foundDev.getAddress());
                    devTxtView.setText(foundDev.getName());
                    //foundDev.createBond();
                    myConnectedGatt = foundDev.connectGatt(this, true, myGattCallb);
                }
            }
        } else if (requestCode == REQUEST_CONFIG_BT) {
            if (resultCode == RESULT_OK) {
                Log.d("Blue", "REQUEST_CONFIG_BT returned");
                Bundle xtra = data.getExtras();
                this.getIntent().putExtras(xtra);
            }
        }
    }

    public void scanBtnClick(View V) {
        Intent i = new Intent(this, scanActivity.class);
        startActivityForResult(i, REQUEST_SCAN_BT);
    }

    public void conBtnClick(View V) {
        Intent i = new Intent(this, ConActivity.class);
        startActivityForResult(i, REQUEST_CONFIG_BT);
    }

    public void prgBtnClick(View V) {
        // TODO:
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
        finish();
    }

    public void notifyOnAlert() {
        Log.d("Blue", "notify has been called");
//        new AlertDialog.Builder(C)
//                .setTitle("Shock Event!")
//                .setMessage("Shock Event Threshold has been met.")
//                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        // do nothing for now
//                    }
//                })
//                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        // do nothing for now
//                    }
//                })
//                .setIcon(android.R.drawable.ic_dialog_alert)
//                .create()
//                .show();

alertDialogSEV.show();
    }

    private class BluetoothLeService extends Service {
        private final String TAG = BluetoothLeService.class.getSimpleName();

        private int mConnected = STATE_DISCONNECTED;

        private static final int STATE_DISCONNECTED = 0;
        private static final int STATE_CONNECTING = 1;
        private static final int STATE_CONNECTED = 2;

        private final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
        private final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
        private final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_SERVICES_DISCOVERED";
        private final static String ACTION_GATT_ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_GATT_ACTION_AVAILABLE";
        private final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    //private BluetoothGattCallback myGattCallb = new BluetoothGattCallback() {
    private class BLEGattCallback extends BluetoothGattCallback {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);

                if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("Blue", "STATE CONNECTED");
                    gatt.discoverServices();
                } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("Blue", "STATE DISCONNECTED");
                } else if (status != BluetoothGatt.GATT_SUCCESS) gatt.disconnect();
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);

                for (BluetoothGattService bluetoothGattService : gatt.getServices()) {
                    Log.d("Blue", "step 1");
                    Log.d("Blue", "Service is: " + bluetoothGattService.getUuid().toString());
                    if (bluetoothGattService.getUuid().toString().contains("4c1e")) {
                        Log.d("Blue", "step 2");
                        for (BluetoothGattCharacteristic charact : bluetoothGattService.getCharacteristics()) {
                            UUID checky = charact.getUuid();
                            Log.d("Blue", "Characteristic is: " + checky.toString());
                            if(checky.compareTo(INTEG) == 0) {
                                Log.d("Blue", "Found Integ");

                                gatt.setCharacteristicNotification(charact, true);
                                BluetoothGattDescriptor desc = charact.getDescriptor(CHAR_UPDATE_NOT_DESC2);
                                if(desc != null) {
                                    Log.d("Blue", "Found Desc");
                                    desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                    gatt.writeDescriptor(desc);
                                }

                            }
                        }
                    }
                }
                Log.d("Blue", "Leaving Service Discovery");
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                Log.d("Blue", "Characteristic write: " + characteristic.toString());
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                Log.d("Blue", "Characteristic changed: " + characteristic.getValue().toString());

                //if(CHAR_UPDATE_NOT_DESC2.compareTo(characteristic.getUuid()) == 0) {
//do function call
                    Log.d("Blue", "In characteristic changed, awaiting to notify UI");
                  notifyOnAlert();

               //}
            }

            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                super.onReliableWriteCompleted(gatt, status);
            }
        }

    private final BroadcastReceiver mGattUpdateRx = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {

                }
            }
        };
}
