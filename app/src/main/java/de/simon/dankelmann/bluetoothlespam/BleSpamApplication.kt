package de.simon.dankelmann.bluetoothlespam

import android.app.Application
import de.simon.dankelmann.bluetoothlespam.Handlers.AdvertisementSetQueueHandler
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothHelpers
import de.simon.dankelmann.bluetoothlespam.Helpers.ThemeManager
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IAdvertisementService
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IBluetoothLeScanService
import de.simon.dankelmann.bluetoothlespam.Services.BluetoothLeScanService


class BleSpamApplication : Application() {

    lateinit var advertisementService: IAdvertisementService
        private set

    lateinit var queueHandler: AdvertisementSetQueueHandler
        private set

    lateinit var scanService: IBluetoothLeScanService
        private set

    override fun onCreate() {
        // Apply the user's theme preference before calling super.onCreate()
        // to ensure the theme is set before any UI is created
        ThemeManager.getInstance().applyTheme(this)

        super.onCreate()

        setupAdvertisementService()
        scanService = BluetoothLeScanService(this)
    }

    fun setupAdvertisementService() {
        val newAdvertisementService = BluetoothHelpers.getAdvertisementService(this)

        if (!::queueHandler.isInitialized) {
            advertisementService = newAdvertisementService
            queueHandler = AdvertisementSetQueueHandler(this, advertisementService)
            return
        }

        val txPowerLevel = advertisementService.getTxPowerLevel()
        newAdvertisementService.setTxPowerLevel(txPowerLevel)
        queueHandler.setAdvertisementService(newAdvertisementService)
        advertisementService = newAdvertisementService
    }
}
