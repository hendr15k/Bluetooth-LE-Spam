package de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators

import android.bluetooth.le.AdvertisingSetParameters
import android.os.ParcelUuid
import de.simon.dankelmann.bluetoothlespam.Callbacks.GenericAdvertisingCallback
import de.simon.dankelmann.bluetoothlespam.Callbacks.GenericAdvertisingSetCallback
import de.simon.dankelmann.bluetoothlespam.Constants.Constants
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertiseMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetRange
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Enums.PrimaryPhy
import de.simon.dankelmann.bluetoothlespam.Enums.SecondaryPhy
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.ServiceData
import java.util.UUID

class HidAdvertisementSetGenerator : IAdvertisementSetGenerator {

    // Based on the "NameFlood" protocol of the Flipper Zero BLE Spam app:
    // https://github.com/Flipper-XFW/Xtreme-Apps/blob/dev/ble_spam/protocols/nameflood.c
    //
    // The Flipper crafts a raw advertising packet with a custom local name, the
    // Human Interface Device (HID) service UUID (0x1812) and a TX power level.
    // Android's advertising API does not allow a custom local name, so this uses
    // the device's own Bluetooth name instead.

    private val _hidServiceUuid = ParcelUuid(UUID.fromString(Constants.UUID_HUMAN_INTERFACE_DEVICE))

    override fun getAdvertisementSets(inputData: Map<String, String>?): List<AdvertisementSet> {
        val advertisementSet = AdvertisementSet()
        advertisementSet.target = AdvertisementTarget.ADVERTISEMENT_TARGET_HID
        advertisementSet.type = AdvertisementSetType.ADVERTISEMENT_TYPE_HID
        advertisementSet.range = AdvertisementSetRange.ADVERTISEMENTSET_RANGE_CLOSE

        // Advertise Settings
        advertisementSet.advertiseSettings.advertiseMode = AdvertiseMode.ADVERTISEMODE_LOW_LATENCY
        advertisementSet.advertiseSettings.txPowerLevel = TxPowerLevel.TX_POWER_HIGH
        advertisementSet.advertiseSettings.connectable = false
        advertisementSet.advertiseSettings.timeout = 0

        // Advertising Parameters
        advertisementSet.advertisingSetParameters.legacyMode = true
        advertisementSet.advertisingSetParameters.interval = AdvertisingSetParameters.INTERVAL_MIN
        advertisementSet.advertisingSetParameters.txPowerLevel = TxPowerLevel.TX_POWER_HIGH
        advertisementSet.advertisingSetParameters.primaryPhy = PrimaryPhy.PHY_LE_1M
        advertisementSet.advertisingSetParameters.secondaryPhy = SecondaryPhy.PHY_LE_1M

        // AdvertiseData
        advertisementSet.advertiseData.includeDeviceName = true

        val serviceData = ServiceData()
        serviceData.serviceUuid = _hidServiceUuid
        serviceData.serviceData = null
        advertisementSet.advertiseData.services.add(serviceData)
        advertisementSet.advertiseData.includeTxPower = true

        // General Data
        advertisementSet.title = "Bluetooth Keyboard"

        // Callbacks
        advertisementSet.advertisingSetCallback = GenericAdvertisingSetCallback()
        advertisementSet.advertisingCallback = GenericAdvertisingCallback()

        return listOf(advertisementSet)
    }
}
