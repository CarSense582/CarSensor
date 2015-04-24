/**
 * Created by michael on 4/23/15.
 */
package com.example.michael.carsensor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.example.michael.dataserverlib.DataService;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class CarSensorService extends DataService<CarSensor> {
    private final static String TAG = CarSensorService.class.getSimpleName();
    private BluetoothGattCharacteristic characteristicTx = null;
    private RBLService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice = null;
    private String mDeviceAddress;

    private boolean flag = true;
    private boolean connState = false;
    private boolean scanFlag = false;
    private char[] rxBuf = new char[100];
    private int rxPos    = 0;
    private boolean rxTxnProgress = false;
    final private char START_CHAR = '>';
    final private char END_CHAR   = '\n';

    private byte[] data = new byte[3];
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 2000;

    private Timer mOBDRequestTimer = new Timer();

    private int throttle = 0, rpm = 0, speed = 0, load = 0;

    final private static char[] hexArray = { '0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((RBLService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Disconnected",
                        Toast.LENGTH_SHORT).show();
            } else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                Toast.makeText(getApplicationContext(), "Connected",
                        Toast.LENGTH_SHORT).show();
                getGattService(mBluetoothLeService.getFreematicsGattService());
            } else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
                data = intent.getByteArrayExtra(RBLService.EXTRA_DATA);
                getOBD(data);
                //readAnalogInValue(data);
            } else if (RBLService.ACTION_GATT_RSSI.equals(action)) {
                displayData(intent.getStringExtra(RBLService.EXTRA_DATA));
            }
        }
    };
    private void displayData(String data) {
        if (data != null) {
            Log.i(TAG, "RSSI: " + data);
        }
    }

    private void getOBD(byte[] data) {
        if (data == null) {
            Log.v(TAG, "Got null OBD data");
            return;
        }
        Log.v(TAG, String.format("Got OBD data length: %d", data.length));

        for (int i = 0; i < data.length; i++) {
            if (i + 2 < data.length) {
                // Throttle
                if (data[i] == 't') {
                    throttle = 0;
                    throttle |= data[++i] & 0x000000FF;
                    throttle <<= 8;
                    throttle |= data[++i] & 0x000000FF;
                }
                // RPM
                if (data[i] == 'r') {
                    rpm = 0;
                    rpm |= data[++i] & 0x000000FF;
                    rpm <<= 8;
                    rpm |= data[++i] & 0x000000FF;
                }
                // Speed
                if (data[i] == 's') {
                    speed = 0;
                    speed |= data[++i] & 0x000000FF;
                    speed <<= 8;
                    speed |= data[++i] & 0x000000FF;
                }
                // Engine load
                if (data[i] == 'l') {
                    load = 0;
                    load |= data[++i] & 0x000000FF;
                    load <<= 8;
                    load |= data[++i] & 0x000000FF;
                }
            }
            if (data[i] == 'e') {
                /*
                if (mOBDlog != null) {
                    String logLine = String.format("%d,%d,%d,%d,%d\n", System.currentTimeMillis() / 1000L, throttle, rpm, speed, load);
                    Log.v(TAG, "Writing to OBD log: " + logLine);
                    try {
                        mOBDlog.write(logLine.toCharArray());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                */
            }
            String logLine = String.format("%d,%d,%d,%d,%d\n", System.currentTimeMillis() / 1000L, throttle, rpm, speed, load);
            Log.v(TAG, "Writing to OBD log: " + logLine);
        }

        String t = new Integer(throttle).toString();
    }

    private void readAnalogInValue(byte[] data) {

        char [] arr = new char[data.length+1];
        for (int i = 0; i < data.length; ++i) {
            char c = (char) data[i];
            arr[i] = c;
            if(rxTxnProgress == false) {
                if(c == START_CHAR) {
                    rxPos = 0;
                    rxTxnProgress = true;
                    continue;
                }
            } else {
                if(c == END_CHAR) {
                    rxBuf[rxPos] = '\0';
                    System.out.println(rxBuf);
                    //Restart txn
                    rxPos = 0;
                    rxTxnProgress = false;
                } else {
                    rxBuf[rxPos++] = c;
                }
            }
        }
    }
    private void startReadRssi() {
        new Thread() {
            public void run() {

                while (flag) {
                    mBluetoothLeService.readRssi();
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
        }.start();
    }

    private void getGattService(BluetoothGattService gattService) {
        if (gattService == null)
            return;

        startReadRssi();

        characteristicTx = gattService
                .getCharacteristic(RBLService.UUID_FREEMATICS_CHARACTERISTIC);

        BluetoothGattCharacteristic characteristicRx = gattService
                .getCharacteristic(RBLService.UUID_FREEMATICS_CHARACTERISTIC);
        mBluetoothLeService.setCharacteristicNotification(characteristicRx,
                true);
        mBluetoothLeService.readCharacteristic(characteristicRx);

        mOBDRequestTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                //characteristicTx.setValue(">");
                //mBluetoothLeService.writeCharacteristic(characteristicTx);
            }
        }, 0, 1000);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(RBLService.ACTION_GATT_RSSI);

        return intentFilter;
    }

    private void scanLeDevice() {
        new Thread() {

            @Override
            public void run() {
                mBluetoothAdapter.startLeScan(mLeScanCallback);

                try {
                    Thread.sleep(SCAN_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }.start();
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             final byte[] scanRecord) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] serviceUuidBytes = new byte[16];
                    String serviceUuid = "";
                    for (int i = 32, j = 0; i >= 17; i--, j++) {
                        serviceUuidBytes[j] = scanRecord[i];
                    }
                    serviceUuid = bytesToHex(serviceUuidBytes);
                    Log.e(TAG, "{{{" + stringToUuidString(serviceUuid));
                    if (stringToUuidString(serviceUuid).equals(
                            RBLGattAttributes.BLE_FREEMATICS_DEVICE
                                    .toUpperCase(Locale.ENGLISH))) {
                        mDevice = device;
                    }
                }
            }).start();
        }
    };

    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private String stringToUuidString(String uuid) {
        StringBuffer newString = new StringBuffer();
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(0, 8));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(8, 12));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(12, 16));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(16, 20));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(20, 32));

        return newString.toString();
    }

    @Override
    public DataService.ServiceTimes setupTimes() {
        ServiceTimes times = super.setupTimes();
        times.maxReadResponseTime  = 100;
        times.maxWriteResponseTime = 10;
        times.sensorPeriod         = 1000;
        return times;
    }
    //Driver modelled methods
    @Override
    public void open(){
        sensor = new CarSensor();
        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
                    .show();
        }

        final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        Intent gattServiceIntent = new Intent(this,
                RBLService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        if (scanFlag == false) {
            scanLeDevice();

            Timer mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (mDevice != null) {
                        mDeviceAddress = mDevice.getAddress();
                        mBluetoothLeService.connect(mDeviceAddress);
                        scanFlag = true;
                        Log.i(TAG, "Connected to BLE Device");
                    } else {
                        Log.i(TAG, "Couldn't search BLE Shield Device");
                    }
                }
            }, SCAN_PERIOD);
        }

        System.out.println(connState);
        if(mBluetoothLeService != null && mDeviceAddress != null) {
            mBluetoothLeService.connect(mDeviceAddress);
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }
    //Reads modify sensor
    @Override
    public void readAsync() {
        readPeriodic();
    }
    @Override
    public void readPeriodic() {
        if(characteristicTx != null && mBluetoothLeService != null) {
            characteristicTx.setValue(">");
            mBluetoothLeService.writeCharacteristic(characteristicTx);
        }
        sensor.throttle = throttle;
        sensor.load     = load;
        sensor.rpm      = rpm;
        sensor.speed    = speed;
    }
    //Writes use sensor to work with actual hadware
    @Override
    public void writeAsync() {
        //Write to sensor
    }
    @Override
    public void writePeriodic() {
        //Write to sensor periodically
    }
    @Override
    public void close() {
        //Do nothing
        mBluetoothLeService.close();
        mOBDRequestTimer.cancel();
        mBluetoothLeService.disconnect();
        flag = false;
        unregisterReceiver(mGattUpdateReceiver);
        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
        }
    }
}