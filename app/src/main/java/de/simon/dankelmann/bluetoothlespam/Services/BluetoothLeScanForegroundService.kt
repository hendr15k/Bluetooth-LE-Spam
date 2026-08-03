package de.simon.dankelmann.bluetoothlespam.Services

import android.app.Notification
import android.app.Notification.FOREGROUND_SERVICE_IMMEDIATE
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavDeepLinkBuilder
import de.simon.dankelmann.bluetoothlespam.BleSpamApplication
import de.simon.dankelmann.bluetoothlespam.Enums.stringRes
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBluetoothLeScanCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IBluetoothLeScanService
import de.simon.dankelmann.bluetoothlespam.MainActivity
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult
import de.simon.dankelmann.bluetoothlespam.Models.SpamPackageScanResult
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import de.simon.dankelmann.bluetoothlespam.R

class BluetoothLeScanForegroundService: IBluetoothLeScanCallback, Service() {

    private val _channelId = "BluetoothLeSpamScanService"
    private val _channelName = "Bluetooth Le Spam Scan Service"
    private val _channelDescription = "Bluetooth Le Spam Notifications"

    private val _binder: IBinder = LocalBinder()

    private var notifyOnNewSpam = true
    private var notifyOnNewFlipper = true

    companion object {
        private val _logTag = "AdvertisementScanForegroundService"

        fun startService(context: Context): Boolean {
            if (!PermissionCheck.hasScanPermission(context)) {
                Log.w(_logTag, "Cannot start scan service without scan permission")
                return false
            }

            val startIntent = Intent(context, BluetoothLeScanForegroundService::class.java)
            return try {
                ContextCompat.startForegroundService(context, startIntent)
                true
            } catch (error: SecurityException) {
                Log.e(_logTag, "Unable to start scan foreground service", error)
                false
            } catch (error: IllegalStateException) {
                Log.e(_logTag, "Unable to start scan foreground service", error)
                false
            }
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, BluetoothLeScanForegroundService::class.java)
            context.stopService(stopIntent)
        }
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        val foregroundServiceType = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            else -> 0
        }
        ServiceCompat.startForeground(
            this,
            2,
            createNotification(
                getString(R.string.spam_detecting_title),
                getString(R.string.spam_detecting_text),
                false
            ),
            foregroundServiceType,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(_logTag, "Started BLE Scan Foreground Service")

        val scanService = (applicationContext as BleSpamApplication).scanService
        scanService.addBluetoothLeScanServiceCallback(this)
        if (!scanService.startScanning()) {
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return _binder
    }

    inner class LocalBinder : Binder() {
        val service: BluetoothLeScanForegroundService
            get() = this@BluetoothLeScanForegroundService
    }

    override fun onDestroy() {
        Log.d(_logTag, "Destroying the Service")
        super.onDestroy()

        val scanService = (applicationContext as BleSpamApplication).scanService
        scanService.removeBluetoothLeScanServiceCallback(this)
        scanService.stopScanning()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val mChannel = NotificationChannel(_channelId, _channelName, NotificationManager.IMPORTANCE_HIGH)
            mChannel.description = _channelDescription
            mChannel.enableLights(true)
            mChannel.lightColor = Color.BLUE
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    private fun createNotification(
        title: String,
        subTitle: String,
        alertOnlyOnce: Boolean
    ): Notification {

        val pendingIntentTargeted = NavDeepLinkBuilder(this)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.nav_spam_detector)
            .createPendingIntent()

        val notificationView = RemoteViews(packageName, R.layout.bluetooth_le_scan_foreground_service_notification)
        notificationView.setTextViewText(
            R.id.bluetoothLeScanningForegroundNotificationTitle,
            title
        )
        notificationView.setTextViewText(
            R.id.bluetoothLeScanningForegroundNotificationSubTitle,
            subTitle
        )

        return NotificationCompat.Builder(this, _channelId)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(subTitle)
            .setSmallIcon(R.drawable.bluetooth)
            .setContentIntent(pendingIntentTargeted)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setChannelId(_channelId)
            .setOngoing(true)
            .setOnlyAlertOnce(alertOnlyOnce)
            .setCustomBigContentView(notificationView)
            .setCustomContentView(notificationView)
            .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE).build()
    }

    private fun updateNotification(title:String, subTitle: String, alertOnlyOnce:Boolean, id:Int){
        val scanService = (applicationContext as BleSpamApplication).scanService
        if (scanService.isScanning()) {
            val notification = createNotification(title, subTitle, alertOnlyOnce)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(id, notification)
        }
    }

    override fun onScanStateChanged(isScanning: Boolean) {
        if (!isScanning) {
            stopSelf()
        }
    }

    override fun onScanResult(scanResult: ScanResult) {
        //var model = BluetoothLeScanResult.parseFromScanResult(scanResult)
        //updateNotification(model.deviceName)
    }

    override fun onFlipperDeviceDetected(flipperDeviceScanResult: FlipperDeviceScanResult, alreadyKnown: Boolean) {
        if(!alreadyKnown || notifyOnNewFlipper){
            updateNotification("New Flipper: " + flipperDeviceScanResult.deviceName, "${flipperDeviceScanResult.address} | ${flipperDeviceScanResult.rssi} dBm", !notifyOnNewFlipper, 2)
        }
        notifyOnNewFlipper = false
    }

    override fun onFlipperListUpdated() {
        val scanService = (applicationContext as BleSpamApplication).scanService
        if (scanService.getFlipperDevicesList().isEmpty()) {
            notifyOnNewFlipper = true
        }
    }

    override fun onSpamResultPackageDetected(
        spamPackageScanResult: SpamPackageScanResult,
        alreadyKnown: Boolean
    ) {
        val spamPackageTypeText = getString(spamPackageScanResult.spamPackageType.stringRes())
        updateNotification(
            getString(R.string.spam_detected_title),
            spamPackageTypeText + " | " + spamPackageScanResult.address,
            !notifyOnNewSpam,
            2
        )
        notifyOnNewSpam = false
    }

    override fun onSpamResultPackageListUpdated() {
        val scanService = (applicationContext as BleSpamApplication).scanService
        if (scanService.getSpamPackageScanResultList().isEmpty()) {
            notifyOnNewSpam = true
        }
    }
}
