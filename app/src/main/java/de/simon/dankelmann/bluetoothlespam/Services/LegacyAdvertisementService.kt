package de.simon.dankelmann.bluetoothlespam.Services

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementError
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothHelpers.Companion.bluetoothAdapter
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IAdvertisementService
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck

class LegacyAdvertisementService(
    private val context: Context,
): IAdvertisementService {

    // private
    private val _logTag = "AdvertisementService"
    private var _advertiser: BluetoothLeAdvertiser? = null
    private var _advertisementServiceCallbacks:MutableList<IAdvertisementServiceCallback> = mutableListOf()
    private var _currentAdvertisementSet: AdvertisementSet? = null
    private var _txPowerLevel:TxPowerLevel? = null

    override fun startAdvertisement(advertisementSet:AdvertisementSet){
        if (!advertisementSet.validate()) {
            Log.w(_logTag, "Advertisement Set could not be validated - data too large")
            dispatchFailed(advertisementSet, AdvertisementError.ADVERTISE_FAILED_DATA_TOO_LARGE)
            return
        }
        if (!PermissionCheck.hasAdvertisePermission(context)) {
            Log.d(_logTag, "Missing permission to execute advertisement")
            dispatchFailed(advertisementSet, AdvertisementError.ADVERTISE_FAILED_UNKNOWN)
            return
        }

        val preparedAdvertisementSet = prepareAdvertisementSet(advertisementSet)
        _currentAdvertisementSet = preparedAdvertisementSet

        try {
            val advertiser = context.bluetoothAdapter()?.bluetoothLeAdvertiser
            if (advertiser == null) {
                Log.d(_logTag, "Advertiser is null")
                _currentAdvertisementSet = null
                dispatchFailed(advertisementSet, AdvertisementError.ADVERTISE_FAILED_FEATURE_UNSUPPORTED)
                return
            }

            _advertiser = advertiser
            if (preparedAdvertisementSet.scanResponse != null) {
                advertiser.startAdvertising(
                    preparedAdvertisementSet.advertiseSettings.build(),
                    preparedAdvertisementSet.advertiseData.build(),
                    preparedAdvertisementSet.scanResponse!!.build(),
                    preparedAdvertisementSet.advertisingCallback
                )
            } else {
                advertiser.startAdvertising(
                    preparedAdvertisementSet.advertiseSettings.build(),
                    preparedAdvertisementSet.advertiseData.build(),
                    preparedAdvertisementSet.advertisingCallback
                )
            }
            Log.d(_logTag, "Started Legacy Advertisement")
        } catch (error: SecurityException) {
            Log.e(_logTag, "Unable to start advertisement", error)
            _currentAdvertisementSet = null
            dispatchFailed(advertisementSet, AdvertisementError.ADVERTISE_FAILED_UNKNOWN)
        } catch (error: IllegalStateException) {
            Log.e(_logTag, "Unable to start advertisement", error)
            _currentAdvertisementSet = null
            dispatchFailed(advertisementSet, AdvertisementError.ADVERTISE_FAILED_INTERNAL_ERROR)
        }
    }

    override fun stopAdvertisement(){
        val currentAdvertisementSet = _currentAdvertisementSet
        if (currentAdvertisementSet == null) {
            Log.d(_logTag, "Current Legacy Advertising Set is null")
            return
        }

        try {
            if (PermissionCheck.hasAdvertisePermission(context)) {
                _advertiser?.stopAdvertising(currentAdvertisementSet.advertisingCallback)
            } else {
                Log.d(_logTag, "Missing permission to stop advertisement")
            }
        } catch (error: SecurityException) {
            Log.e(_logTag, "Unable to stop advertisement", error)
        } catch (error: IllegalStateException) {
            Log.e(_logTag, "Unable to stop advertisement", error)
        } finally {
            if (_currentAdvertisementSet === currentAdvertisementSet) {
                _currentAdvertisementSet = null
            }
            _advertisementServiceCallbacks.toList().forEach {
                it.onAdvertisementSetStop(currentAdvertisementSet)
            }
        }
    }

    override fun setTxPowerLevel(txPowerLevel:TxPowerLevel){
        _txPowerLevel = txPowerLevel
        Log.d(_logTag, "Setting TX POWER")
    }

    override fun getTxPowerLevel(): TxPowerLevel{
        if(_txPowerLevel != null){
            return _txPowerLevel!!
        }
        return TxPowerLevel.TX_POWER_HIGH
    }

    fun prepareAdvertisementSet(advertisementSet: AdvertisementSet):AdvertisementSet{
        if(_txPowerLevel != null){
            advertisementSet.advertiseSettings.txPowerLevel = _txPowerLevel!!
            advertisementSet.advertisingSetParameters.txPowerLevel = _txPowerLevel!!
        }
        advertisementSet.advertisingCallback = getAdvertisingCallback(advertisementSet)
        return advertisementSet
    }

    override fun addAdvertisementServiceCallback(callback: IAdvertisementServiceCallback){
        if(!_advertisementServiceCallbacks.contains(callback)){
            _advertisementServiceCallbacks.add(callback)
        }
    }
    override fun removeAdvertisementServiceCallback(callback: IAdvertisementServiceCallback){
        if(_advertisementServiceCallbacks.contains(callback)){
            _advertisementServiceCallbacks.remove(callback)
        }
    }

    override fun isLegacyService(): Boolean {
        return true
    }

    private fun dispatchFailed(advertisementSet: AdvertisementSet?, error: AdvertisementError) {
        _advertisementServiceCallbacks.toList().forEach {
            it.onAdvertisementSetFailed(advertisementSet, error)
        }
    }

    private fun getAdvertisingCallback(advertisementSet: AdvertisementSet):AdvertiseCallback{
        return object : AdvertiseCallback() {

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)

                val advertisementError = when (errorCode) {
                    AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> AdvertisementError.ADVERTISE_FAILED_ALREADY_STARTED
                    AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> AdvertisementError.ADVERTISE_FAILED_FEATURE_UNSUPPORTED
                    AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> AdvertisementError.ADVERTISE_FAILED_INTERNAL_ERROR
                    AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> AdvertisementError.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS
                    AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> AdvertisementError.ADVERTISE_FAILED_DATA_TOO_LARGE
                    else -> AdvertisementError.ADVERTISE_FAILED_UNKNOWN
                }

                dispatchFailed(advertisementSet, advertisementError)
                if (_currentAdvertisementSet === advertisementSet) {
                    _currentAdvertisementSet = null
                }
            }

            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                _advertisementServiceCallbacks.toList().forEach {
                    it.onAdvertisementSetStart(advertisementSet)
                    it.onAdvertisementSetSucceeded(advertisementSet)
                }
            }
        }
    }
}
