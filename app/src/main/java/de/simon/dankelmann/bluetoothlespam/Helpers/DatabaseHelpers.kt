package de.simon.dankelmann.bluetoothlespam.Helpers

import android.os.ParcelUuid
import androidx.sqlite.db.SupportSQLiteDatabase
import de.simon.dankelmann.bluetoothlespam.Database.AppDatabase
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseDataEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseDataManufacturerSpecificDataEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseDataServiceDataEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseSettingsEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisingSetParametersEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.PeriodicAdvertisingParametersEntity
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetRange
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers.Companion.toHexString
import de.simon.dankelmann.bluetoothlespam.Models.AdvertiseData
import de.simon.dankelmann.bluetoothlespam.Models.AdvertiseSettings
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisingSetParameters
import de.simon.dankelmann.bluetoothlespam.Models.ManufacturerSpecificData
import de.simon.dankelmann.bluetoothlespam.Models.PeriodicAdvertisingParameters
import de.simon.dankelmann.bluetoothlespam.Models.ServiceData
import java.util.UUID

class DatabaseHelpers {
    companion object{
        private const val _logTag = "DatabaseHelpers"

        fun saveAdvertisementSet(advertisementSet: AdvertisementSet):Int{

            val database = AppDatabase.getInstance()

            val advertisementSetEntity:AdvertisementSetEntity = AdvertisementSetEntity(
                advertisementSet.id,
                advertisementSet.title,
                advertisementSet.target,
                advertisementSet.type,
                advertisementSet.duration,
                advertisementSet.maxExtendedAdvertisingEvents,
                advertisementSet.range,
                0 ,
                0 ,
                0,
                0,
                0,
                0
            )

            val advertiseSettingsEntity = AdvertiseSettingsEntity(
                advertisementSet.advertiseSettings.id,
                advertisementSet.advertiseSettings.advertiseMode,
                advertisementSet.advertiseSettings.txPowerLevel,
                advertisementSet.advertiseSettings.connectable,
                advertisementSet.advertiseSettings.timeout
            )

            val advertiseSettingsId = database.advertiseSettingsDao().insertItem(advertiseSettingsEntity).toInt()
            advertisementSetEntity.advertiseSettingsId = advertiseSettingsId

            val advertisingSetParametersEntity = AdvertisingSetParametersEntity(
                advertisementSet.advertisingSetParameters.id,
                advertisementSet.advertisingSetParameters.legacyMode,
                advertisementSet.advertisingSetParameters.interval,
                advertisementSet.advertisingSetParameters.txPowerLevel,
                advertisementSet.advertisingSetParameters.includeTxPowerLevel,
                advertisementSet.advertisingSetParameters.primaryPhy,
                advertisementSet.advertisingSetParameters.secondaryPhy,
                advertisementSet.advertisingSetParameters.scanable,
                advertisementSet.advertisingSetParameters.connectable,
                advertisementSet.advertisingSetParameters.anonymous)
            val advertisingSetParametersId = database.advertisingSetParametersDao().insertItem(advertisingSetParametersEntity).toInt()
            advertisementSetEntity.advertisingSetParametersId = advertisingSetParametersId

            val advertiseDataEntityId = saveAdvertiseData(advertisementSet.advertiseData, database)
            advertisementSetEntity.advertiseDataId = advertiseDataEntityId

            val scanResponseId: Int? = advertisementSet.scanResponse?.let {
                saveAdvertiseData(it, database)
            }
            advertisementSetEntity.scanResponseId = scanResponseId

            val periodicAdvertiseDataId: Int? = advertisementSet.periodicAdvertiseData?.let {
                saveAdvertiseData(it, database)
            }
            advertisementSetEntity.periodicAdvertiseDataId = periodicAdvertiseDataId

            advertisementSet.periodicAdvertisingParameters?.let { periodicAdvertisingParameters ->
                val periodicAdvertisingParametersEntity = PeriodicAdvertisingParametersEntity(
                    periodicAdvertisingParameters.id,
                    periodicAdvertisingParameters.includeTxPowerLevel,
                    periodicAdvertisingParameters.interval
                )
                val periodicAdvertisingParametersId = database.periodicAdvertisingParametersDao().insertItem(periodicAdvertisingParametersEntity).toInt()
                advertisementSetEntity.periodicAdvertisingParametersId = periodicAdvertisingParametersId
            }

            return database.advertisementSetDao().insertItem(advertisementSetEntity).toInt()
        }
        fun saveAdvertiseData(advertiseData: AdvertiseData, database: AppDatabase): Int{
            val advertiseDataEntity = AdvertiseDataEntity(
                advertiseData.id,
                advertiseData.includeDeviceName,
                advertiseData.includeTxPower
            )

            // SAVE
            val advertiseDataId = database.advertiseDataDao().insertItem(advertiseDataEntity).toInt()

            // SAVE SERVICE DATA
            advertiseData.services.forEach {service ->
                val advertiseDataServiceDataEntity = AdvertiseDataServiceDataEntity(
                    service.id,
                    advertiseDataId,
                    UUID.fromString(service.serviceUuid.toString()),
                    service.serviceData?.toHexString()
                )

                database.advertiseDataServiceDataDao().insertItem(advertiseDataServiceDataEntity)
            }

            // SAVE MANUFACTURER DATA
            advertiseData.manufacturerData.forEach { manufacturerSpecificData ->
                val manufacturerSpecificDataEntity = AdvertiseDataManufacturerSpecificDataEntity(
                    manufacturerSpecificData.id,
                    advertiseDataId,
                    manufacturerSpecificData.manufacturerId,
                    manufacturerSpecificData.manufacturerSpecificData.toHexString()
                )

                database.advertiseDataManufacturerSpecificDataDao().insertItem(manufacturerSpecificDataEntity)
            }

            // RETURN ID
            return advertiseDataId
        }

        fun getAdvertisementSetFromEntity(advertisementSetEntity: AdvertisementSetEntity):AdvertisementSet{
            val advertisementSet = AdvertisementSet()

            // Data
            advertisementSet.id = advertisementSetEntity.id
            advertisementSet.title = advertisementSetEntity.title
            advertisementSet.target = advertisementSetEntity.target
            advertisementSet.type = advertisementSetEntity.type
            advertisementSet.duration = advertisementSetEntity.duration
            advertisementSet.maxExtendedAdvertisingEvents = advertisementSetEntity.maxExtendedAdvertisingEvents
            advertisementSet.range = advertisementSetEntity.range

            val database = AppDatabase.getInstance()

            // Advertise Settings
            try {
                database.advertiseSettingsDao().findById(advertisementSetEntity.advertiseSettingsId)?.let { entity ->
                    advertisementSet.advertiseSettings = AdvertiseSettings().apply {
                        id = entity.id
                        advertiseMode = entity.advertiseMode
                        txPowerLevel = entity.txPowerLevel
                        connectable = entity.connectable
                        timeout = entity.timeout
                    }
                }
            } catch (e: Exception) {
                // Linked row missing — ignore and leave default AdvertiseSettings in place.
            }

            // AdvertisingSetParameters
            try {
                database.advertisingSetParametersDao().findById(advertisementSetEntity.advertisingSetParametersId)?.let { entity ->
                    advertisementSet.advertisingSetParameters = AdvertisingSetParameters().apply {
                        id = entity.id
                        legacyMode = entity.legacyMode
                        interval = entity.interval
                        txPowerLevel = entity.txPowerLevel
                        includeTxPowerLevel = entity.includeTxPowerLevel
                        primaryPhy = entity.primaryPhy
                        secondaryPhy = entity.secondaryPhy
                        scanable = entity.scanable
                        connectable = entity.connectable
                        anonymous = entity.anonymous
                    }
                }
            } catch (e: Exception) {
                // Linked row missing — ignore and leave default AdvertisingSetParameters in place.
            }

            advertisementSetEntity.advertiseDataId?.let { id ->
                database.advertiseDataDao().findById(id)?.let { entity ->
                    advertisementSet.advertiseData = getAdvertiseDataFromEntity(entity, database)
                }
            }

            advertisementSetEntity.scanResponseId?.let { id ->
                database.advertiseDataDao().findById(id)?.let { entity ->
                    advertisementSet.scanResponse = getAdvertiseDataFromEntity(entity, database)
                }
            }

            advertisementSetEntity.periodicAdvertiseDataId?.let { id ->
                database.advertiseDataDao().findById(id)?.let { entity ->
                    advertisementSet.periodicAdvertiseData = getAdvertiseDataFromEntity(entity, database)
                }
            }

            advertisementSetEntity.periodicAdvertisingParametersId?.let { paramId ->
                database.periodicAdvertisingParametersDao().findById(paramId)?.let { entity ->
                    advertisementSet.periodicAdvertisingParameters = PeriodicAdvertisingParameters().apply {
                        this.id = entity.id
                        includeTxPowerLevel = entity.includeTxPowerLevel
                        interval = entity.interval
                    }
                }
            }

            return advertisementSet
        }

        fun getAdvertiseDataFromEntity(advertiseDataEntity: AdvertiseDataEntity, database: AppDatabase):AdvertiseData{
            val advertiseData = AdvertiseData()

            advertiseData.id = advertiseDataEntity.id
            advertiseData.includeDeviceName = advertiseDataEntity.includeDeviceName
            advertiseData.includeTxPower = advertiseDataEntity.includeTxPower

            val manufacturerSpecificDataEntities = database.advertiseDataManufacturerSpecificDataDao().findByAdvertiseDataId(advertiseDataEntity.id)
            manufacturerSpecificDataEntities.forEach{ manufacturerSpecificDataEntity ->
                val manufacturerSpecificData = ManufacturerSpecificData()
                manufacturerSpecificData.id = manufacturerSpecificDataEntity.id
                manufacturerSpecificData.manufacturerId = manufacturerSpecificDataEntity.manufacturerId
                manufacturerSpecificData.manufacturerSpecificData = StringHelpers.decodeHex(manufacturerSpecificDataEntity.manufacturerSpecificData)

                advertiseData.manufacturerData.add(manufacturerSpecificData)
            }

            val advertiseDataServiceDataEntities = database.advertiseDataServiceDataDao().findByAdvertiseDataId(advertiseDataEntity.id)
            advertiseDataServiceDataEntities.forEach{ advertiseDataServiceDataEntity ->
                val serviceData = ServiceData()

                serviceData.id = advertiseDataServiceDataEntity.id
                serviceData.serviceUuid = ParcelUuid.fromString(advertiseDataServiceDataEntity.serviceUuid.toString())
                if(advertiseDataServiceDataEntity.serviceData != null){
                    serviceData.serviceData = StringHelpers.decodeHex(advertiseDataServiceDataEntity.serviceData!!)
                }

                advertiseData.services.add(serviceData)
            }

            return advertiseData
        }

        fun getAllAdvertisementSetsForTarget(advertisementTarget: AdvertisementTarget):List<AdvertisementSet>{
            val database = AppDatabase.getInstance()
            val advertisementSetEntities = database.advertisementSetDao().findByTarget(advertisementTarget)
            return getAdvertisementSetListFromEntities(advertisementSetEntities)
        }

        fun getAllAdvertisementSetsForType(advertisementSetType: AdvertisementSetType):List<AdvertisementSet>{
            val database = AppDatabase.getInstance()
            val advertisementSetEntities = database.advertisementSetDao().findByType(advertisementSetType)
            return getAdvertisementSetListFromEntities(advertisementSetEntities)
        }

        fun getAdvertisementSetListFromEntities(entities: List<AdvertisementSetEntity>):List<AdvertisementSet>{
            val advertisementSets = mutableListOf<AdvertisementSet>()

            entities.forEach { entitiy ->
                val advertisementSet = getAdvertisementSetFromEntity(entitiy)
                advertisementSets.add(advertisementSet)
            }

            return advertisementSets.toList()
        }
    }
}