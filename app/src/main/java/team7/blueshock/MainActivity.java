/*
Low Power Wireless Shock Detection System
Developed by Team7

Codename: BlueShock
Revision:1
Change:6

Notes:
    Base UUID: 4c1e0000-4c64-41ab-b51f-9797339e4ab7
    Config: 0x0788
    Shock: 0xA53C
    DATA: 0xF6C2

    Current Extras in use:
        CONFIG          bsconfig object passed containing ui values
        BLUE            Bluetoothdevice returned from scan activity
        EVENT           shockevent object passed for display in detail
        DATA            Temporary until object array parceling is fixed

    team7.blueshock.ui
        TXCHAIN         Indicates the beginning of the shock event data transmission
        ALERT           Indicates shock event occurred
        EVENTRDY        Indicates shock event data has completed transmission and is ready to view
        HOLD            Indicates pending connection to sensor device and holds user until connection complete
        PROG            Indicates programming complete
        CONSTATE        Indicates connection state
*/

package team7.blueshock;

        import android.app.AlertDialog;
        import android.app.ProgressDialog;
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
        import android.graphics.Color;
        import android.os.Build;
        import android.support.v4.content.LocalBroadcastManager;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.util.Log;
        import android.view.View;
        import android.widget.Button;
        import android.widget.TextView;
        import android.widget.Toast;
        import java.text.SimpleDateFormat;
        import java.util.Date;
        import java.util.LinkedList;
        import java.util.Locale;
        import java.util.Queue;
        import java.util.TimeZone;
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

    // Notification Descriptor
    private static final UUID CHAR_UPDATE_NOT_DESC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final String ble_not_supported = "Bluetooth Low Energy capability could not be located";
    private static final int REQUEST_ENABLE_BT = 1, REQUEST_SCAN_BT = 2, REQUEST_CONFIG_BT = 3, REQUEST_DETAIL_BT = 4;
    private static final int CON_UNKWN = 0, CON_COND = 1, CON_DCOND = 2;
    private final int xSet = 0b00000100, ySet = 0b00000010, zSet = 0b00000001;

    private boolean LOAD = true, SINGLETX = false, TXCOMP = false;
    private static int txTotal = 0;
    private float[] xData = new float[33];
    private float[] yData = new float[33];
    private float[] zData = new float[33];

    private BluetoothGatt myConnectedGatt;

    private TextView shkSetTxtView, devTxtView, axisXTxtView, axisYTxtView, axisZTxtView, comboTextBtn;
    private ProgressDialog progress;
    private Button btn;

    private final Arbiter arby = new Arbiter();
    private BlueShockConfig bsConfig = new BlueShockConfig();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final int bad_data_marker = 0xDEAD;

        for(int i = 0; i < 33; i++) {
            xData[i] = bad_data_marker;
            yData[i] = bad_data_marker;
            zData[i] = bad_data_marker;
        }

        // OS Catch - Ensure minimum OS version that supports BLE
        if (Build.VERSION.SDK_INT < 18) {
            // Detect if OS is lower then SDK 18 - first release supporting BLE
            // If so terminate application - BLE does not exist on this platform
            // TODO need graceful application termination and notification dialog
            Toast.makeText(this, "Android Version Not Supported, Requires Kitkat or higher.", Toast.LENGTH_LONG).show();
            finish();
        }

        final int colorSnapUI = Color.MAGENTA;

        btn = (Button) findViewById(R.id.setBtn);

        // UI Text element indicating user set shock threshold
        shkSetTxtView = (TextView) findViewById(R.id.shkSetTxtView);
        assert shkSetTxtView != null;
        shkSetTxtView.setTextColor(colorSnapUI);

        // UI Text element indicating connected ble device
        devTxtView = (TextView) findViewById(R.id.devTxtView);
        assert devTxtView != null;
        devTxtView.setTextColor(colorSnapUI);

        // UI Text elements indicating user axis selected
        axisXTxtView = (TextView) findViewById(R.id.axisXTxtView);
        axisYTxtView = (TextView) findViewById(R.id.axisYTxtView);
        axisZTxtView = (TextView) findViewById(R.id.axisZTxtView);
        axisXTxtView.setTextColor(colorSnapUI);
        axisYTxtView.setTextColor(colorSnapUI);
        axisZTxtView.setTextColor(colorSnapUI);
        axisXTxtView.setVisibility(View.INVISIBLE);
        axisYTxtView.setVisibility(View.INVISIBLE);
        axisZTxtView.setVisibility(View.INVISIBLE);

        // UI Text element indicating last paired ble device
        // TODO: element is treated as a button - implement reconnect on push
        comboTextBtn = (TextView) findViewById(R.id.textViewCombo);

        BroadcastReceiver uiCommRx = createBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(uiCommRx, new IntentFilter("team7.blueshock.ui"));
    }

    @Override
    protected void onResume() {
        super.onResume();

        restore_settings();
        progBtnCntl();

        // Hardware Catch - Determine if hardware has BLE capability
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, ble_not_supported, Toast.LENGTH_LONG).show();
            finish();
        }

        // Init - Bluetooth
        // TODO need better error returns for wrong OS / hardware
        BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBleAdap = btManager.getAdapter();

        // If BLE is not enabled, Request Enable
        if (mBleAdap == null || !mBleAdap.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if(mBleAdap != null && mBleAdap.getBondedDevices().size() > 0) {
            comboTextBtn.setVisibility(View.VISIBLE);
            int si = mBleAdap.getBondedDevices().size();
            Object[] oldBonds = mBleAdap.getBondedDevices().toArray();
            comboTextBtn.setText(((BluetoothDevice)oldBonds[si - 1]).getName());
            comboTextBtn.setTextColor(Color.BLUE);
        }
        else comboTextBtn.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myConnectedGatt.close();
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
                bsConfig = data.getParcelableExtra("CONFIG");
                getIntent().putExtras(data);

                if (foundDev != null) {
                    Log.d("Blue", foundDev.getName() + " - " + foundDev.getAddress());
                    myConnectedGatt = foundDev.connectGatt(this, true, myGattCallb);
                    foundDev.createBond();

                    Intent i = new Intent("team7.blueshock.ui");
                    i.putExtra("HOLD", true);
                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);

                    progBtnCntl();
                }
            }
        }
        else if (requestCode == REQUEST_CONFIG_BT) {
            if (resultCode == RESULT_OK) {
                Log.d("Blue", "REQUEST_CONFIG_BT Result OK");
                getIntent().putExtras(data);

                bsConfig = data.getParcelableExtra("CONFIG");
                restore_settings();

                progBtnCntl();
            }
        }
        else if (requestCode == REQUEST_DETAIL_BT) {
            if(resultCode == RESULT_OK) {
                Log.d("Blue", "REQUEST_DETAIL_BT Result OK");
            }
        }
    }

    private BroadcastReceiver createBroadcastReceiver() {
        return new BroadcastReceiver() {
            private final String TAG = "UI_RX";

            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getBooleanExtra("ALERT", false)) {
                    notifyOnAlert();
                }
                else if(intent.getBooleanExtra("HOLD", false)) {
                    onConnectHold();
                }
                else if(intent.getBooleanExtra("TXCHAIN", false)) {
                    if(txTotal > 0) {
                        txTotal--;

                        if (LOAD) onLoadHold();
                        LOAD = false;

                        TXCOMP = false;

                        progress.incrementProgressBy(1);    // Increment loading dialog

                        arby.writeChar_N(DATA_TXTOTAL, txTotal);
                    }
                    else if(txTotal == 0) {
                        LOAD = true;                        // Reset loading flag
                        SINGLETX = false;

                        if(!TXCOMP) {
                            arby.writeChar_N(DATA_TXTOTAL, txTotal);
                            TXCOMP = true;                  // Reset transmission complete flag
                        }
                        else {
                            // TODO: REMOVE - FOR DEBUGGING ONLY
                            for (float f : xData) Log.d(TAG, "----> " + f);

                            txTotal = 33;

                            Intent i = new Intent("team7.blueshock.ui");
                            i.putExtra("EVENTRDY", true);
                            LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
                        }
                    }
                    else Log.d(TAG, "ERROR ON TXCHAIN PROCESS");
                }
                else if(intent.getBooleanExtra("EVENTRDY", false)) {
                    progress.dismiss();         // Dismiss loading message
                    notifyOnView();
                }
                else if(intent.getBooleanExtra("PROG", false)) {
                    notifyOnProg();
                }
                else if(intent.getIntExtra("CONSTATE", 0) == CON_COND) {
                    notifyOnConnectionChange(CON_COND);
                }
                else if(intent.getIntExtra("CONSTATE", 0) == CON_DCOND) {
                    notifyOnConnectionChange(CON_DCOND);
                }
            }
        };
    }

    private void restore_settings() {
        if(bsConfig.getShockThreshold() > 0) shkSetTxtView.setText(Integer.toString(bsConfig.getShockThreshold()));

        if (bsConfig.isxBoxSet()) axisXTxtView.setVisibility(View.VISIBLE);
        else axisXTxtView.setVisibility(View.INVISIBLE);
        if (bsConfig.isyBoxSet()) axisYTxtView.setVisibility(View.VISIBLE);
        else axisYTxtView.setVisibility(View.INVISIBLE);
        if (bsConfig.iszBoxSet()) axisZTxtView.setVisibility(View.VISIBLE);
        else axisZTxtView.setVisibility(View.INVISIBLE);
    }

    public void scanBtnClick(View V) {
        Intent i = new Intent(this, scanActivity.class);
        i.putExtra("CONFIG", bsConfig);
        startActivityForResult(i, REQUEST_SCAN_BT);
    }

    public void conBtnClick(View V) {
        Intent i = new Intent(this, ConActivity.class);
        i.putExtra("CONFIG", bsConfig);
        startActivityForResult(i, REQUEST_CONFIG_BT);
    }

    public void prgBtnClick(View V) {
        int axis_val = 0;

        if(bsConfig.isxBoxSet()) axis_val ^= xSet;
        if(bsConfig.isyBoxSet()) axis_val ^= ySet;
        if(bsConfig.iszBoxSet()) axis_val ^= zSet;

        Log.d("DEBUG", "axis val ->" + axis_val + " " + xSet + " " + ySet + " " + zSet);
        bsConfig.setAxisBox(axis_val);

        float threshadjust = bsConfig.getShockThreshold();
        threshadjust /= 0.78;

        arby.writeChar_N(CONFIG_THRESH, (int)threshadjust);
        arby.writeChar_N(CONFIG_AXIS, axis_val);

    }

    private void progBtnCntl() {
        btn.setEnabled(bsConfig.isPAIRED() & bsConfig.isSETUP());
        btn.refreshDrawableState();
    }

    public void comboBtnClick(View V) {
        // TODO: finish this section. Preliminary research indicates have to go through scanning process
        // and find Bluetooth device. This may be lessened since address is already stored in paired set.
        // Toast.makeText(this, "Oh trying to connect are we?", Toast.LENGTH_LONG).show();
//        comboTextBtn.getText();
//        UUID[] ugly = new UUID[1];
//        ugly[0] = ALERT_SERV;
    }

    private void onLoadHold() {
        progress = new ProgressDialog(this);
        progress.setMessage("Receiving Data from the sensor");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setIndeterminate(false);
        progress.setProgress(0);
        progress.setMax(33);
        progress.setCanceledOnTouchOutside(false);
        progress.show();
    }

    private void onConnectHold() {
        progress = new ProgressDialog(this);
        progress.setMessage("Connecting to Sensor");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setIndeterminate(true);
        progress.setCanceledOnTouchOutside(false);
        progress.setProgress(0);
        progress.show();
    }

    private void notifyOnAlert() {
        new AlertDialog.Builder(this)
                .setTitle("Shock Event!")
                .setMessage("Shock Event Threshold of " + bsConfig.getShockThreshold() + " has been met!")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .create()
                .show();
    }

    private void notifyOnView() {
        new AlertDialog.Builder(this)
                .setTitle("Shock Details")
                .setMessage("Shock details are ready to view. Would you like to view details?")
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
                .setCancelable(false)
                .create()
                .show();
    }

    private void notifyOnProg() {
        new AlertDialog.Builder(this)
                .setTitle("Programming Complete")
                .setMessage("Programming Complete")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .create()
                .show();
    }

    private void notifyOnConnectionChange(int conStatus) {
        String stringy = new String();

        switch(conStatus) {
            case CON_UNKWN:
                stringy = "Connection State Unknown";
                devTxtView.setText("");
                bsConfig.setPAIRED(false);
                break;
            case CON_COND:
                stringy = "Connection Established";
                devTxtView.setText(myConnectedGatt.getDevice().getName());
                bsConfig.setPAIRED(true);
                break;
            case CON_DCOND:
                stringy = "Connection Lost";
                devTxtView.setText("NOT CONNECTED");
                bsConfig.setPAIRED(false);
                break;
        }

        progBtnCntl();

        new AlertDialog.Builder(this)
                .setTitle("Connection Status")
                .setMessage(stringy)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .create()
                .show();
    }

    private void call_detail() {
        SimpleDateFormat when = new SimpleDateFormat("yyyy-MM-dd@HH:mm", Locale.US);
        when.setTimeZone(TimeZone.getDefault());
        String now = when.format(new Date(System.currentTimeMillis()));

        //TODO generate shock event and pass it to detail activity
        ShockEvent shocking = new ShockEvent(bsConfig.generateShockID(), bsConfig.getShockThreshold(), now, bsConfig.getAxisBox());
        shocking.setxData(xData);
        shocking.setyData(yData);
        shocking.setzData(zData);

        Intent i = new Intent(this, DetailActivity.class);
        i.putExtra("EVENT", shocking);

        if(bsConfig.isxBoxSet()) i.putExtra("DATA", xData);
        if(bsConfig.isyBoxSet()) i.putExtra("DATA", yData);
        if(bsConfig.iszBoxSet()) i.putExtra("DATA", zData);

        i.putExtras(getIntent());
        startActivityForResult(i, REQUEST_DETAIL_BT);
    }

    private final BluetoothGattCallback myGattCallb = new BluetoothGattCallback() {
        private final String TAG = "GATT-CALL-BACK";

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);

                if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "STATE CONNECTED");

                    Intent i = new Intent("team7.blueshock.ui");
                    i.putExtra("CONSTATE", CON_COND);
                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);

                    gatt.discoverServices();
                }
                else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "STATE DISCONNECTED");

                    Intent i = new Intent("team7.blueshock.ui");
                    i.putExtra("CONSTATE", CON_DCOND);
                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
                }
                else if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "STATE DISCONNECTED");

                    Intent i = new Intent("team7.blueshock.ui");
                    i.putExtra("CONSTATE", CON_DCOND);
                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);

                progress.dismiss();

                if(status == BluetoothGatt.GATT_SUCCESS) {
                    for (BluetoothGattService bluetoothGattService : gatt.getServices()) {
                        if (bluetoothGattService.getUuid().toString().contains("4c1e")) {
                            for (BluetoothGattCharacteristic charact : bluetoothGattService.getCharacteristics()) {
                                UUID checky = charact.getUuid();

                                if (checky.compareTo(DATA_TXTOTAL) == 0) {
                                    arby.writeNotifyChar_N(charact);
                                } else if (checky.compareTo(DATA_XDATA) == 0) {
                                    Log.d(TAG, "Found XDATA");
                                } else if (checky.compareTo(DATA_YDATA) == 0) {
                                    Log.d(TAG, "Found YDATA");
                                } else if (checky.compareTo(DATA_ZDATA) == 0) {
                                    Log.d(TAG, "Found ZDATA");
                                } else if (checky.compareTo(ALERT_EVENT) == 0) {
                                    arby.writeNotifyChar_N(charact);
                                }
                            }
                        }
                    }
                }
                else Log.d(TAG, "onServiceDiscovered Error: " + status);
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);

                if(status == BluetoothGatt.GATT_SUCCESS) {
                    UUID checky = characteristic.getUuid();

                    if (checky.compareTo(DATA_TXTOTAL) == 0) {
                        txTotal = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                        Log.d(TAG, "TXTOTAL: " + txTotal);
                    }
                    else if(checky.compareTo(DATA_XDATA) == 0) {
                        xData[txTotal] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 0);
                        Log.d(TAG, "xData-----> " + xData[txTotal]);
                    }
                    else if(checky.compareTo(DATA_YDATA) == 0) {
                        yData[txTotal] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 0);
                            Log.d(TAG, "yData-----> " + yData[txTotal]);
                    }
                    else if(checky.compareTo(DATA_ZDATA) == 0) {
                        zData[txTotal] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 0);
                            Log.d(TAG, "zData-----> " + zData[txTotal]);
                    }
                }
                arby.process_Queue_N();
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);

                if(characteristic.getUuid().compareTo(CONFIG_AXIS) == 0) {
                    Intent i = new Intent("team7.blueshock.ui");
                    i.putExtra("PROG", true);
                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
                }

                arby.characteristicQueue.remove();
                arby.process_Queue_N();
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);

                UUID checkUUID = characteristic.getUuid();

                if(checkUUID.compareTo(ALERT_EVENT) == 0) {
                    Log.d(TAG, "ALERT characteristic changed");
                    TXCOMP = false;

                    Intent i = new Intent("team7.blueshock.ui");
                    i.putExtra("ALERT", true);
                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
                }
                else if(checkUUID.compareTo(DATA_TXTOTAL) == 0) {
                    txTotal = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,0);
                    Log.d(TAG, "TXTOTAL characteristic changed. Starting at " + txTotal);

                    if(bsConfig.isxBoxSet()) {
                        Log.d(TAG, "on write X selected");
                        gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_XDATA));
                    }
                    else if(bsConfig.isyBoxSet()) {
                        Log.d(TAG, "on write Y selected");
                        gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_YDATA));
                    }
                    else if(bsConfig.iszBoxSet()) {
                        Log.d(TAG, "on write Z selected");
                        gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_ZDATA));
                    }
                    else if(bsConfig.isxBoxSet() && bsConfig.iszBoxSet()) {
                        Log.d(TAG, "on write X & Z selected");
                        gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_XDATA));
                        gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_ZDATA));
                    }
                    else if(bsConfig.isxBoxSet() && bsConfig.isyBoxSet()) {
                        Log.d(TAG, "on write X & Y selected");
                        gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_XDATA));
                        gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_YDATA));
                    }
                    else if(bsConfig.isyBoxSet() && bsConfig.iszBoxSet()) {
                        Log.d(TAG, "on write Y & Z selected");
                        gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_YDATA));
                        gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_ZDATA));
                    }
                    else if(bsConfig.isxBoxSet() && bsConfig.isyBoxSet() && bsConfig.iszBoxSet()) {
                        Log.d(TAG, "on write X, Y, Z selected");
                        gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_XDATA));
                        gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_YDATA));
                        gatt.readCharacteristic(gatt.getService(ALERT_SERV).getCharacteristic(DATA_ZDATA));
                    }

                    Intent i = new Intent("team7.blueshock.ui");
                    i.putExtra("TXCHAIN", true);
                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
                }
                arby.process_Queue_N();
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
        private final String TAG = "ARB";
        private Queue<BluetoothGattCharacteristic> characteristicQueue = new LinkedList<>();
        private Queue<BluetoothGattDescriptor> descriptorQueue = new LinkedList<>();

        public void process_Queue_N() {
            if(characteristicQueue.size() > 0) myConnectedGatt.writeCharacteristic(characteristicQueue.element());
            Log.d(TAG, "Processing Queue: " + characteristicQueue.size());
        }

        public void writeNotifyChar_N(BluetoothGattCharacteristic C) {
            myConnectedGatt.setCharacteristicNotification(C, true);
            BluetoothGattDescriptor desc = C.getDescriptor(CHAR_UPDATE_NOT_DESC);
            writeDesc_N(desc);
        }

        public void writeChar_N(BluetoothGattCharacteristic C) {
            characteristicQueue.add(C);
            if(characteristicQueue.size() == 1) myConnectedGatt.writeCharacteristic(C);
        }

        public void writeChar_N(UUID who, int val) {
            BluetoothGattCharacteristic C = myConnectedGatt.getService(ALERT_SERV).getCharacteristic(who);
            C.setValue(val,BluetoothGattCharacteristic.FORMAT_UINT8,0);
            characteristicQueue.add(C);
            Log.d(TAG, "Queue size: " + characteristicQueue.size());
            if(characteristicQueue.size() == 1) myConnectedGatt.writeCharacteristic(C);
        }

        public void writeDesc_N(BluetoothGattDescriptor D) {
            D.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            descriptorQueue.add(D);
            if(descriptorQueue.size() == 1) myConnectedGatt.writeDescriptor(D);
        }

        private void write_notification(UUID who) {
            BluetoothGattCharacteristic character = myConnectedGatt.getService(ALERT_SERV).getCharacteristic(who);
            myConnectedGatt.setCharacteristicNotification(character, true);
            myConnectedGatt.writeCharacteristic(character);

            myConnectedGatt.setCharacteristicNotification(character, true);
            BluetoothGattDescriptor desc = character.getDescriptor(CHAR_UPDATE_NOT_DESC);

            if(character.getDescriptor(CHAR_UPDATE_NOT_DESC) == null) {
                Log.d(TAG, "DESC NULL");
                desc = new BluetoothGattDescriptor(CHAR_UPDATE_NOT_DESC, 0);
            }
            else if(character.getDescriptor(CHAR_UPDATE_NOT_DESC) != null) {
                Log.d(TAG, "DESC NOT NULL");
                desc = character.getDescriptor(CHAR_UPDATE_NOT_DESC);
            }
            else Log.d(TAG, "in DESC else statement");

            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            myConnectedGatt.writeDescriptor(desc);
            Log.d(TAG, "Writing notification " + who.toString());
        }
    }
}
