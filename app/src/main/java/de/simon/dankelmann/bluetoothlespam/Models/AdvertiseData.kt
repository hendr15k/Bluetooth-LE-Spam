package de.simon.dankelmann.bluetoothlespam.Models

import android.bluetooth.le.AdvertiseData
import android.os.ParcelUuid
import android.util.Log
import java.io.Serializable

class AdvertiseData : Serializable {
    private var _logTag = "AdvertiseData"

    var id = 0
    var includeDeviceName = true
    var includeTxPower = true

    var manufacturerData = mutableListOf<ManufacturerSpecificData>()
    var services = mutableListOf<ServiceData>()

    companion object {
        const val MAX_LEGACY_ADVERTISING_DATA_SIZE = 31
        // Least-significant 64 bits of the Bluetooth Base UUID 00000000-0000-1000-8000-00805F9B34FB.
        // Used to detect 16-bit / 32-bit short UUIDs, which are encoded in fewer bytes on the wire.
        private const val BLUETOOTH_BASE_UUID_LEAST_SIGNIFICANT_BITS = -9223371485494954757L
    }

    fun getRawDataSize(): Int {
        var size = 0

        services.forEach { service ->
            if (service.serviceUuid != null) {
                val uuidBytes = getUuidSizeBytes(service.serviceUuid!!)
                size += 1 + 1 + uuidBytes
                if (service.serviceData != null) {
                    size += 1 + 1 + uuidBytes + service.serviceData!!.size
                }
            }
        }

        manufacturerData.forEach { mfgData ->
            size += 1 + 1 + 2 + mfgData.manufacturerSpecificData.size
        }

        if (includeTxPower) {
            size += 1 + 1 + 1
        }

        return size
    }

    /**
     * Returns the number of bytes a ParcelUuid occupies in a legacy BLE AD structure.
     * 16-bit UUIDs (e.g. Fast Pair 0xFE2C) take 2 bytes, 32-bit take 4, full 128-bit take 16.
     */
    private fun getUuidSizeBytes(parcelUuid: ParcelUuid): Int {
        val uuid = parcelUuid.uuid
        // Short Bluetooth UUIDs share the Base UUID's lower 96 bits.
        if (uuid.leastSignificantBits != BLUETOOTH_BASE_UUID_LEAST_SIGNIFICANT_BITS) return 16
        val mostSignificantBits = uuid.mostSignificantBits
        if ((mostSignificantBits and 0xFFFFFFFFL) != 0x00001000L) return 16
        val shortUuid = mostSignificantBits ushr 32
        return if ((shortUuid and 0xFFFF0000L) == 0L) 2 else 4
    }

    fun validate(): Boolean {
        val size = getRawDataSize()
        if (size > MAX_LEGACY_ADVERTISING_DATA_SIZE) {
            Log.w(_logTag, "AdvertiseData exceeds ${MAX_LEGACY_ADVERTISING_DATA_SIZE} bytes (raw: $size bytes)")
            return false
        }
        return true
    }
    fun build() : AdvertiseData?{
        if(validate()){
            var builder = AdvertiseData.Builder()

            builder.setIncludeDeviceName(includeDeviceName)
            
            services.forEach {
                if(it.serviceUuid != null){
                    builder.addServiceUuid(it.serviceUuid)
                    if(it.serviceData != null){
                        builder.addServiceData(it.serviceUuid, it.serviceData)
                    }
                }
            }

            builder.setIncludeTxPowerLevel(includeTxPower)

            manufacturerData.forEach {
                builder.addManufacturerData(it.manufacturerId, it.manufacturerSpecificData)
            }

            return builder.build()
        } else {
            Log.d(_logTag, "AdvertiseDataModel could not be built because its invalid")
        }
        return null
    }
}