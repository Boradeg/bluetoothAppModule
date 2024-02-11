package com.example.bluetoothappmodule

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId", "MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val enableBluetoothButton = findViewById<Button>(R.id.btn)

        enableBluetoothButton.setOnClickListener {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                // Device doesn't support Bluetooth
                Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show()
            } else {
                if (bluetoothAdapter.isEnabled) {
                    // Bluetooth is already enabled
                    Toast.makeText(this, "Bluetooth is already on", Toast.LENGTH_SHORT).show()
                } else {
                    // Bluetooth is not enabled, open intent to turn it on
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Bluetooth was successfully enabled
                Toast.makeText(this, "Bluetooth turned on successfully", Toast.LENGTH_SHORT).show()
            } else {
                // User declined to enable Bluetooth or an error occurred
                Toast.makeText(this, "Failed to turn on Bluetooth", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 1
    }
}
