package com.halil.ozel.bluetoothchatapp

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.halil.ozel.bluetoothchatapp.BluetoothHelper.isBleEnabled
import com.halil.ozel.bluetoothchatapp.BluetoothHelper.isLocationEnabled
import com.halil.ozel.bluetoothchatapp.BluetoothHelper.isLocationPermissionsGranted
import com.halil.ozel.bluetoothchatapp.BluetoothHelper.isLocationRequired
import com.halil.ozel.bluetoothchatapp.BluetoothHelper.markLocationPermissionRequested
import com.halil.ozel.bluetoothchatapp.databinding.ActivityConnectBinding
import java.util.UUID


class ConnectBtActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConnectBinding
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var deviceListAdapter: ArrayAdapter<String>? = null
    private val devices: MutableList<BluetoothDevice> = ArrayList()
//    private var statusTextView: TextView? = null
//    private var scanButton: Button? = null
//    private var deviceListView: ListView? = null

    private val TAG = "MainActivity"
    private val REQUEST_ENABLE_BT = 1
//    private const val PERMISSION_REQUEST_LOCATION = 2
    private val SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    private val CHARACTERISTIC_RX_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    private val CHARACTERISTIC_TX_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    private val CONNECT_PERMISSIONS_CODE = 1024
    private val REQUEST_ACCESS_COARSE_LOCATION = 1022 // random number
    private val BLE_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    private val ANDROID_12_BLE_PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    fun requestBlePermissions(activity: Activity?, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ActivityCompat.requestPermissions(activity!!,
                ANDROID_12_BLE_PERMISSIONS, requestCode)
        else
            ActivityCompat.requestPermissions(activity!!,
                BLE_PERMISSIONS, requestCode)
    }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d("TAG", "device")
            val device = result.device
            if (!devices.contains(device)) {
                devices.add(device)
                deviceListAdapter!!.add(device.name)
                deviceListAdapter!!.notifyDataSetChanged()
            }
        }
    }
    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to device")
                bluetoothGatt = gatt
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from device")
                if (bluetoothGatt != null) {
                    bluetoothGatt!!.close()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                val rxCharacteristic = service.getCharacteristic(CHARACTERISTIC_RX_UUID)
                val txCharacteristic = service.getCharacteristic(CHARACTERISTIC_TX_UUID)
                // Do whatever you need with the characteristics

                // Call writeCommand here after the connection is established
                val command = "0".toByteArray()
                writeCommand(command)
            }
        }

        // Write command to ESP32
        fun writeCommand(command: ByteArray) {
            val service = bluetoothGatt?.getService(SERVICE_UUID)
            val txCharacteristic = service?.getCharacteristic(CHARACTERISTIC_TX_UUID)
            txCharacteristic?.value = command
            bluetoothGatt?.writeCharacteristic(txCharacteristic)
        }


        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (CHARACTERISTIC_TX_UUID == characteristic.uuid) {
                val data = characteristic.value
                // Handle received data
                Log.d(TAG, "Received data from ESP32: ${data.decodeToString()}")

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        statusTextView = findViewById(R.id.statusTextView)
//        scanButton = findViewById(R.id.scanButton)
//        deviceListView = binding.deviceListView
        deviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        binding.deviceListView.setAdapter(deviceListAdapter)

        // Initialize BluetoothAdapter
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
//        else {
//            canConnect();
//        }

//        else {
//            checkLocationPermission();
//        }
        binding.scanButton.setOnClickListener(View.OnClickListener {
            setupConnection() }
        )
        binding.deviceListView.setOnItemClickListener(OnItemClickListener { parent, view, position, id ->
            val selectedDevice = devices[position]
            connectToDevice(selectedDevice)
            bluetoothAdapter?.cancelDiscovery()
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bluetoothGatt != null) {
            bluetoothGatt!!.close()
        }
    }

    private fun setupConnection() {
//        Log.d("TAG_Connect",canConnect().toString());
        if (canConnect())
            if (isLocationPermissionsGranted(this)) {
            Log.d("TAG_Connect", "Location permision granted")
            if (isBleEnabled(this)) {
                if (isLocationRequired(this) || isLocationEnabled(
                        this
                    )
                ) {
                    startScan()
                    //                        customDialogFragment = CustomDialogFragmentNew {
//                            it?.let {
//                                if(isConnected(it))
//                                    connectBt(it)
//                            }
//                        }
//                    customDialogFragment = CustomDialogFragmentNew()

//                        supportFragmentManager.let { customDialogFragment?.show(it, "dilaog") }
//                    val intent = Intent(this@SetUpStadiaActivity,ScannerActivity::class.java)
//                    startActivity(intent);
                } else {
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }
            } else {
                val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivity(enableIntent)
            }
        }
        else {
            // Request Bluetooth-related permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                var allPermissionsGranted = true
                for (permission in BLE_PERMISSIONS) {
                    if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false
                        break
                    }
                }
                if (allPermissionsGranted) {
                    // Permissions are already granted
                    markLocationPermissionRequested(this)
                } else {
                    markLocationPermissionRequested(this)
                    ActivityCompat.requestPermissions(
                        this,
                        BLE_PERMISSIONS,
                        REQUEST_ACCESS_COARSE_LOCATION
                    )
                }
            } else {
                // Permissions are already granted (pre-Android 6.0)
                markLocationPermissionRequested(this)
            }
        }
    }

    private fun startScan() {
        binding.statusTextView.text = "Scanning..."
        val scanner = bluetoothAdapter!!.bluetoothLeScanner
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(null, scanSettings, scanCallback)
    }

    fun stopScan() {
        binding.statusTextView.text = "Tap to scan again"
        val scanner = bluetoothAdapter!!.bluetoothLeScanner
        scanner.stopScan(scanCallback)
//        mScannerStateLiveData.scanningStopped()
    }

    private fun connectToDevice(device: BluetoothDevice) {
        device.connectGatt(this, false, gattCallback)
    }

    private fun canConnect(): Boolean {
        val deniedPermissions: MutableList<String> = ArrayList()
        if (!checkPermission(Manifest.permission.BLUETOOTH_SCAN)) deniedPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
        if (!checkPermission(Manifest.permission.BLUETOOTH_CONNECT)) deniedPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        return if (deniedPermissions.isEmpty()||Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            true
        }
        else {
            requestBlePermissions(this, CONNECT_PERMISSIONS_CODE)
            false
        }
    }

    private fun checkPermission(permission: String): Boolean {
        return (ActivityCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED)
    }

    companion object {

    }
}
