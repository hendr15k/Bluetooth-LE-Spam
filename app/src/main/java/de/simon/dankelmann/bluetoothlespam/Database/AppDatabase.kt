package de.simon.dankelmann.bluetoothlespam.Database

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityActionModalAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityIos17CrashAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityNewAirtagPopUpAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityNewDevicePopUpAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityNotYourDevicePopUpAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.EasySetupBudsAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.EasySetupWatchAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.FastPairDevicesAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.FastPairDebugAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.FastPairNonProductionAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.FastPairPhoneSetupAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.LovespousePlayAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.LovespouseStopAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.SwiftPairAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Database.Dao.AdvertiseDataDao
import de.simon.dankelmann.bluetoothlespam.Database.Dao.AdvertiseDataManufacturerSpecificDataDao
import de.simon.dankelmann.bluetoothlespam.Database.Dao.AdvertiseDataServiceDataDao
import de.simon.dankelmann.bluetoothlespam.Database.Dao.AdvertiseSettingsDao
import de.simon.dankelmann.bluetoothlespam.Database.Dao.AdvertisementSetCollectionDao
import de.simon.dankelmann.bluetoothlespam.Database.Dao.AdvertisementSetDao
import de.simon.dankelmann.bluetoothlespam.Database.Dao.AdvertisementSetListDao
import de.simon.dankelmann.bluetoothlespam.Database.Dao.AdvertisingSetParametersDao
import de.simon.dankelmann.bluetoothlespam.Database.Dao.AssociationCollectionListDao
import de.simon.dankelmann.bluetoothlespam.Database.Dao.AssociationListSetDao
import de.simon.dankelmann.bluetoothlespam.Database.Dao.PeriodicAdvertisingParametersDao
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseDataEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseDataManufacturerSpecificDataEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseDataServiceDataEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseSettingsEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetCollectionEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetListEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisingSetParametersEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AssociatonCollectionListEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AssociationListSetEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.PeriodicAdvertisingParametersEntity
import de.simon.dankelmann.bluetoothlespam.Database.Migrations.Migration_1_2
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers.Companion.toHexString
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import java.util.concurrent.atomic.AtomicBoolean

