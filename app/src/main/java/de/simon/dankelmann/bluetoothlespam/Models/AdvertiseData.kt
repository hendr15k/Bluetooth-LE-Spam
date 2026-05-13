package de.simon.dankelmann.bluetoothlespam.Models

import android.bluetooth.le.AdvertiseData
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
    }

    fun getRawDataSize(): Int {
        var size = 0

        services.forEach { service ->
            if (service.serviceUuid != null) {
                val uuidBytes = 16
                size += 1 + uuidBytes
                if (service.serviceData != null) {
                    size += service.serviceData!!.size
                }
            }
        }

        manufacturerData.forEach { mfgData ->
            size += 3 + mfgData.manufacturerSpecificData.size
        }

        return size
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