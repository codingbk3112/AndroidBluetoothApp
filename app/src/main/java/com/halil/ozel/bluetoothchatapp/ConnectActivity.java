package com.halil.ozel.bluetoothchatapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ConnectActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_LOCATION = 2;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private ArrayAdapter<String> deviceListAdapter;
    private List<BluetoothDevice> devices = new ArrayList<>();

    private TextView statusTextView;
    private Button scanButton;
    private ListView deviceListView;

    private static final UUID SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID CHARACTERISTIC_RX_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID CHARACTERISTIC_TX_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final int CONNECT_PERMISSIONS_CODE = 1024;
    private static final int REQUEST_ACCESS_COARSE_LOCATION = 1022; // random number


    private static final String[] BLE_PERMISSIONS = new String[]{
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
    };

    private static final String[] ANDROID_12_BLE_PERMISSIONS = new String[]{
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
    };

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d("TAG","device");
            BluetoothDevice device = result.getDevice();
            if (!devices.contains(device)) {
                devices.add(device);
                deviceListAdapter.add(device.getName());
                deviceListAdapter.notifyDataSetChanged();
            }
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to device");
                bluetoothGatt = gatt;
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from device");
                if (bluetoothGatt != null) {
                    bluetoothGatt.close();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                BluetoothGattCharacteristic rxCharacteristic = service.getCharacteristic(CHARACTERISTIC_RX_UUID);
                BluetoothGattCharacteristic txCharacteristic = service.getCharacteristic(CHARACTERISTIC_TX_UUID);
                // Do whatever you need with the characteristics
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (CHARACTERISTIC_TX_UUID.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                // Handle received data
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        statusTextView = findViewById(R.id.statusTextView);
        scanButton = findViewById(R.id.scanButton);
        deviceListView = findViewById(R.id.deviceListView);
        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceListView.setAdapter(deviceListAdapter);

        // Initialize BluetoothAdapter
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else
        {
//            canConnect();
        }

//        else {
//            checkLocationPermission();
//        }

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                setupConnection();

        }});

        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice selectedDevice = devices.get(position);
                connectToDevice(selectedDevice);
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
        }
    }


//    private void setupConnection() {
////        Log.d("TAG_Connect",canConnect().toString());
//        if (canConnect())
//            if (BluetoothHelper.INSTANCE.isLocationPermissionsGranted(this)) {
//                Log.d("TAG_Connect", "Location permision granted");
////                Log.d("TAG_Connect+Bt_enabled",BluetoothHelper.INSTANCE.isBleEnabled(this@SetUpStadiaActivity).toString())
////                Log.d("TAG_Connect+Bthelper",BluetoothHelper.INSTANCE.toString());
//                // Bluetooth must be enabled
//                if (BluetoothHelper.INSTANCE.isBleEnabled(this)) {
//                    if (BluetoothHelper.INSTANCE.isLocationRequired(this) || BluetoothHelper.INSTANCE.isLocationEnabled(
//                            this
//                    )
//                    ) {
//                        startScan();
////                        customDialogFragment = CustomDialogFragmentNew {
////                            it?.let {
////                                if(isConnected(it))
////                                    connectBt(it)
////                            }
////                        }
////                    customDialogFragment = CustomDialogFragmentNew()
//
////                        supportFragmentManager.let { customDialogFragment?.show(it, "dilaog") }
////                    val intent = Intent(this@SetUpStadiaActivity,ScannerActivity::class.java)
////                    startActivity(intent);
//                    } else {
//                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                        startActivity(intent);
//                    }
//                } else {
////                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
////                startActivityForResult(enableBtIntent, 1)
//                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                    startActivity(enableIntent);
//                }
//            }
//
//            else {
//                // Request Bluetooth-related permissions
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    boolean allPermissionsGranted = true;
//                    for (String permission : BLE_PERMISSIONS) {
//                        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
//                            allPermissionsGranted = false;
//                            break;
//                        }
//                    }
//
//                    if (allPermissionsGranted) {
//                        // Permissions are already granted
//                        BluetoothHelper.INSTANCE.markLocationPermissionRequested(this);
//                    } else {
//                        BluetoothHelper.INSTANCE.markLocationPermissionRequested(this);
//                        ActivityCompat.requestPermissions(
//                                this,
//                                BLE_PERMISSIONS,
//                                REQUEST_ACCESS_COARSE_LOCATION
//                        );
//                    }
//                }
//
//                else {
//                // Permissions are already granted (pre-Android 6.0)
//                BluetoothHelper.INSTANCE.markLocationPermissionRequested(this);
//                }
//            }
//    }


//    private void checkLocationPermission() {
//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                    PERMISSION_REQUEST_LOCATION);
//        }
//    }

    private void startScan() {
        statusTextView.setText("Scanning...");
        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        scanner.startScan(null, scanSettings, scanCallback);
    }

    private void connectToDevice(BluetoothDevice device) {
        device.connectGatt(this, false, gattCallback);
    }


    private boolean canConnect(){

        List<String> deniedPermissions = new ArrayList<>();

        if(!checkPermission(android.Manifest.permission.BLUETOOTH_SCAN))
            deniedPermissions.add(android.Manifest.permission.BLUETOOTH_SCAN);
        if(!checkPermission(android.Manifest.permission.BLUETOOTH_CONNECT))
            deniedPermissions.add(android.Manifest.permission.BLUETOOTH_CONNECT);

        if(deniedPermissions.isEmpty())
        {
            return true;
        }
        else {
//			requestRuntimePermissions(
//					"Bluetooth permissions request",
//					"Bluetooth permissions request rationale",
//		//			CONNECT_PERMISSIONS_CODE,
//					deniedPermissions.toArray(new String[0]));
            requestBlePermissions(this, CONNECT_PERMISSIONS_CODE);
            return false;
        }
    }

    public static void requestBlePermissions(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ActivityCompat.requestPermissions(activity, ANDROID_12_BLE_PERMISSIONS, requestCode);
        else
            ActivityCompat.requestPermissions(activity, BLE_PERMISSIONS, requestCode);
    }

    private boolean checkPermission(@NonNull String permission){
        return ActivityCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED;
    }


}