@androidx.room.Database(
    entities = [AdvertiseDataEntity::class,
                AdvertiseDataManufacturerSpecificDataEntity::class,
                AdvertiseDataServiceDataEntity::class,
                AdvertisementSetCollectionEntity::class,
                AdvertisementSetEntity::class,
                AdvertisementSetListEntity::class,
                AdvertiseSettingsEntity::class,
                AdvertisingSetParametersEntity::class,
                AssociatonCollectionListEntity::class,
                AssociationListSetEntity::class,
                PeriodicAdvertisingParametersEntity::class],
    version = 2,
    exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    @Volatile
    var isSeeding = false
    abstract fun advertiseDataDao(): AdvertiseDataDao
    abstract fun advertiseDataManufacturerSpecificDataDao(): AdvertiseDataManufacturerSpecificDataDao
    abstract fun advertiseDataServiceDataDao(): AdvertiseDataServiceDataDao

    abstract fun advertisementSetCollectionDao(): AdvertisementSetCollectionDao

    abstract fun advertisementSetDao(): AdvertisementSetDao

    abstract fun advertisementSetListDao(): AdvertisementSetListDao

    abstract fun advertiseSettingsDao(): AdvertiseSettingsDao

    abstract fun advertisingSetParametersDao(): AdvertisingSetParametersDao

    abstract fun associationCollectionListDao(): AssociationCollectionListDao

    abstract fun associationListSetDao(): AssociationListSetDao

    abstract fun periodicAdvertisingParametersDao(): PeriodicAdvertisingParametersDao


    companion object {
        private const val _logTag = "AppDatabase"
        private const val CATALOG_PREFS = "database_catalog"
        private const val CATALOG_VERSION_KEY = "catalog_version"
        private const val CATALOG_VERSION = 4
        private var INSTANCE: AppDatabase? = null
        private val catalogSyncStarted = AtomicBoolean(false)
        private val fastPairTypes = setOf(
            AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEVICE,
            AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_NON_PRODUCTION,
            AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_PHONE_SETUP,
            AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEBUG,
        )

        fun getInstance(): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(AppContext.getContext()).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "BluetoothLeSpamDatabase.db")
                .addCallback(seedDatabaseCallback(context))
                .addMigrations(Migration_1_2)
                //.fallbackToDestructiveMigration()
                .build()

        private fun catalogKey(advertisementSet: AdvertisementSet): String {
            val base = "${advertisementSet.target}|${advertisementSet.type}|${advertisementSet.title}"
            if (advertisementSet.type !in fastPairTypes) {
                return base
            }

            val serviceData = advertisementSet.advertiseData.services
                .map { service ->
                    "${service.serviceUuid}:${service.serviceData?.toHexString().orEmpty()}"
                }
                .sorted()
                .joinToString(",")
            return "$base|$serviceData"
        }

        private fun catalogKey(
            advertisementSet: AdvertisementSetEntity,
            serviceDataByAdvertiseDataId: Map<Int, List<AdvertiseDataServiceDataEntity>>,
        ): String {
            val base = "${advertisementSet.target}|${advertisementSet.type}|${advertisementSet.title}"
            if (advertisementSet.type !in fastPairTypes) {
                return base
            }

            val serviceData = serviceDataByAdvertiseDataId[advertisementSet.advertiseDataId]
                .orEmpty()
                .map { service -> "${service.serviceUuid}:${service.serviceData.orEmpty()}" }
                .sorted()
                .joinToString(",")
            return "$base|$serviceData"
        }

        private fun seedDatabaseCallback(context: Context): Callback {
            return object : Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    syncCatalog(context.applicationContext)
                }
            }
        }

        private fun syncCatalog(context: Context) {
            if (!catalogSyncStarted.compareAndSet(false, true)) {
                return
            }

            Thread({
                val database = getInstance()
                database.isSeeding = true
                Log.d(_logTag, "Starting database catalog sync")

                try {
                    val preferences = context.getSharedPreferences(CATALOG_PREFS, Context.MODE_PRIVATE)
                    val catalogVersion = preferences.getInt(CATALOG_VERSION_KEY, 0)
                    if (
                        catalogVersion >= CATALOG_VERSION &&
                        database.advertisementSetDao().getAll().isNotEmpty()
                    ) {
                        return@Thread
                    }

                    val generatedAdvertisementSets = listOf(
                        FastPairDevicesAdvertisementSetGenerator(),
                        FastPairPhoneSetupAdvertisementSetGenerator(),
                        FastPairNonProductionAdvertisementSetGenerator(),
                        FastPairDebugAdvertisementSetGenerator(),
                        ContinuityNotYourDevicePopUpAdvertisementSetGenerator(),
                        ContinuityNewDevicePopUpAdvertisementSetGenerator(),
                        ContinuityNewAirtagPopUpAdvertisementSetGenerator(),
                        ContinuityActionModalAdvertisementSetGenerator(),
                        ContinuityIos17CrashAdvertisementSetGenerator(),
                        SwiftPairAdvertisementSetGenerator(),
                        EasySetupWatchAdvertisementSetGenerator(),
                        EasySetupBudsAdvertisementSetGenerator(),
                        LovespousePlayAdvertisementSetGenerator(),
                        LovespouseStopAdvertisementSetGenerator(),
                    ).flatMap { generator -> generator.getAdvertisementSets(null) }

                    database.runInTransaction {
                        val serviceDataByAdvertiseDataId = database.advertiseDataServiceDataDao()
                            .getAll()
                            .groupBy { serviceData -> serviceData.advertiseDataId }
                        val existingSets = database.advertisementSetDao().getAll()
                            .associateByTo(mutableMapOf()) { entity ->
                                catalogKey(entity, serviceDataByAdvertiseDataId)
                            }

                        generatedAdvertisementSets.forEach { advertisementSet ->
                            val key = catalogKey(advertisementSet)
                            val existingSet = existingSets[key]
                            if (existingSet == null) {
                                DatabaseHelpers.saveAdvertisementSet(advertisementSet)
                            } else if (advertisementSet.type == AdvertisementSetType.ADVERTISEMENT_TYPE_SWIFT_PAIRING) {
                                advertisementSet.advertiseData.manufacturerData.firstOrNull()?.let { data ->
                                    database.advertiseDataManufacturerSpecificDataDao()
                                        .updateManufacturerData(
                                            existingSet.advertiseDataId,
                                            data.manufacturerId,
                                            data.manufacturerSpecificData.toHexString(),
                                        )
                                }
                            }
                        }
                    }

                    preferences.edit()
                        .putInt(CATALOG_VERSION_KEY, CATALOG_VERSION)
                        .apply()
                } finally {
                    database.isSeeding = false
                    Log.d(_logTag, "Database catalog sync finished")
                }
            }, "database-catalog-sync").start()
        }
    }
}
