package com.halil.ozel.bluetoothchatapp

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.preference.PreferenceManager
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
//import com.sachtech.stadia.utils.BluetoothConnector
import java.io.IOException
import java.util.*

object BluetoothHelper {
    val bluetoothAdapter: BluetoothAdapter =  BluetoothAdapter.getDefaultAdapter()
    var onDeviceScan: ((BluetoothDevice) -> Unit)? = null
    private const val PREFS_LOCATION_NOT_REQUIRED = "location_not_required"
    private const val PREFS_PERMISSION_REQUESTED = "permission_requested"
    private const val DEFAULT_UUID = "00001101-0000-1000-8000-00805f9b34fb"
    val uuid = UUID.fromString(DEFAULT_UUID)

    fun isEnabled(): Boolean {
        return bluetoothAdapter.isEnabled == true
    }

    /**
     * Checks whether Bluetooth is enabled.
     *
     * @return true if Bluetooth is enabled, false otherwise.
     */
    fun isBleEnabled(context: Context): Boolean {
        val bluetoothManager: BluetoothManager? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ContextCompat.getSystemService(
                    context,
                    BluetoothManager::class.java
                )
            }
            else {
                null
            }


        val bluetoothAdapter: BluetoothAdapter? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bluetoothManager?.adapter
            } else {
                BluetoothAdapter.getDefaultAdapter()
            }


        val adapter = bluetoothAdapter
        return adapter != null && adapter.isEnabled
    }

    /**
     * Checks for required permissions.
     *
     * @return true if permissions are already granted, false otherwise.
     */
    fun isLocationPermissionsGranted(context: Context?): Boolean {
        return ContextCompat.checkSelfPermission(
            context!!,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Returns true if location permission has been requested at least twice and
     * user denied it, and checked 'Don't ask again'.
     *
     * @param activity the activity
     * @return true if permission has been denied and the popup will not come up any more, false otherwise
     */
    fun isLocationPermissionDeniedForever(activity: Activity?): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(activity)
        return (!isLocationPermissionsGranted(activity) // Location permission must be denied
                && preferences.getBoolean(
            PREFS_PERMISSION_REQUESTED,
            false
        ) // Permission must have been requested before
                && !ActivityCompat.shouldShowRequestPermissionRationale(
            activity!!,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )) // This method should return false
    }

    /**
     * On some devices running Android Marshmallow or newer location BackGroundServices must be enabled in order to scan for Bluetooth LE devices.
     * This method returns whether the Location has been enabled or not.
     *
     * @return true on Android 6.0+ if location mode is different than LOCATION_MODE_OFF. It always returns true on Android versions prior to Marshmallow.
     */
    fun isLocationEnabled(context: Context): Boolean {
        if (isMarshmallowOrAbove()) {
            var locationMode = Settings.Secure.LOCATION_MODE_OFF
            try {
                locationMode = Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.LOCATION_MODE
                )
            } catch (e: SettingNotFoundException) {
                // do nothing
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF
        }
        return true
    }

    /**
     * Location enabled is required on some phones running Android Marshmallow or newer (for example on Nexus and Pixel devices).
     *
     * @param context the context
     * @return false if it is known that location is not required, true otherwise
     */
    fun isLocationRequired(context: Context?): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getBoolean(PREFS_LOCATION_NOT_REQUIRED, isMarshmallowOrAbove())
    }

    /**
     * When a Bluetooth LE packet is received while Location is disabled it means that Location
     * is not required on this device in order to scan for LE devices. This is a case of Samsung phones, for example.
     * Save this information for the future to keep the Location info hidden.
     *
     * @param context the context
     */
    fun markLocationNotRequired(context: Context?) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putBoolean(PREFS_LOCATION_NOT_REQUIRED, false).apply()
    }

    /**
     * The first time an app requests a permission there is no 'Don't ask again' checkbox and
     * [ActivityCompat.shouldShowRequestPermissionRationale] returns false.
     * This situation is similar to a permission being denied forever, so to distinguish both cases
     * a flag needs to be saved.
     *
     * @param context the context
     */
    fun markLocationPermissionRequested(context: Context?) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putBoolean(PREFS_PERMISSION_REQUESTED, true).apply()
    }

    fun isMarshmallowOrAbove(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    /**
     * check if BLE Supported device
     */
    fun isBLESupported(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    /**
     * get BluetoothManager
     */
    fun getManager(context: Context): BluetoothManager {
        return context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    fun getPairedDevice(): Array<BluetoothDevice> {
        return bluetoothAdapter.bondedDevices.toTypedArray()
    }

    fun startScan(onDeviceScan: (BluetoothDevice) -> Unit) {
        this.onDeviceScan = onDeviceScan
        bluetoothAdapter.startDiscovery()
    }

    fun cancelSacn() {
        onDeviceScan = null
        bluetoothAdapter.cancelDiscovery()
    }




    val bReciever: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                // Create a new device item
                onDeviceScan?.invoke(device!!)

            }
        }
    }



}