package com.example.bluetoothappmodule

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        BluetoothAdapter.getDefaultAdapter()
    }

    private lateinit var deviceListView: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var deviceListAdapter: ArrayAdapter<String>

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    handleBluetoothDevice(intent)

                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        deviceListView = findViewById(R.id.deviceListView)
        progressBar = findViewById(R.id.discoveryProgressBar)
        deviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        deviceListView.adapter = deviceListAdapter

        val enableBluetoothButton = findViewById<Button>(R.id.enableBluetoothButton)
        val showNearbyDevicesButton = findViewById<Button>(R.id.showNearbyDevicesButton)

        enableBluetoothButton.setOnClickListener {
            enableBluetooth()
        }

        showNearbyDevicesButton.setOnClickListener {

            showNearbyDevices()
        }
        deviceListView.setOnItemClickListener { _, _, position, _ ->
            progressBar.visibility= View.VISIBLE
            val deviceInfo = deviceListAdapter.getItem(position)
            deviceInfo?.let {
                val address = it.split(" - ")[1] // Assuming the format is "Name - Address"
                val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(address)
                bluetoothDevice?.let { device ->
                    pairDevice(device)
                }
            }
           // progressBar.visibility= View.GONE
        }
    }
    private fun pairDevice(device: BluetoothDevice) {
        try {
            val method = device.javaClass.getMethod("createBond")
            method.invoke(device)
            showToast("Pairing with device: ${device.address}")

        } catch (e: Exception) {
            showToast("Pairing failed: ${e.message}")
            Toast.makeText(this, "pair device exc", Toast.LENGTH_SHORT).show()
            progressBar.visibility= View.GONE
        }
    }

    private fun enableBluetooth() {
        bluetoothAdapter?.takeUnless { it.isEnabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    private fun showNearbyDevices() {
        //deviceListAdapter.clear()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
        } else {
            startBluetoothDiscovery()
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION_PERMISSION
        )
    }

    private fun startBluetoothDiscovery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            if (bluetoothAdapter != null && bluetoothAdapter!!.isEnabled) {
                bluetoothAdapter!!.startDiscovery()
            } else {
                showToast("Bluetooth is not enabled")
            }
        } else {
            // Permission not granted, request it
            requestLocationPermission()
        }
    }

    private val discoveredDevicesSet = mutableSetOf<String>()

    private fun handleBluetoothDevice(intent: Intent) {
        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        device?.let {
            val deviceInfo = "${it.name} - ${it.address}"
            if (deviceInfo !in discoveredDevicesSet) {
                discoveredDevicesSet.add(deviceInfo)
                deviceListAdapter.add(deviceInfo)
            }
        }
    }



    private fun showToast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    startBluetoothDiscovery()
                } else {
                    showToast("Permission denied. Unable to discover nearby devices.")
                }
                return
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    override fun onResume() {
        Toast.makeText(this, "onResume", Toast.LENGTH_SHORT).show()

        progressBar.visibility= View.GONE
        super.onResume()
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    }

    override fun onPause() {

        super.onPause()
        unregisterReceiver(bluetoothReceiver)
    }

    override fun onDestroy() {

        super.onDestroy()
        bluetoothAdapter?.cancelDiscovery()
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_LOCATION_PERMISSION = 2
    }
}
