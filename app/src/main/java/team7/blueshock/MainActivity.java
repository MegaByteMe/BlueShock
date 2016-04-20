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
        import android.content.BroadcastReceiver;
        import android.content.Context;
        import android.content.DialogInterface;
        import android.content.Intent;
        import android.content.IntentFilter;
        import android.content.pm.PackageManager;
        import android.os.Build;
        import android.os.Handler;
        import android.os.Message;
        import android.support.v4.content.LocalBroadcastManager;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.util.Log;
        import android.view.View;
        import android.view.Window;
        import android.widget.Button;
        import android.widget.TextView;
        import android.widget.Toast;

        import java.util.LinkedList;
        import java.util.Queue;
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

    private static final UUID CHAR_UPDATE_NOT_DESC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final String ble_not_supported = "Bluetooth Low Energy capability could not be located";

    private static int REQUEST_ENABLE_BT = 1, REQUEST_SCAN_BT = 2, REQUEST_CONFIG_BT = 3, REQUEST_DETAIL_BT = 4;

    public boolean PAIR = false, SETUP = false;
    public int SHKVAL = 0;
    public static int txTotal = 0;
    public int[] xData = new int[32];
    public int[] yData = new int[32];
    public int[] zData = new int[32];

    private BluetoothAdapter mBleAdap;
    private BluetoothManager btManager;
    private BluetoothGatt myConnectedGatt;
    private Arbiter arby;
    private BroadcastReceiver uiCommRx;

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

        uiCommRx = createBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(uiCommRx, new IntentFilter("team7.blueshock.ui"));

        arby = new Arbiter();
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
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.d("BLUE", "REQUEST_ENABLE_BT Result OK");
            }
        }
        else if (requestCode == REQUEST_SCAN_BT) {
            if (resultCode == RESULT_OK) {
                BluetoothDevice foundDev = data.getParcelableExtra("BLUE");

                if (foundDev != null) {
                    Log.d("Blue", foundDev.getName() + " - " + foundDev.getAddress());
                    devTxtView.setText(foundDev.getName());
                    myConnectedGatt = foundDev.connectGatt(this, true, myGattCallb);
                    foundDev.createBond();

                    PAIR = true;
                    progBtnCntl(PAIR && SETUP);

                    progress(1);
                }
            }
        }
        else if (requestCode == REQUEST_CONFIG_BT) {
            if (resultCode == RESULT_OK) {
                Log.d("Blue", "REQUEST_CONFIG_BT Result OK");

                Bundle xtra = data.getExtras();
                getIntent().putExtras(xtra);
                restore_settings(xtra);

                SETUP = true;
                progBtnCntl(PAIR && SETUP);
            }
        }
    }

    private void progress(int value) {

    }

    private BroadcastReceiver createBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getBooleanExtra("ALERT", false) == true) {
                    notifyOnAlert();
                    //myConnectedGatt.readCharacteristic(myConnectedGatt.getService(ALERT_SERV).getCharacteristic(DATA_TXTOTAL));
                }
                else if(intent.getBooleanExtra("LOAD", false) == true) {
                    progress(0);
                }
                else if(intent.getBooleanExtra("TXCHAIN", false) == true) {
                    if(txTotal > 0) {
                        txTotal--;
                        arby.writeChar_N(DATA_TXTOTAL, txTotal);
                    }
                }
            }
        };
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

        if(axisXTxtView.getVisibility() == View.VISIBLE) axis_val |= 0b00000100;
        if(axisYTxtView.getVisibility() == View.VISIBLE) axis_val |= 0b00000010;
        if(axisZTxtView.getVisibility() == View.VISIBLE) axis_val |= 0b00000001;

