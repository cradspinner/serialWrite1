package com.andyle9191.myapplication7

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.content.getSystemService
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    lateinit var m_usbManager: UsbManager
    var m_usbDevice: UsbDevice? = null
    var m_serial: UsbSerialDevice? = null
    var m_connection: UsbDeviceConnection? = null
    var ACTION_USB_PERMISSION = "permission"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        m_usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        registerReceiver(broadcastReceiver, filter)


        button_on.setOnClickListener { sendData("o") }
        button2_off.setOnClickListener { sendData("x") }
        button3_connect.setOnClickListener { startUsbConnecting() }
        button4_disconnect.setOnClickListener { disconnect() }
    }

    private fun startUsbConnecting() {
        val usbDevice: HashMap<String, UsbDevice>? = m_usbManager.deviceList
        if (!usbDevice?.isEmpty()!!) {
            var keep = true
            usbDevice.forEach { entry ->
                m_usbDevice = entry.value
                val deviceVenderId: Int? = m_usbDevice?.vendorId
                Log.i("serial", "vendorId: " + deviceVenderId)
                if (deviceVenderId == 1027) {//**********update for different devices****************************************!!!!
                    val intent: PendingIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
                    m_usbManager.requestPermission(m_usbDevice, intent)
                    keep = false
                    Log.i("serial", "connection successful")
                } else {
                    m_connection = null
                    m_usbDevice = null
                    Log.i("serial", "unable to connect")
                }
            }
            if (!keep) {
                return
            }
        } else {
            Log.i("serial", "no usb device connected")
        }
    }

    private fun sendData(input: String) {
        m_serial?.write(input.toByteArray())
        Log.i("serial", "sending_data: " + input.toByteArray())
    }

    private fun disconnect() {
        m_serial?.close()
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action!! == ACTION_USB_PERMISSION) {
                val granted: Boolean = intent.extras!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                if (granted) {
                    m_connection = m_usbManager.openDevice(m_usbDevice)
                    m_serial = UsbSerialDevice.createUsbSerialDevice(m_usbDevice,m_connection)
                    if (m_serial != null) {
                        if (m_serial!!.open()) {
                            m_serial!!.setBaudRate(9600)
                            m_serial!!.setDataBits(UsbSerialInterface.DATA_BITS_8)
                            m_serial!!.setStopBits(UsbSerialInterface.STOP_BITS_1)
                            m_serial!!.setParity(UsbSerialInterface.PARITY_NONE)
                            m_serial!!.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
                            //setup here to setup reading serial
                        } else {
                            Log.i("serial", "port not open")
                        }
                    } else {
                        Log.i("serial", " port is null")
                    }
                } else {
                    Log.i("serial", "permission not granted")
                }
            } else if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                startUsbConnecting()

            } else if (intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                disconnect()
            }

        }
    }


}
