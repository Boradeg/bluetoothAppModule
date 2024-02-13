package com.example.bluetoothappmodule

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.shashank.sony.fancytoastlib.FancyToast

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        BluetoothAdapter.getDefaultAdapter()
    }

    private lateinit var deviceListView: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var deviceListAdapter: ArrayAdapter<String>
    private val pairedDevices = mutableListOf<BluetoothDevice>()
    private lateinit var pairedDeviceListView: ListView
    private lateinit var pairedDeviceListAdapter: ArrayAdapter<String>
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    handleBluetoothDevice(intent)
                }
            }
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val actionBar = supportActionBar
        if (actionBar != null) {
            val textColor = Color.parseColor("#FDFDFD")
            actionBar.setTitle("Bluetooth App")
            actionBar.setLogo(R.drawable.bluetooth)
            actionBar.setDisplayShowTitleEnabled(true)
            actionBar.setTitle(Html.fromHtml("<font color='$textColor'>Bluetooth App</font>"))
        }
        pairedDeviceListView = findViewById(R.id.pairedDeviceListView)
        // refresh = findViewById(R.id.refresh)
        pairedDeviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        pairedDeviceListView.adapter = pairedDeviceListAdapter
        // Register BroadcastReceiver to listen for ACTION_FOUND
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))

        // Populate the paired devices list
        populatePairedDevicesList()

        deviceListView = findViewById(R.id.deviceListView)
        progressBar = findViewById(R.id.discoveryProgressBar)
        deviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        deviceListView.adapter = deviceListAdapter
        showNearbyDevices()
        val enableBluetoothButton = findViewById<Button>(R.id.enableBluetoothButton)
        val showNearbyDevicesButton = findViewById<Button>(R.id.showNearbyDevicesButton)
        enableBluetoothButton.setOnClickListener {
            enableBluetooth()
        }

        showNearbyDevicesButton.setOnClickListener {
            showNearbyDevices()
        }
        pairedDeviceListView.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = parent.getItemAtPosition(position) as String
            showToastInfo("Clicked on: $selectedItem")
            // Retrieve the BluetoothDevice object associated with the clicked item
            val selectedDeviceAddress =
                selectedItem.split(" - ")[1] // Assuming the format is "Name - Address"
            val bluetoothDevice = pairedDevices.find { it.address == selectedDeviceAddress }
            // Pass the BluetoothDevice object to another function
            bluetoothDevice?.let { device ->
                showUnpairDialog(device)
            }
        }

        deviceListView.setOnItemClickListener { _, _, position, _ ->
            progressBar.visibility = View.VISIBLE
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

    @SuppressLint("MissingPermission")
    private fun pairDevice(device: BluetoothDevice) {
        when (device.bondState) {
            BluetoothDevice.BOND_BONDED -> {
                showUnpairDialog(device)
                showToastInfo("Device ${device.name} is already paired")
                progressBar.visibility = View.GONE
                updatePairedDevicesList() // Update the list of paired devices
            }

            BluetoothDevice.BOND_NONE -> {
                try {
                    val method = device.javaClass.getMethod("createBond")
                    method.invoke(device)
                    showToastInfo("Pairing with device: ${device.address}")
                    populatePairedDevicesList()
                    // Notify the adapter about the changes in the data set
                    pairedDeviceListAdapter.notifyDataSetChanged()


                } catch (e: Exception) {
                    showToastError("Pairing failed: ${e.message}")
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun populatePairedDevicesList() {
        // Clear the existing list
        pairedDevices.clear()
        pairedDeviceListAdapter.clear()

        // Get the list of paired devices
        val pairedDevicesSet = bluetoothAdapter?.bondedDevices

        // Add paired devices to the list and adapter
        pairedDevicesSet?.forEach { device ->
            pairedDevices.add(device)
            pairedDeviceListAdapter.add("${device.name} - ${device.address}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun updatePairedDevicesList() {
        // Clear the existing list
        pairedDevices.clear()
        pairedDeviceListAdapter.clear()

        // Get the list of paired devices
        val pairedDevicesSet = bluetoothAdapter?.bondedDevices

        // Add paired devices to the list and adapter
        pairedDevicesSet?.forEach { device ->
            pairedDevices.add(device)
            pairedDeviceListAdapter.add("${device.name} - ${device.address}")
        }

        // Notify the adapter about the changes in the data set
        pairedDeviceListAdapter.notifyDataSetChanged()
    }
    @SuppressLint("MissingPermission")
    private fun showUnpairDialog(device: BluetoothDevice) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Unpair Device")
        builder.setMessage("Do you want to unpair ${device.name}?")
        builder.setPositiveButton("Yes") { _, _ ->

            unpairDevice(device)
            // After unpairing, update the paired devices list
            updatePairedDevicesList()

        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }


    @SuppressLint("MissingPermission")
    private fun unpairDevice(device: BluetoothDevice) {
        try {
            val method = device.javaClass.getMethod("removeBond")
            method.invoke(device)
            showToastSuccess("Device ${device.name} unpaired successfully")
            // Remove device from the list of paired devices
            pairedDevices.remove(device)
            // Remove device from the list view
            pairedDeviceListAdapter.remove("${device.name} - ${device.address}")
            populatePairedDevicesList()
        } catch (e: Exception) {
            showToastError("Failed to unpair device: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableBluetooth() {
        bluetoothAdapter?.takeUnless { it.isEnabled }?.apply {
            // Bluetooth is not enabled, start the enable Bluetooth intent
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } ?: run {
            // Bluetooth is already enabled, show toast message
            showToastWarning("Bluetooth is already enabled")
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
                showToastWarning("Bluetooth is not enabled")
            }
        } else {
            // Permission not granted, request it
            requestLocationPermission()
        }
    }

    private val discoveredDevicesSet = mutableSetOf<String>()

    @SuppressLint("MissingPermission")
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
    private fun showToastSuccess(message: String) {
        FancyToast.makeText(this,message,FancyToast.LENGTH_LONG,FancyToast.SUCCESS,true).show();

    }
    private fun showToastError(message: String) {
        FancyToast.makeText(this,message,FancyToast.LENGTH_LONG,FancyToast.ERROR,true).show();

    }
    private fun showToastInfo(message: String) {
        FancyToast.makeText(this,message,FancyToast.LENGTH_LONG,FancyToast.INFO,true).show();

    }
    private fun showToastWarning(message: String) {
        FancyToast.makeText(this,message,FancyToast.LENGTH_LONG,FancyToast.WARNING,true).show();

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
                    showToastError("Permission denied. Unable to discover nearby devices.")
                }
                return
            }

            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        populatePairedDevicesList()
        //Toast.makeText(this, "onResume", Toast.LENGTH_SHORT).show()
        progressBar.visibility = View.GONE
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    }

    override fun onPause() {

        super.onPause()
        unregisterReceiver(bluetoothReceiver)
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {

        super.onDestroy()
        bluetoothAdapter?.cancelDiscovery()
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_LOCATION_PERMISSION = 2
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                // Handle refresh button click event here
                // Perform refresh operation
                //Toast.makeText(this, "Refresh button clicked", Toast.LENGTH_SHORT).show()
                populatePairedDevicesList()
                // Notify the adapter about the changes in the data set
                pairedDeviceListAdapter.notifyDataSetChanged()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


}