//        BluetoothGattCharacteristic shk = myConnectedGatt.getService(ALERT_SERV).getCharacteristic(CONFIG_THRESH);
//        BluetoothGattCharacteristic axis = myConnectedGatt.getService(ALERT_SERV).getCharacteristic(CONFIG_AXIS);
//        shk.setValue(shock_val, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//        axis.setValue(axis_val, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//
//        arby.writeChar_N(shk);
//        arby.writeChar_N(axis);

        arby.writeChar_N(CONFIG_THRESH, shock_val);
        arby.writeChar_N(CONFIG_AXIS, axis_val);

        Log.d("Blue", "Program button pushed");
    }

    public void progBtnCntl(boolean e) {
        btn.setEnabled(e);
        btn.refreshDrawableState();
    }

    public void notifyOnAlert() {
        new AlertDialog.Builder(this)
                .setTitle("Shock Event!")
                .setMessage("Shock Event Threshold has been met. View details?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        call_detail();
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

        Intent i = new Intent("team7.blueshock.ui");
        i.putExtra("ALERT", false);
        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
    }

    public void call_detail() {
        Intent i = new Intent(this, DetailActivity.class);
        startActivityForResult(i, REQUEST_DETAIL_BT);
    }

    private BluetoothGattCallback myGattCallb = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);

                if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("Blue", "STATE CONNECTED");
                    gatt.discoverServices();

                    Intent i = new Intent("team7.blueshock.ui");
                    i.putExtra("LOAD", true);
                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
                }
                else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("Blue", "STATE DISCONNECTED");
                }
                else if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.d("Blue", "STATE DISCONNECTED");
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);

                if(status == BluetoothGatt.GATT_SUCCESS) {
                    for (BluetoothGattService bluetoothGattService : gatt.getServices()) {
                        Log.d("Blue", "Service is: " + bluetoothGattService.getUuid().toString());
                        if (bluetoothGattService.getUuid().toString().contains("4c1e")) {
                            for (BluetoothGattCharacteristic charact : bluetoothGattService.getCharacteristics()) {
                                UUID checky = charact.getUuid();
                                Log.d("Blue", "Characteristic is: " + checky.toString());

                                if (checky.compareTo(DATA_TXTOTAL) == 0) {
                                    Log.d("Blue", "Found TXTOTAL");
                                    arby.writeNotifyChar_N(charact);
                                } else if (checky.compareTo(DATA_XDATA) == 0) {
                                    Log.d("Blue", "Found XDATA");
                                } else if (checky.compareTo(DATA_YDATA) == 0) {
                                    Log.d("Blue", "Found YDATA");
                                } else if (checky.compareTo(DATA_ZDATA) == 0) {
                                    Log.d("Blue", "Found ZDATA");
                                } else if (checky.compareTo(ALERT_EVENT) == 0) {
                                    Log.d("Blue", "Found ALERT");
                                    arby.writeNotifyChar_N(charact);
                                }
                            }
                        }
                    }
                }
                else Log.d("Blue", "onServiceDiscovered: " + status);
                Log.d("Blue", "Leaving Service Discovery");
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);

                if(status == BluetoothGatt.GATT_SUCCESS) {

                    Log.d("Blue", "onCharacteristicRead: " + characteristic.toString());
                    UUID checky = characteristic.getUuid();

                    if (checky.compareTo(DATA_TXTOTAL) == 0) {
                        txTotal = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,0);
                        Log.d("Blue", "TXTOTAL was read " + txTotal);

                        Intent i = new Intent("team7.blueshock.ui");
                        i.putExtra("TXCHAIN", true);
                        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);

                    }
                    else if(checky.compareTo(DATA_XDATA) == 0) {
                        xData[txTotal] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,0);
                    }
                    else if(checky.compareTo(DATA_YDATA) == 0) {
                        yData[txTotal] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,0);
                    }
                    else if(checky.compareTo(DATA_ZDATA) == 0) {
                        zData[txTotal] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,0);
                    }
                }

                arby.process_Queue_N();
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);

                if(status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("Blue", "onCharacteristicWrite: " + characteristic.toString());

                    if (characteristic.getUuid().compareTo(DATA_TXTOTAL) == 0) {
                        if(axisXTxtView.getVisibility() == View.VISIBLE) {
                            Log.d("Blue", "on write X selected");
                            gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_XDATA));
                        }
                        else if(axisYTxtView.getVisibility() == View.VISIBLE) {
                            Log.d("Blue", "on write Y selected");
                            gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_YDATA));
                        }
                        else if(axisYTxtView.getVisibility() == View.VISIBLE) {
                            Log.d("Blue", "on write Z selected");
                            gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_ZDATA));
                        }
                        else if(axisXTxtView.getVisibility() == View.VISIBLE && axisZTxtView.getVisibility() == View.VISIBLE) {
                            Log.d("Blue", "on write X & Z selected");
                            gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_XDATA));
                            gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_ZDATA));
                        }
                        else if(axisXTxtView.getVisibility() == View.VISIBLE && axisYTxtView.getVisibility() == View.VISIBLE) {
                            Log.d("Blue", "on write X & Y selected");
                            gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_XDATA));
                            gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_YDATA));
                        }
                        else if(axisYTxtView.getVisibility() == View.VISIBLE && axisZTxtView.getVisibility() == View.VISIBLE) {
                            Log.d("Blue", "on write Y & Z selected");
                            gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_YDATA));
                            gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_ZDATA));
                        }
                        else if(axisXTxtView.getVisibility() == View.VISIBLE && axisYTxtView.getVisibility() == View.VISIBLE &&
                                axisZTxtView.getVisibility() == View.VISIBLE) {
                            Log.d("Blue", "on write X, Y, Z selected");
                            gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_XDATA));
                            gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_YDATA));
                            gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_ZDATA));
                        }
                    }
                }
                else Log.d("Blue", "onCharacteristicWrite: " + status);

                arby.characteristicQueue.remove();
                arby.process_Queue_N();
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);

                UUID checkUUID = characteristic.getUuid();

                Log.d("Blue", "onCharacteristic change");

                if(checkUUID.compareTo(ALERT_EVENT) == 0) {
                    Log.d("Blue", "ALERT characteristic changed");

                    Intent i = new Intent("team7.blueshock.ui");
                    i.putExtra("ALERT", true);
                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
                }
                else if(checkUUID.compareTo(DATA_TXTOTAL) == 0) {
                    Log.d("Blue", "TXTOTAL characteristic changed");
                    gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_TXTOTAL));
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

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            arby.descriptorQueue.remove();
            if(arby.descriptorQueue.size() > 0) gatt.writeDescriptor(arby.descriptorQueue.element());
        }
    };

    private class Arbiter {
        private static final int SET_NOTIFY = 1;
        private static final int SET_READ = 2;
        private static final int SET_WRITE = 3;

        private boolean BLOCKED = false;

        private int que_pos = 0;
        private int[] opQueue = new int[16];
        private int[] valQueue = new int[16];
        private UUID[] idQueue = new UUID[16];

        private Queue<BluetoothGattCharacteristic> characteristicQueue = new LinkedList<BluetoothGattCharacteristic>();
        private Queue<BluetoothGattDescriptor> descriptorQueue = new LinkedList<BluetoothGattDescriptor>();

        public int getCharQueSize() { return characteristicQueue.size(); }
        public int getDescQueSize() { return descriptorQueue.size(); }

        public void process_Queue_N() {
            if(characteristicQueue.size() > 0) myConnectedGatt.writeCharacteristic(characteristicQueue.element());
        }

        public void writeNotifyChar_N(BluetoothGattCharacteristic C) {
            myConnectedGatt.setCharacteristicNotification(C, true);
            BluetoothGattDescriptor desc = C.getDescriptor(CHAR_UPDATE_NOT_DESC);
            writeDesc_N(desc);
        }

        public void writeChar_N(BluetoothGattCharacteristic C) {
            Log.d("ARB-writeCHar_N", "Queue size: " + characteristicQueue.size());
            characteristicQueue.add(C);
            Log.d("ARB-writeCHar_N", "Queue size: " + characteristicQueue.size());
            if(characteristicQueue.size() == 1) myConnectedGatt.writeCharacteristic(C);
        }

        public void writeChar_N(UUID who, int val) {
            BluetoothGattCharacteristic C = myConnectedGatt.getService(ALERT_SERV).getCharacteristic(who);
            C.setValue(val,BluetoothGattCharacteristic.FORMAT_UINT8,0);
            Log.d("ARB-writeCHar_N", "Queue size: " + characteristicQueue.size());
            characteristicQueue.add(C);
            Log.d("ARB-writeCHar_N", "Queue size: " + characteristicQueue.size());
            if(characteristicQueue.size() == 1) myConnectedGatt.writeCharacteristic(C);
        }

        public void writeDesc_N(BluetoothGattDescriptor D) {
            D.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            descriptorQueue.add(D);
            if(descriptorQueue.size() == 1) myConnectedGatt.writeDescriptor(D);
        }

        public void unBLOCK() { BLOCKED = false; }

        public void arb(int type, int value, UUID who) {
            Log.d("ARB", "In Arb");
            if(BLOCKED) schedule(type, value, who);
            else if(!BLOCKED) {
                BLOCKED = true;
                sort_op(type, value, who);
            }
            else Log.d("Blue", "Error has occured in arb.");
        }

        public void run_que() {
            if(!BLOCKED & que_pos > 0) {
                que_pos--;
                sort_op(opQueue[que_pos], valQueue[que_pos], idQueue[que_pos]);
            }
            else if(BLOCKED & que_pos > 0) Log.d("ARB", "Blocked! pos: " + que_pos);
            else if(!BLOCKED & que_pos == 0) Log.d("ARB", "NOT BLOCKED and Queue empty");
            else if(BLOCKED & que_pos == 0) Log.d("ARB", "BLOCKED and Queue is empty");
            else Log.d("ARB", "other... in run_que pos: " + que_pos);
        }

        private void schedule(int type, int value, UUID who) {
            Log.d("ARB", "Scheduling " + who.toString());
            opQueue[que_pos] = type;
            valQueue[que_pos] = value;
            idQueue[que_pos] = who;
            que_pos++;
        }

        private void sort_op(int type, int value, UUID who) {
            switch(type) {
                case SET_NOTIFY:
                    write_notification(who);
                    break;
                case SET_READ:
                    break;
                case SET_WRITE:
                    wr_char(value, who);
                    break;
                default:
                    Log.d("ARB", "Something went wrong");
                    break;
            }
            if (que_pos > 0) que_pos--;
        }

        private void write_notification(UUID who) {
            BluetoothGattCharacteristic character = myConnectedGatt.getService(ALERT_SERV).getCharacteristic(who);
            myConnectedGatt.setCharacteristicNotification(character, true);
            myConnectedGatt.writeCharacteristic(character);

            myConnectedGatt.setCharacteristicNotification(character, true);
            BluetoothGattDescriptor desc = character.getDescriptor(CHAR_UPDATE_NOT_DESC);

            if(character.getDescriptor(CHAR_UPDATE_NOT_DESC) == null) {
                Log.d("ARB", "DESC NULL");
                desc = new BluetoothGattDescriptor(CHAR_UPDATE_NOT_DESC, 0);
            }
            else if(character.getDescriptor(CHAR_UPDATE_NOT_DESC) != null) {
                Log.d("ARB", "DESC NOT NULL");
                desc = character.getDescriptor(CHAR_UPDATE_NOT_DESC);
            }
            else {
                Log.d("ARB", "in DESC else statement");
            }
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            myConnectedGatt.writeDescriptor(desc);
            Log.d("ARB", "Writing notification " + who.toString());
            unBLOCK();
        }

        private void wr_char(int val, UUID who) {
            BluetoothGattCharacteristic character = myConnectedGatt.getService(ALERT_SERV).getCharacteristic(who);
            byte[] me = new byte[1];
            me[0]=(byte) val;
            character.setValue(me);
            myConnectedGatt.writeCharacteristic(character);
            Log.d("ARB", "Writing Characteristic " + who.toString());
        }
    }

}
